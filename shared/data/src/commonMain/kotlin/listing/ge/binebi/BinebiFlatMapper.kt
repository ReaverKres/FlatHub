package listing.ge.binebi

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.Json
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
 */
object BinebiFlatMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["data"].asArrayOrNull() ?: return emptyList()
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapItem(item, adType) }.getOrNull()
        }
    }

    private fun mapItem(item: JsonObject, adType: AdType): AppFlat {
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
        val lat = item["map_lat"].contentOrNull()?.toDoubleOrNull()
            ?: item["map_lat"].doubleOrNull()
        val lon = item["map_lon"].contentOrNull()?.toDoubleOrNull()
            ?: item["map_lon"].doubleOrNull()
        val coords = if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
            Coordinates(lat, lon)
        } else null
        val phone = item["mobile"].contentOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val created = item["created_at"].contentOrNull() ?: item["renew_date"].contentOrNull()
        val owner = item["is_owner"].intOrNull() == 1
        val detailUrl = "https://binebi.ge/gancxadebebi/$id"
        val district = item["district"].asObjectOrNull()?.get("title").contentOrNull()
            ?: item["district"].asObjectOrNull()?.get("name").contentOrNull()
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
            publishedAt = created?.let { parseInstant(it) },
            publishedAtServer = created,
            publishedAtUi = created,
            imageUrls = images,
            priceUsd = if (isUsd) price else null,
            priceByn = if (!isUsd) price else null,
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

    private fun parseInstant(raw: String): Instant? {
        val candidates = listOf(
            raw.trim(),
            raw.trim().replace(' ', 'T'),
            raw.trim().replace(' ', 'T').let { if (it.endsWith('Z')) it else "${it}Z" },
        )
        for (c in candidates) {
            runCatching { return Instant.parse(c) }
        }
        return null
    }

    private const val CURRENCY_USD = 1
}
