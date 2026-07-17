package listing.pl.otodom

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
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

object OtodomFlatMapper {
    fun mapSearchAds(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["pageProps"]
            ?.jsonObject
            ?.get("data")
            ?.jsonObject
            ?.get("searchAds")
            ?.jsonObject
            ?.get("items")
            ?.jsonArray
            ?: return emptyList()
        return items.mapNotNull { el ->
            runCatching { mapItem(el.jsonObject, adType) }.getOrNull()
        }
    }

    private fun mapItem(item: JsonObject, adType: AdType): AppFlat {
        val id = item["id"]?.jsonPrimitive?.longOrNull
            ?: error("missing id")
        val slug = item["slug"]?.jsonPrimitive?.contentOrNull
        val detailUrl = slug?.let { "https://www.otodom.pl/pl/oferta/$it" }
            ?: "https://www.otodom.pl/pl/oferta/$id"
        val pricePln = item["totalPrice"]?.jsonObject
            ?.get("value")
            ?.jsonPrimitive
            ?.doubleOrNull
        val rooms = roomsNumber(item["roomsNumber"]?.jsonPrimitive?.contentOrNull)
        val area = item["areaInSquareMeters"]?.jsonPrimitive?.doubleOrNull
            ?: item["areaInSquareMeters"]?.jsonPrimitive?.intOrNull?.toDouble()
        val location = item["location"]?.jsonObject
        val street = location?.get("address")?.jsonObject
            ?.get("street")?.jsonObject
            ?.get("name")?.jsonPrimitive?.contentOrNull
            ?: item["street"]?.jsonPrimitive?.contentOrNull
        val city = location?.get("address")?.jsonObject
            ?.get("city")?.jsonObject
            ?.get("name")?.jsonPrimitive?.contentOrNull
            ?: item["city"]?.jsonPrimitive?.contentOrNull
        val district = location
            ?.get("reverseGeocoding")?.jsonObject
            ?.get("locations")?.jsonArray
            ?.mapNotNull { it.jsonObject }
            ?.firstOrNull { it["locationLevel"]?.jsonPrimitive?.contentOrNull == "district" }
            ?.get("name")?.jsonPrimitive?.contentOrNull
        val address = listOfNotNull(street, district, city).joinToString(", ").ifBlank { city }
        val floor = parseFloorToken(item["floorNumber"]?.jsonPrimitive?.contentOrNull)
        val images = item["images"]?.jsonArray?.mapNotNull { img ->
            img.jsonObject["large"]?.jsonPrimitive?.contentOrNull
                ?: img.jsonObject["medium"]?.jsonPrimitive?.contentOrNull
        }
        val created = item["createdAtFirst"]?.jsonPrimitive?.contentOrNull
            ?: item["dateCreated"]?.jsonPrimitive?.contentOrNull
        val publishedAt = created?.let { parseInstant(it) }
        val isPrivateOwner = item["isPrivateOwner"]?.jsonPrimitive?.booleanOrNull
        val title = item["title"]?.jsonPrimitive?.contentOrNull
        val shortDescription = item["shortDescription"]?.jsonPrimitive?.contentOrNull

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.OTODOM,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAt?.let {
                DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
            },
            imageUrls = images,
            // PL: local currency stored in priceByn field until price model rename
            priceUsd = null,
            priceByn = pricePln,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = (shortDescription ?: title).stripHtmlToPlainText(),
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = null,
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
            owner = isPrivateOwner,
        )
    }

    private fun roomsNumber(raw: String?): Int? = when (raw?.uppercase()) {
        "ONE" -> 1
        "TWO" -> 2
        "THREE" -> 3
        "FOUR" -> 4
        "FIVE" -> 5
        "SIX_OR_MORE" -> 6
        else -> null
    }

    private fun parseFloorToken(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        Regex("(\\d+)").find(raw)?.value?.toIntOrNull()?.let { return it }
        return when (raw.uppercase()) {
            "GROUND", "GROUND_FLOOR", "PARTER" -> 0
            "FIRST" -> 1
            "SECOND" -> 2
            "THIRD" -> 3
            "FOURTH" -> 4
            "FIFTH" -> 5
            "SIXTH" -> 6
            "SEVENTH" -> 7
            "EIGHTH" -> 8
            "NINTH" -> 9
            "TENTH" -> 10
            else -> null
        }
    }

    private fun parseInstant(raw: String): Instant? {
        val candidates = listOf(
            raw,
            raw.replace(' ', 'T').let { if (it.endsWith('Z') || '+' in it) it else "${it}Z" },
        )
        for (c in candidates) {
            runCatching { return Instant.parse(c) }
        }
        return null
    }
}
