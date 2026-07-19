package listing.pl.morizon

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

object MorizonFlatMapper {
    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val nodes = root["data"].asObjectOrNull()
            ?.get("searchResult").asObjectOrNull()
            ?.get("properties").asObjectOrNull()
            ?.get("nodes").asArrayOrNull()
            ?: return emptyList()
        return nodes.mapNotNull { el ->
            val node = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapNode(node, adType) }.getOrNull()
        }
    }

    private fun mapNode(node: JsonObject, adType: AdType): AppFlat {
        val adId = node["id"].longOrNull()
            ?: node["idOnFrontend"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val path = node["url"].contentOrNull()
        val detailUrl = when {
            path == null -> "https://www.morizon.pl"
            path.startsWith("http") -> path
            else -> "https://www.morizon.pl$path"
        }
        val priceObj = node["price"].asObjectOrNull()
        val pricePln = priceObj?.get("amount").doubleOrNull()
        val rooms = parseRooms(node["numberOfRooms"].contentOrNull())
        val area = node["area"].doubleOrNull()
        val floorParts = parseFloor(node["floorFormatted"].contentOrNull())
        val location = node["location"].asObjectOrNull()
        val locationParts = location?.get("location").asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
            .orEmpty()
        val city = locationParts.getOrNull(0)
        val district = locationParts.getOrNull(1)
        val street = location?.get("street").contentOrNull()
        val address = listOfNotNull(street, district, city).joinToString(", ").ifBlank { city }
        val mapCenter = location?.get("map").asObjectOrNull()
            ?.get("center").asObjectOrNull()
        val lat = mapCenter?.get("latitude").doubleOrNull()
        val lon = mapCenter?.get("longitude").doubleOrNull()
        val coordinates = if (lat != null && lon != null) Coordinates(lat, lon) else null
        val photos = node["photos"].asArrayOrNull()?.mapNotNull { photo ->
            decodePhotoUrl(photo.asObjectOrNull()?.get("id").contentOrNull())
        }
        val contact = node["contact"].asObjectOrNull()
        val person = contact?.get("person").asObjectOrNull()
        val company = contact?.get("company").asObjectOrNull()
        val ownerName = person?.get("name").contentOrNull()
            ?: company?.get("name").contentOrNull()
        val phones = (person?.get("phones") ?: company?.get("phones"))
            .asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
        val isOwner =
            person?.get("type").contentOrNull().equals("OWNER", ignoreCase = true)
        val created = node["addedAt"].contentOrNull()
            ?: node["refreshedAt"].contentOrNull()
        val publishedAt = created?.let { parseLooseInstant(it) }
        val title = node["title"].contentOrNull()
        val description = (node["description"].contentOrNull() ?: title).stripHtmlToPlainText()

        return AppFlat(
            adId = adId,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = phones, ownerName = ownerName),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.MORIZON,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAt?.let {
                DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
            },
            imageUrls = photos,
            secondPrice = null,
            mainPrice = pricePln,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floorParts.first,
            totalFloors = floorParts.second,
            sleepingPlaces = null,
            isStudio = rooms == 1 && (title?.contains("kawalerka", ignoreCase = true) == true),
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
            owner = isOwner,
        )
    }

    private fun parseRooms(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        return raw.filter { it.isDigit() }.toIntOrNull()
    }

    private fun parseFloor(raw: String?): Pair<Int?, Int?> {
        if (raw.isNullOrBlank()) return null to null
        val digits = Regex("(\\d+)").findAll(raw).map { it.value.toInt() }.toList()
        return digits.getOrNull(0) to digits.getOrNull(1)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodePhotoUrl(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            Base64.decode(encoded).decodeToString()
                .replace("http://", "https://")
        }.getOrNull()
    }

    private fun parseLooseInstant(raw: String): Instant? {
        val candidates = listOf(
            raw,
            if (raw.length == 10) "${raw}T00:00:00Z" else null,
            raw.replace(' ', 'T').let { if (it.endsWith('Z') || '+' in it) it else "${it}Z" },
        ).filterNotNull()
        for (c in candidates) {
            runCatching { return Instant.parse(c) }
        }
        return null
    }
}
