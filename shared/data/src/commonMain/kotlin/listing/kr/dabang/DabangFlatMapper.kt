package listing.kr.dabang

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.asPrimitiveOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull

/**
 * Maps Dabang list JSON → [AppFlat]. Prices in 만원 → KRW (×10_000).
 * See tmp/kr/api/dabang/NOTES.md.
 */
object DabangFlatMapper {
    private const val MANWON = 10_000.0

    fun mapSearch(root: JsonObject, requestedAdType: AdType): List<AppFlat> {
        val result = root["result"].asObjectOrNull() ?: return emptyList()
        val roomList = result["roomList"].asArrayOrNull() ?: return emptyList()
        return roomList.mapNotNull { el ->
            val room = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapRoom(room, requestedAdType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = true),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.DABANG,
        flatDetailUrl = detailUrl ?: "${DabangApiClient.BASE}/",
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

    fun roomIdFromDetailUrl(url: String): String? =
        ROOM_ID_RE.find(url)?.groupValues?.get(1)

    fun detailUrl(roomId: String): String = "${DabangApiClient.BASE}/room/$roomId"

    fun roomIdToAdId(roomId: String): Long {
        val hex = roomId.replace("-", "")
        return hex.take(15).toLongOrNull(16)
            ?: (roomId.hashCode().toLong() and 0x7FFF_FFFFL)
    }

    private fun mapRoom(room: JsonObject, requestedAdType: AdType): AppFlat {
        val roomId = room["id"].contentOrNull() ?: error("missing room id")
        val adId = roomIdToAdId(roomId)
        val priceTypeName = room["priceTypeName"].contentOrNull().orEmpty()
        val priceTitleRaw = room["priceTitle"].contentOrNull().orEmpty()
        val effectiveAdType = resolveAdType(priceTypeName, requestedAdType)
        val (mainPrice, secondPrice) = mapPrices(priceTypeName, priceTitleRaw, effectiveAdType)

        val loc = room["randomLocation"].asObjectOrNull()
        val lat = loc?.get("lat").doubleOrNull()
        val lng = loc?.get("lng").doubleOrNull()
        val coords =
            if (lat != null && lng != null) Coordinates(latitude = lat, longitude = lng) else null

        val images = room["imgUrlList"].asArrayOrNull()
            ?.mapNotNull { el ->
                el.contentOrNull()
                    ?: el.asObjectOrNull()?.get("url").contentOrNull()
            }
            ?.map { normalizeImageUrl(it) }
            ?.distinct()
            ?.take(10)
            ?.ifEmpty { null }

        val roomTypeName = room["roomTypeName"].contentOrNull().orEmpty()
        val rooms = parseRooms(roomTypeName)
        val isStudio = rooms == 1 && !roomTypeName.contains("투룸")

        val roomDesc = room["roomDesc"].contentOrNull()
        val parsedDesc = parseRoomDesc(roomDesc)
        val dongName = room["dongName"].contentOrNull()
        val complexName = room["complexName"].contentOrNull()
        val title = room["roomTitle"].contentOrNull()
        val address = listOfNotNull(complexName, dongName, title)
            .distinct()
            .joinToString(" · ")
            .ifBlank { null }

        val isOwner = room["isOwner"].asPrimitiveOrNull()?.booleanOrNull
        val isDirect = room["isDirect"].asPrimitiveOrNull()?.booleanOrNull
        val owner = when {
            isOwner == true || isDirect == true -> true
            isOwner == false -> false
            else -> null
        }

        return AppFlat(
            adId = adId,
            adType = effectiveAdType,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                // List is the best anonymous payload; detail is login-gated (403).
                isDetailLoaded = true,
            ),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            listingInsights = null,
            flatPlatform = FlatPlatform.DABANG,
            flatDetailUrl = detailUrl(roomId),
            publishedAt = null,
            publishedAtServer = room["seq"].longOrNull()?.toString()
                ?: room["seq"].intOrNull()?.toString(),
            publishedAtUi = null,
            imageUrls = images,
            secondPrice = secondPrice,
            mainPrice = mainPrice,
            rooms = rooms,
            district = dongName,
            address = address,
            metroStation = null,
            description = title ?: roomDesc,
            yearBuilt = null,
            totalArea = parsedDesc.areaM2,
            livingArea = null,
            kitchenArea = null,
            floor = parsedDesc.floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = isStudio,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = priceTypeName.ifBlank { null },
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

    private fun resolveAdType(priceTypeName: String, requested: AdType): AdType = when {
        priceTypeName.contains("매매") -> AdType.SALE
        priceTypeName.contains("전세") -> AdType.JEONSE
        requested is AdType.JEONSE && priceTypeName.contains("월세") -> AdType.RENT
        requested is AdType.SALE && !priceTypeName.contains("매매") -> requested
        else -> requested
    }

    private fun mapPrices(
        priceTypeName: String,
        priceTitle: String,
        adType: AdType,
    ): Pair<Double?, Double?> = when {
        priceTypeName.contains("월세") || priceTitle.contains('/') -> {
            val (depositMan, monthlyMan) = parseWolseManwon(priceTitle)
            val monthly = monthlyMan?.times(MANWON)
            val deposit = depositMan?.times(MANWON)
            monthly to deposit
        }

        priceTypeName.contains("전세") || adType is AdType.JEONSE -> {
            parseManwonAmount(priceTitle)?.times(MANWON) to null
        }

        priceTypeName.contains("매매") || adType is AdType.SALE -> {
            parseManwonAmount(priceTitle)?.times(MANWON) to null
        }

        else -> parseManwonAmount(priceTitle)?.times(MANWON) to null
    }

    /** deposit/monthly in 만원 tokens, e.g. `"100/30"`, `"1억1000/5"`. */
    fun parseWolseManwon(priceTitle: String): Pair<Double?, Double?> {
        val parts = priceTitle.split('/')
        if (parts.size != 2) return parseManwonAmount(priceTitle) to null
        return parseManwonToken(parts[0]) to parseManwonToken(parts[1])
    }

    /** Single price in 만원, including Korean 억 notation. */
    fun parseManwonAmount(raw: String): Double? = parseManwonToken(raw.trim())

    fun parseManwonToken(token: String): Double? {
        val cleaned = token.trim()
        if (cleaned.isEmpty()) return null
        val eok = EOK_RE.find(cleaned)
        if (eok != null) {
            val eokVal = eok.groupValues[1].toDoubleOrNull() ?: return null
            val tail = eok.groupValues[2].toDoubleOrNull() ?: 0.0
            return eokVal * 10_000.0 + tail
        }
        return cleaned.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
    }

    private fun parseRooms(roomTypeName: String): Int? = when {
        roomTypeName.contains("포룸") || roomTypeName.contains("4룸") -> 4
        roomTypeName.contains("쓰리") || roomTypeName.contains("3룸") -> 3
        roomTypeName.contains("투룸") || roomTypeName.contains("2룸") -> 2
        roomTypeName.contains("원룸") || roomTypeName.contains("1.5") || roomTypeName.contains("오픈") -> 1
        roomTypeName.contains("오피스텔") -> 1
        else -> Regex("""(\d+)\s*룸""").find(roomTypeName)?.groupValues?.get(1)?.toIntOrNull()
    }

    /** Ensure scheme for protocol-relative CDN URLs (`//dthumb…`). */
    private fun normalizeImageUrl(raw: String): String {
        val url = raw.trim()
        if (url.isEmpty()) return url
        return when {
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") -> "https://" + url.removePrefix("http://")
            else -> url
        }
    }

    private data class ParsedDesc(val floor: Int?, val areaM2: Double?)

    private fun parseRoomDesc(desc: String?): ParsedDesc {
        if (desc.isNullOrBlank()) return ParsedDesc(null, null)
        val floor = FLOOR_RE.find(desc)?.groupValues?.get(1)?.toIntOrNull()
        val area = AREA_RE.find(desc)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        return ParsedDesc(floor = floor, areaM2 = area)
    }

    private val ROOM_ID_RE = Regex("""/room/([0-9a-fA-F]+)""")
    private val EOK_RE = Regex("""(\d+)억(\d*)""")
    private val FLOOR_RE = Regex("""(\d+)층""")
    private val AREA_RE = Regex("""([\d.]+)\s*m²""")
}
