package listing.jp.athome

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.jp.parseAreaSqm
import listing.jp.parseYenLabel
import listing.jp.stableAdId
import utils.stripHtmlToPlainText

/**
 * athome.co.jp rent list SSR HTML + detail JSON-LD enrichment.
 */
object AthomeRentHtmlParser {
    private val json = Json { ignoreUnknownKeys = true }

    private val buildingSplitRe =
        Regex("""<div class="p-property p-property--building""", RegexOption.IGNORE_CASE)
    private val roomBoxRe =
        Regex("""data-bukken-no="(\d+)"[^>]*data-kencd="(\d+)"""", RegexOption.IGNORE_CASE)
    private val buildingTitleRe =
        Regex("""p-property__title--building">([^<]+)<""", RegexOption.IGNORE_CASE)
    private val addressRe =
        Regex(
            """u-icon--map-mini[^>]*></i></dt>\s*<dd><strong>([^<]+)</strong>""",
            RegexOption.IGNORE_CASE
        )
    private val metroRe =
        Regex("""u-icon--train-mini[^>]*></i></dt>\s*<dd>([^<]+)</dd>""", RegexOption.IGNORE_CASE)
    private val yearBuiltRe = Regex("""(\d{4})年\s*\d{1,2}月""")
    private val buildingImgRe =
        Regex(
            """(https://www\.athome\.co\.jp/image_files/path/[^"?]+\.(?:jpeg|jpg|png))""",
            RegexOption.IGNORE_CASE
        )
    private val detailHrefRe =
        Regex("""href="(/chintai/\d+/[^"]*)"""", RegexOption.IGNORE_CASE)
    private val rentRe =
        Regex("""p-property__information-rent">([\d.]+)</b>\s*万円""", RegexOption.IGNORE_CASE)
    private val mgmtRe =
        Regex(
            """p-property__information-rent[^>]*>[\s\S]*?<span>([^<]+)</span>\s*</p>""",
            RegexOption.IGNORE_CASE
        )
    private val shikikinRe =
        Regex("""p-property__room-keymoney[^>]*>[\s\S]*?<p>([^<]*)</p>""", RegexOption.IGNORE_CASE)
    private val reikinRe =
        Regex("""shikirei_text_paid">([^<]+)<""", RegexOption.IGNORE_CASE)
    private val madoriRe =
        Regex("""p-property__floor">\s*([^<]+)\s*<""", RegexOption.IGNORE_CASE)
    private val areaRe = Regex("""([\d.]+)m&sup2;""", RegexOption.IGNORE_CASE)
    private val roomImgRe =
        Regex(
            """(https://www\.athome\.co\.jp/image_files/path/[^"?]+\.(?:jpeg|jpg|png))""",
            RegexOption.IGNORE_CASE
        )
    private val ldJsonRe =
        Regex(
            """<script type="application/ld\+json">\s*([\s\S]*?)</script>""",
            RegexOption.IGNORE_CASE
        )

    fun parseSearch(html: String): List<AppFlat> {
        AthomeApiClient.ensureNotBlocked(html, "Athome rent list parse")
        val chunks = buildingSplitRe.split(html).drop(1)
        if (chunks.isEmpty()) return emptyList()
        return chunks.flatMap { chunk -> parseBuilding(chunk) }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val apartment = extractApartmentLd(html) ?: return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
        )
        val coords = parseGeo(apartment)
        val address = apartment["address"]?.asObjectOrNull()?.let { addr ->
            listOfNotNull(
                addr["addressRegion"]?.contentOrNull(),
                addr["addressLocality"]?.contentOrNull(),
            ).joinToString(" ").ifBlank { null }
        } ?: base.address
        val area = apartment["floorSize"]?.asObjectOrNull()?.get("value")?.contentOrNull()
            ?.toDoubleOrNull() ?: base.totalArea
        val images = apartment["photo"]?.asArrayOrNull()
            ?.mapNotNull { it.asObjectOrNull()?.get("contentUrl")?.contentOrNull() }
            ?.distinct()
            ?.ifEmpty { null }
            ?: base.imageUrls
        val agentPhone = extractAgentPhone(html)
        val description = apartment["description"]?.contentOrNull()?.stripHtmlToPlainText()
            ?: base.description

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            coordinates = coords ?: base.coordinates,
            address = address,
            totalArea = area,
            imageUrls = images,
            description = description,
            contactInformation = ContactInformation(
                phones = agentPhone?.let { listOf(it) } ?: base.contactInformation?.phones,
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
        flatPlatform = FlatPlatform.ATHOME,
        flatDetailUrl = detailUrl ?: "$BASE/chintai/",
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

    private fun parseBuilding(chunk: String): List<AppFlat> {
        val buildingTitle = buildingTitleRe.find(chunk)?.groupValues?.get(1)?.trim()
        val address = addressRe.find(chunk)?.groupValues?.get(1)?.trim()
        val metro = metroRe.find(chunk)?.groupValues?.get(1)?.trim()
        val yearBuilt = yearBuiltRe.find(chunk)?.groupValues?.get(1)?.toIntOrNull()
        val buildingImages =
            buildingImgRe.findAll(chunk).map { decode(it.groupValues[1]) }.distinct().toList()
        val roomMatches = roomBoxRe.findAll(chunk).toList()
        if (roomMatches.isEmpty()) return emptyList()

        return roomMatches.mapIndexedNotNull { index, match ->
            val bukkenNo = match.groupValues[1]
            val start = match.range.first
            val end = roomMatches.getOrNull(index + 1)?.range?.first ?: chunk.length
            val roomChunk = chunk.substring(start, minOf(end, chunk.length))
            mapRoom(
                bukkenNo = bukkenNo,
                roomChunk = roomChunk,
                buildingTitle = buildingTitle,
                address = address,
                metro = metro,
                yearBuilt = yearBuilt,
                buildingImages = buildingImages,
            )
        }
    }

    private fun mapRoom(
        bukkenNo: String,
        roomChunk: String,
        buildingTitle: String?,
        address: String?,
        metro: String?,
        yearBuilt: Int?,
        buildingImages: List<String>,
    ): AppFlat? {
        val adId = bukkenNo.toLongOrNull() ?: stableAdId(bukkenNo)
        val detailPath = detailHrefRe.find(roomChunk)?.groupValues?.get(1)
            ?: "/chintai/$bukkenNo/"
        val detailUrl = if (detailPath.startsWith("http")) detailPath else BASE + detailPath

        val rentMan = rentRe.find(roomChunk)?.groupValues?.get(1)
        val mainPrice = rentMan?.let { parseYenLabel("${it}万円") }
        val mgmtRaw = mgmtRe.find(roomChunk)?.groupValues?.get(1)?.trim()
        val secondPrice =
            mgmtRaw?.takeIf { it != "－" && it.isNotBlank() }?.let { parseYenLabel(it) }

        val shikikin =
            shikikinRe.find(roomChunk)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val reikin =
            reikinRe.find(roomChunk)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
        val madori = madoriRe.find(roomChunk)?.groupValues?.get(1)?.trim()
        val area = areaRe.find(roomChunk)?.groupValues?.get(1)?.let { parseAreaSqm("${it}m²") }
        val roomImg = roomImgRe.find(roomChunk)?.groupValues?.get(1)?.let { decode(it) }
        val images = buildList {
            roomImg?.let { add(it) }
            addAll(buildingImages)
        }.distinct().ifEmpty { null }

        val secondaryParts = listOfNotNull(
            shikikin?.let { "敷金: $it" },
            reikin?.let { "礼金: $it" },
        )
        val description = listOfNotNull(
            buildingTitle,
            secondaryParts.joinToString(" · ").ifBlank { null },
        ).joinToString("\n").ifBlank { null }

        return AppFlat(
            adId = adId,
            adType = AdType.RENT,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.ATHOME,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = images,
            mainPrice = mainPrice,
            secondPrice = secondPrice,
            totalArea = area,
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
            rooms = parseMadoriRooms(madori),
            district = buildingTitle?.substringBefore(" ")?.takeIf { it.isNotBlank() },
            address = address,
            metroStation = metro,
            description = description,
            yearBuilt = yearBuilt,
        )
    }

    private fun extractApartmentLd(html: String): JsonObject? {
        for (match in ldJsonRe.findAll(html)) {
            val root = runCatching { json.parseToJsonElement(match.groupValues[1]) }.getOrNull()
                ?: continue
            findApartment(root)?.let { return it }
        }
        return null
    }

    private fun findApartment(element: kotlinx.serialization.json.JsonElement): JsonObject? {
        val obj = element.asObjectOrNull() ?: return null
        when (obj["@type"]?.contentOrNull()) {
            "Apartment" -> return obj
        }
        obj["@graph"]?.asArrayOrNull()?.forEach { child ->
            findApartment(child)?.let { return it }
        }
        return null
    }

    private fun extractAgentPhone(html: String): String? {
        for (match in ldJsonRe.findAll(html)) {
            val root = runCatching { json.parseToJsonElement(match.groupValues[1]) }.getOrNull()
                ?: continue
            val phone = findAgentPhone(root)
            if (!phone.isNullOrBlank()) return phone
        }
        return null
    }

    private fun findAgentPhone(element: kotlinx.serialization.json.JsonElement): String? {
        val obj = element.asObjectOrNull() ?: return null
        if (obj["@type"]?.contentOrNull() == "RealEstateAgent") {
            return obj["telephone"]?.contentOrNull()
        }
        obj["@graph"]?.asArrayOrNull()?.forEach { child ->
            findAgentPhone(child)?.let { return it }
        }
        return null
    }

    private fun parseGeo(apartment: JsonObject): Coordinates? {
        val geo = apartment["geo"]?.asObjectOrNull() ?: return null
        val lat = geo["latitude"]?.contentOrNull()?.toDoubleOrNull() ?: return null
        val lng = geo["longitude"]?.contentOrNull()?.toDoubleOrNull() ?: return null
        return Coordinates(lat, lng)
    }

    private fun parseMadoriRooms(madori: String?): Int? {
        if (madori.isNullOrBlank()) return null
        return Regex("""(\d+)""").find(madori)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun decode(raw: String): String = raw
        .replace("&amp;", "&")
        .substringBefore('?')

    private const val BASE = AthomeApiClient.BASE
}
