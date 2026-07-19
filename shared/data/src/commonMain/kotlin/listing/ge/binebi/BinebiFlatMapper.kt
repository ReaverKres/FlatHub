package listing.ge.binebi

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText
import kotlin.time.Instant

/**
 * Maps Binebi POST search JSON (`data[]`) → [AppFlat].
 * Currency: 1 ≈ USD, 3 ≈ GEL (observed on Tbilisi rent).
 *
 * Coords: `map_lat`/`map_lon` are often empty; fall back to district/city
 * `coordinates` strings in `lon;lat` form.
 */
object BinebiFlatMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["data"].asArrayOrNull() ?: return emptyList()
        val pathById = parseDetailPathById(root["html"].contentOrNull())
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapItem(item, adType, pathById) }.getOrNull()
        }
    }

    private fun mapItem(
        item: JsonObject,
        adType: AdType,
        pathById: Map<Long, String>,
    ): AppFlat {
        val id = item["id"].longOrNull() ?: error("missing id")
        val price = item["price"].doubleOrNull()
        val currency = item["price_currency"].intOrNull()
        val isUsd = currency == CURRENCY_USD
        val title = localized(item["title"].contentOrNull())
        val comment = localized(item["comment"].contentOrNull())
        val address = item["address"].contentOrNull()
        val rooms = item["rooms"].intOrNull() ?: item["bedrooms"].intOrNull()
        val area = item["total_square"].doubleOrNull()?.takeIf { it > 0 }
        val floor = item["floor_current"].contentOrNull()?.toIntOrNull()
            ?: item["floor_current"].intOrNull()
        val totalFloors = item["floor_total"].contentOrNull()?.toIntOrNull()
            ?: item["floor_total"].intOrNull()
        val coords = parseCoordinates(item)
        val phone = item["mobile"].contentOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val created = item["created_at"].contentOrNull() ?: item["renew_date"].contentOrNull()
        val publishedAt = created?.let { parseInstant(it) }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        }
        val owner = item["is_owner"].intOrNull() == 1
        val detailPath = pathById[id] ?: "gancxadebebi/$id"
        val detailUrl = "https://binebi.ge/$detailPath"
        val districtObj = firstDistrict(item)
        val district = districtObj?.get("title").contentOrNull()
            ?: districtObj?.get("name").contentOrNull()
            ?: districtObj?.get("slug").contentOrNull()
            ?: item["district"].contentOrNull()
        val images = item["images"].asArrayOrNull()?.mapNotNull { img ->
            val o = img.asObjectOrNull() ?: return@mapNotNull null
            val filename = o["filename"].contentOrNull() ?: return@mapNotNull null
            "https://cdn.binebi.ge/uploads/homes/$id/thumbnail/$filename"
        }?.ifEmpty { null }

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(
                phones = phone?.let { listOf(it) },
                ownerName = null,
            ),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.BINEBI,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            mainPrice = price,
            secondPrice = null,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = (comment ?: title).stripHtmlToPlainText(),
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = totalFloors,
            sleepingPlaces = null,
            isStudio = rooms == 0,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = null,
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            amenities = null,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = owner,
        )
    }

    private fun localized(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return trimmed
        return runCatching {
            val obj = json.parseToJsonElement(trimmed).asObjectOrNull()
            obj?.get("ka").contentOrNull()
                ?: obj?.get("ru").contentOrNull()
                ?: obj?.get("en").contentOrNull()
        }.getOrNull() ?: trimmed
    }

    private fun parseDetailPathById(html: String?): Map<Long, String> {
        if (html.isNullOrBlank()) return emptyMap()
        return DETAIL_HREF_RE.findAll(html).mapNotNull { m ->
            val id = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val slug = m.groupValues[2]
            id to "gancxadebebi/$id-$slug"
        }.toMap()
    }

    private fun firstDistrict(item: JsonObject): JsonObject? {
        item["district"].asArrayOrNull()?.firstOrNull()?.asObjectOrNull()?.let { return it }
        return item["district"].asObjectOrNull()
    }

    private fun parseCoordinates(item: JsonObject): Coordinates? {
        val lat = numberOrNull(item["map_lat"])
        val lon = numberOrNull(item["map_lon"])
        if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
            return Coordinates(lat, lon)
        }
        parseLonLatPair(item["map_coords"].contentOrNull())?.let { return it }
        parseLonLatPair(firstDistrict(item)?.get("coordinates").contentOrNull())?.let { return it }
        parseLonLatPair(item["city"].asObjectOrNull()?.get("coordinates").contentOrNull())?.let {
            return it
        }
        return null
    }

    /** Binebi stores location as `longitude;latitude`. */
    private fun parseLonLatPair(raw: String?): Coordinates? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(';', ',').mapNotNull { it.trim().toDoubleOrNull() }
        if (parts.size < 2) return null
        val longitude = parts[0]
        val latitude = parts[1]
        if (latitude == 0.0 && longitude == 0.0) return null
        return Coordinates(latitude, longitude)
    }

    private fun numberOrNull(el: JsonElement?): Double? {
        val fromText = el.contentOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
        if (fromText != null) return fromText
        return el.doubleOrNull()
    }

    private fun parseInstant(raw: String): Instant? {
        val trimmed = raw.trim()
        val candidates = listOf(
            trimmed,
            trimmed.replace(' ', 'T'),
            trimmed.replace(' ', 'T').let { if (it.endsWith('Z')) it else "${it}Z" },
            // "2026-07-17 03:10:20" → assume UTC if no zone
            trimmed.replace(' ', 'T').let { t ->
                if (t.contains('+') || t.endsWith('Z')) t else "${t}Z"
            },
        )
        for (c in candidates) {
            runCatching { return Instant.parse(c) }
        }
        return null
    }

    private val DETAIL_HREF_RE =
        Regex("""gancxadebebi/(\d+)-([^"'?\s]+)""", RegexOption.IGNORE_CASE)

    private const val CURRENCY_USD = 1
}
