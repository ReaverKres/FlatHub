package listing.pl.olx

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Instant

object OlxPlFlatMapper {
    fun mapOffers(root: JsonObject, adType: AdType): List<AppFlat> {
        val data = root["data"]?.jsonArray ?: return emptyList()
        return data.mapNotNull { el ->
            runCatching { mapOffer(el.jsonObject, adType) }.getOrNull()
        }
    }

    private fun mapOffer(item: JsonObject, adType: AdType): AppFlat {
        val id = item["id"]?.jsonPrimitive?.longOrNull ?: error("missing id")
        val url = item["url"]?.jsonPrimitive?.contentOrNull
            ?: "https://www.olx.pl/d/oferta/ID$id"
        val title = item["title"]?.jsonPrimitive?.contentOrNull
        val description = item["description"]?.jsonPrimitive?.contentOrNull
        val created = item["created_time"]?.jsonPrimitive?.contentOrNull
            ?: item["last_refresh_time"]?.jsonPrimitive?.contentOrNull
        val publishedAt = created?.let { runCatching { Instant.parse(it) }.getOrNull() }

        val params = item["params"]?.jsonArray ?: JsonArray(emptyList())
        val pricePln = paramPrice(params)
        val rooms = paramRooms(params)
        val area = paramArea(params)
        val floor = paramFloor(params)

        val map = item["map"]?.jsonObject
        val lat = map?.get("lat")?.jsonPrimitive?.doubleOrNull
        val lon = map?.get("lon")?.jsonPrimitive?.doubleOrNull
        val coordinates = if (lat != null && lon != null) Coordinates(lat, lon) else null

        val location = item["location"]?.jsonObject
        val city = location?.get("city")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
        val district =
            location?.get("district")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
        val address = listOfNotNull(district, city).joinToString(", ").ifBlank { city }

        val photos = item["photos"]?.jsonArray?.mapNotNull { photo ->
            photo.jsonObject["link"]?.jsonPrimitive?.contentOrNull
                ?.replace("{width}x{height}", "800x600")
                ?.replace("{width}", "800")
                ?.replace("{height}", "600")
        }

        val isBusiness = item["business"]?.jsonPrimitive?.booleanOrNull == true
        val ownerName = item["contact"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            ?: item["user"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = ownerName),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.OLX_PL,
            flatDetailUrl = url,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = created,
            imageUrls = photos,
            priceUsd = null,
            // PL: local currency stored in priceByn until price model rename
            priceByn = pricePln,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = description ?: title,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = rooms == 1 && (title?.contains("Kawalerka", ignoreCase = true) == true),
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
            owner = !isBusiness,
        )
    }

    private fun paramPrice(params: JsonArray): Double? {
        val price = params.findParam("price") ?: return null
        return price["value"]?.jsonObject?.get("value")?.jsonPrimitive?.doubleOrNull
    }

    private fun paramRooms(params: JsonArray): Int? {
        val rooms = params.findParam("rooms") ?: return null
        val key = rooms["value"]?.jsonObject?.get("key")?.jsonPrimitive?.contentOrNull
            ?: return null
        return when (key.lowercase()) {
            "one" -> 1
            "two" -> 2
            "three" -> 3
            "four" -> 4
            else -> key.filter { it.isDigit() }.toIntOrNull()
        }
    }

    private fun paramArea(params: JsonArray): Double? {
        val m = params.findParam("m") ?: return null
        val key = m["value"]?.jsonObject?.get("key")?.jsonPrimitive?.contentOrNull
        return key?.replace(',', '.')?.toDoubleOrNull()
    }

    private fun paramFloor(params: JsonArray): Int? {
        val floor = params.findParam("floor_select") ?: return null
        val label = floor["value"]?.jsonObject?.get("label")?.jsonPrimitive?.contentOrNull
        return label?.filter { it.isDigit() }?.toIntOrNull()
    }

    private fun JsonArray.findParam(key: String): JsonObject? =
        firstOrNull { el ->
            el.jsonObject["key"]?.jsonPrimitive?.contentOrNull == key
        }?.jsonObject
}
