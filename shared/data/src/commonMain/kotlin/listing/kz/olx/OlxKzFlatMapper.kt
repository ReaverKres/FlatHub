package listing.kz.olx

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import utils.stripHtmlToPlainText
import kotlin.time.Instant

object OlxKzFlatMapper {
    fun mapOffers(root: JsonObject, adType: AdType): List<AppFlat> {
        val data = root["data"]?.jsonArray ?: return emptyList()
        return data.mapNotNull { el ->
            runCatching { mapSingleOffer(el.jsonObject, adType) }.getOrNull()
        }
    }

    fun mapSingleOffer(item: JsonObject, adType: AdType): AppFlat {
        val id = item["id"]?.jsonPrimitive?.longOrNull ?: error("missing id")
        val url = item["url"]?.jsonPrimitive?.contentOrNull
            ?: "https://www.olx.kz/d/obyavlenie/ID$id"
        val title = item["title"]?.jsonPrimitive?.contentOrNull
        val description = item["description"]?.jsonPrimitive?.contentOrNull
        val created = item["created_time"]?.jsonPrimitive?.contentOrNull
            ?: item["last_refresh_time"]?.jsonPrimitive?.contentOrNull
        val publishedAt = created?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        }

        val params = item["params"]?.jsonArray ?: JsonArray(emptyList())
        val priceKzt = paramPrice(params)
        val rooms = paramRooms(params)
        val area = paramArea(params)
        val floor = paramFloor(params)
        val totalFloors = paramInt(params, "etazhnost_doma")
        val yearBuilt = paramInt(params, "godpostroiki")

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
            flatPlatform = FlatPlatform.OLX_KZ,
            flatDetailUrl = url,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAtUi,
            imageUrls = photos,
            priceUsd = null,
            priceByn = priceKzt,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = (description ?: title).stripHtmlToPlainText(),
            yearBuilt = yearBuilt,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = totalFloors,
            sleepingPlaces = null,
            isStudio = rooms == 0 ||
                    title?.contains("студи", ignoreCase = true) == true ||
                    paramLabel(params, "planirovka")?.contains("студи", ignoreCase = true) == true,
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
        val rooms = params.findParam("kolichestvokomnat") ?: return null
        val value = rooms["value"]?.jsonObject ?: return null
        value["key"]?.let { keyEl ->
            keyEl.asIntOrFirstArrayInt()?.let { return it }
        }
        return value["label"]?.jsonPrimitive?.contentOrNull
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
    }

    private fun paramArea(params: JsonArray): Double? {
        val m = params.findParam("obshayaploshad") ?: return null
        val key = m["value"]?.jsonObject?.get("key")?.jsonPrimitive?.contentOrNull
        return key?.replace(',', '.')?.toDoubleOrNull()
    }

    private fun paramFloor(params: JsonArray): Int? = paramInt(params, "etazh")

    private fun paramInt(params: JsonArray, key: String): Int? {
        val p = params.findParam(key) ?: return null
        val value = p["value"]?.jsonObject ?: return null
        value["key"]?.asIntOrFirstArrayInt()?.let { return it }
        return value["label"]?.jsonPrimitive?.contentOrNull
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
    }

    private fun paramLabel(params: JsonArray, key: String): String? =
        params.findParam(key)
            ?.get("value")
            ?.jsonObject
            ?.get("label")
            ?.jsonPrimitive
            ?.contentOrNull

    private fun JsonElement.asIntOrFirstArrayInt(): Int? {
        jsonPrimitive.intOrNull?.let { return it }
        jsonPrimitive.contentOrNull?.filter { it.isDigit() }?.toIntOrNull()?.let { return it }
        return runCatching {
            jsonArray.firstOrNull()?.jsonPrimitive?.contentOrNull
                ?.filter { it.isDigit() }
                ?.toIntOrNull()
        }.getOrNull()
    }

    private fun JsonArray.findParam(key: String): JsonObject? =
        firstOrNull { el ->
            el.jsonObject["key"]?.jsonPrimitive?.contentOrNull == key
        }?.jsonObject
}
