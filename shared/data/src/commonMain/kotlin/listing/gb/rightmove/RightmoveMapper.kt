package listing.gb.rightmove

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
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText

/**
 * Maps Rightmove JSON list / `__NEXT_DATA__` / `PAGE_MODEL` → [AppFlat].
 * GBP → [AppFlat.mainPrice]. See tmp/gb/api/rightmove/NOTES.md.
 */
object RightmoveMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val gbpRe = Regex("""£([\d,]+(?:\.\d+)?)""")

    fun mapSearchJson(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["properties"].asArrayOrNull() ?: return emptyList()
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapListItem(item, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun parseSearchHtml(html: String, adType: AdType): List<AppFlat> {
        val next = extractNextData(html) ?: return emptyList()
        val items = next["props"].asObjectOrNull()
            ?.get("pageProps").asObjectOrNull()
            ?.get("searchResults").asObjectOrNull()
            ?.get("properties").asArrayOrNull()
            ?: return emptyList()
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapListItem(item, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val propertyData = extractPageModel(html)?.get("propertyData").asObjectOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        val price = parsePriceAmount(propertyData["prices"].asObjectOrNull())
            ?: propertyData["prices"].asObjectOrNull()
                ?.get("primaryPrice").contentOrNull()?.let(::parseGbpDisplay)
            ?: base.mainPrice
        val rooms = propertyData["bedrooms"].intOrNull() ?: base.rooms
        val area = parseAreaSqft(propertyData) ?: base.totalArea
        val loc = propertyData["location"].asObjectOrNull()
        val lat = loc?.get("latitude").doubleOrNull()
            ?: propertyData["latitude"].doubleOrNull()
        val lng = loc?.get("longitude").doubleOrNull()
            ?: propertyData["longitude"].doubleOrNull()
        val coordinates =
            if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates
        val address = propertyData["address"].asObjectOrNull()
            ?.get("displayAddress").contentOrNull()
            ?: base.address
        val phones = parsePhones(propertyData["contactInfo"].asObjectOrNull())
            ?: base.contactInformation?.phones
        val owner = propertyData["customer"].asObjectOrNull()
            ?.get("branchDisplayName").contentOrNull()
            ?: propertyData["customer"].asObjectOrNull()
                ?.get("companyName").contentOrNull()
            ?: base.contactInformation?.ownerName
        val description = propertyData["text"].asObjectOrNull()
            ?.get("description").contentOrNull()
            ?.stripHtmlToPlainText()
            ?: base.description
        val images = propertyData["images"].asArrayOrNull()
            ?.mapNotNull { img ->
                img.asObjectOrNull()?.get("url").contentOrNull()
                    ?: img.asObjectOrNull()?.get("srcUrl").contentOrNull()
            }
            ?.distinct()
            ?.take(20)
            ?.ifEmpty { null }
            ?: base.imageUrls
        val floor = propertyData["floorLevel"].contentOrNull()?.toIntOrNull() ?: base.floor
        val published = parsePublishedAt(propertyData)

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            mainPrice = price,
            rooms = rooms,
            totalArea = area,
            floor = floor,
            coordinates = coordinates,
            address = address,
            district = address?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
                ?: base.district,
            description = description,
            imageUrls = images,
            publishedAt = published?.first ?: base.publishedAt,
            publishedAtServer = published?.second ?: base.publishedAtServer,
            publishedAtUi = published?.third ?: base.publishedAtUi,
            contactInformation = ContactInformation(phones = phones, ownerName = owner),
        )
    }

    fun listStub(adId: Long, adType: AdType = AdType.RENT): AppFlat = AppFlat(
        adId = adId,
        adType = adType,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.RIGHTMOVE,
        flatDetailUrl = "https://www.rightmove.co.uk/properties/$adId",
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

    private fun mapListItem(item: JsonObject, adType: AdType): AppFlat {
        val adId = item["id"].longOrNull()
            ?: item["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val priceObj = item["price"].asObjectOrNull()
        val price = parsePriceAmount(priceObj)
            ?: priceObj?.get("displayPrices").asArrayOrNull()
                ?.firstOrNull()?.asObjectOrNull()
                ?.get("displayPrice").contentOrNull()?.let(::parseGbpDisplay)
            ?: item["displayPrice"].contentOrNull()?.let(::parseGbpDisplay)
        val rooms = item["bedrooms"].intOrNull()
        val lat = item["latitude"].doubleOrNull()
            ?: item["location"].asObjectOrNull()?.get("latitude").doubleOrNull()
        val lng = item["longitude"].doubleOrNull()
            ?: item["location"].asObjectOrNull()?.get("longitude").doubleOrNull()
        val coordinates =
            if (lat != null && lng != null) Coordinates(lat, lng) else null
        val detailPath = item["propertyUrl"].contentOrNull()
            ?.substringBefore('#')
            ?.takeIf { it.isNotBlank() }
        val detailUrl = if (detailPath?.startsWith("http") == true) {
            detailPath
        } else {
            "https://www.rightmove.co.uk${detailPath ?: "/properties/$adId"}"
        }
        val images = buildList {
            item["propertyImages"].asObjectOrNull()
                ?.get("images").asArrayOrNull()
                ?.forEach { img ->
                    img.asObjectOrNull()?.get("srcUrl").contentOrNull()?.let { add(it) }
                }
            item["images"].asArrayOrNull()?.forEach { img ->
                img.asObjectOrNull()?.get("srcUrl").contentOrNull()?.let { add(it) }
                    ?: img.asObjectOrNull()?.get("url").contentOrNull()?.let { add(it) }
            }
            item["thumbnailPhotos"].asArrayOrNull()?.forEach { img ->
                img.asObjectOrNull()?.get("url").contentOrNull()?.let { add(it) }
            }
        }.distinct().take(20).ifEmpty { null }
        val address = item["displayAddress"].contentOrNull()
        val description = item["summary"].contentOrNull()?.stripHtmlToPlainText()
            ?: item["propertyTypeFullDescription"].contentOrNull()
        val phones = item["contactTelephoneNumber"].contentOrNull()?.let { listOf(it) }
            ?: item["customer"].asObjectOrNull()
                ?.get("contactTelephone").contentOrNull()?.let { listOf(it) }
        val owner = item["customer"].asObjectOrNull()
            ?.get("branchDisplayName").contentOrNull()
        val published = parsePublishedAt(item)
        val isStudio = rooms == 0 ||
                item["propertyType"].contentOrNull()?.contains("studio", ignoreCase = true) == true

        return AppFlat(
            adId = adId,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = phones, ownerName = owner),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.RIGHTMOVE,
            flatDetailUrl = detailUrl,
            publishedAt = published?.first,
            publishedAtServer = published?.second,
            publishedAtUi = published?.third,
            imageUrls = images,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms?.takeIf { it > 0 },
            district = address?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() },
            address = address,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = parseAreaSqft(item),
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
            owner = null,
        )
    }

    /** Returns (publishedAt, publishedAtServer, publishedAtUi) from real Rightmove date fields. */
    private fun parsePublishedAt(obj: JsonObject): Triple<kotlin.time.Instant?, String?, String?>? {
        val parsed = listing.gb.GbPublishedAt.fromRightmove(obj) ?: return null
        return Triple(parsed.publishedAt, parsed.publishedAtServer, parsed.publishedAtUi)
    }

    private fun parsePriceAmount(priceObj: JsonObject?): Double? =
        priceObj?.get("amount").doubleOrNull()
            ?: priceObj?.get("amount").contentOrNull()?.replace(",", "")?.toDoubleOrNull()

    private fun parseGbpDisplay(raw: String): Double? {
        val m = gbpRe.find(raw.replace('\u00A0', ' ')) ?: return null
        return m.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    /** Rightmove area on wire is sq ft — store as m² for Flatzen cards/filter. */
    private fun parseAreaSqft(obj: JsonObject): Double? {
        obj["sizings"].asArrayOrNull()?.forEach { sizing ->
            val s = sizing.asObjectOrNull() ?: return@forEach
            val unit = s["unit"].contentOrNull()?.lowercase()
            val min = s["minimumSize"].doubleOrNull() ?: s["minimumSize"].contentOrNull()
                ?.replace(",", "")?.toDoubleOrNull()
            if (min != null && unit?.contains("sqft") == true) {
                return min * SQFT_TO_M2
            }
            if (min != null && unit?.contains("sqm") == true) {
                return min
            }
        }
        return obj["size"].asObjectOrNull()?.get("value").doubleOrNull()
    }

    private fun parsePhones(contact: JsonObject?): List<String>? {
        if (contact == null) return null
        val numbers = buildList {
            contact["telephoneNumbers"].asObjectOrNull()?.let { t ->
                t["localNumber"].contentOrNull()?.let { add(it) }
                t["internationalNumber"].contentOrNull()?.let { add(it) }
            }
            contact["telephoneNumbers"].asArrayOrNull()?.forEach { el ->
                el.asObjectOrNull()?.get("localNumber").contentOrNull()?.let { add(it) }
            }
            contact["telephone"].contentOrNull()?.let { add(it) }
        }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return numbers.ifEmpty { null }
    }

    private fun extractNextData(html: String): JsonObject? {
        val marker = "id=\"__NEXT_DATA__\""
        val idx = html.indexOf(marker)
        if (idx < 0) return null
        val start = html.indexOf('>', idx) + 1
        if (start <= 0) return null
        val end = html.indexOf("</script>", start)
        if (end < 0) return null
        return runCatching {
            json.parseToJsonElement(html.substring(start, end)).asObjectOrNull()
        }.getOrNull()
    }

    private fun extractPageModel(html: String): JsonObject? {
        val marker = "PAGE_MODEL = "
        val idx = html.indexOf(marker)
        if (idx < 0) return null
        var start = idx + marker.length
        while (start < html.length && html[start].isWhitespace()) start++
        if (start >= html.length || html[start] != '{') return null
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until html.length) {
            val c = html[i]
            if (inString) {
                if (escaped) escaped = false
                else if (c == '\\') escaped = true
                else if (c == '"') inString = false
                continue
            }
            when (c) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        val blob = html.substring(start, i + 1)
                        return runCatching {
                            json.parseToJsonElement(blob).asObjectOrNull()
                        }.getOrNull()
                    }
                }
            }
        }
        return null
    }

    private const val SQFT_TO_M2 = 0.092903
}
