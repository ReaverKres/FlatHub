package listing.kr.zigbang

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
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull
import listing.kr.zigbang.ZigbangFlatMapper.listStub
import utils.stripHtmlToPlainText
import kotlin.time.Instant

/**
 * Map search pins + batch list (`/house/property/v1/items/list`) → [AppFlat].
 * See tmp/kr/api/zigbang/NOTES.md.
 */
object ZigbangFlatMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private const val MANWON = 10_000.0

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

    /**
     * Batch list → cards. Preserves [pins] order; missing API rows fall back to [listStub].
     * Does not mark detail loaded (phones / full gallery still need v3).
     */
    fun mapBatch(
        raw: String,
        requestedAdType: AdType,
        pins: List<MapPin>,
    ): List<AppFlat> {
        val root = runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
            ?: return pins.map { listStub(it, requestedAdType) }
        val byId = root["items"].asArrayOrNull().orEmpty().mapNotNull { el ->
            val obj = el.asObjectOrNull() ?: return@mapNotNull null
            val id = obj["item_id"].longOrNull() ?: return@mapNotNull null
            id to obj
        }.toMap()
        return pins.map { pin ->
            val item = byId[pin.itemId]
            if (item == null) {
                listStub(pin, requestedAdType)
            } else {
                runCatching { mapBatchItem(item, requestedAdType, pin) }
                    .getOrElse { listStub(pin, requestedAdType) }
            }
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

    private fun mapBatchItem(
        item: JsonObject,
        requestedAdType: AdType,
        pin: MapPin,
    ): AppFlat {
        val itemId = item["item_id"].longOrNull() ?: pin.itemId
        val salesType = item["sales_type"].contentOrNull()
        val effectiveAdType = salesTypeToAdType(salesType) ?: requestedAdType
        val serviceType = item["service_type"].contentOrNull()

        val depositMan = item["deposit"].doubleOrNull()
            ?: item["deposit"].intOrNull()?.toDouble()
        val rentMan = item["rent"].doubleOrNull()
            ?: item["rent"].intOrNull()?.toDouble()

        // Batch 매매 puts sale price in `deposit` (v3 uses `price.sales`).
        val (mainPrice, secondPrice) = when (effectiveAdType) {
            is AdType.SALE -> depositMan?.let { it * MANWON } to null
            is AdType.JEONSE -> depositMan?.let { it * MANWON } to null
            else -> {
                val monthly = rentMan?.takeIf { it > 0 }?.let { it * MANWON }
                val deposit = depositMan?.let { it * MANWON }
                monthly to deposit
            }
        }

        val loc = item["location"].asObjectOrNull()
            ?: item["random_location"].asObjectOrNull()
        val lat = loc?.get("lat").doubleOrNull() ?: pin.lat
        val lng = loc?.get("lng").doubleOrNull() ?: pin.lng

        val thumb = item["images_thumbnail"].contentOrNull()
            ?.let { normalizeImageUrl(it) }
            ?.let { listOf(it) }

        val addrOrigin = item["addressOrigin"].asObjectOrNull()
        val district = listOfNotNull(
            addrOrigin?.get("local2").contentOrNull(),
            addrOrigin?.get("local3").contentOrNull(),
        ).joinToString(" ").ifBlank {
            item["address"].contentOrNull()
        }
        val address = addrOrigin?.get("fullText").contentOrNull()
            ?: addrOrigin?.get("localText").contentOrNull()
            ?: item["address1"].contentOrNull()
            ?: item["address"].contentOrNull()

        val sizeM2 = item["size_m2"].doubleOrNull()
            ?: item["전용면적"].asObjectOrNull()?.get("m2").doubleOrNull()
            ?: item["전용면적"].asObjectOrNull()?.get("m2").intOrNull()?.toDouble()

        val floor = item["floor"].contentOrNull()?.toIntOrNull()
            ?: item["floor_string"].contentOrNull()?.toIntOrNull()
        val totalFloors = item["building_floor"].contentOrNull()?.toIntOrNull()

        val roomCode = item["room_type"].contentOrNull()
        val roomTitle = item["room_type_title"].contentOrNull()
        val rooms = parseRoomTypeCode(roomCode)
            ?: ZigbangDetailMapper.parseRooms(roomTitle.orEmpty(), serviceType.orEmpty())
        val isStudio = rooms == 1

        val title = item["title"].contentOrNull()?.stripHtmlToPlainText()
        val regDate = item["reg_date"].contentOrNull()
        val publishedAt = regDate?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: regDate

        return AppFlat(
            adId = itemId,
            adType = effectiveAdType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = Coordinates(latitude = lat, longitude = lng),
            commercialInfo = null,
            flatPlatform = FlatPlatform.ZIGBANG,
            flatDetailUrl = detailUrl(itemId, serviceType),
            publishedAt = publishedAt,
            publishedAtServer = regDate,
            publishedAtUi = publishedAtUi,
            imageUrls = thumb,
            secondPrice = secondPrice,
            mainPrice = mainPrice,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = title,
            yearBuilt = null,
            totalArea = sizeM2,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = totalFloors,
            sleepingPlaces = null,
            isStudio = isStudio,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = null,
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            amenities = item["tags"].asArrayOrNull()
                ?.mapNotNull { it.contentOrNull() }
                ?.takeIf { it.isNotEmpty() },
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = null,
        )
    }

    private fun salesTypeToAdType(salesType: String?): AdType? = when (salesType?.trim()) {
        "매매" -> AdType.SALE
        "전세" -> AdType.JEONSE
        "월세" -> AdType.RENT
        else -> null
    }

    /**
     * Batch `room_type` codes (recon vs v3 labels):
     * 01 오픈형원룸, 02 분리형원룸, 03 복층형원룸 → 1;
     * 04 투룸 → 2; 05 쓰리룸 → 3; 06+ → 4+.
     */
    internal fun parseRoomTypeCode(code: String?): Int? {
        val n = code?.trim()?.toIntOrNull() ?: return null
        return when {
            n in 1..3 -> 1
            n == 4 -> 2
            n == 5 -> 3
            n >= 6 -> n - 2 // 06→4, 07→5 …
            else -> null
        }
    }
}
