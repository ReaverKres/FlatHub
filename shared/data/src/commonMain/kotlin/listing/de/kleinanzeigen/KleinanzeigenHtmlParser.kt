package listing.de.kleinanzeigen

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import listing.de.DeRelativeTime
import utils.stripHtmlToPlainText

/**
 * Kleinanzeigen.de list cards.
 * Mobile Belen: `<li class="adlist--item" data-adid data-href>`.
 * Desktop sample: `<article class="aditem" …>`.
 */
object KleinanzeigenHtmlParser {
    private val liItemRe = Regex(
        """<li\b([^>]*\bdata-adid="(\d+)"[^>]*)>""",
        RegexOption.IGNORE_CASE,
    )
    private val articleItemRe = Regex(
        """<article\b([^>]*\bdata-adid="(\d+)"[^>]*)>""",
        RegexOption.IGNORE_CASE,
    )
    private val hrefAttrRe = Regex(
        """data-href="([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val titleMobileRe = Regex(
        """adlist--item--boldtitle[^>]*>\s*<a[^>]*>\s*([^<]+)\s*</a>""",
        RegexOption.IGNORE_CASE,
    )
    private val titleDesktopRe = Regex(
        """<a class="ellipsis"[^>]*>\s*([^<]+)\s*</a>""",
        RegexOption.IGNORE_CASE,
    )
    private val priceMobileRe = Regex(
        """adlist--item--price[^>]*>\s*([^<]+)\s*<""",
        RegexOption.IGNORE_CASE,
    )
    private val priceDesktopRe = Regex(
        """aditem-main--middle--price-shipping--price[^>]*>\s*([\d.]+(?:,\d+)?)\s*€""",
        RegexOption.IGNORE_CASE,
    )
    private val locMobileRe = Regex(
        """adlist--item--info--location[^>]*>\s*([^<]+)\s*<""",
        RegexOption.IGNORE_CASE,
    )
    private val locDesktopRe = Regex(
        """aditem-main--top--left[^>]*>[\s\S]*?</i>\s*([^<]+)\s*<""",
        RegexOption.IGNORE_CASE,
    )
    private val dateMobileRe = Regex(
        """adlist--item--info--date[^>]*>\s*([^<]+)\s*<""",
        RegexOption.IGNORE_CASE,
    )
    private val dateDesktopRe = Regex(
        """aditem-main--top--right[^>]*>\s*([^<]+)\s*<""",
        RegexOption.IGNORE_CASE,
    )
    private val descMobileRe = Regex(
        """description-preview[^>]*>\s*([^<]+)\s*<""",
        RegexOption.IGNORE_CASE,
    )
    private val descDesktopRe = Regex(
        """aditem-main--middle--description[^>]*>\s*([^<]+)\s*<""",
        RegexOption.IGNORE_CASE,
    )
    private val attrsMobileRe = Regex(
        """adlist--item--attributes[^>]*>\s*([\s\S]*?)\s*</div>""",
        RegexOption.IGNORE_CASE,
    )
    private val tagsDesktopRe = Regex(
        """aditem-main--middle--tags[^>]*>\s*([\s\S]*?)\s*</p>""",
        RegexOption.IGNORE_CASE,
    )
    private val areaRe = Regex("""([\d]+(?:,\d+)?)\s*m(?:²|2)""", RegexOption.IGNORE_CASE)
    private val roomsRe = Regex("""(\d+)\s*(?:Zimmer|Zi\.?)""", RegexOption.IGNORE_CASE)
    private val imgRe = Regex(
        """(?:src|data-src|contentUrl"\s*:\s*")(https://img\.kleinanzeigen\.de/[^"\s]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val phoneRe = Regex(
        """(?:tel:|id="viewad-contact-phone"[^>]*>)(\+?[\d\s/-]{8,})""",
        RegexOption.IGNORE_CASE,
    )
    private val vipPhoneRe = Regex(
        """adPhoneNumber["']?\s*[:=]\s*["']([^"']+)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val ogLatRe = Regex(
        """property="og:latitude"\s+content="([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val ogLngRe = Regex(
        """property="og:longitude"\s+content="([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val detailDescRe = Regex(
        """id="viewad-description-text"[^>]*>\s*([\s\S]*?)\s*</div>""",
        RegexOption.IGNORE_CASE,
    )

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val matches = liItemRe.findAll(html).toList().ifEmpty {
            articleItemRe.findAll(html).toList()
        }
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { index, match ->
            val adId = match.groupValues[2].toLongOrNull() ?: return@mapIndexedNotNull null
            val openTag = match.groupValues[1]
            val href = hrefAttrRe.find(openTag)?.groupValues?.get(1)
                ?: hrefAttrRe.find(match.value)?.groupValues?.get(1)
                ?: return@mapIndexedNotNull null
            val start = match.range.first
            val end = matches.getOrNull(index + 1)?.range?.first ?: (start + 8_000)
            val chunk = html.substring(start, minOf(end, html.length))
            runCatching {
                mapCard(adId, href, chunk, adType)
            }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val description = detailDescRe.find(html)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 40 }
            ?: base.description
        val vipPhones = vipPhoneRe.findAll(html)
            .map { it.groupValues[1].replace(Regex("""[\s/-]"""), "") }
            .filter { it.length >= 8 }
        val phones = (vipPhones + phoneRe.findAll(html)
            .map { it.groupValues[1].replace(Regex("""[\s/-]"""), "") }
            .filter { it.length >= 8 })
            .distinct()
            .toList()
            .ifEmpty { base.contactInformation?.phones.orEmpty() }
        val price = priceMobileRe.find(html)?.groupValues?.get(1)?.let { parseEuroLoose(it) }
            ?: priceDesktopRe.find(html)?.groupValues?.get(1)?.let { parseEuro(it) }
            ?: euroLoose(html)
            ?: base.mainPrice
        val rooms = roomsRe.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: base.rooms
        val area = areaRe.find(html)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: base.totalArea
        val images = imgRe.findAll(html).map { decodeHtml(it.groupValues[1]) }
            .distinct().take(20).toList()
            .ifEmpty { base.imageUrls.orEmpty() }
        val lat = ogLatRe.find(html)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val lng = ogLngRe.find(html)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val coordinates =
            if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            mainPrice = price,
            rooms = rooms,
            totalArea = area,
            coordinates = coordinates,
            imageUrls = images.ifEmpty { null },
            contactInformation = ContactInformation(
                phones = phones.ifEmpty { null },
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    fun listStub(adId: Long): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.KLEINANZEIGEN,
        flatDetailUrl = "https://www.kleinanzeigen.de/",
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
        adId: Long,
        href: String,
        chunk: String,
        adType: AdType,
    ): AppFlat {
        val title = (titleMobileRe.find(chunk) ?: titleDesktopRe.find(chunk))
            ?.groupValues?.get(1)?.trim()?.replace("&amp;", "&")
        val price = priceMobileRe.find(chunk)?.groupValues?.get(1)?.let { parseEuroLoose(it) }
            ?: priceDesktopRe.find(chunk)?.groupValues?.get(1)?.let { parseEuro(it) }
        val loc = (locMobileRe.find(chunk) ?: locDesktopRe.find(chunk))
            ?.groupValues?.get(1)?.trim()
        val desc = (descMobileRe.find(chunk) ?: descDesktopRe.find(chunk))
            ?.groupValues?.get(1)?.trim()?.stripHtmlToPlainText()
        val attrs = (attrsMobileRe.find(chunk)?.groupValues?.get(1)
            ?: tagsDesktopRe.find(chunk)?.groupValues?.get(1)).orEmpty()
        val area = areaRe.find(attrs)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: areaRe.find(title.orEmpty())?.groupValues?.get(1)?.replace(',', '.')
                ?.toDoubleOrNull()
        val rooms = roomsRe.find(attrs)?.groupValues?.get(1)?.toIntOrNull()
            ?: roomsRe.find(title.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
        val images = imgRe.findAll(chunk).map { decodeHtml(it.groupValues[1]) }
            .distinct().take(5).toList().ifEmpty { null }
        val detailUrl = if (href.startsWith("http")) href else "https://www.kleinanzeigen.de$href"
        val dateRaw = (dateMobileRe.find(chunk) ?: dateDesktopRe.find(chunk))
            ?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        val publishedAt = DeRelativeTime.parse(dateRaw)
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: dateRaw

        return AppFlat(
            adId = adId,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.KLEINANZEIGEN,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = dateRaw,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = loc,
            address = loc,
            metroStation = null,
            description = desc ?: title,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = title?.contains("Studio", ignoreCase = true) == true,
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

    private fun parseEuroLoose(raw: String): Double? {
        val m = Regex("""([\d.]+(?:,\d+)?)""").find(raw.replace('\u00A0', ' ')) ?: return null
        return parseEuro(m.groupValues[1])
    }

    private fun euroLoose(html: String): Double? {
        val m = Regex("""([\d.]+(?:,\d+)?)\s*€""").find(html) ?: return null
        return parseEuro(m.groupValues[1])
    }

    private fun decodeHtml(url: String): String =
        url.replace("&amp;", "&")
            .replace("&#61;", "=")
            .replace("&equals;", "=")
}
