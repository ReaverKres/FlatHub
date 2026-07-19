package listing.ge.ss

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import entities.TbilisiMetroStations
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
import listing.core.intOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText
import kotlin.time.Instant

object SsFlatMapper {
    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["realStateItemModel"].asArrayOrNull() ?: return emptyList()
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapItem(item, adType) }.getOrNull()
        }
    }

    private fun mapItem(item: JsonObject, adType: AdType): AppFlat {
        val id = item["applicationId"].longOrNull() ?: error("missing applicationId")
        val detailSlug = item["detailUrl"].contentOrNull()
        val detailUrl = detailSlug?.let { "https://home.ss.ge/ka/udzravi-qoneba/l/$it" }
            ?: "https://home.ss.ge/ka/udzravi-qoneba/l/$id"
        val price = item["price"].asObjectOrNull()
        val priceGel = price?.get("priceGeo").doubleOrNull()
        val priceUsd = price?.get("priceUsd").doubleOrNull()
        val priceGelSq = price?.get("unitPriceGeo").doubleOrNull()
        val priceUsdSq = price?.get("unitPriceUsd").doubleOrNull()
        val addressObj = item["address"].asObjectOrNull()
        val street = listOfNotNull(
            addressObj?.get("streetTitle").contentOrNull(),
            addressObj?.get("streetNumber").contentOrNull(),
        ).joinToString(" ").ifBlank { null }
        val district = addressObj?.get("subdistrictTitle").contentOrNull()
            ?: addressObj?.get("districtTitle").contentOrNull()
        val city = addressObj?.get("cityTitle").contentOrNull()
        val address = listOfNotNull(street, district, city).joinToString(", ").ifBlank { city }
        val images = item["appImages"].asArrayOrNull()?.mapNotNull { img ->
            img.asObjectOrNull()?.get("fileName").contentOrNull()?.replace("_Thumb", "")
        }
        val metro = TbilisiMetroStations.canonicalizeStationTitle(
            item["nearbySubwayStations"].asArrayOrNull()
                ?.firstOrNull()
                ?.asObjectOrNull()
                ?.get("stationTitle")
                .contentOrNull()
        )
        val created = item["createDate"].contentOrNull()
            ?: item["orderDate"].contentOrNull()
        val publishedAt = created?.let { parseInstant(it) }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        }
        val rooms = item["numberOfBedrooms"].intOrNull()
        val floor = item["floorNumber"].contentOrNull()
            ?.let { Regex("(\\d+)").find(it)?.value?.toIntOrNull() }
            ?: item["floorNumber"].intOrNull()
        val title = item["title"].contentOrNull() ?: item["shortTitle"].contentOrNull()
        val description = item["description"].contentOrNull() ?: title
        val lat = item["locationLatitude"].doubleOrNull()
            ?: item["latitude"].doubleOrNull()
            ?: item["lat"].doubleOrNull()
        val lon = item["locationLongitude"].doubleOrNull()
            ?: item["longitude"].doubleOrNull()
            ?: item["lng"].doubleOrNull()
            ?: item["lon"].doubleOrNull()
        val coords = if (lat != null && lon != null && lat != 0.0 && lon != 0.0) {
            Coordinates(lat, lon)
        } else {
            null
        }

        // List payload is rich enough for cards; phones still deferred (CF / auth).
        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.SS_GE,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            mainPrice = priceGel,
            secondPrice = priceUsd,
            mainPriceSquare = priceGelSq,
            secondPriceSquare = priceUsdSq,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = metro,
            description = description.stripHtmlToPlainText(),
            yearBuilt = null,
            totalArea = item["totalArea"].doubleOrNull(),
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = item["totalAmountOfFloor"].intOrNull(),
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

    private fun parseInstant(raw: String): Instant? {
        val candidates = listOf(
            raw,
            raw.replace(' ', 'T'),
        )
        for (c in candidates) {
            runCatching { return Instant.parse(c) }
        }
        return null
    }
}
