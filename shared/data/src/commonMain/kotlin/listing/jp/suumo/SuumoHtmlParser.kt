package listing.jp.suumo

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import listing.jp.isMadoriStudio
import listing.jp.parseAreaSqm
import listing.jp.parseMadoriRooms
import listing.jp.parseYenLabel
import listing.jp.stableAdId
import utils.stripHtmlToPlainText

/**
 * SUUMO SSR HTML — rent cassette list + used-mansion sale property_unit cards.
 * List has no coords; sale detail may expose map lat/lng. See tmp/jp/api/suumo/NOTES.md.
 */
object SuumoHtmlParser {
    private val rentRowRe =
        Regex("""<tr class="js-cassette_link">([\s\S]*?)</tr>""", RegexOption.IGNORE_CASE)
    private val saleUnitRe =
        Regex("""<div class="property_unit(?:\s[^"]*)?">""", RegexOption.IGNORE_CASE)

    private val bukkenCodeRe =
        Regex("""value="(\d{10,12})"""")
    private val detailHrefRe =
        Regex("""href="(/chintai/jnc_\d+/\?bc=\d+)"""")
    private val rentPriceRe =
        Regex("""cassetteitem_price--rent[^"]*"[^>]*>[\s\S]*?>([^<]+)<""")
    private val adminPriceRe =
        Regex("""cassetteitem_price--administration[^"]*"[^>]*>([^<]+)<""")
    private val depositPriceRe =
        Regex("""cassetteitem_price--deposit[^"]*"[^>]*>([^<]+)<""")
    private val gratuityPriceRe =
        Regex("""cassetteitem_price--gratuity[^"]*"[^>]*>([^<]+)<""")
    private val madoriRe =
        Regex("""class="cassetteitem_madori"[^>]*>([^<]+)<""")
    private val areaRe =
        Regex("""class="cassetteitem_menseki"[^>]*>([^<]+)<""")
    private val addressRe =
        Regex("""class="cassetteitem_detail-col1"[^>]*>([^<]+)<""")
    private val stationRe =
        Regex("""class="cassetteitem_detail-text"[^>]*>([^<]+)<""")
    private val titleRe =
        Regex("""class="cassetteitem_content-title"[^>]*>([^<]+)<""")
    private val floorCellRe =
        Regex("""<td>\s*(\d+階)\s*</td>""")

    private val saleDetailRe =
        Regex("""href="(/ms/chuko/[^"]+/nc_\d+/)"""")
    private val saleNcRe =
        Regex("""nc_(\d+)""")
    private val salePriceRe =
        Regex("""<dt class="dottable-vm">販売価格</dt>\s*<dd class="dottable-vm">\s*<span class="dottable-value">([^<]+)</span>""")
    private val saleAddressRe =
        Regex("""<dt>所在地</dt>\s*<dd>([^<]+)</dd>""")
    private val saleStationRe =
        Regex("""<dt>沿線・駅</dt>\s*<dd>([^<]+)</dd>""")
    private val saleNameRe =
        Regex("""<dt class="dottable-vm">物件名</dt>\s*<dd class="dottable-vm">([^<]+)</dd>""")
    private val saleMadoriRe =
        Regex("""<dt>間取り</dt>\s*<dd>([^<]+)</dd>""")
    private val saleAreaRe =
        Regex("""<dt>専有面積</dt>\s*<dd>([^<]+)</dd>""")
    private val saleTitleRe =
        Regex("""class="property_unit-title"[\s\S]*?<a href="[^"]*"[^>]*>([^<]+)</a>""")

    private val imgRelRe =
        Regex("""rel="(https://img01\.suumo\.com/[^"]+)"""")
    private val imgDataSrcRe =
        Regex("""data-src="(https://img01\.suumo\.com/[^"]+)""")
    private val imgFrontRe =
        Regex(
            """(?:rel|data-src|src)="(https?://(?:img01\.)?suumo\.(?:jp|com)/[^"]+\.(?:jpg|jpeg|png)[^"]*)"""",
            RegexOption.IGNORE_CASE
        )

    private val detailRentEmphasisRe =
        Regex("""class="property_view_note-emphasis"[^>]*>([^<]+)<""")
    private val detailAdminRe =
        Regex("""管理費[^:]*:&nbsp;([^<]+)<""")
    private val detailDepositRe =
        Regex("""敷金:&nbsp;([^<]+)<""")
    private val detailGratuityRe =
        Regex("""礼金:&nbsp;([^<]+)<""")

    private val mapCoordsRe =
        Regex("""lat:'([\d.]+)',\s*lng:'([\d.]+)'""")
    private val phoneRe =
        Regex("""(?:TEL:)?(0120-\d+)""", RegexOption.IGNORE_CASE)
    private val descBlockRe =
        Regex("""<div class="property_view_detail[^"]*">([\s\S]*?)</div>\s*<div class="property_view""")

    fun parseSearch(html: String, adType: AdType): List<AppFlat> =
        when (adType) {
            is AdType.SALE -> parseSaleSearch(html)
            else -> parseRentSearch(html)
        }

    fun mergeDetail(base: AppFlat, html: String): AppFlat =
        when (base.adType) {
            is AdType.SALE -> mergeSaleDetail(base, html)
            else -> mergeRentDetail(base, html)
        }

    fun listStub(adId: Long, adType: AdType = AdType.RENT): AppFlat = AppFlat(
        adId = adId,
        adType = adType,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.SUUMO,
        flatDetailUrl = "${SuumoApiClient.BASE}/",
        publishedAt = null,
        publishedAtServer = null,
        publishedAtUi = null,
        imageUrls = null,
        mainPrice = null,
        secondPrice = null,
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

    private fun parseRentSearch(html: String): List<AppFlat> {
        if (!html.contains("js-cassette_link")) return emptyList()
        val rowMatches = rentRowRe.findAll(html).toList()
        if (rowMatches.isEmpty()) return emptyList()
        return rowMatches.mapNotNull { rowMatch ->
            val rowStart = rowMatch.range.first
            val contextStart = (rowStart - 8_000).coerceAtLeast(0)
            val context = html.substring(contextStart, rowStart)
            val address = decode(addressRe.find(context)?.groupValues?.get(1))
            val metro = stationRe.findAll(context).map { decode(it.groupValues[1]) }.firstOrNull()
            val title = decode(titleRe.find(context)?.groupValues?.get(1))
            val buildingImage = imgRelRe.find(context)?.groupValues?.get(1)?.let(::decode)
            runCatching {
                mapRentRow(
                    rowHtml = rowMatch.groupValues[1],
                    address = address,
                    metro = metro,
                    title = title,
                    buildingImage = buildingImage,
                )
            }.getOrNull()
        }.distinctBy { it.adId }
    }

    private fun mapRentRow(
        rowHtml: String,
        address: String?,
        metro: String?,
        title: String?,
        buildingImage: String?,
    ): AppFlat? {
        val bukkenCode = bukkenCodeRe.find(rowHtml)?.groupValues?.get(1) ?: return null
        val detailPath = detailHrefRe.find(rowHtml)?.groupValues?.get(1)
        val detailUrl = detailPath?.let { "${SuumoApiClient.BASE}$it" } ?: "${SuumoApiClient.BASE}/"
        val rentLabel = rentPriceRe.find(rowHtml)?.groupValues?.get(1)
        val adminLabel = adminPriceRe.find(rowHtml)?.groupValues?.get(1)
        val depositLabel = depositPriceRe.find(rowHtml)?.groupValues?.get(1)
        val gratuityLabel = gratuityPriceRe.find(rowHtml)?.groupValues?.get(1)
        val madori = decode(madoriRe.find(rowHtml)?.groupValues?.get(1))
        val areaLabel = areaRe.find(rowHtml)?.groupValues?.get(1)
        val floorLabel = floorCellRe.find(rowHtml)?.groupValues?.get(1)
        val floor = floorLabel?.filter { it.isDigit() }?.toIntOrNull()
        val rowImage = imgRelRe.find(rowHtml)?.groupValues?.get(1)?.let(::decode)
        val images = listOfNotNull(rowImage, buildingImage).distinct().ifEmpty { null }

        val secondaryParts = listOfNotNull(
            depositLabel?.takeIf { it.isNotBlank() && it != "－" }?.let { "敷金: $it" },
            gratuityLabel?.takeIf { it.isNotBlank() && it != "－" }?.let { "礼金: $it" },
        )
        val description = listOfNotNull(
            title,
            secondaryParts.joinToString(" · ").ifBlank { null },
        ).joinToString("\n").ifBlank { null }

        return AppFlat(
            adId = stableAdId(bukkenCode),
            adType = AdType.RENT,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.SUUMO,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = images,
            mainPrice = parseYenLabel(rentLabel),
            secondPrice = parseYenLabel(adminLabel),
            totalArea = parseAreaSqm(areaLabel),
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
            description = description,
            yearBuilt = null,
        )
    }

    private fun parseSaleSearch(html: String): List<AppFlat> {
        if (!html.contains("property_unit")) return emptyList()
        val starts = saleUnitRe.findAll(html).map { it.range.first }.toList()
        if (starts.isEmpty()) return emptyList()
        return starts.mapIndexedNotNull { index, start ->
            val end = starts.getOrNull(index + 1) ?: (start + 15_000)
            val chunk = html.substring(start, minOf(end, html.length))
            runCatching { mapSaleUnit(chunk) }.getOrNull()
        }.distinctBy { it.adId }
    }

    private fun mapSaleUnit(chunk: String): AppFlat? {
        val detailPath = saleDetailRe.find(chunk)?.groupValues?.get(1) ?: return null
        val ncId = saleNcRe.find(detailPath)?.groupValues?.get(1) ?: return null
        val detailUrl = "${SuumoApiClient.BASE}$detailPath"
        val priceLabel = salePriceRe.find(chunk)?.groupValues?.get(1)
        val address = decode(saleAddressRe.find(chunk)?.groupValues?.get(1))
        val metro = decode(saleStationRe.find(chunk)?.groupValues?.get(1))
        val buildingName = decode(saleNameRe.find(chunk)?.groupValues?.get(1))
        val madori = decode(saleMadoriRe.find(chunk)?.groupValues?.get(1))
        val areaLabel = saleAreaRe.find(chunk)?.groupValues?.get(1)
        val headline = decode(saleTitleRe.find(chunk)?.groupValues?.get(1))
        val image = imgRelRe.find(chunk)?.groupValues?.get(1)?.let(::decode)

        return AppFlat(
            adId = stableAdId(ncId),
            adType = AdType.SALE,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.SUUMO,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = image?.let { listOf(it) },
            mainPrice = parseYenLabel(priceLabel),
            secondPrice = null,
            totalArea = parseAreaSqm(areaLabel),
            livingArea = null,
            kitchenArea = null,
            floor = null,
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
            description = listOfNotNull(buildingName, headline).joinToString("\n").ifBlank { null },
            yearBuilt = null,
        )
    }

    private fun mergeRentDetail(base: AppFlat, html: String): AppFlat {
        val rentLabel = detailRentEmphasisRe.find(html)?.groupValues?.get(1)
        val adminLabel = detailAdminRe.find(html)?.groupValues?.get(1)
        val depositLabel = detailDepositRe.find(html)?.groupValues?.get(1)
        val gratuityLabel = detailGratuityRe.find(html)?.groupValues?.get(1)
        val images = imgDataSrcRe.findAll(html).mapNotNull { decode(it.groupValues[1]) }
            .filter { !it.contains("_t.jpg") && !it.contains("_rt.jpg") }
            .distinct().take(20).toList()
            .ifEmpty {
                imgFrontRe.findAll(html).mapNotNull { decode(it.groupValues[1]) }
                    .filter { it.contains("/gazo/") }
                    .distinct().take(20).toList()
            }
            .ifEmpty { base.imageUrls.orEmpty() }
        val secondaryParts = listOfNotNull(
            depositLabel?.takeIf { it.isNotBlank() && it != "-" && it != "－" }?.let { "敷金: $it" },
            gratuityLabel?.takeIf { it.isNotBlank() && it != "-" && it != "－" }
                ?.let { "礼金: $it" },
        )
        val description = listOfNotNull(
            base.description?.lineSequence()?.firstOrNull(),
            secondaryParts.joinToString(" · ").ifBlank { null },
            descBlockRe.find(html)?.groupValues?.get(1)?.stripHtmlToPlainText()
                ?.takeIf { it.length > 40 },
        ).joinToString("\n").ifBlank { base.description }

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            mainPrice = parseYenLabel(rentLabel) ?: base.mainPrice,
            secondPrice = parseYenLabel(adminLabel) ?: base.secondPrice,
            imageUrls = images.ifEmpty { null },
            description = description,
        )
    }

    private fun mergeSaleDetail(base: AppFlat, html: String): AppFlat {
        val priceLabel = salePriceRe.find(html)?.groupValues?.get(1)
        val coordsMatch = mapCoordsRe.find(html)
        val coordinates = coordsMatch?.let {
            val lat = it.groupValues[1].toDoubleOrNull()
            val lng = it.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null) Coordinates(lat, lng) else null
        } ?: base.coordinates
        val phones = phoneRe.findAll(html).map { it.groupValues[1] }.distinct().toList()
        val images = imgFrontRe.findAll(html).mapNotNull { decode(it.groupValues[1]) }
            .filter { it.contains("/gazo/") || it.contains("resizeImage") }
            .distinct().take(20).toList()
            .ifEmpty { base.imageUrls.orEmpty() }
        val madori = decode(saleMadoriRe.find(html)?.groupValues?.get(1))
        val areaLabel = saleAreaRe.find(html)?.groupValues?.get(1)

        return base.copy(
            flatDevInfo = FlatDevInfo(
                isDetailData = true,
                isDetailLoaded = true,
                coordsEnriched = coordinates != null,
            ),
            mainPrice = parseYenLabel(priceLabel) ?: base.mainPrice,
            coordinates = coordinates,
            rooms = parseMadoriRooms(madori) ?: base.rooms,
            totalArea = parseAreaSqm(areaLabel) ?: base.totalArea,
            imageUrls = images.ifEmpty { null },
            contactInformation = ContactInformation(
                phones = phones.ifEmpty { base.contactInformation?.phones },
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    private fun decode(raw: String?): String? =
        raw?.replace("&amp;", "&")?.replace("&nbsp;", " ")?.trim()?.takeIf { it.isNotBlank() }
}
