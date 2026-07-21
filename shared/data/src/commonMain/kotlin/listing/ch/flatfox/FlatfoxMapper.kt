package listing.ch.flatfox

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import listing.ch.flatfox.FlatfoxMapper.mapPinStub
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull
import kotlin.time.Instant

/**
 * Flatfox list/detail JSON → [AppFlat]. CHF → [AppFlat.mainPrice].
 * See tmp/ch/api/flatfox/NOTES.md.
 */
object FlatfoxMapper {
    data class Pin(
        val pk: Long,
        val lat: Double,
        val lng: Double,
        val price: Double?,
    )

    fun parsePins(array: List<kotlinx.serialization.json.JsonElement>): List<Pin> =
        array.mapNotNull { el ->
            val obj = el.asObjectOrNull() ?: return@mapNotNull null
            val pk = obj["pk"].longOrNull() ?: return@mapNotNull null
            val lat = obj["latitude"].doubleOrNull() ?: return@mapNotNull null
            val lng = obj["longitude"].doubleOrNull() ?: return@mapNotNull null
            val unit = obj["price_unit"].contentOrNull()
            if (unit != null && unit != "monthly") return@mapNotNull null
            Pin(pk = pk, lat = lat, lng = lng, price = obj["price_display"].doubleOrNull())
        }

    /** Preserve [pins] order; missing API rows fall back to [mapPinStub]. */
    fun mapBatch(listings: List<JsonObject>, pins: List<Pin>): List<AppFlat> {
        val byPk = listings.mapNotNull { obj ->
            val pk = obj["pk"].longOrNull() ?: return@mapNotNull null
            pk to obj
        }.toMap()
        return pins.map { pin ->
            val listing = byPk[pin.pk]
            if (listing == null) {
                mapPinStub(pin)
            } else {
                mapListing(listing, pin = pin) ?: mapPinStub(pin)
            }
        }
    }

    fun mapListing(
        listing: JsonObject,
        pin: Pin? = null,
        markDetailLoaded: Boolean = false,
    ): AppFlat? {
        if (!isRentListing(listing)) return null
        val pk = listing["pk"].longOrNull() ?: pin?.pk ?: return null
        val adType = AdType.RENT
        val mainPrice = monthlyPrice(listing) ?: pin?.price
        val coords = coordinates(listing, pin)
        val images = imageUrls(listing)
        val rooms = parseRooms(listing["number_of_rooms"].contentOrNull())
        val area = listing["surface_living"].doubleOrNull()
            ?: listing["livingspace"].doubleOrNull()
            ?: listing["space_display"].doubleOrNull()
        val publishedRaw = listing["published"].contentOrNull()
        val publishedAt = publishedRaw?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        }
        val agency = listing["agency"].asObjectOrNull()
        val agencyName = agency?.get("name").contentOrNull()?.takeIf { it.isNotBlank() }
        val detailPath = listing["url"].contentOrNull()
        val detailUrl = if (!detailPath.isNullOrBlank()) {
            if (detailPath.startsWith("http")) detailPath else "${FlatfoxApiClient.BASE}$detailPath"
        } else {
            "${FlatfoxApiClient.BASE}/en/flat/$pk/"
        }

        return AppFlat(
            adId = pk,
            adType = adType,
            flatDevInfo = FlatDevInfo(
                isDetailData = markDetailLoaded,
                isDetailLoaded = markDetailLoaded,
            ),
            contactInformation = ContactInformation(phones = null, ownerName = agencyName),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.FLATFOX,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = publishedRaw,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            secondPrice = null,
            mainPrice = mainPrice,
            rooms = rooms,
            district = listing["city"].contentOrNull(),
            address = listing["public_address"].contentOrNull()
                ?: listing["street"].contentOrNull(),
            metroStation = null,
            description = listing["description"].contentOrNull()?.trim()
                ?.takeIf { it.isNotEmpty() },
            yearBuilt = listing["year_built"].intOrNull(),
            totalArea = area,
            livingArea = listing["surface_living"].doubleOrNull()
                ?: listing["livingspace"].doubleOrNull(),
            kitchenArea = null,
            floor = listing["floor"].intOrNull(),
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = rooms == 1,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = null,
            windowDirections = null,
            buildingImprovements = attributeLabels(listing),
            prepaymentType = null,
            amenities = furnishedAmenities(listing),
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = agencyName.isNullOrBlank(),
        )
    }

    fun mapPinStub(pin: Pin): AppFlat = listStub(pin.pk).copy(
        mainPrice = pin.price,
        coordinates = Coordinates(latitude = pin.lat, longitude = pin.lng),
    )

    fun mergeDetail(base: AppFlat, listing: JsonObject?): AppFlat {
        if (listing == null) {
            return base.copy(
                flatDevInfo = base.flatDevInfo.copy(isDetailData = true, isDetailLoaded = true),
            )
        }
        return mapListing(listing, markDetailLoaded = true)
            ?.copy(
                savedInFavorites = base.savedInFavorites,
                isViewed = base.isViewed,
                dislike = base.dislike,
            ) ?: base.copy(
            flatDevInfo = base.flatDevInfo.copy(isDetailData = true, isDetailLoaded = true),
        )
    }

    fun listStub(adId: Long): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.FLATFOX,
        flatDetailUrl = "${FlatfoxApiClient.BASE}/en/flat/$adId/",
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

    private fun isRentListing(listing: JsonObject): Boolean {
        if (listing["offer_type"].contentOrNull() != "RENT") return false
        if (listing["price_unit"].contentOrNull() != "monthly") return false
        return when (listing["object_category"].contentOrNull()) {
            "APARTMENT", "HOUSE", "SHARED" -> true
            else -> false
        }
    }

    private fun monthlyPrice(listing: JsonObject): Double? =
        listing["rent_gross"].doubleOrNull()
            ?: listing["price_display"].doubleOrNull()
            ?: listing["rent_net"].doubleOrNull()

    private fun coordinates(listing: JsonObject, pin: Pin?): Coordinates? {
        val lat = listing["latitude"].doubleOrNull() ?: pin?.lat
        val lng = listing["longitude"].doubleOrNull() ?: pin?.lng
        return if (lat != null && lng != null) Coordinates(
            latitude = lat,
            longitude = lng
        ) else null
    }

    /**
     * With `expand=images`, `images[]` are objects with signed paths
     * (`url_listing_search` / `url_thumb_m`). Bare media ids (no expand) cannot be turned
     * into working `/thumb/listing/{id}/` URLs (HTTP 400).
     */
    private fun imageUrls(listing: JsonObject): List<String>? {
        val fromExpand = listing["images"].asArrayOrNull().orEmpty().mapNotNull { el ->
            val obj = el.asObjectOrNull() ?: return@mapNotNull null
            absoluteMediaUrl(
                obj["url_listing_search"].contentOrNull()
                    ?: obj["url_thumb_m"].contentOrNull()
                    ?: obj["url"].contentOrNull(),
            )
        }
        if (fromExpand.isNotEmpty()) return fromExpand.distinct().take(10)
        return null
    }

    private fun absoluteMediaUrl(raw: String?): String? {
        val path = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (path.startsWith("http")) path else "${FlatfoxApiClient.BASE}$path"
    }

    private fun parseRooms(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        return raw.replace(',', '.').toDoubleOrNull()?.let { kotlin.math.ceil(it).toInt() }
    }

    private fun attributeLabels(listing: JsonObject): List<String>? =
        listing["attributes"].asArrayOrNull()
            ?.mapNotNull { it.asObjectOrNull()?.get("name").contentOrNull() }
            ?.distinct()
            ?.ifEmpty { null }

    private fun furnishedAmenities(listing: JsonObject): List<String>? {
        val furnished =
            listing["is_furnished"].contentOrNull()?.equals("true", ignoreCase = true) == true
        return if (furnished) listOf("furnished") else null
    }
}
