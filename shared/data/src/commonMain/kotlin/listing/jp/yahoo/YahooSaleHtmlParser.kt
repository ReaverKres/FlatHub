package listing.jp.yahoo

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.jp.isMadoriStudio
import listing.jp.parseAreaSqm
import listing.jp.parseMadoriRooms
import listing.jp.parseYenLabel
import listing.jp.stableAdId

/**
 * Yahoo!不動産 used-mansion sale SSR HTML — list cassettes + detail context.
 * See tmp/jp/api/yahoo/NOTES.md
 */
object YahooSaleHtmlParser {
    private val json = Json { ignoreUnknownKeys = true }

    private val cpItemRe =
        Regex("""<li class="ListCassette2__cpItem _listCassette">([\s\S]*?)</li>""")
    private val detailRe =
        Regex("""/used/mansion/detail_corp/(b\d+)/""")
    private val buildingNameRe =
        Regex("""ListCassette2__ttl__txt">([^<]+)<""")
    private val infoDtlRe =
        Regex("""ListCassette2__info__dtl">([^<]+)<""")
    private val roomTitleRe =
        Regex("""ListCassette2__ttl__txt--inner">([^<]+)<""")
    private val priceRe =
        Regex("""ListCassette2__info__price">\s*([^<\n]+)""")
    private val phoneRe =
        Regex("""ListCassette2__contact__phone__number">([^<]+)""")
    private val listImgRe =
        Regex("""ListCassette2__imgM__innerFit__img"[^>]*src="([^"]+)"""")
    private val detailImgRe =
        Regex(
            """(?:src|srcset)="(https://realestate-pctr\.c\.yimg\.jp/[^"]+\.(?:jpg|jpeg|png|webp)[^"]*)"""",
            RegexOption.IGNORE_CASE
        )
    private val floorRe =
        Regex("""(\d+)階""")
    private val ssrContextRe =
        Regex("""window\.__SERVER_SIDE_CONTEXT__\s*=\s*(\{.*?\});\s*/\*""")

    fun parseSearch(html: String): List<AppFlat> {
        if (!html.contains("detail_corp")) return emptyList()
        val seen = mutableSetOf<String>()
        return cpItemRe.findAll(html).mapNotNull { match ->
            val chunk = match.groupValues[1]
            val buildingId = detailRe.find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
            if (!seen.add(buildingId)) return@mapNotNull null
            val contextStart = (match.range.first - 12_000).coerceAtLeast(0)
            val context = html.substring(contextStart, match.range.first)
            val roomTitle = decode(roomTitleRe.find(context)?.groupValues?.get(1))
            val buildingName = decode(buildingNameRe.find(context)?.groupValues?.get(1))
            val infoDtls =
                infoDtlRe.findAll(context).mapNotNull { decode(it.groupValues[1]) }.toList()
            val metro = infoDtls.firstOrNull { it.contains("駅") || it.contains("徒歩") }
            val address = infoDtls.firstOrNull {
                it.contains("都") || it.contains("府") || it.contains("県") || it.contains("道")
            }
            mapSaleListing(
                buildingId = buildingId,
                roomTitle = roomTitle,
                buildingName = buildingName,
                metro = metro,
                address = address,
                priceLabel = priceRe.find(chunk)?.groupValues?.get(1),
                phone = phoneRe.find(chunk)?.groupValues?.get(1),
                image = listImgRe.find(chunk)?.groupValues?.get(1),
            )
        }.distinctBy { it.adId }.toList()
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val context = extractServerSideContext(html)
        val lat = context?.get("lat")?.contentOrNull()?.toDoubleOrNull()
        val lon = context?.get("lon")?.contentOrNull()?.toDoubleOrNull()
        val coordinates = if (lat != null && lon != null) {
            Coordinates(lat, lon)
        } else {
            base.coordinates
        }
        val buildingName =
            context?.get("buildingName")?.contentOrNull()?.trim()?.takeIf { it.isNotBlank() }
        val address = context?.get("address")?.contentOrNull()?.trim()?.takeIf { it.isNotBlank() }
        val phones = phoneRe.findAll(html).map { it.groupValues[1].trim() }.distinct().toList()
        val images = detailImgRe.findAll(html)
            .mapNotNull { decode(it.groupValues[1]) }
            .filter { !it.contains("noimage") && !it.contains("shop_image") }
            .distinct()
            .take(20)
            .toList()
            .ifEmpty { base.imageUrls.orEmpty() }
        val priceLabel = priceRe.find(html)?.groupValues?.get(1)
        val roomTitle = decode(roomTitleRe.find(html)?.groupValues?.get(1))
        val madori = roomTitle?.substringAfterLast('/')?.trim()
        val floor = roomTitle?.let { floorRe.find(it)?.groupValues?.get(1)?.toIntOrNull() }

        return base.copy(
            flatDevInfo = FlatDevInfo(
                isDetailData = true,
                isDetailLoaded = true,
                coordsEnriched = coordinates != null && base.coordinates == null,
            ),
            coordinates = coordinates,
            address = address ?: base.address,
            metroStation = base.metroStation,
            mainPrice = parseYenLabel(priceLabel) ?: base.mainPrice,
            floor = floor ?: base.floor,
            rooms = parseMadoriRooms(madori) ?: base.rooms,
            isStudio = isMadoriStudio(madori) || base.isStudio == true,
            totalArea = roomTitle?.let { parseAreaSqm(it) } ?: base.totalArea,
            imageUrls = images.ifEmpty { null },
            description = listOfNotNull(buildingName, roomTitle?.substringAfterLast('/')?.trim())
                .joinToString("\n")
                .ifBlank { base.description },
            contactInformation = ContactInformation(
                phones = phones.ifEmpty { base.contactInformation?.phones },
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    fun listStub(adId: Long): AppFlat = YahooFlatMapper.listStub(adId, AdType.SALE)

    private fun mapSaleListing(
        buildingId: String,
        roomTitle: String?,
        buildingName: String?,
        metro: String?,
        address: String?,
        priceLabel: String?,
        phone: String?,
        image: String?,
    ): AppFlat {
        val detailUrl = "${YahooApiClient.BASE}/used/mansion/detail_corp/$buildingId/"
        val madori = roomTitle?.substringAfterLast('/')?.trim()
        val floor = roomTitle?.let { floorRe.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val phones = phone?.trim()?.takeIf { it.isNotBlank() }?.let { listOf(it) }
        val images = image?.let { decode(it) }?.let { listOf(it) }

        return AppFlat(
            adId = stableAdId(buildingId),
            adType = AdType.SALE,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = phones, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.YAHOO_RE,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = images,
            mainPrice = parseYenLabel(priceLabel),
            secondPrice = null,
            totalArea = roomTitle?.let { parseAreaSqm(it) },
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = isMadoriStudio(madori),
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
            rooms = parseMadoriRooms(madori),
            district = null,
            address = address,
            metroStation = metro,
            description = listOfNotNull(buildingName, madori).joinToString("\n").ifBlank { null },
            yearBuilt = null,
        )
    }

    private fun extractServerSideContext(html: String): JsonObject? {
        val raw = ssrContextRe.find(html)?.groupValues?.get(1) ?: return null
        return runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
    }

    private fun decode(raw: String?): String? =
        raw?.replace("&amp;", "&")
            ?.replace("&nbsp;", " ")
            ?.replace(Regex("<[^>]+>"), "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
}
