package listing.gb.openrent

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import utils.stripHtmlToPlainText

/**
 * OpenRent SSR search cards + embedded JS coord arrays; detail enrichment.
 * GBP monthly → [AppFlat.mainPrice]. See tmp/gb/api/openrent/NOTES.md.
 */
object OpenRentHtmlParser {
    private const val SITE_ORIGIN = "https://www.openrent.co.uk"

    private val cardRe = Regex(
        """<a href="(/property-to-rent/[^"]+/(\d+))" class="pli search-property-card""",
        RegexOption.IGNORE_CASE,
    )
    private val propertyIdsRe = Regex(
        """var\s+PROPERTYIDS\s*=\s*\[\s*([\d,\s]+)\s*\]""",
        RegexOption.IGNORE_CASE,
    )
    private val latsRe = Regex(
        """var\s+PROPERTYLISTLATITUDES\s*=\s*\[\s*([\d.\-,\s]+)\s*\]""",
        RegexOption.IGNORE_CASE,
    )
    private val lngsRe = Regex(
        """var\s+PROPERTYLISTLONGITUDES\s*=\s*\[\s*([\d.\-,\s]+)\s*\]""",
        RegexOption.IGNORE_CASE,
    )
    private val monthlyPriceRe = Regex(
        """class="pim[^"]*"[\s\S]*?(?:&#xA3;|£)([\d,]+(?:\.\d+)?)""",
        RegexOption.IGNORE_CASE,
    )
    private val titleRe = Regex(
        """class="fw-medium text-primary fs-3">([^<]+)<""",
        RegexOption.IGNORE_CASE,
    )
    private val bedsRe = Regex(
        """<li>(\d+)\s+Bed(?:s|room)?</li>""",
        RegexOption.IGNORE_CASE,
    )
    private val imageRe = Regex(
        """(?:src|data-src)="(//imagescdn\.openrent\.co\.uk/[^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val mapCoordsRe = Regex(
        """id="(?:map|property-map)"[^>]*\sdata-lat="([^"]+)"[^>]*\sdata-lng="([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val mapCoordsReAlt = Regex(
        """data-lat="([^"]+)"[^>]*data-lng="([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val descriptionRe = Regex(
        """id="descriptionText"[\s\S]*?<div>\s*([\s\S]*?)\s*</div>""",
        RegexOption.IGNORE_CASE,
    )
    private val detailImageRe = Regex(
        """(?:href|src)="(//imagescdn\.openrent\.co\.uk/listings/\d+/[^"]+\.(?:JPG|jpg|png|webp))"""",
        RegexOption.IGNORE_CASE,
    )
    private val detailPriceRe = Regex(
        """(?:&#xA3;|£)([\d,]+(?:\.\d+)?)\s*(?:p/m|per month)""",
        RegexOption.IGNORE_CASE,
    )
    private val updatedAgoRe = Regex(
        """Last updated around\s+([^<]+)""",
        RegexOption.IGNORE_CASE,
    )

    fun parseSearch(html: String): List<AppFlat> {
        val coordLookup = buildCoordLookup(html)
        val matches = cardRe.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { index, match ->
            val href = match.groupValues[1]
            val adId = match.groupValues[2].toLongOrNull() ?: return@mapIndexedNotNull null
            val start = match.range.first
            val end = matches.getOrNull(index + 1)?.range?.first ?: (start + 12_000)
            val chunk = html.substring(start, minOf(end, html.length))
            runCatching { mapCard(adId, href, chunk, coordLookup[adId]) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val coords =
            mapCoordsRe.find(html)?.let { parseCoords(it.groupValues[1], it.groupValues[2]) }
                ?: mapCoordsReAlt.find(html)
                    ?.let { parseCoords(it.groupValues[1], it.groupValues[2]) }
                ?: base.coordinates
        val description = descriptionRe.find(html)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 30 }
            ?: base.description
        val price = detailPriceRe.find(html)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: base.mainPrice
        val title = titleRe.find(html)?.groupValues?.get(1)?.trim()?.decodeHtmlEntities()
        val images = detailImageRe.findAll(html)
            .map { normalizeUrl(it.groupValues[1]) }
            .distinct()
            .take(12)
            .toList()
            .ifEmpty { base.imageUrls.orEmpty() }
        val rooms = parseRoomsFromText(title ?: base.address.orEmpty(), html) ?: base.rooms
        val isStudio = title?.contains("studio", ignoreCase = true) == true ||
                base.isStudio == true

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            mainPrice = price,
            rooms = rooms,
            isStudio = if (isStudio) true else base.isStudio,
            coordinates = coords,
            address = title ?: base.address,
            imageUrls = images.ifEmpty { null },
            contactInformation = ContactInformation(
                phones = null,
                ownerName = base.contactInformation?.ownerName,
            ),
            owner = true,
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.OPENRENT,
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
        owner = true,
    )

    private fun mapCard(
        adId: Long,
        href: String,
        chunk: String,
        coords: Coordinates?,
    ): AppFlat {
        val price = monthlyPriceRe.find(chunk)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
        val title = titleRe.find(chunk)?.groupValues?.get(1)?.trim()?.decodeHtmlEntities()
        val rooms = parseRoomsFromText(title.orEmpty(), chunk)
        val isStudio = title?.contains("studio", ignoreCase = true) == true
        val image = imageRe.find(chunk)?.groupValues?.get(1)?.let(::normalizeUrl)
        val detailUrl = "$SITE_ORIGIN$href"
        val snippet = chunk
            .substringAfter("line-clamp-2", missingDelimiterValue = "")
            .substringAfter('>', missingDelimiterValue = "")
            .substringBefore('<', missingDelimiterValue = "")
            .trim()
            .decodeHtmlEntities()
            .takeIf { it.length > 20 }
        val published = listing.gb.GbPublishedAt.fromOpenRentRelative(
            updatedAgoRe.find(chunk)?.groupValues?.get(1),
        )

        return AppFlat(
            adId = adId,
            adType = AdType.RENT,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.OPENRENT,
            flatDetailUrl = detailUrl,
            publishedAt = published?.publishedAt,
            publishedAtServer = published?.publishedAtServer,
            publishedAtUi = published?.publishedAtUi,
            imageUrls = image?.let { listOf(it) },
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = null,
            address = title,
            metroStation = null,
            description = snippet ?: title,
            yearBuilt = null,
            totalArea = null,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = if (isStudio) true else null,
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
            owner = true,
        )
    }

    private fun buildCoordLookup(html: String): Map<Long, Coordinates> {
        val ids = propertyIdsRe.find(html)?.groupValues?.get(1)
            ?.split(',')
            ?.mapNotNull { it.trim().toLongOrNull() }
            .orEmpty()
        if (ids.isEmpty()) return emptyMap()
        val lats = latsRe.find(html)?.groupValues?.get(1)
            ?.split(',')
            ?.mapNotNull { it.trim().toDoubleOrNull() }
            .orEmpty()
        val lngs = lngsRe.find(html)?.groupValues?.get(1)
            ?.split(',')
            ?.mapNotNull { it.trim().toDoubleOrNull() }
            .orEmpty()
        return ids.mapIndexedNotNull { index, id ->
            val lat = lats.getOrNull(index) ?: return@mapIndexedNotNull null
            val lng = lngs.getOrNull(index) ?: return@mapIndexedNotNull null
            id to Coordinates(lat, lng)
        }.toMap()
    }

    private fun parseRoomsFromText(title: String, htmlChunk: String): Int? {
        bedsRe.find(htmlChunk)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }
        if (title.contains("studio", ignoreCase = true)) return 0
        val bedInTitle = Regex("""(\d+)\s+Bed""", RegexOption.IGNORE_CASE).find(title)
            ?.groupValues?.get(1)?.toIntOrNull()
        if (bedInTitle != null) return bedInTitle
        if (title.contains("room in", ignoreCase = true) ||
            htmlChunk.contains("Room Available", ignoreCase = true)
        ) {
            return 1
        }
        return null
    }

    private fun parseCoords(latRaw: String, lngRaw: String): Coordinates? {
        val lat = latRaw.toDoubleOrNull() ?: return null
        val lng = lngRaw.toDoubleOrNull() ?: return null
        return Coordinates(lat, lng)
    }

    private fun normalizeUrl(url: String): String =
        if (url.startsWith("//")) "https:$url" else url

    private fun String.decodeHtmlEntities(): String =
        replace("&#xA3;", "£")
            .replace("&amp;", "&")
            .replace("&#x2022;", "•")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .trim()
}
