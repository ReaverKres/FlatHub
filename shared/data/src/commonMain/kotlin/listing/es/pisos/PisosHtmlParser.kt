package listing.es.pisos

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import utils.stripHtmlToPlainText

/**
 * SSR HTML parser for pisos.com `ad-preview` cards + inline JSON-LD geo.
 * See tmp/es/api/pisos/NOTES.md.
 */
object PisosHtmlParser {
    private val cardRe =
        Regex(
            """<div id="(\d+)\.(\d+)" class="ad-preview[^"]*"\s+data-lnk-href="([^"]+)"""",
            RegexOption.IGNORE_CASE,
        )
    private val priceDataRe =
        Regex("""data-ad-price="(\d+)"""", RegexOption.IGNORE_CASE)
    private val priceTextRe =
        Regex(
            """class="ad-preview__price"[^>]*>\s*([\d.]+)\s*€""",
            RegexOption.IGNORE_CASE,
        )
    private val titleRe =
        Regex(
            """class="ad-preview__title"[^>]*>\s*([^<]+)\s*<""",
            RegexOption.IGNORE_CASE,
        )
    private val subtitleRe =
        Regex(
            """class="ad-preview__subtitle"[^>]*>\s*([^<]+)\s*<""",
            RegexOption.IGNORE_CASE,
        )
    private val roomsRe =
        Regex("""(\d+)\s*habs?\.?""", RegexOption.IGNORE_CASE)
    private val areaRe =
        Regex("""(\d+(?:[.,]\d+)?)\s*m(?:²|2|&#xB2;)""", RegexOption.IGNORE_CASE)
    private val floorRe =
        Regex("""(\d+)\s*ª?\s*planta|Bajo""", RegexOption.IGNORE_CASE)
    private val phoneRe =
        Regex("""data-number="(\d{7,})"""", RegexOption.IGNORE_CASE)
    private val imgRe =
        Regex(
            """(?:src|data-src|srcset)="(https://fotos\.imghs\.net/[^"\s]+)""",
            RegexOption.IGNORE_CASE,
        )
    private val ldGeoRe =
        Regex(
            """"geo"\s*:\s*\{\s*"@type"\s*:\s*"GeoCoordinates"\s*,\s*"latitude"\s*:\s*"([^"]+)"\s*,\s*"longitude"\s*:\s*"([^"]+)"""",
            RegexOption.IGNORE_CASE,
        )
    private val detailDescRe =
        Regex(
            """class="[^"]*description[^"]*"[^>]*>\s*([\s\S]*?)\s*</div>""",
            RegexOption.IGNORE_CASE,
        )

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val matches = cardRe.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { index, match ->
            val listingId = match.groupValues[1].toLongOrNull() ?: return@mapIndexedNotNull null
            val agencyId = match.groupValues[2]
            val href = match.groupValues[3]
            val start = match.range.first
            val end = matches.getOrNull(index + 1)?.range?.first ?: (start + 14_000)
            val chunk = html.substring(start, minOf(end, html.length))
            runCatching { mapCard(listingId, agencyId, href, chunk, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val description = detailDescRe.find(html)?.groupValues?.get(1)
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 40 }
            ?: base.description
        val phones = phoneRe.findAll(html).map { it.groupValues[1] }.distinct().toList()
            .ifEmpty { base.contactInformation?.phones.orEmpty() }
        val price = priceDataRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: base.mainPrice
        val rooms = roomsRe.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: base.rooms
        val area = areaRe.find(html)?.groupValues?.get(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?: base.totalArea
        val coords = ldGeoRe.find(html)?.let { m ->
            val lat = m.groupValues[1].replace(',', '.').toDoubleOrNull()
            val lng = m.groupValues[2].replace(',', '.').toDoubleOrNull()
            if (lat != null && lng != null) Coordinates(lat, lng) else null
        } ?: base.coordinates
        val images = imgRe.findAll(html).map { it.groupValues[1].substringBefore(' ') }
            .distinct()
            .take(20)
            .toList()
            .ifEmpty { base.imageUrls.orEmpty() }

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            mainPrice = price,
            rooms = rooms,
            totalArea = area,
            coordinates = coords,
            imageUrls = images.ifEmpty { null },
            contactInformation = ContactInformation(
                phones = phones.ifEmpty { null },
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    private fun mapCard(
        listingId: Long,
        agencyId: String,
        href: String,
        chunk: String,
        adType: AdType,
    ): AppFlat {
        val price = priceDataRe.find(chunk)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: priceTextRe.find(chunk)?.groupValues?.get(1)
                ?.replace(".", "")
                ?.toDoubleOrNull()
        val title = titleRe.find(chunk)?.groupValues?.get(1)?.trim()?.decodeHtmlEntities()
        val subtitle = subtitleRe.find(chunk)?.groupValues?.get(1)?.trim()?.decodeHtmlEntities()
        val rooms = roomsRe.find(chunk)?.groupValues?.get(1)?.toIntOrNull()
        val area = areaRe.find(chunk)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val floorMatch = floorRe.find(chunk)
        val floor = when {
            floorMatch == null -> null
            floorMatch.value.contains("Bajo", ignoreCase = true) -> 0
            else -> floorMatch.groupValues.getOrNull(1)?.toIntOrNull()
        }
        val phone = phoneRe.find(chunk)?.groupValues?.get(1)
        val image = imgRe.find(chunk)?.groupValues?.get(1)?.substringBefore(' ')
        val coords = ldGeoRe.find(chunk)?.let { m ->
            val lat = m.groupValues[1].replace(',', '.').toDoubleOrNull()
            val lng = m.groupValues[2].replace(',', '.').toDoubleOrNull()
            if (lat != null && lng != null) Coordinates(lat, lng) else null
        }
        val detailUrl = if (href.startsWith("http")) href else "https://www.pisos.com$href"
        val district = subtitle
            ?.substringAfter("Distrito ", missingDelimiterValue = "")
            ?.substringBefore('.')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: subtitle?.substringBefore('(')?.trim()

        return AppFlat(
            adId = listingId,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(
                phones = phone?.let { listOf(it) },
                ownerName = null,
            ),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.PISOS,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = image?.let { listOf(it) },
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = district,
            address = subtitle ?: title,
            metroStation = null,
            description = title,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = rooms == 0,
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

    fun listStub(adId: Long, detailUrl: String? = null, adType: AdType = AdType.RENT): AppFlat =
        AppFlat(
            adId = adId,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.PISOS,
            flatDetailUrl = detailUrl ?: "https://www.pisos.com/",
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

    private fun String.decodeHtmlEntities(): String =
        replace("&nbsp;", " ")
            .replace("&#xF3;", "ó")
            .replace("&#xE1;", "á")
            .replace("&#xE9;", "é")
            .replace("&#xED;", "í")
            .replace("&#xFA;", "ú")
            .replace("&#xF1;", "ñ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .trim()
}
