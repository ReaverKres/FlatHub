package listing.kz.krisha

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import utils.stripHtmlToPlainText
import kotlin.time.Instant

/**
 * SSR HTML parser for Krisha list cards + `window.data` on detail.
 */
object KrishaHtmlParser {
    private val json = Json { ignoreUnknownKeys = true }

    private val cardSplitRe =
        Regex("""<div[^>]*class="[^"]*a-card[^"]*"[^>]*data-id="(\d{6,})"[^>]*>""")
    private val titleRe =
        Regex("""class="a-card__title[^"]*"[^>]*>\s*([^<]+)\s*<""", RegexOption.IGNORE_CASE)

    // Nested: <span class="a-card__price-text">300&nbsp;000&nbsp;<span class="currency…
    private val priceRe =
        Regex(
            """class="a-card__price-text"[^>]*>\s*((?:\d|&nbsp;|\s|\u00a0)+)""",
            RegexOption.IGNORE_CASE,
        )
    private val subtitleRe =
        Regex("""class="a-card__subtitle[^"]*"[^>]*>\s*([^<]+)\s*<""", RegexOption.IGNORE_CASE)
    private val previewRe =
        Regex(
            """class="a-card__text-preview"[^>]*>\s*([\s\S]*?)\s*</div>""",
            RegexOption.IGNORE_CASE,
        )
    private val imgRe =
        Regex(
            """<img[^>]+src="(https://krisha-photos\.kcdn\.online/[^"]+)"""",
            RegexOption.IGNORE_CASE
        )
    private val roomsRe = Regex("""(\d+)\s*-\s*комнат""", RegexOption.IGNORE_CASE)
    private val areaRe = Regex("""(\d+(?:[.,]\d+)?)\s*м""", RegexOption.IGNORE_CASE)
    private val floorRe = Regex("""(\d+)\s*этаж""", RegexOption.IGNORE_CASE)

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val matches = cardSplitRe.findAll(html).toList()
        if (matches.isEmpty()) return emptyList()
        return matches.mapIndexedNotNull { index, match ->
            val id = match.groupValues[1].toLongOrNull() ?: return@mapIndexedNotNull null
            val start = match.range.first
            val end = matches.getOrNull(index + 1)?.range?.first ?: (start + 12_000)
            val chunk = html.substring(start, minOf(end, html.length))
            runCatching { mapCard(id, chunk, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val data = extractWindowData(html) ?: return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
        )
        val advert = data["advert"]?.jsonObject
        val advertUi = data["adverts"]?.jsonArray?.firstOrNull()?.jsonObject
        val price = advert?.get("price")?.jsonPrimitive?.doubleOrNull ?: base.mainPrice
        val rooms = advert?.get("rooms")?.jsonPrimitive?.intOrNull ?: base.rooms
        val area = advert?.get("square")?.jsonPrimitive?.doubleOrNull ?: base.totalArea
        val title = advert?.get("title")?.jsonPrimitive?.contentOrNull
        val addressTitle = advert?.get("addressTitle")?.jsonPrimitive?.contentOrNull
        val fullAddress = advertUi?.get("fullAddress")?.jsonPrimitive?.contentOrNull
        val district = advert?.get("address")?.jsonObject
            ?.get("district")?.jsonPrimitive?.contentOrNull
            ?: base.district
        val description = advertUi?.get("description")?.jsonPrimitive?.contentOrNull
            ?.stripHtmlToPlainText()
            ?: base.description
        val priceM2 = advertUi?.get("priceM2")?.jsonPrimitive?.doubleOrNull
            ?: base.secondPriceSquare
        val map = advert?.get("map")?.jsonObject
        val lat = map?.get("lat")?.jsonPrimitive?.doubleOrNull
        val lon = map?.get("lon")?.jsonPrimitive?.doubleOrNull
        val coordinates =
            if (lat != null && lon != null) Coordinates(lat, lon) else base.coordinates
        val photos = advert?.get("photos")?.jsonArray?.mapNotNull { el ->
            el.jsonObject["src"]?.jsonPrimitive?.contentOrNull
                ?: el.jsonObject["path"]?.jsonPrimitive?.contentOrNull
        }?.ifEmpty { null } ?: base.imageUrls
        val created = advertUi?.get("createdAt")?.jsonPrimitive?.contentOrNull
            ?: advertUi?.get("addedAt")?.jsonPrimitive?.contentOrNull
        val publishedAt = created?.let {
            runCatching { Instant.parse("${it}T00:00:00Z") }.getOrNull()
                ?: runCatching { Instant.parse(it) }.getOrNull()
        } ?: base.publishedAt
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: base.publishedAtUi
        val ownerName = advert?.get("ownerName")?.jsonPrimitive?.contentOrNull
            ?: base.contactInformation?.ownerName
        val floor =
            title?.let { floorRe.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: base.floor

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            mainPrice = price,
            mainPriceSquare = priceM2,
            rooms = rooms,
            totalArea = area,
            floor = floor,
            district = district,
            address = fullAddress ?: addressTitle ?: base.address,
            description = description ?: title ?: base.description,
            coordinates = coordinates,
            imageUrls = photos,
            publishedAtServer = created ?: base.publishedAtServer,
            publishedAtUi = publishedAtUi,
            publishedAt = publishedAt,
            contactInformation = ContactInformation(
                phones = base.contactInformation?.phones,
                ownerName = ownerName,
            ),
        )
    }

    private fun mapCard(id: Long, chunk: String, adType: AdType): AppFlat {
        val title = titleRe.find(chunk)?.groupValues?.get(1)?.trim()
            ?.replace("&nbsp;", " ")
            ?.decodeHtmlEntities()
        val price = priceRe.find(chunk)?.groupValues?.get(1)
            ?.replace("&nbsp;", "", ignoreCase = true)
            ?.replace(Regex("""[^\d]"""), "")
            ?.toDoubleOrNull()
            ?.takeIf { it > 0 }
        val subtitle = subtitleRe.find(chunk)?.groupValues?.get(1)?.trim()
            ?.replace("&nbsp;", " ")
            ?.decodeHtmlEntities()
        val preview = previewRe.find(chunk)?.groupValues?.get(1)?.stripHtmlToPlainText()
        val image = imgRe.find(chunk)?.groupValues?.get(1)
        val rooms = title?.let { roomsRe.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val area = title?.let {
            areaRe.find(it)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        }
        val floor = title?.let { floorRe.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val district = subtitle?.substringBefore(',')?.trim()

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.KRISHA,
            flatDetailUrl = "https://krisha.kz/a/show/$id",
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = image?.let { listOf(it) },
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = district,
            address = subtitle,
            metroStation = null,
            description = preview ?: title,
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

    /** Room miss fallback so detail HTML can still be fetched/merged. */
    fun listStub(adId: Long, adType: AdType = AdType.RENT): AppFlat = AppFlat(
        adId = adId,
        adType = adType,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.KRISHA,
        flatDetailUrl = "https://krisha.kz/a/show/$adId",
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

    private fun extractWindowData(html: String): JsonObject? {
        val marker = "window.data = "
        val start = html.indexOf(marker)
        if (start < 0) return null
        val jsonStart = start + marker.length
        var depth = 0
        var inStr = false
        var esc = false
        var i = jsonStart
        while (i < html.length) {
            val c = html[i]
            if (inStr) {
                if (esc) esc = false
                else if (c == '\\') esc = true
                else if (c == '"') inStr = false
            } else {
                when (c) {
                    '"' -> inStr = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            val raw = html.substring(jsonStart, i + 1)
                            return runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
                        }
                    }
                }
            }
            i++
        }
        return null
    }

    private fun String.decodeHtmlEntities(): String =
        replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
}
