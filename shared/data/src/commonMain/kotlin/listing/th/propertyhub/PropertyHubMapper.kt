package listing.th.propertyhub

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.asPrimitiveOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText
import kotlin.time.Instant

/**
 * PropertyHub `_next/data` → [AppFlat]. THB → [AppFlat.priceByn].
 * See tmp/th/api/propertyhub/NOTES.md.
 */
object PropertyHubMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun parseSearch(raw: String, adType: AdType): List<AppFlat> {
        val root = runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
            ?: return emptyList()
        val listings = root["pageProps"].asObjectOrNull()
            ?.get("resultListings").asArrayOrNull()
            ?: return emptyList()
        return listings.mapNotNull { el ->
            val obj = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapListing(obj, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.PROPERTYHUB,
        flatDetailUrl = detailUrl ?: "https://propertyhub.in.th/en",
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

    private fun mapListing(obj: JsonObject, adType: AdType): AppFlat {
        val id = obj["id"].longOrNull()
            ?: obj["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val slug = obj["slug"].contentOrNull().orEmpty()
        val propertyType = obj["propertyType"].contentOrNull()?.lowercase() ?: "condo"
        val detailUrl = "https://propertyhub.in.th/en/$propertyType/$slug-$id"
        val title = obj["title"].contentOrNull()?.stripHtmlToPlainText()
        val project = obj["project"].asObjectOrNull()
        val projectName = project?.get("nameEnglish").contentOrNull()
            ?: project?.get("name").contentOrNull()
        val projectAddr = project?.get("address").contentOrNull()
        val loc = obj["location"].asObjectOrNull()
        val lat = loc?.get("lat").doubleOrNull()
        val lng = loc?.get("lng").doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null
        val room = obj["roomInformation"].asObjectOrNull()
        val beds = room?.get("numberOfBed").intOrNull()
        val area = room?.get("roomArea").doubleOrNull()
        val floor = room?.get("onFloor").contentOrNull()?.toIntOrNull()
            ?: room?.get("onFloor").intOrNull()
        val priceObj = obj["price"].asObjectOrNull()
        val price = when {
            adType is AdType.SALE || obj["postType"].contentOrNull() == "FOR_SALE" ->
                priceObj?.get("forSale").asObjectOrNull()?.get("price").doubleOrNull()

            else ->
                priceObj?.get("forRent").asObjectOrNull()
                    ?.get("monthly").asObjectOrNull()
                    ?.get("price").doubleOrNull()
        }
        val contacts = obj["contactInformation"].asArrayOrNull().orEmpty()
        val phones = contacts.flatMap { c ->
            c.asObjectOrNull()?.get("phone").asArrayOrNull().orEmpty()
                .mapNotNull { it.contentOrNull() }
        }.ifEmpty { null }
        val contact = contacts.firstOrNull()?.asObjectOrNull()
        val ownerName = contact?.get("name").contentOrNull()
        val isAgency = contact?.get("isAgency")?.asPrimitiveOrNull()?.booleanOrNull
        val companyName = contact?.get("companyName").contentOrNull()?.trim().orEmpty()
        val owner = when {
            isAgency == true || companyName.isNotEmpty() -> false
            isAgency == false -> true
            else -> null
        }
        val cover = obj["coverPicture"].contentOrNull()?.let { path ->
            if (path.startsWith("http")) path else "https://cdn.propertyhub.in.th$path"
        }
        val created = obj["refreshedAt"].contentOrNull()
            ?: obj["createdAt"].contentOrNull()
        val publishedAt = created?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: created
        val effectiveAdType = when (obj["postType"].contentOrNull()) {
            "FOR_SALE" -> AdType.SALE
            else -> adType
        }
        val hasDetailBits = coords != null || !phones.isNullOrEmpty()

        return AppFlat(
            adId = id,
            adType = effectiveAdType,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = hasDetailBits,
            ),
            contactInformation = ContactInformation(phones = phones, ownerName = ownerName),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.PROPERTYHUB,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAtUi,
            imageUrls = cover?.let { listOf(it) },
            priceUsd = null,
            priceByn = price,
            rooms = beds,
            district = projectAddr,
            address = listOfNotNull(projectName, projectAddr, title).distinct().joinToString(" · ")
                .ifBlank { null },
            metroStation = null,
            description = title,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = beds == 0,
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
            owner = owner,
        )
    }
}
