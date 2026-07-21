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
 * v3 item detail → [AppFlat]. Prices in 만원 × 10_000 → KRW.
 * See tmp/kr/api/zigbang/NOTES.md.
 */
object ZigbangDetailMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private const val MANWON = 10_000.0

    fun parseDetail(raw: String, adType: AdType, base: AppFlat? = null): AppFlat? {
        val root = runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
            ?: return null
        val item = root["item"].asObjectOrNull() ?: return null
        return mapItem(root, item, adType, base)
    }

    fun mergeInto(base: AppFlat, raw: String): AppFlat =
        parseDetail(raw, base.getAdTypeNonNull(), base) ?: base

    private fun mapItem(
        root: JsonObject,
        item: JsonObject,
        adType: AdType,
        base: AppFlat?,
    ): AppFlat {
        val itemId = item["itemId"].longOrNull() ?: base?.adId ?: error("missing itemId")
        val serviceType = item["serviceType"].contentOrNull()
        val salesType = item["salesType"].contentOrNull()
        val effectiveAdType = salesTypeToAdType(salesType) ?: adType

        val priceObj = item["price"].asObjectOrNull()
        val depositMan = priceObj?.get("deposit").doubleOrNull()
            ?: priceObj?.get("deposit").intOrNull()?.toDouble()
        val rentMan = priceObj?.get("rent").doubleOrNull()
            ?: priceObj?.get("rent").intOrNull()?.toDouble()
        val salesMan = priceObj?.get("sales").doubleOrNull()
            ?: priceObj?.get("sales").intOrNull()?.toDouble()

        val (mainPrice, secondPrice) = when (effectiveAdType) {
            is AdType.SALE -> salesMan?.let { it * MANWON } to null
            is AdType.JEONSE -> depositMan?.let { it * MANWON } to null
            else -> {
                val monthly = rentMan?.let { it * MANWON }
                val deposit = depositMan?.let { it * MANWON }
                monthly to deposit
            }
        }

        val loc = item["location"].asObjectOrNull()
        val lat = loc?.get("lat").doubleOrNull() ?: base?.coordinates?.latitude
        val lng = loc?.get("lng").doubleOrNull() ?: base?.coordinates?.longitude
        val coords = if (lat != null && lng != null) {
            Coordinates(latitude = lat, longitude = lng)
        } else {
            base?.coordinates
        }

        val agent = root["agent"].asObjectOrNull()
        val realtor = root["realtor"].asObjectOrNull()
        val phones = listOfNotNull(
            agent?.get("agentPhone").contentOrNull(),
            realtor?.get("phone").contentOrNull(),
            realtor?.get("officePhone").contentOrNull(),
        ).distinct().ifEmpty { null }
        val ownerName = agent?.get("agentName").contentOrNull()
            ?: realtor?.get("name").contentOrNull()
            ?: base?.contactInformation?.ownerName
        val agentType = agent?.get("agentUserType").contentOrNull().orEmpty()
        val owner = when {
            agentType.contains("공인중개사") -> false
            phones != null -> false
            else -> base?.owner
        }

        val images = item["images"].asArrayOrNull()
            ?.mapNotNull { el ->
                // images[] may be plain URL strings or { url / src } objects
                el.contentOrNull()
                    ?: el.asObjectOrNull()?.get("url").contentOrNull()
                    ?: el.asObjectOrNull()?.get("src").contentOrNull()
            }
            ?.map { ZigbangFlatMapper.normalizeImageUrl(it) }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: base?.imageUrls

        val addrOrigin = item["addressOrigin"].asObjectOrNull()
        val district = listOfNotNull(
            addrOrigin?.get("local2").contentOrNull(),
            addrOrigin?.get("local3").contentOrNull(),
        ).joinToString(" ").ifBlank { base?.district }
        val address = addrOrigin?.get("fullText").contentOrNull()
            ?: addrOrigin?.get("localText").contentOrNull()
            ?: item["jibunAddress"].contentOrNull()
            ?: base?.address

        val areaObj = item["area"].asObjectOrNull()
        val totalArea = areaObj?.get("전용면적M2").doubleOrNull()
            ?: areaObj?.get("전용면적M2").intOrNull()?.toDouble()
            ?: base?.totalArea

        val floorObj = item["floor"].asObjectOrNull()
        val floor = floorObj?.get("floor").contentOrNull()?.toIntOrNull()
            ?: base?.floor
        val totalFloors = floorObj?.get("allFloors").contentOrNull()?.toIntOrNull()
            ?: base?.totalFloors

        val metro = root["subways"].asArrayOrNull()
            ?.firstOrNull()?.asObjectOrNull()?.get("name").contentOrNull()
            ?: base?.metroStation

        val title = item["title"].contentOrNull()?.stripHtmlToPlainText()
        val description = item["description"].contentOrNull()?.stripHtmlToPlainText()
            ?: title
            ?: base?.description

        val roomType = item["roomType"].contentOrNull().orEmpty()
        val rooms = parseRooms(roomType, serviceType.orEmpty()) ?: base?.rooms
        val isStudio = when {
            rooms == 1 -> true
            roomType.contains("원룸") || roomType.contains("오픈") -> true
            base?.isStudio == true -> true
            else -> false
        }

        val updated = item["updatedAt"].contentOrNull()
        val publishedAt = updated?.let { runCatching { parseZigbangInstant(it) }.getOrNull() }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: updated ?: base?.publishedAtUi

        val amenities = item["options"].asArrayOrNull()
            ?.mapNotNull { it.contentOrNull() }
            ?.takeIf { it.isNotEmpty() }

        val buildingImprovements = buildList {
            if (item["elevator"].asPrimitiveOrNull()?.booleanOrNull == true) add("elevator")
            item["parkingAvailableText"].contentOrNull()?.let { add(it) }
        }.ifEmpty { base?.buildingImprovements }

        return AppFlat(
            adId = itemId,
            adType = effectiveAdType,
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            contactInformation = ContactInformation(phones = phones, ownerName = ownerName),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.ZIGBANG,
            flatDetailUrl = ZigbangFlatMapper.detailUrl(itemId, serviceType),
            publishedAt = publishedAt ?: base?.publishedAt,
            publishedAtServer = updated ?: base?.publishedAtServer,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            secondPrice = secondPrice ?: base?.secondPrice,
            mainPrice = mainPrice ?: base?.mainPrice,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = metro,
            description = description,
            yearBuilt = base?.yearBuilt,
            totalArea = totalArea,
            livingArea = base?.livingArea,
            kitchenArea = base?.kitchenArea,
            floor = floor,
            totalFloors = totalFloors,
            sleepingPlaces = base?.sleepingPlaces,
            isStudio = isStudio,
            bathroomType = item["bathroomCount"].contentOrNull()?.let { "$it" }
                ?: base?.bathroomType,
            balcony = base?.balcony,
            repairType = base?.repairType,
            condition = base?.condition,
            windowDirections = item["roomDirection"].contentOrNull()?.let { listOf(it) }
                ?: base?.windowDirections,
            buildingImprovements = buildingImprovements,
            prepaymentType = base?.prepaymentType,
            amenities = amenities ?: base?.amenities,
            kitchenEquipment = base?.kitchenEquipment,
            forWhom = base?.forWhom,
            parkingInfo = item["parkingAvailableText"].contentOrNull() ?: base?.parkingInfo,
            owner = owner,
        )
    }

    private fun salesTypeToAdType(salesType: String?): AdType? = when (salesType?.trim()) {
        "매매" -> AdType.SALE
        "전세" -> AdType.JEONSE
        "월세" -> AdType.RENT
        else -> null
    }

    /** Map Korean room labels → room count (원룸=1, 투룸=2, …). */
    internal fun parseRooms(roomType: String, serviceType: String = ""): Int? {
        val text = "$roomType $serviceType"
        return when {
            text.contains("쓰리룸") || text.contains("3룸") || text.contains("쓰리") -> 3
            text.contains("포룸") || text.contains("4룸") -> 4
            text.contains("투룸") || text.contains("2룸") -> 2
            text.contains("원룸") || text.contains("1.5") || text.contains("오픈형") -> 1
            text.contains("오피스텔") -> 1
            else -> Regex("""(\d+)\s*룸""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    /** `2026-07-20 20:23:59` (KST wall clock; treat as local). */
    private fun parseZigbangInstant(raw: String): Instant {
        val normalized = raw.trim().replace(' ', 'T')
        return Instant.parse("${normalized}Z")
    }
}
