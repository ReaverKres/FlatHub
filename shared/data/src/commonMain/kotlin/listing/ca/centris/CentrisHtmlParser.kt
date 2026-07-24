package listing.ca.centris

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import listing.ca.isCaSaleDeal
import listing.ca.stableCaAdId
import utils.stripHtmlToPlainText

/**
 * Centris.ca SSR list + detail → [AppFlat]. CAD → [AppFlat.mainPrice].
 * See tmp/ca/api/centris/NOTES.md.
 */
object CentrisHtmlParser {
    private const val SITE_ORIGIN = "https://www.centris.ca"

    private val cardSplit = "property-thumbnail-item thumbnailItem"
    private val sortSeedRe = Regex("""id="sortSeed">(\d+)<""")
    private val skuRe = Regex("""itemprop="sku"\s+content="(\d+)"""")
    private val detailPathRe = Regex(
        """class="a-more-detail"\s+href="(/en/[^"]+/(\d+))"""",
        RegexOption.IGNORE_CASE,
    )
    private val priceMetaRe = Regex("""itemprop="price"\s+content="([\d.]+)"""")
    private val streetRe = Regex("""class="address"[\s\S]*?<div>([^<]+)</div>""")
    private val areaRe = Regex("""class="address"[\s\S]*?<div>[^<]+</div>\s*<div>([^<]+)</div>""")
    private val categoryRe = Regex("""class="category"[\s\S]*?<div>\s*([^<]+)\s*</div>""")
    private val cacRe = Regex("""class='cac'>(\d+)<""")
    private val sdbRe = Regex("""class='sdb'>(\d+)<""")
    private val latRe = Regex("""data-lat="([\d.\-]+)"""")
    private val lngRe = Regex("""data-lng="([\d.\-]+)"""")
    private val imageRe = Regex("""itemprop="image"\s+src="([^"]+)"""")
    private val telRe = Regex("""href="tel:([^"]+)"""")
    private val detailLatRe = Regex("""id="PropertyLat"[^>]*>([\d.\-]+)<""")
    private val detailLngRe = Regex("""id="PropertyLng"[^>]*>([\d.\-]+)<""")
    private val detailLatMetaRe = Regex("""itemprop="latitude"\s+content="([\d.\-]+)"""")
    private val detailLngMetaRe = Regex("""itemprop="longitude"\s+content="([\d.\-]+)"""")
    private val mosaicPhotoUrlsRe = Regex(
        """window\.MosaicPhotoUrls\s*=\s*\[(.*?)\];""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val mosaicPhotoUrlRe = Regex(""""(https://mspublic\.centris\.ca/media\.ashx[^"]+)"""")
    private val dataPhotoUrlsRe = Regex("""data-photo-urls="([^"]+)"""")
    private val ogImageRe = Regex(
        """property="og:image"\s+content="(https://mspublic\.centris\.ca/media\.ashx[^"]+)"""",
    )
    private val metaDescriptionRe = Regex("""<meta name="description"\s+content="([^"]+)"""")
    private val ogDescriptionRe = Regex("""property="og:description"\s+content="([^"]+)"""")
    private val legacyDescriptionRe = Regex(
        """id="propertyDescription"[\s\S]*?<div[^>]*>([\s\S]*?)</div>""",
        RegexOption.IGNORE_CASE,
    )

    fun extractSortSeed(html: String): String? = sortSeedRe.find(html)?.groupValues?.get(1)

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        if (!html.contains(cardSplit)) return emptyList()
        val indices = cardSplit.toRegex().findAll(html).map { it.range.first }.toList()
        if (indices.isEmpty()) return emptyList()
        return indices.mapIndexedNotNull { index, start ->
            val end = indices.getOrNull(index + 1) ?: minOf(start + 15_000, html.length)
            val chunk = html.substring(start, end)
            runCatching { mapCard(chunk, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val lat = detailLatRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: detailLatMetaRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val lng = detailLngRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: detailLngMetaRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates
        val phones = telRe.findAll(html)
            .map { it.groupValues[1].filter { ch -> ch.isDigit() || ch == '+' } }
            .filter { it.length >= 10 }
            .distinct()
            .take(3)
            .toList()
            .ifEmpty { null }
        val description = parseDetailDescription(html) ?: base.description
        val imageUrls = parseDetailPhotos(html).ifEmpty { base.imageUrls.orEmpty() }

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            coordinates = coords,
            imageUrls = imageUrls.distinct().ifEmpty { null },
            contactInformation = ContactInformation(
                phones = phones,
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    /** Full gallery from MosaicPhotoUrls / data-photo-urls (list cards only expose one thumbnail). */
    internal fun parseDetailPhotos(html: String): List<String> {
        mosaicPhotoUrlsRe.find(html)?.groupValues?.get(1)?.let { block ->
            val fromMosaic = mosaicPhotoUrlRe.findAll(block)
                .map { normalizePhotoUrl(it.groupValues[1]) }
                .toList()
            if (fromMosaic.isNotEmpty()) return fromMosaic.distinct()
        }

        dataPhotoUrlsRe.find(html)?.groupValues?.get(1)
            ?.split(',')
            ?.map { normalizePhotoUrl(it.trim()) }
            ?.filter { it.startsWith("https://") }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?.let { return it }

        return ogImageRe.findAll(html)
            .map { normalizePhotoUrl(it.groupValues[1]) }
            .distinct()
            .toList()
    }

    /**
     * Centris SSR embeds remarks in meta tags; [property-public-remarks-container] is filled by JS.
     */
    internal fun parseDetailDescription(html: String): String? {
        metaDescriptionRe.find(html)?.groupValues?.get(1)
            ?.decodeHtml()
            ?.trim()
            ?.replace(Regex("""\.{2,}$"""), "")
            ?.trimEnd()
            ?.takeIf { it.length > 40 }
            ?.let { return it }

        ogDescriptionRe.find(html)?.groupValues?.get(1)
            ?.decodeHtml()
            ?.trim()
            ?.replace(Regex("""\.{2,}$"""), "")
            ?.trimEnd()
            ?.takeIf { it.length > 40 }
            ?.let { return it }

        return legacyDescriptionRe.find(html)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 40 }
    }

    private fun normalizePhotoUrl(raw: String): String =
        raw.decodeHtml()
            .replace("\\u0026", "&")
            .replace(Regex("""&w=\d+&h=\d+"""), "&w=1260&h=1024")

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.CENTRIS,
        flatDetailUrl = detailUrl ?: SITE_ORIGIN,
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
        owner = false,
    )

    private fun mapCard(chunk: String, adType: AdType): AppFlat? {
        val mls = skuRe.find(chunk)?.groupValues?.get(1)
            ?: detailPathRe.find(chunk)?.groupValues?.get(2)
            ?: return null
        val adId = mls.toLongOrNull() ?: stableCaAdId(mls)

        val path = detailPathRe.find(chunk)?.groupValues?.get(1)
        val detailUrl = path?.let { "$SITE_ORIGIN$it" } ?: "$SITE_ORIGIN/en/$mls"

        val price = priceMetaRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
        val street = streetRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val borough = areaRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val category = categoryRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val address = listOfNotNull(street, borough).joinToString(", ").ifBlank { null }

        val lat = latRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
        val lng = lngRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null

        val rooms = cacRe.find(chunk)?.groupValues?.get(1)?.toIntOrNull()
        val image = imageRe.find(chunk)?.groupValues?.get(1)?.replace("&amp;", "&")

        val mappedAdType = if (adType.isCaSaleDeal()) AdType.SALE else AdType.RENT

        return AppFlat(
            adId = adId,
            adType = mappedAdType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.CENTRIS,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = image?.let { listOf(it) },
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = borough,
            address = address,
            metroStation = null,
            description = category ?: borough,
            yearBuilt = null,
            totalArea = null,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = null,
            bathroomType = sdbRe.find(chunk)?.groupValues?.get(1)?.let { "$it bath" },
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
            owner = false,
        )
    }

    private fun String.decodeHtml(): String =
        replace("\\u0026", "&")
            .replace("&#xE9;", "é")
            .replace("&#xE8;", "è")
            .replace("&#xEA;", "ê")
            .replace("&#xE0;", "à")
            .replace("&#xE2;", "â")
            .replace("&#xEE;", "î")
            .replace("&#xEF;", "ï")
            .replace("&#xF4;", "ô")
            .replace("&#xFB;", "û")
            .replace("&#xE7;", "ç")
            .replace("&#xB2;", "²")
            .replace("&#x2019;", "'")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .trim()
}
