package listing.kr.zigbang

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.Json
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.doubleOrNull
import listing.core.longOrNull

/**
 * Map search pins (`/v2/items/oneroom` etc.) → minimal [AppFlat] stubs (coords only; price from detail).
 * See tmp/kr/api/zigbang/NOTES.md.
 */
object ZigbangFlatMapper {
    private val json = Json { ignoreUnknownKeys = true }

    data class MapPin(
        val itemId: Long,
        val lat: Double,
        val lng: Double,
    )

    fun parseMapPins(raw: String): List<MapPin> {
        val root = runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
            ?: return emptyList()
        return root["items"].asArrayOrNull().orEmpty().mapNotNull { el ->
            val obj = el.asObjectOrNull() ?: return@mapNotNull null
            val id = obj["itemId"].longOrNull() ?: return@mapNotNull null
            val lat = obj["lat"].doubleOrNull() ?: return@mapNotNull null
            val lng = obj["lng"].doubleOrNull() ?: return@mapNotNull null
            MapPin(itemId = id, lat = lat, lng = lng)
        }
    }

    fun listStub(
        pin: MapPin,
        adType: AdType,
        serviceType: String? = null,
    ): AppFlat = AppFlat(
        adId = pin.itemId,
        adType = adType,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = Coordinates(latitude = pin.lat, longitude = pin.lng),
        commercialInfo = null,
        flatPlatform = FlatPlatform.ZIGBANG,
        flatDetailUrl = detailUrl(pin.itemId, serviceType),
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

    fun listStub(adId: Long): AppFlat = listStub(
        pin = MapPin(adId, lat = 0.0, lng = 0.0),
        adType = AdType.RENT,
    ).copy(coordinates = null)

    fun detailUrl(itemId: Long, serviceType: String?): String {
        val segment = serviceTypeToSegment(serviceType)
        return "https://www.zigbang.com/home/$segment/items/$itemId"
    }

    fun serviceTypeToSegment(serviceType: String?): String = when (serviceType?.trim()) {
        "오피스텔" -> "officetel"
        "빌라", "연립", "다세대" -> "villa"
        else -> "oneroom"
    }

    /**
     * Zigbang CDN (`ic.zigbang.com`) returns HTTP 400 without a width query.
     * `?w=800` is the minimal working form from recon.
     */
    fun normalizeImageUrl(raw: String): String {
        val url = raw.trim()
        if (url.isEmpty()) return url
        if (!url.contains("ic.zigbang.com", ignoreCase = true)) return url
        if (url.contains("w=", ignoreCase = true)) return url
        return if (url.contains('?')) "$url&w=800" else "$url?w=800"
    }
}
