package listing.pl.otodom

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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
        val street = item["street"]?.jsonPrimitive?.contentOrNull
        val city = item["city"]?.jsonPrimitive?.contentOrNull
        val address = listOfNotNull(street, city).joinToString(", ").ifBlank { city }
        val images = item["images"]?.jsonArray?.mapNotNull { img ->
            img.jsonObject["large"]?.jsonPrimitive?.contentOrNull
                ?: img.jsonObject["medium"]?.jsonPrimitive?.contentOrNull
        }
        val created = item["createdAtFirst"]?.jsonPrimitive?.contentOrNull
            ?: item["dateCreated"]?.jsonPrimitive?.contentOrNull
        val publishedAt = created?.let { parseInstant(it) }

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
            publishedAtUi = created,
            imageUrls = images,
            // PL: local currency stored in priceByn field until price model rename
            priceUsd = null,
            priceByn = pricePln,
            rooms = rooms,
            district = null,
            address = address,
            metroStation = null,
            description = item["title"]?.jsonPrimitive?.contentOrNull,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = null,
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
            owner = null,
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
