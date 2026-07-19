package listing.de.immowelt

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import utils.stripHtmlToPlainText

/**
 * Immowelt SERP HTML cards. UUID expose id → stable Long for [AppFlat.adId].
 * Detail often DataDome-blocked — list photos from card gallery; coords ≈ Bezirk centroid.
 * List HTML has **no** publish date → [AppFlat.publishedAt] stays null (sorts after dated IS24/KA;
 * inventing “now” broke free-tier feed-delay / NEWEST order).
 */
object ImmoweltHtmlParser {
    private val cardLinkRe = Regex(
        """href="(https://www\.immowelt\.de/expose/([a-f0-9-]{20,}))"[^>]*title="([^"]+)"""",
        RegexOption.IGNORE_CASE,
    )
    private val euroRe = Regex("""([\d.]+(?:,\d+)?)\s*€""")
    private val roomsRe = Regex("""(\d+)\s*Zimmer""", RegexOption.IGNORE_CASE)
    private val areaRe = Regex("""([\d]+(?:,\d+)?)\s*m²""")
    private val floorRe = Regex("""(\d+)\.\s*Geschoss""", RegexOption.IGNORE_CASE)

    /** Listing gallery photos (w=626); exclude tiny agency logos (h=50). */
    private val listingImgRe = Regex(
        """src="(https://mms\.immowelt\.de/[^"]+\?[^"]*w=626[^"]*)"""",
        RegexOption.IGNORE_CASE,
    )
    private val anyMmsImgRe = Regex(
        """src="(https://mms\.immowelt\.de/[^"]+\.jpg[^"]*)"""",
        RegexOption.IGNORE_CASE,
    )
    private val photoAltRe = Regex(
        """card-mfe-picture-box-gallery[^>]*>[\s\S]*?<img[^>]*alt="([^"]+)"""",
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
                val chunk = html.substring(start, minOf(end, html.length))
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
        val images = listingImgRe.findAll(html).map { decode(it.groupValues[1]) }
            .distinct().take(20).toList()
            .ifEmpty {
                anyMmsImgRe.findAll(html).map { decode(it.groupValues[1]) }
                    .filter { !it.contains("h=50") }
                    .distinct().take(20).toList()
            }
            .ifEmpty { base.imageUrls.orEmpty() }

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
        flatPlatform = FlatPlatform.IMMOWELT,
        flatDetailUrl = detailUrl ?: "https://www.immowelt.de/",
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

    fun uuidToAdId(uuid: String): Long {
        val hex = uuid.replace("-", "")
        val hi = hex.take(15).toLongOrNull(16) ?: (uuid.hashCode().toLong() and 0x7FFF_FFFFL)
        return hi and 0x7FFF_FFFF_FFFF_FFFFL
    }

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
        val images = listingImgRe.findAll(chunk).map { decode(it.groupValues[1]) }
            .distinct().take(8).toList()
            .ifEmpty {
                anyMmsImgRe.findAll(chunk).map { decode(it.groupValues[1]) }
                    .filter { !it.contains("h=50") && it.contains("w=") }
                    .distinct().take(8).toList()
            }
            .ifEmpty { null }
        val alt = photoAltRe.find(chunk)?.groupValues?.get(1)
            ?: Regex("""alt="([^"]*\d{5}[^"]*)"""", RegexOption.IGNORE_CASE)
                .find(chunk)?.groupValues?.get(1)
        val address = parseAddressFromAlt(alt) ?: cityHint
        val district = extractBezirk(alt) ?: extractBezirk(title) ?: cityHint
        val coordinates = BerlinBezirkCentroids.forName(district)
            ?: BerlinBezirkCentroids.forName(alt)
            ?: BerlinBezirkCentroids.forName(title)

        return AppFlat(
            adId = uuidToAdId(uuid),
            adType = adType,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = false,
                coordsEnriched = coordinates != null,
            ),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.IMMOWELT,
            flatDetailUrl = url,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = images,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = district,
            address = address,
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

    /** Alt often ends with `Street N District Berlin PLZ`. */
    private fun parseAddressFromAlt(alt: String?): String? {
        if (alt.isNullOrBlank()) return null
        val cleaned = alt.replace("&amp;", "&").trim()
        // Drop leading price/rooms noise: keep from first capital letter after m² / Geschoss
        val afterMeta = Regex(
            """(?:Geschoss|Zimmer|m²)\s+(.+)$""",
            RegexOption.IGNORE_CASE,
        ).find(cleaned)?.groupValues?.get(1)?.trim()
        return (afterMeta ?: cleaned).takeIf { it.length in 8..120 }
    }

    private fun extractBezirk(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return BerlinBezirkCentroids.names.firstOrNull { name ->
            text.contains(name, ignoreCase = true)
        }
    }

    private fun parseEuro(raw: String): Double? =
        raw.replace(".", "").replace(',', '.').toDoubleOrNull()

    private fun decode(url: String): String = url.replace("&amp;", "&")
}

/**
 * Berlin Bezirke centroids (OSM) — approx map pins when Immowelt detail is DataDome-blocked.
 */
object BerlinBezirkCentroids {
    val names: List<String> get() = byName.keys.toList()

    private val byName: Map<String, Coordinates> = mapOf(
        "Mitte" to Coordinates(52.5200, 13.4050),
        "Friedrichshain-Kreuzberg" to Coordinates(52.4990, 13.4440),
        "Friedrichshain" to Coordinates(52.5150, 13.4540),
        "Kreuzberg" to Coordinates(52.4980, 13.4030),
        "Pankow" to Coordinates(52.5690, 13.4020),
        "Charlottenburg-Wilmersdorf" to Coordinates(52.5080, 13.2640),
        "Charlottenburg" to Coordinates(52.5160, 13.3040),
        "Wilmersdorf" to Coordinates(52.4870, 13.3200),
        "Spandau" to Coordinates(52.5350, 13.2000),
        "Steglitz-Zehlendorf" to Coordinates(52.4330, 13.2410),
        "Steglitz" to Coordinates(52.4560, 13.3220),
        "Zehlendorf" to Coordinates(52.4340, 13.2580),
        "Tempelhof-Schöneberg" to Coordinates(52.4630, 13.3850),
        "Tempelhof" to Coordinates(52.4660, 13.3850),
        "Schöneberg" to Coordinates(52.4820, 13.3520),
        "Neukölln" to Coordinates(52.4810, 13.4350),
        "Treptow-Köpenick" to Coordinates(52.4450, 13.5750),
        "Treptow" to Coordinates(52.4570, 13.4980),
        "Köpenick" to Coordinates(52.4430, 13.5790),
        "Marzahn-Hellersdorf" to Coordinates(52.5350, 13.5870),
        "Marzahn" to Coordinates(52.5450, 13.5650),
        "Hellersdorf" to Coordinates(52.5360, 13.6050),
        "Lichtenberg" to Coordinates(52.5220, 13.4990),
        "Reinickendorf" to Coordinates(52.5750, 13.3400),
    )

    fun forName(text: String?): Coordinates? {
        if (text.isNullOrBlank()) return null
        // Prefer longer names first (Friedrichshain-Kreuzberg before Friedrichshain)
        return byName.entries
            .sortedByDescending { it.key.length }
            .firstOrNull { text.contains(it.key, ignoreCase = true) }
            ?.value
    }
}
