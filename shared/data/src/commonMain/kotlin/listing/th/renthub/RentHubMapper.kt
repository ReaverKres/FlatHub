package listing.th.renthub

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.longOrNull

/**
 * RentHub apartment listings → [AppFlat]. THB → [AppFlat.priceByn].
 * Buildings with monthly min–max price. See tmp/th/api/renthub/NOTES.md.
 */
object RentHubMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseSearch(raw: String): List<AppFlat> {
        val root = runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
            ?: return emptyList()
        val listings = root["pageProps"].asObjectOrNull()
            ?.get("listings").asArrayOrNull()
            ?: return emptyList()
        return listings.mapNotNull { el ->
            val obj = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapListing(obj) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, raw: String): AppFlat {
        val root = runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
        val listing = root?.get("pageProps").asObjectOrNull()?.get("listing").asObjectOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        val mapped = runCatching { mapListing(listing) }.getOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        return mapped.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = mapped.description?.takeIf { it.isNotBlank() } ?: base.description,
            contactInformation = ContactInformation(
                phones = mapped.contactInformation?.phones ?: base.contactInformation?.phones,
                ownerName = mapped.contactInformation?.ownerName
                    ?: base.contactInformation?.ownerName,
            ),
            coordinates = mapped.coordinates ?: base.coordinates,
            imageUrls = mapped.imageUrls?.takeIf { it.isNotEmpty() } ?: base.imageUrls,
            priceByn = mapped.priceByn ?: base.priceByn,
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.RENTHUB,
        flatDetailUrl = detailUrl ?: "https://www.renthub.in.th/en",
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

    private fun mapListing(obj: JsonObject): AppFlat {
        val id = obj["id"].longOrNull()
            ?: obj["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val slug = obj["slug"].contentOrNull().orEmpty()
        val name = obj["name"].contentOrNull() ?: obj["title"].contentOrNull()
        val province = obj["province"].contentOrNull()
        val district = obj["district"].contentOrNull()
        val subdistrict = obj["subdistrict"].contentOrNull()
        val address = listOfNotNull(name, subdistrict, district, province)
            .distinct()
            .joinToString(", ")
            .ifBlank { null }
        val loc = obj["location"].asObjectOrNull()
        val lat = loc?.get("lat").doubleOrNull()
        val lng = loc?.get("lng").doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null
        val monthly = obj["price"].asObjectOrNull()?.get("monthly").asObjectOrNull()
        val price = monthly?.get("minPrice").doubleOrNull()
            ?: monthly?.get("maxPrice").doubleOrNull()
        val cover = obj["coverPicture"].contentOrNull()?.let { path ->
            if (path.startsWith("http")) path else "https://www.renthub.in.th$path"
        }
        val contacts = obj["contactInformation"].asArrayOrNull().orEmpty()
        val phones = contacts.flatMap { c ->
            val phoneArr = c.asObjectOrNull()?.get("phone").asArrayOrNull().orEmpty()
            phoneArr.mapNotNull { p ->
                when {
                    p.asObjectOrNull() != null ->
                        p.asObjectOrNull()?.get("phoneNumber").contentOrNull()

                    else -> p.contentOrNull()
                }
            }
        }.ifEmpty { null }
        val ownerName = contacts.firstOrNull()?.asObjectOrNull()?.get("name").contentOrNull()
        val detailUrl = "https://www.renthub.in.th/en/$slug"
        val hasDetailBits = coords != null || !phones.isNullOrEmpty()

        return AppFlat(
            adId = id,
            adType = AdType.RENT,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = hasDetailBits,
            ),
            contactInformation = ContactInformation(phones = phones, ownerName = ownerName),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.RENTHUB,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = cover?.let { listOf(it) },
            priceUsd = null,
            priceByn = price,
            rooms = null,
            district = district,
            address = address,
            metroStation = null,
            description = name,
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
    }
}
