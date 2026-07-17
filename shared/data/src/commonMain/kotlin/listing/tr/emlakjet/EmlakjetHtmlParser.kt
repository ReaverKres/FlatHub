package listing.tr.emlakjet

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
import kotlin.time.Instant

/**
 * Emlakjet SERP cards (`data-listing-id`) + detail JSON-LD / coords.
 * TRY → [AppFlat.priceByn]. Turkish `2+1` → rooms = 2+1 = 3 (matches site ld+json).
 * See tmp/tr/api/emlakjet/NOTES.md.
 */
object EmlakjetHtmlParser {
    private val cardRe = Regex("""data-listing-id="(\d{6,})"""")
    private val hrefRe = Regex("""data-listing-href="(/ilan/[^"]+)"""")
    private val altRe = Regex("""data-listing-alt="([^"]+)"""")
    private val imgPrefixRe = Regex("""data-listing-image-prefix="([^"]+)"""")
    private val imgSuffixesRe = Regex("""data-listing-image-suffixes="([^"]+)"""")
    private val priceRe = Regex("""([\d.]+)\s*₺""")
    private val roomsPlusRe = Regex("""(\d+)\+(\d+)""")
    private val areaRe = Regex("""([\d]+(?:[.,]\d+)?)\s*m²""")
    private val floorRe = Regex("""(\d+)\.\s*Kat""", RegexOption.IGNORE_CASE)
    private val dateRe = Regex("""(\d{1,2}\.\d{1,2}\.\d{4})""")
    private val districtRe = Regex("""İstanbul,\s*([^|<]+)""", RegexOption.IGNORE_CASE)
    private val cityDistrictRe = Regex("""([A-Za-zÇĞİÖŞÜçğıöşü\s]+),\s*([A-Za-zÇĞİÖŞÜçğıöşü\s]+)""")
    private val coordPairRe =
        Regex("""(2[6-9]|3[0-9]|4[0-5])\.\d{4,}\s*,\s*(3[6-9]|4[0-2])\.\d{4,}""")
    private val phoneRe = Regex("""(?:tel:|href="tel:)(\+?[\d\s/-]{8,})""", RegexOption.IGNORE_CASE)
    private val descRe = Regex(
        """"description"\s*:\s*"((?:\\.|[^"\\])*)"""",
    )
    private val ldRoomsRe = Regex(
        """"numberOfRooms"\s*:\s*(\d+)""",
    )
    private val ldPriceRe = Regex(
        """"price"\s*:\s*(\d+(?:\.\d+)?)""",
    )
    private val ldDateRe = Regex(
        """"dateModified"\s*:\s*"([^"]+)"""",
    )

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val matches = cardRe.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { index, m ->
            runCatching {
                val id = m.groupValues[1].toLongOrNull() ?: return@mapIndexedNotNull null
                val start = m.range.first
                val end = matches.getOrNull(index + 1)?.range?.first ?: (start + 12_000)
                val chunk = html.substring(start, minOf(end, html.length))
                mapCard(id, chunk, adType)
            }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val description = descRe.find(html)?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 40 }
            ?: base.description
        val phones = phoneRe.findAll(html)
            .map { it.groupValues[1].replace(Regex("""[\s/-]"""), "") }
            .distinct()
            .toList()
            .ifEmpty { base.contactInformation?.phones.orEmpty() }
        val price = ldPriceRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
            ?: priceRe.find(html)?.groupValues?.get(1)?.let { parseTry(it) }
            ?: base.priceByn
        val rooms = ldRoomsRe.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: base.rooms
        val area = areaRe.find(html)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: base.totalArea
        val coords = parseCoords(html) ?: base.coordinates
        val images = Regex(
            """https://imaj\.emlakjet\.com/listing/\d+/[A-F0-9]+\.jpg""",
            RegexOption.IGNORE_CASE
        )
            .findAll(html)
            .map { it.value }
            .distinct()
            .take(20)
            .toList()
            .ifEmpty { base.imageUrls.orEmpty() }
        val publishedRaw = ldDateRe.find(html)?.groupValues?.get(1)
        val publishedAt = publishedRaw?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: base.publishedAt
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: base.publishedAtUi

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description,
            priceByn = price,
            rooms = rooms,
            totalArea = area,
            coordinates = coords,
            imageUrls = images.ifEmpty { null },
            publishedAt = publishedAt,
            publishedAtServer = publishedRaw ?: base.publishedAtServer,
            publishedAtUi = publishedAtUi,
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
        flatPlatform = FlatPlatform.EMLAKJET,
        flatDetailUrl = detailUrl ?: "https://www.emlakjet.com/",
        publishedAt = null,
        publishedAtServer = null,
        publishedAtUi = null,
        imageUrls = null,
        priceUsd = null,
        priceByn = null,
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

    private fun mapCard(id: Long, chunk: String, adType: AdType): AppFlat {
        val href = hrefRe.find(chunk)?.groupValues?.get(1)
            ?: "/ilan/$id"
        val url = "https://www.emlakjet.com$href"
        val alt = altRe.find(chunk)?.groupValues?.get(1)?.replace("&amp;", "&")
        val text = chunk.replace(Regex("""<[^>]+>"""), " ").replace(Regex("""\s+"""), " ")
        val price = priceRe.find(chunk)?.groupValues?.get(1)?.let { parseTry(it) }
            ?: priceRe.find(text)?.groupValues?.get(1)?.let { parseTry(it) }
        val rooms = parseRooms(chunk) ?: parseRooms(text) ?: parseRooms(alt.orEmpty())
        val area = areaRe.find(chunk)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            ?: areaRe.find(text)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        val floor = floorRe.find(text)?.groupValues?.get(1)?.toIntOrNull()
        val dateRaw = dateRe.find(text)?.groupValues?.get(1)
        val publishedAt = DeRelativeTime.parse(dateRaw)
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: dateRaw
        val district = districtRe.find(text)?.groupValues?.get(1)?.trim()
            ?: cityDistrictRe.find(text)?.groupValues?.get(2)?.trim()
        val address = cityDistrictRe.find(text)?.let {
            "${it.groupValues[1].trim()}, ${it.groupValues[2].trim()}"
        } ?: district
        val images = parseImages(chunk)
        val isStudio = alt?.contains("stüdyo", ignoreCase = true) == true ||
                (alt?.contains("studio", ignoreCase = true) == true && rooms == 1)

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.EMLAKJET,
            flatDetailUrl = url,
            publishedAt = publishedAt,
            publishedAtServer = dateRaw,
            publishedAtUi = publishedAtUi,
            imageUrls = images.ifEmpty { null },
            priceUsd = null,
            priceByn = price,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = alt,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = isStudio,
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

    /** `2+1` → 3 (salon counted), same as Emlakjet ld+json `numberOfRooms`. */
    fun parseRooms(raw: String): Int? {
        val plus = roomsPlusRe.find(raw) ?: return null
        val a = plus.groupValues[1].toIntOrNull() ?: return null
        val b = plus.groupValues[2].toIntOrNull() ?: return null
        return a + b
    }

    fun parseTry(raw: String): Double? {
        val cleaned = raw.replace(".", "").replace(',', '.').trim()
        return cleaned.toDoubleOrNull()
    }

    private fun parseImages(chunk: String): List<String> {
        val prefix = imgPrefixRe.find(chunk)?.groupValues?.get(1) ?: return emptyList()
        val suffixesRaw = imgSuffixesRe.find(chunk)?.groupValues?.get(1)
            ?.replace("&quot;", "\"")
            ?.replace("&#34;", "\"")
            ?: return emptyList()
        val suffixes = Regex(""""([^"]+\.jpg)"""", RegexOption.IGNORE_CASE)
            .findAll(suffixesRaw)
            .map { it.groupValues[1] }
            .toList()
        return suffixes.take(10).map { "$prefix$it" }
    }

    /** Site embeds `lng,lat` (e.g. `28.98,41.07`). */
    private fun parseCoords(html: String): Coordinates? {
        val m = coordPairRe.find(html) ?: return null
        val a = m.groupValues[0].substringBefore(',').trim().toDoubleOrNull() ?: return null
        val b = m.groupValues[0].substringAfter(',').trim().toDoubleOrNull() ?: return null
        // Turkey: lon ~26–45, lat ~36–42
        return if (a in 26.0..45.0 && b in 36.0..43.0) {
            Coordinates(latitude = b, longitude = a)
        } else if (b in 26.0..45.0 && a in 36.0..43.0) {
            Coordinates(latitude = a, longitude = b)
        } else null
    }
}
