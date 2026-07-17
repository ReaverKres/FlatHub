package listing.ae.propertyfinder

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
import kotlinx.serialization.json.Json
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
 * Maps Property Finder `__NEXT_DATA__` → [AppFlat]. AED → [AppFlat.priceByn].
 * Area is sqft on the wire → m². See tmp/ae/api/propertyfinder/NOTES.md.
 */
object PropertyFinderMapper {
    private val json = Json { ignoreUnknownKeys = true }

    private const val SQFT_TO_M2 = 0.092903

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val next = extractNextData(html) ?: return emptyList()
        val listings = next["props"].asObjectOrNull()
            ?.get("pageProps").asObjectOrNull()
            ?.get("searchResult").asObjectOrNull()
            ?.get("listings").asArrayOrNull()
            ?: return emptyList()
        return listings.mapNotNull { el ->
            val wrapper = el.asObjectOrNull() ?: return@mapNotNull null
            if (wrapper["listing_type"].contentOrNull() != "property") return@mapNotNull null
            val property = wrapper["property"].asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapProperty(property, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val next = extractNextData(html)
        val property = next?.get("props").asObjectOrNull()
            ?.get("pageProps").asObjectOrNull()
            ?.get("property").asObjectOrNull()
            ?: next?.get("props").asObjectOrNull()
                ?.get("pageProps").asObjectOrNull()
                ?.get("result").asObjectOrNull()
                ?.get("property").asObjectOrNull()
        if (property == null) {
            return base.copy(flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true))
        }
        val mapped = runCatching { mapProperty(property, base.adType ?: AdType.RENT) }.getOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        return mapped.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = mapped.description?.takeIf { it.length > (base.description?.length ?: 0) }
                ?: base.description,
            contactInformation = ContactInformation(
                phones = mapped.contactInformation?.phones
                    ?: base.contactInformation?.phones,
                ownerName = mapped.contactInformation?.ownerName
                    ?: base.contactInformation?.ownerName,
            ),
            imageUrls = mapped.imageUrls?.takeIf { it.isNotEmpty() } ?: base.imageUrls,
            coordinates = mapped.coordinates ?: base.coordinates,
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.PROPERTY_FINDER,
        flatDetailUrl = detailUrl ?: "https://www.propertyfinder.ae/",
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

    private fun mapProperty(property: JsonObject, adType: AdType): AppFlat {
        val id = property["id"].longOrNull()
            ?: property["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val price = property["price"].asObjectOrNull()?.get("value").doubleOrNull()
        val location = property["location"].asObjectOrNull()
        val coordsObj = location?.get("coordinates").asObjectOrNull()
        val lat = coordsObj?.get("lat").doubleOrNull()
        val lon = coordsObj?.get("lon").doubleOrNull()
            ?: coordsObj?.get("lng").doubleOrNull()
        val coordinates =
            if (lat != null && lon != null) Coordinates(lat, lon) else null
        val fullName = location?.get("full_name").contentOrNull()
        val pathName = location?.get("path_name").contentOrNull()
        val district = location?.get("name").contentOrNull()
            ?: pathName?.substringAfterLast(',')?.trim()
            ?: fullName?.substringBefore(',')?.trim()
        val bedroomsRaw = property["bedrooms"].contentOrNull()
            ?: property["bedrooms"].intOrNull()?.toString()
        val (rooms, isStudio) = parseBedrooms(bedroomsRaw)
        val sizeObj = property["size"].asObjectOrNull()
        val sizeValue = sizeObj?.get("value").doubleOrNull()
        val sizeUnit = sizeObj?.get("unit").contentOrNull().orEmpty()
        val totalArea = sizeValue?.let { v ->
            if (sizeUnit.contains("sqft", ignoreCase = true) || sizeUnit.contains(
                    "ft",
                    ignoreCase = true
                )
            ) {
                v * SQFT_TO_M2
            } else {
                v
            }
        }
        val images = property["images"].asArrayOrNull()?.mapNotNull { img ->
            val o = img.asObjectOrNull() ?: return@mapNotNull null
            o["medium"].contentOrNull() ?: o["small"].contentOrNull()
        }?.distinct()?.take(20)?.ifEmpty { null }
        val phones = buildList {
            property["contact_options"].asArrayOrNull()?.forEach { opt ->
                val o = opt.asObjectOrNull() ?: return@forEach
                if (o["type"].contentOrNull() == "phone") {
                    o["value"].contentOrNull()?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
                }
            }
            property["broker"].asObjectOrNull()?.get("phone").contentOrNull()
                ?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        }.distinct().ifEmpty { null }
        val agentName = property["agent"].asObjectOrNull()?.get("name").contentOrNull()
            ?: property["broker"].asObjectOrNull()?.get("name").contentOrNull()
        val listedDate = property["listed_date"].contentOrNull()
        val publishedAt = listedDate?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: listedDate
        val url = property["share_url"].contentOrNull()
            ?: "https://www.propertyfinder.ae/en/plp/$id.html"
        val title = property["title"].contentOrNull()?.stripHtmlToPlainText()
        val hasCoordsOrPhone = coordinates != null || !phones.isNullOrEmpty()
        val propertyTypeLabel = property["property_type"].contentOrNull()
        val commercialInfo = if (adType.isCommercial) {
            CommercialInfo(
                numberOfRooms = null,
                propertyType = AeCommercialTypes.fromPropertyFinderLabel(propertyTypeLabel),
            )
        } else {
            null
        }
        // Commercial SERP often has no bedrooms — leave rooms null (UI hides the field).
        val effectiveRooms = if (adType.isCommercial) null else rooms
        val effectiveStudio = if (adType.isCommercial) null else isStudio

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = hasCoordsOrPhone,
            ),
            contactInformation = ContactInformation(phones = phones, ownerName = agentName),
            coordinates = coordinates,
            commercialInfo = commercialInfo,
            flatPlatform = FlatPlatform.PROPERTY_FINDER,
            flatDetailUrl = url,
            publishedAt = publishedAt,
            publishedAtServer = listedDate,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            priceUsd = null,
            priceByn = price,
            rooms = effectiveRooms,
            district = district,
            address = fullName ?: pathName,
            metroStation = null,
            description = title,
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

    /** Studio → rooms 0 + isStudio; UI shows 1. */
    fun parseBedrooms(raw: String?): Pair<Int?, Boolean?> {
        if (raw.isNullOrBlank()) return null to null
        val t = raw.trim()
        if (t.equals("studio", ignoreCase = true) || t == "0") {
            return 0 to true
        }
        val n = t.toIntOrNull() ?: return null to null
        return n to (n == 0)
    }

    private fun extractNextData(html: String): JsonObject? {
        val marker = "id=\"__NEXT_DATA__\""
        val idx = html.indexOf(marker)
        if (idx < 0) return null
        val start = html.indexOf('>', idx) + 1
        if (start <= 0) return null
        val end = html.indexOf("</script>", start)
        if (end < 0) return null
        return runCatching {
            json.parseToJsonElement(html.substring(start, end)).asObjectOrNull()
        }.getOrNull()
    }
}
