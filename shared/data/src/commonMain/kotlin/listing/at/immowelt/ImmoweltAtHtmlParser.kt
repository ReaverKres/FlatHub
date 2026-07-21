package listing.at.immowelt

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import listing.de.immowelt.ImmoweltHtmlParser
import utils.stripHtmlToPlainText

/**
 * Immowelt.at SERP HTML cards — fork of DE parser with `.at` host.
 * List HTML has no publish date → [AppFlat.publishedAt] stays null.
 *
 * Gallery imgs often sit **above** the covering `<a href=expose>` in the card MFE;
 * we look back for `card-mfe-picture` / classified-card so `w=626` thumbs are captured.
 * Tiny agency logos use `h=50` and must not be used as listing photos.
 */
object ImmoweltAtHtmlParser {
    private val cardLinkRe = Regex(
        """href="(https://www\.immowelt\.at/expose/([a-f0-9-]{20,}))"[^>]*title="([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val euroRe = Regex("""([\d.]+(?:,\d+)?)\s*€""")
    private val roomsRe = Regex("""(\d+)\s*Zimmer""", RegexOption.IGNORE_CASE)
    private val areaRe = Regex("""([\d]+(?:,\d+)?)\s*m²""")
    private val floorRe = Regex("""(\d+)\.\s*Geschoss""", RegexOption.IGNORE_CASE)

    /** Listing gallery photos (signed `ci_seal` + `w=626`). */
    private val listingImgRe = Regex(
        """(?:src|data-src|href)="(https://mms\.immowelt\.de/[^"]+\.jpe?g\?[^"]*w=626[^"]*)"""",
        RegexOption.IGNORE_CASE,
    )
    private val anyMmsImgRe = Regex(
        """(?:src|data-src|href)="(https://mms\.immowelt\.de/[^"]+\.jpe?g[^"]*)"""",
        RegexOption.IGNORE_CASE,
    )
    private val phoneRe = Regex("""(?:tel:|href="tel:)(\+?[\d\s/-]{8,})""", RegexOption.IGNORE_CASE)
    private val descRe = Regex(
        """data-testid="[^"]*description[^"]*"[^>]*>\s*([\s\S]*?)\s*</""",
        RegexOption.IGNORE_CASE,
    )

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val matches = cardLinkRe.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { index, m ->
            runCatching {
                val url = m.groupValues[1]
                val uuid = m.groupValues[2]
                val title = m.groupValues[3].replace("&amp;", "&")
                val start = m.range.first
                val end = matches.getOrNull(index + 1)?.range?.first ?: (start + 12_000)
                // Gallery carousel is rendered above the covering link inside the card.
                val lookBack = html.lastIndexOf("classified-card-", start)
                    .takeIf { it >= 0 && start - it < 20_000 }
                    ?: html.lastIndexOf("card-mfe-picture", start)
                        .takeIf { it >= 0 && start - it < 20_000 }
                    ?: (start - 8_000).coerceAtLeast(0)
                val chunk = html.substring(lookBack, minOf(end, html.length))
                mapCard(uuid, url, title, chunk, adType)
            }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val description = descRe.find(html)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 40 }
            ?: base.description
        val phones = phoneRe.findAll(html)
            .map { it.groupValues[1].replace(Regex("""[\s/-]"""), "") }
            .distinct()
            .toList()
            .ifEmpty { base.contactInformation?.phones.orEmpty() }
        val price = euroRe.find(html)?.groupValues?.get(1)?.let { parseEuro(it) } ?: base.mainPrice
        val rooms = roomsRe.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: base.rooms
        val area = areaRe.find(html)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: base.totalArea
        val images = extractListingImages(html).ifEmpty { base.imageUrls.orEmpty() }

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            mainPrice = price,
            rooms = rooms,
            totalArea = area,
            imageUrls = images.ifEmpty { null },
            contactInformation = ContactInformation(
                phones = phones.ifEmpty { null },
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.IMMOWELT_AT,
        flatDetailUrl = detailUrl ?: "https://www.immowelt.at/",
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

    private fun mapCard(
        uuid: String,
        url: String,
        title: String,
        chunk: String,
        adType: AdType,
    ): AppFlat {
        val price = euroRe.find(title)?.groupValues?.get(1)?.let { parseEuro(it) }
        val rooms = roomsRe.find(title)?.groupValues?.get(1)?.toIntOrNull()
        val area = areaRe.find(title)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val floor = floorRe.find(title)?.groupValues?.get(1)?.toIntOrNull()
        val parts = title.split(" - ").map { it.trim() }
        val cityHint = parts.getOrNull(1)
        val headline = parts.firstOrNull()
        val images = extractListingImages(chunk)

        return AppFlat(
            adId = ImmoweltHtmlParser.uuidToAdId(uuid),
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.IMMOWELT_AT,
            flatDetailUrl = url,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = images,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = cityHint,
            address = cityHint,
            metroStation = null,
            description = headline,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = headline?.contains("Studio", ignoreCase = true) == true,
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
    }

    private fun parseEuro(raw: String): Double? =
        raw.replace(".", "").replace(',', '.').toDoubleOrNull()

    private fun decode(url: String): String =
        url.replace("&amp;", "&").replace("&#x3D;", "=").replace("&#61;", "=")

    /** Prefer signed `w=626` gallery thumbs; never use `h=50` logos. */
    private fun extractListingImages(html: String): List<String> {
        val primary = listingImgRe.findAll(html)
            .map { decode(it.groupValues[1]) }
            .filter { isListingPhoto(it) }
            .distinct()
            .take(12)
            .toList()
        if (primary.isNotEmpty()) return primary
        return anyMmsImgRe.findAll(html)
            .map { decode(it.groupValues[1]) }
            .filter { isListingPhoto(it) }
            .distinct()
            .take(12)
            .toList()
    }

    private fun isListingPhoto(url: String): Boolean {
        if (!url.contains("mms.immowelt.de", ignoreCase = true)) return false
        if (url.contains("h=50")) return false
        // Require a real listing width (logo preloads omit w=).
        return url.contains("w=626") || url.contains("w=800") || url.contains("w=1200")
    }
}
