package listing.us.zumper

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import entities.ListingInsights
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
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
 * Maps Zumper `window.__PRELOADED_STATE__` SERP + detail → [AppFlat].
 * USD → [AppFlat.mainPrice]. See tmp/us/api/zumper/NOTES.md.
 */
object ZumperMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private const val IMAGE_CDN = "https://img.zumpercdn.com"
    private const val SITE_ORIGIN = "https://www.zumper.com"
    private val preloadedMarker = "window.__PRELOADED_STATE__"

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val state = extractPreloadedState(html) ?: return emptyList()
        val currentSearch = state["currentSearch"].asObjectOrNull() ?: return emptyList()
        val medianLookup = buildMedianLookup(currentSearch, adType)
        val items = currentSearch["listables"].asObjectOrNull()
            ?.get("listables").asArrayOrNull()
            ?: return emptyList()
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapListable(item, adType, medianLookup) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val state = extractPreloadedState(html)
        val entity = state?.get("detail").asObjectOrNull()
            ?.get("entity").asObjectOrNull()
            ?.get("data").asObjectOrNull()
        val supplemental = entity?.get("supplemental_content").asObjectOrNull()
        val description = entity?.get("description").contentOrNull()
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 20 }
            ?: supplemental?.get("description").contentOrNull()
                ?.stripHtmlToPlainText()
                ?.takeIf { it.length > 20 }
            ?: entity?.get("short_description").contentOrNull()
                ?.stripHtmlToPlainText()
                ?.takeIf { it.length > 20 }
            ?: base.description
        val detailAmenities = entity?.get("amenity_tags").asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
            .orEmpty()
        val amenities = (base.amenities.orEmpty() + detailAmenities).distinct().ifEmpty { null }
        val phone = entity?.get("phone").contentOrNull()?.takeIf { it.isNotBlank() }
        val phones = when {
            !phone.isNullOrBlank() -> listOf(phone)
            else -> base.contactInformation?.phones
        }
        val detailImages = entity?.get("image_ids").asArrayOrNull()
            ?.mapNotNull { it.longOrNull()?.let(::imageUrl) }
            ?: entity?.get("media").asArrayOrNull()
                ?.mapNotNull {
                    it.asObjectOrNull()?.get("media_id").longOrNull()?.let(::imageUrl)
                }
        val images = detailImages?.distinct()?.take(10)
        val lat = entity?.get("lat").doubleOrNull()
        val lng = entity?.get("lng").doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            amenities = amenities,
            contactInformation = ContactInformation(
                phones = phones,
                ownerName = base.contactInformation?.ownerName,
            ),
            coordinates = coords,
            imageUrls = images?.ifEmpty { null } ?: base.imageUrls,
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.ZUMPER,
        flatDetailUrl = detailUrl ?: SITE_ORIGIN,
        publishedAt = null,
        publishedAtServer = null,
        publishedAtUi = null,
        imageUrls = null,
        secondPrice = null,
        mainPrice = null,
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

    private fun mapListable(
        item: JsonObject,
        adType: AdType,
        medianLookup: MedianLookup,
    ): AppFlat {
        val id = item["listing_id"].longOrNull()
            ?: item["listing_id"].contentOrNull()?.toLongOrNull()
            ?: error("missing listing_id")
        val minBedrooms = item["min_bedrooms"].intOrNull()
        val isStudio = minBedrooms == 0
        val rooms = when {
            isStudio -> 0
            else -> minBedrooms
        }
        val price = item["min_price"].doubleOrNull()
        val neighborhood = item["neighborhood_name"].contentOrNull()
        val city = item["city"].contentOrNull()
        val district = neighborhood ?: city
        val street = item["address"].contentOrNull()
        val buildingName = item["building_name"].contentOrNull()
        val address = listOfNotNull(street, buildingName, city)
            .distinct()
            .joinToString(", ")
            .ifBlank { null }
        val lat = item["lat"].doubleOrNull()
        val lng = item["lng"].doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null
        val phone = item["phone"].contentOrNull()?.takeIf { it.isNotBlank() }
        val agentName = item["agent_name"].contentOrNull()?.takeIf { it.isNotBlank() }
        val imageUrls = item["image_ids"].asArrayOrNull()
            ?.mapNotNull { it.longOrNull()?.let(::imageUrl) }
            ?.distinct()
            ?.take(10)
            ?.ifEmpty { null }
        val path = item["url"].contentOrNull() ?: error("missing url")
        val detailUrl = if (path.startsWith("http")) path else "$SITE_ORIGIN$path"
        val listedSec = item["listed_on"].longOrNull() ?: item["created_on"].longOrNull()
        val publishedAt = listedSec?.let { Instant.fromEpochSeconds(it) }
        val publishedAtServer = listedSec?.toString()
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: publishedAtServer
        val amenityTags =
            item["amenity_tags"].asArrayOrNull()?.mapNotNull { it.contentOrNull() }.orEmpty()
        val buildingAmenityTags = item["building_amenity_tags"].asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
            .orEmpty()
        val amenities = (amenityTags + buildingAmenityTags).distinct().ifEmpty { null }
        val propertyType = item["property_type"].intOrNull()
        val propertySubtype = mapPropertySubtype(propertyType)
        val median = medianLookup.medianFor(minBedrooms, propertyType)
        val listingInsights = if (price != null && median != null && median > 0.0) {
            ListingInsights(
                priceVsAreaAvgPercent = ((price - median) / median) * 100.0,
                propertySubtype = propertySubtype,
            )
        } else if (propertySubtype != null) {
            ListingInsights(propertySubtype = propertySubtype)
        } else {
            null
        }
        val description = item["short_description"].contentOrNull()
            ?.stripHtmlToPlainText()
            ?.takeIf { it.isNotBlank() }

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(
                phones = phone?.let { listOf(it) },
                ownerName = agentName,
            ),
            coordinates = coords,
            commercialInfo = null,
            listingInsights = listingInsights,
            flatPlatform = FlatPlatform.ZUMPER,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = publishedAtServer,
            publishedAtUi = publishedAtUi,
            imageUrls = imageUrls,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = item["min_square_feet"].doubleOrNull(),
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = if (isStudio) true else null,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = null,
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            amenities = amenities,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = if (agentName != null) false else null,
        )
    }

    private fun extractPreloadedState(html: String): JsonObject? {
        val markerIdx = html.indexOf(preloadedMarker)
        if (markerIdx < 0) return null
        val assignIdx = html.indexOf('=', markerIdx)
        if (assignIdx < 0) return null
        val start = html.indexOf('{', assignIdx)
        if (start < 0) return null
        val scriptEnd = html.indexOf("</script>", start).takeIf { it >= 0 } ?: html.length
        val endExclusive = extractBalancedObjectEnd(html, start, scriptEnd) ?: return null
        val raw = html.substring(start, endExclusive)
        return runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
    }

    private fun extractBalancedObjectEnd(text: String, start: Int, limit: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until limit.coerceAtMost(text.length)) {
            val ch = text[i]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else when (ch) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i + 1
                }
            }
        }
        return null
    }

    private fun buildMedianLookup(currentSearch: JsonObject, adType: AdType): MedianLookup {
        if (adType is AdType.SALE) return MedianLookup(emptyMap())
        val rentalData = currentSearch["rentalData"].asObjectOrNull()
            ?.get("data").asObjectOrNull()
            ?: return MedianLookup(emptyMap())
        val bedPropertyType = rentalData["bed_property_type"].asObjectOrNull()
            ?: return MedianLookup(emptyMap())
        val map = LinkedHashMap<Pair<String, String>, Double>()
        for ((bedKey, bedValue) in bedPropertyType) {
            val types = bedValue.asObjectOrNull() ?: continue
            for ((typeKey, typeValue) in types) {
                val median = typeValue.asObjectOrNull()?.get("median_rent").doubleOrNull()
                    ?: continue
                map[bedKey to typeKey.uppercase()] = median
            }
        }
        return MedianLookup(map)
    }

    private fun mapPropertySubtype(propertyType: Int?): String? = when (propertyType) {
        1 -> "house"
        2 -> "condo"
        3 -> "townhouse"
        4 -> "apartment"
        5 -> "room"
        else -> null
    }

    private fun rentalCategory(propertyType: Int?): String = when (propertyType) {
        2 -> "CFR"
        else -> "AFR"
    }

    private fun bedKey(minBedrooms: Int?): String? = when (minBedrooms) {
        null -> null
        0 -> "STUDIO"
        in 1..9 -> "${minBedrooms}_BED"
        else -> null
    }

    private fun imageUrl(imageId: Long): String = "$IMAGE_CDN/$imageId/1280x960"

    private class MedianLookup(
        private val values: Map<Pair<String, String>, Double>,
    ) {
        fun medianFor(minBedrooms: Int?, propertyType: Int?): Double? {
            val bed = bedKey(minBedrooms) ?: return null
            val category = rentalCategory(propertyType)
            return values[bed to category]
                ?: values[bed to "AFR"]
        }
    }
}
