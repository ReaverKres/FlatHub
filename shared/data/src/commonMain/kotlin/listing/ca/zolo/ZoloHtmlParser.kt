package listing.ca.zolo

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
 * Zolo.ca gallery SSR → [AppFlat]. CAD → [AppFlat.mainPrice]. Sqft on list.
 * See tmp/ca/api/zolo/NOTES.md.
 */
object ZoloHtmlParser {
    private const val SITE_ORIGIN = "https://www.zolo.ca"

    private val cardSplit = "<article class=\"card-listing"
    private val urlRe = Regex(
        """href="(https://www\.zolo\.ca/[^"]+)"\s+class="(?:xs-block[^"]*card-listing--image-link|tile-overlay-link)""",
        RegexOption.IGNORE_CASE,
    )
    private val imageIdRe = Regex("""class="card-listing--img[^"]*"\s+id="(\d+)"""")
    private val streetRe = Regex("""itemprop="streetAddress">([^<]+)<""")
    private val cityRe = Regex("""itemprop="addressLocality">([^<]+)<""")
    private val provinceRe = Regex("""itemprop="addressRegion">([^<]+)<""")
    private val neighbourhoodRe = Regex("""class="neighbourhood"[^>]*>.*?&bull;([^<]+)<""")
    private val latRe = Regex("""itemprop="latitude"\s+content="([^"]+)"""")
    private val lngRe = Regex("""itemprop="longitude"\s+content="([^"]+)"""")
    private val priceRe = Regex("""itemprop="price"\s+value="(\d+)"""")
    private val priceAltRe = Regex("""itemprop="price"\s+value="(\d+)">([\d,]+)<""")
    private val bedsRe = Regex("""(\d+)\s+bed\b""", RegexOption.IGNORE_CASE)
    private val sqftRe = Regex("""([\d,]+(?:\s*-\s*[\d,]+)?)\s*sqft""", RegexOption.IGNORE_CASE)
    private val imgRe = Regex("""src="(https://photos\.zolo\.ca/[^"]+)"""")
    private val galleryZoomRe = Regex("""data-zoom-src="(https://photos\.zolo\.ca/[^"]+)"""")
    private val twitterImageRe =
        Regex("""twitter:image\d*"\s+content="(https://photos\.zolo\.ca/[^"]+)"""")
    private val ogImageRe =
        Regex("""property="og:image"\s+content="(https://photos\.zolo\.ca/[^"]+)"""")
    private val zoloDataLatRe = Regex("""propertyLat\s*:\s*([\d.\-]+)""")
    private val zoloDataLngRe = Regex("""propertyLng\s*:\s*([\d.\-]+)""")
    private val listingDescriptionRe = Regex(
        """section-listing-content-pad[\s\S]*?<span class="priv">\s*<p>\s*([\s\S]*?)</p>""",
        RegexOption.IGNORE_CASE,
    )
    private val zoloBoilerplateRe = Regex(
        """\b(is currently for (?:rent|sale) and has been available on Zolo\.ca)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val addedDateRe = Regex(
        """<dt class="column-label">Added</dt>\s*<dd class="column-value"><span class="priv">([^<]+)</span>""",
        RegexOption.IGNORE_CASE,
    )
    private val telRe = Regex("""href="tel:([^"]+)"""")

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        if (!html.contains("card-listing")) return emptyList()
        val chunks = html.split(cardSplit).drop(1)
        return chunks.mapNotNull { tail ->
            val chunk = cardSplit + tail
            runCatching { mapCard(chunk, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val lat = zoloDataLatRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val lng = zoloDataLngRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates
        val description = parseDetailDescription(html) ?: base.description
        val imageUrls = parseDetailPhotos(html).ifEmpty { base.imageUrls.orEmpty() }
        val publishedAtUi = addedDateRe.find(html)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val phones = telRe.findAll(html)
            .map { it.groupValues[1].filter { ch -> ch.isDigit() || ch == '+' } }
            .filter { it.length >= 10 }
            .distinct()
            .take(2)
            .toList()
            .ifEmpty { null }

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            coordinates = coords,
            imageUrls = imageUrls.distinct().ifEmpty { null },
            publishedAtUi = publishedAtUi ?: base.publishedAtUi,
            publishedAtServer = publishedAtUi ?: base.publishedAtServer,
            contactInformation = ContactInformation(
                phones = phones ?: base.contactInformation?.phones,
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    /** Full gallery from PDP modal + meta tags (list cards only expose one thumbnail). */
    internal fun parseDetailPhotos(html: String): List<String> {
        val galleryHtml = extractGalleryModal(html)
        val fromModal = galleryZoomRe.findAll(galleryHtml)
            .map { normalizePhotoUrl(it.groupValues[1]) }
            .toList()
        if (fromModal.isNotEmpty()) return fromModal.distinct()

        val fromTwitter = twitterImageRe.findAll(html)
            .map { normalizePhotoUrl(it.groupValues[1]) }
            .toList()
        if (fromTwitter.isNotEmpty()) return fromTwitter.distinct()

        return ogImageRe.findAll(html)
            .map { normalizePhotoUrl(it.groupValues[1]) }
            .distinct()
            .toList()
    }

    internal fun parseDetailDescription(html: String): String? {
        listingDescriptionRe.find(html)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 40 && !zoloBoilerplateRe.containsMatchIn(it) }
            ?.let { return it }

        // Fallback: first non-boilerplate paragraph in listing body.
        val bodyStart = html.indexOf("section-listing-content-pad")
        if (bodyStart < 0) return null
        val body = html.substring(bodyStart, (bodyStart + 12_000).coerceAtMost(html.length))
        val paragraphRe = Regex("""<p>\s*([\s\S]*?)</p>""", RegexOption.IGNORE_CASE)
        return paragraphRe.findAll(body)
            .mapNotNull { match ->
                match.groupValues.getOrNull(1)
                    ?.stripHtmlToPlainText()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
            }
            .firstOrNull { text ->
                text.length > 40 &&
                        !zoloBoilerplateRe.containsMatchIn(text) &&
                        !text.startsWith("We do not have information on any open houses")
            }
    }

    private fun extractGalleryModal(html: String): String {
        val start = html.indexOf("id=\"listingGalleryModal\"")
        if (start < 0) return html
        val end = html.indexOf("</section>", start)
        return if (end > start) html.substring(start, end) else html.substring(start)
    }

    private fun normalizePhotoUrl(url: String): String =
        url.trimEnd('=').replace("&amp;", "&")

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.ZOLO,
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
        val detailUrl = urlRe.find(chunk)?.groupValues?.get(1) ?: return null
        val externalId = imageIdRe.find(chunk)?.groupValues?.get(1)
            ?: detailUrl.substringAfterLast('/')
        val adId = externalId.toLongOrNull() ?: stableCaAdId(externalId)

        val street = streetRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val city = cityRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val province = provinceRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val neighbourhood = neighbourhoodRe.find(chunk)?.groupValues?.get(1)?.decodeHtml()?.trim()
        val address = listOfNotNull(street, city, province)
            .joinToString(", ")
            .ifBlank { null }

        val price = priceRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: priceAltRe.find(chunk)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()

        val lat = latRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
        val lng = lngRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null

        val bedsText = bedsRe.find(chunk)?.groupValues?.get(1)
        val rooms = bedsText?.toIntOrNull()
        val isStudio = chunk.contains("&ndash; bed", ignoreCase = true) ||
                chunk.contains("- bed", ignoreCase = true)

        val sqftRaw = sqftRe.find(chunk)?.groupValues?.get(1)
        val totalArea = parseSqft(sqftRaw)

        val image = imgRe.find(chunk)?.groupValues?.get(1)

        val mappedAdType = if (adType.isCaSaleDeal()) AdType.SALE else AdType.RENT

        return AppFlat(
            adId = adId,
            adType = mappedAdType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.ZOLO,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = image?.let { listOf(it) },
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = neighbourhood,
            address = address,
            metroStation = null,
            description = neighbourhood ?: city,
            yearBuilt = null,
            totalArea = totalArea,
            livingArea = totalArea,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = if (isStudio && rooms == null) true else null,
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
    }

    private fun parseSqft(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val first = raw.split('-').firstOrNull()?.replace(",", "")?.trim()?.toDoubleOrNull()
        return first?.takeIf { it > 0 }
    }

    private fun String.decodeHtml(): String =
        replace("&#xE9;", "é")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .trim()
}
