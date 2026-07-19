package listing.kz.kn

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import utils.stripHtmlToPlainText

/**
 * Regex HTML parser for kn.kz list cards + detail param rows.
 */
object KnHtmlParser {
    private val cardSplitRe =
        Regex("""data-object-id="(\d+)"""")
    private val titleRe =
        Regex(
            """href="/card/\d+"[^>]*class="kn-fs-18[^"]*"[^>]*>\s*([^<]+)\s*<""",
            RegexOption.IGNORE_CASE
        )
    private val titleAttrRe =
        Regex("""href="/card/\d+"[^>]*title="([^"]+)"""", RegexOption.IGNORE_CASE)
    private val priceRe =
        Regex("""(\d[\d\s\u00a0]{2,})\s*₸""")
    private val areaRe =
        Regex(
            """площадь:\s*</span>\s*<span[^>]*>\s*(\d+(?:[.,]\d+)?)\s*м""",
            RegexOption.IGNORE_CASE
        )
    private val floorRe =
        Regex(
            """<span class="fw-bold">(\d+)\s*/\s*(\d+)</span>\s*<span[^>]*>этаж""",
            RegexOption.IGNORE_CASE
        )
    private val roomsRe =
        Regex("""(\d+)\s*-\s*комнат""", RegexOption.IGNORE_CASE)
    private val imgRe =
        Regex(
            """src="(https://www\.kn\.kz/uploads/media/cache/list_card_middle/[^"]+)"""",
            RegexOption.IGNORE_CASE,
        )
    private val paramRe =
        Regex(
            """>(Адрес|Количество комнат|Этаж|Площадь|Этажность)</div>\s*<div>([^<]+)</div>""",
            RegexOption.IGNORE_CASE,
        )
    private val mapLatRe =
        Regex("""data-kn-simple-map-latitude-value="([0-9.]+)"""")
    private val mapLonRe =
        Regex("""data-kn-simple-map-longitude-value="([0-9.]+)"""")
    private val galleryRe =
        Regex(
            """src="(https://www\.kn\.kz/uploads/media/cache/gallery_big/[^"]+)"""",
            RegexOption.IGNORE_CASE,
        )

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val matches = cardSplitRe.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapNotNull { match ->
            val id = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val start = match.range.first
            val end = matches.getOrNull(matches.indexOf(match) + 1)?.range?.first ?: (start + 7000)
            val chunk = html.substring(start, minOf(end, html.length))
            runCatching { mapCard(id, chunk, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(
        base: AppFlat,
        detailHtml: String,
        mapHtml: String?,
        phones: List<String>,
    ): AppFlat {
        val params = paramRe.findAll(detailHtml).associate { m ->
            m.groupValues[1].trim() to m.groupValues[2].trim()
        }
        val price = priceRe.find(detailHtml)?.groupValues?.get(1)
            ?.replace(Regex("""[^\d]"""), "")
            ?.toDoubleOrNull()
            ?: base.mainPrice
        val rooms = params["Количество комнат"]?.toIntOrNull() ?: base.rooms
        val floor = params["Этаж"]?.toIntOrNull() ?: base.floor
        val totalFloors = params["Этажность"]?.toIntOrNull() ?: base.totalFloors
        val area = params["Площадь"]
            ?.let { Regex("""(\d+(?:[.,]\d+)?)""").find(it)?.groupValues?.get(1) }
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?: base.totalArea
        val address = params["Адрес"] ?: base.address
        val images = galleryRe.findAll(detailHtml).map { it.groupValues[1] }.distinct().toList()
            .ifEmpty { base.imageUrls }
        val lat = mapHtml?.let { mapLatRe.find(it)?.groupValues?.get(1)?.toDoubleOrNull() }
        val lon = mapHtml?.let { mapLonRe.find(it)?.groupValues?.get(1)?.toDoubleOrNull() }
        val coordinates =
            if (lat != null && lon != null) Coordinates(lat, lon) else base.coordinates
        val description = Regex(
            """<(?:div|p)[^>]*(?:description|kn-fs-16)[^>]*>\s*([\s\S]{40,5000}?)</(?:div|p)>""",
            RegexOption.IGNORE_CASE,
        ).findAll(detailHtml)
            .map { it.groupValues[1].stripHtmlToPlainText() }
            .firstOrNull { !it.isNullOrBlank() && it.length > (base.description?.length ?: 0) }
            ?: base.description

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            mainPrice = price,
            rooms = rooms,
            floor = floor,
            totalFloors = totalFloors,
            totalArea = area,
            address = address,
            description = description,
            imageUrls = images,
            coordinates = coordinates,
            contactInformation = ContactInformation(
                phones = phones.ifEmpty { null } ?: base.contactInformation?.phones,
                ownerName = base.contactInformation?.ownerName,
            ),
        )
    }

    private fun mapCard(id: Long, chunk: String, adType: AdType): AppFlat {
        val title = titleRe.find(chunk)?.groupValues?.get(1)?.trim()
            ?: titleAttrRe.find(chunk)?.groupValues?.get(1)?.trim()
        val price = priceRe.find(chunk)?.groupValues?.get(1)
            ?.replace(Regex("""[^\d]"""), "")
            ?.toDoubleOrNull()
        val area = areaRe.find(chunk)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: title?.let {
                Regex("""(\d+(?:[.,]\d+)?)\s*м""").find(it)?.groupValues?.get(1)
                    ?.replace(',', '.')?.toDoubleOrNull()
            }
        val floorMatch = floorRe.find(chunk)
        val floor = floorMatch?.groupValues?.get(1)?.toIntOrNull()
        val totalFloors = floorMatch?.groupValues?.get(2)?.toIntOrNull()
        val rooms = title?.let { roomsRe.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val image = imgRe.find(chunk)?.groupValues?.get(1)
        val address = title?.substringAfter(',')?.trim()

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.KN,
            flatDetailUrl = "https://www.kn.kz/card/$id",
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = image?.let { listOf(it) },
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = null,
            address = address,
            metroStation = null,
            description = title,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = totalFloors,
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
}
