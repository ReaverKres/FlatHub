package listing.pl.gratka

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

object GratkaFlatMapper {
    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val nodes = root["data"]
            ?.jsonObject
            ?.get("searchResult")
            ?.jsonObject
            ?.get("properties")
            ?.jsonObject
            ?.get("nodes")
            ?.jsonArray
            ?: return emptyList()
        return nodes.mapNotNull { el ->
            runCatching { mapNode(el.jsonObject, adType) }.getOrNull()
        }
    }

    private fun mapNode(node: JsonObject, adType: AdType): AppFlat {
        val idOnFrontend = node["idOnFrontend"]?.jsonPrimitive?.contentOrNull
        val adId = idOnFrontend?.toLongOrNull()
            ?: node["id"]?.jsonPrimitive?.longOrNull
            ?: error("missing id")
        val path = node["url"]?.jsonPrimitive?.contentOrNull
        val detailUrl = when {
            path == null -> "https://gratka.pl"
            path.startsWith("http") -> path
            else -> "https://gratka.pl$path"
        }
        val pricePln = node["price"]?.jsonObject
            ?.get("amount")
            ?.jsonPrimitive
            ?.contentOrNull
            ?.toDoubleOrNull()
            ?: node["price"]?.jsonObject?.get("amount")?.jsonPrimitive?.doubleOrNull
        val rooms = parseRooms(node["numberOfRooms"]?.jsonPrimitive?.contentOrNull)
        val area = node["area"]?.jsonPrimitive?.contentOrNull?.replace(',', '.')?.toDoubleOrNull()
            ?: node["area"]?.jsonPrimitive?.doubleOrNull
        val floorParts = parseFloor(node["floorFormatted"]?.jsonPrimitive?.contentOrNull)
        val location = node["location"]?.jsonObject
        val locationParts = location?.get("location")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            .orEmpty()
        val city = locationParts.getOrNull(0)
        val district = locationParts.getOrNull(1)
        val street = location?.get("street")?.jsonPrimitive?.contentOrNull
        val address = listOfNotNull(street, district, city).joinToString(", ").ifBlank { city }
        val photos = node["photos"]?.jsonArray?.mapNotNull { photo ->
            decodePhotoUrl(photo.jsonObject["id"]?.jsonPrimitive?.contentOrNull)
        }
        val contact = node["contact"]?.jsonObject
        val person = contact?.get("person")?.jsonObject
        val company = contact?.get("company")?.jsonObject
        val ownerName = person?.get("name")?.jsonPrimitive?.contentOrNull
            ?: company?.get("name")?.jsonPrimitive?.contentOrNull
        val phones = (person?.get("phones") ?: company?.get("phones"))
            ?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
        val isOwner =
            person?.get("type")?.jsonPrimitive?.contentOrNull.equals("OWNER", ignoreCase = true)
        val created = node["addedAt"]?.jsonPrimitive?.contentOrNull
            ?: node["refreshedAt"]?.jsonPrimitive?.contentOrNull
        val publishedAt = created?.let { parseLooseInstant(it) }
        val title = node["title"]?.jsonPrimitive?.contentOrNull
        val description = node["description"]?.jsonPrimitive?.contentOrNull ?: title

        return AppFlat(
            adId = adId,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = phones, ownerName = ownerName),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.GRATKA,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = created,
            imageUrls = photos,
            priceUsd = null,
            priceByn = pricePln,
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
            raw.replace(' ', 'T')
                .let { if (it.endsWith('Z') || '+' in it || it.length == 10) "${it}T00:00:00Z" else it },
            if (raw.length == 10) "${raw}T00:00:00Z" else null,
        ).filterNotNull()
        for (c in candidates) {
            runCatching { return Instant.parse(c) }
        }
        return null
    }
}
