package listing.ae.dubizzle

import entities.AppFlat
import entities.CommercialInfo
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import listing.ae.AeCommercialTypes
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText
import kotlin.time.Instant

/**
 * Maps Dubizzle Algolia hits → [AppFlat]. AED → [AppFlat.priceByn].
 * `_geoloc` axes are often lng/lat-swapped for UAE — normalized here.
 * See tmp/ae/api/dubizzle/NOTES.md.
 */
object DubizzleMapper {
    private const val SQFT_TO_M2 = 0.092903

    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val hits = root["hits"].asArrayOrNull() ?: return emptyList()
        return hits.mapNotNull { el ->
            val hit = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapHit(hit, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mapHit(hit: JsonObject, adType: AdType): AppFlat {
        val id = hit["id"].longOrNull()
            ?: hit["id"].contentOrNull()?.toLongOrNull()
            ?: hit["objectID"].contentOrNull()?.substringAfterLast(':')?.toLongOrNull()
            ?: error("missing id")
        val price = hit["price"].doubleOrNull()
        val bedrooms = hit["bedrooms"].intOrNull()
            ?: hit["bedrooms"].doubleOrNull()?.toInt()
        val isStudio = bedrooms == 0
        val rooms = bedrooms
        val sizeSqft = hit["size"].doubleOrNull()
        val totalArea = sizeSqft?.times(SQFT_TO_M2)
        val neighborhoods = hit["neighborhoods"].asObjectOrNull()
            ?.get("name").asObjectOrNull()
            ?.get("en").asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
            .orEmpty()
        val district = neighborhoods.firstOrNull()
        val building = hit["building"].asObjectOrNull()
            ?.get("name").asObjectOrNull()
            ?.get("en").contentOrNull()
        val address = listOfNotNull(building, neighborhoods.joinToString(", ").ifBlank { null })
            .joinToString(", ").ifBlank { null }
        val coords = parseCoords(hit)
        val photos = hit["photos"].asArrayOrNull()?.mapNotNull { p ->
            p.asObjectOrNull()?.get("main").contentOrNull()
        }.orEmpty()
        val photoMain = hit["photo"].asObjectOrNull()?.get("main").contentOrNull()
        val images = (listOfNotNull(photoMain) + photos).distinct().take(20).ifEmpty { null }
        val addedSec = hit["added"].longOrNull() ?: hit["added"].doubleOrNull()?.toLong()
        val publishedAt = addedSec?.let { Instant.fromEpochSeconds(it) }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        }
        val url = hit["absolute_url"].asObjectOrNull()?.get("en").contentOrNull()
            ?: hit["absolute_url"].contentOrNull()
            ?: "https://dubai.dubizzle.com/"
        val name = hit["name"].asObjectOrNull()?.get("en").contentOrNull()
            ?: hit["name"].contentOrNull()
        val description = hit["description"].asObjectOrNull()?.get("en").contentOrNull()
            ?: hit["description_short"].contentOrNull()
            ?: name
        val agent = hit["agent"].asObjectOrNull()?.get("name").asObjectOrNull()
            ?.get("en").contentOrNull()
            ?: hit["agent_profile"].asObjectOrNull()?.get("name").asObjectOrNull()
                ?.get("en").contentOrNull()
        val listedBy = hit["listed_by"].asObjectOrNull()?.get("value").contentOrNull()
        val owner = listedBy != null && listedBy != "AG"
        val amenities = hit["amenities_v2"].asArrayOrNull()?.mapNotNull { a ->
            a.asObjectOrNull()?.get("en").contentOrNull()
        }?.ifEmpty { null }
        val leafSlug = hit["categories_v2"].asObjectOrNull()
            ?.get("slug").asArrayOrNull()
            ?.firstOrNull()
            ?.contentOrNull()
        val commercialInfo = if (adType.isCommercial) {
            CommercialInfo(
                numberOfRooms = null,
                propertyType = AeCommercialTypes.fromDubizzleSlug(leafSlug),
            )
        } else {
            null
        }
        val effectiveRooms = if (adType.isCommercial) null else rooms
        val effectiveStudio = if (adType.isCommercial) null else isStudio

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                // Site detail is Incapsula-blocked; list hit is the best we get anonymously.
                isDetailLoaded = true,
            ),
            contactInformation = ContactInformation(phones = null, ownerName = agent),
            coordinates = coords,
            commercialInfo = commercialInfo,
            flatPlatform = FlatPlatform.DUBIZZLE,
            flatDetailUrl = url,
            publishedAt = publishedAt,
            publishedAtServer = addedSec?.toString(),
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            priceUsd = null,
            priceByn = price,
            rooms = effectiveRooms,
            district = district,
            address = address,
            metroStation = null,
            description = description?.stripHtmlToPlainText(),
            yearBuilt = null,
            totalArea = totalArea,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = effectiveStudio,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = hit["completion_status"].contentOrNull(),
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            amenities = amenities,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = amenities?.firstOrNull { it.contains("Parking", ignoreCase = true) },
            owner = owner,
        )
    }

    fun listStub(adId: Long): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = true),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.DUBIZZLE,
        flatDetailUrl = "https://dubai.dubizzle.com/",
        publishedAt = null,
        publishedAtServer = null,
        publishedAtUi = null,
        imageUrls = null,
        priceUsd = null,
        priceByn = null,
        rooms = null,
        district = null,
        address = null,
        metroStation = null,
        description = null,
        yearBuilt = null,
        totalArea = null,
        livingArea = null,
        kitchenArea = null,
        floor = null,
        totalFloors = null,
        sleepingPlaces = null,
        isStudio = null,
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

    /**
     * Dubai bbox ≈ lat 24.8–25.4, lng 54.8–55.6.
     * Hits often store `{lat: 55.27, lng: 25.19}` — swap when out of range.
     */
    fun parseCoords(hit: JsonObject): Coordinates? {
        val geo = hit["_geoloc"].asObjectOrNull()
        var a = geo?.get("lat").doubleOrNull()
        var b = geo?.get("lng").doubleOrNull()
        if (a == null || b == null) {
            val mapGeo = hit["map_geo"].contentOrNull() ?: return null
            val parts = mapGeo.split('-', ',')
            if (parts.size < 2) return null
            a = parts[0].toDoubleOrNull()
            b = parts[1].toDoubleOrNull()
        }
        if (a == null || b == null) return null
        return normalizeUae(a, b)
    }

    fun normalizeUae(a: Double, b: Double): Coordinates? {
        val looksLatLng = a in 22.0..27.0 && b in 51.0..57.0
        val looksLngLat = a in 51.0..57.0 && b in 22.0..27.0
        return when {
            looksLatLng -> Coordinates(latitude = a, longitude = b)
            looksLngLat -> Coordinates(latitude = b, longitude = a)
            else -> null
        }
    }
}
