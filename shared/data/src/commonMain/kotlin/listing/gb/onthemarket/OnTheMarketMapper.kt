package listing.gb.onthemarket

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
 * Maps OnTheMarket `__NEXT_DATA__` → [AppFlat]. GBP → [AppFlat.mainPrice].
 * Search: `props.initialReduxState.results.list[]`; detail: `initialReduxState.property`.
 * See tmp/gb/api/onthemarket/NOTES.md.
 */
object OnTheMarketMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val gbpRe = Regex("""£\s*([\d,]+(?:\.\d+)?)\s*(k|m)?""", RegexOption.IGNORE_CASE)

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val next = extractNextData(html) ?: return emptyList()
        val list = next["props"].asObjectOrNull()
            ?.get("initialReduxState").asObjectOrNull()
            ?.get("results").asObjectOrNull()
            ?.get("list").asArrayOrNull()
            ?: return emptyList()
        return list.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapListItem(item, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val next = extractNextData(html) ?: return base
        val property = next["props"].asObjectOrNull()
            ?.get("initialReduxState").asObjectOrNull()
            ?.get("property").asObjectOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        if (property["id"].contentOrNull() == null && property["id"].longOrNull() == null) {
            return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        }
        val mapped =
            runCatching { mapDetailProperty(property, base.adType ?: AdType.RENT) }.getOrNull()
                ?: return base.copy(
                    flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
                )
        return mapped.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = mapped.description?.takeIf { it.length > (base.description?.length ?: 0) }
                ?: base.description,
            contactInformation = ContactInformation(
                phones = mapped.contactInformation?.phones
                    ?: base.contactInformation?.phones,
                ownerName = mapped.contactInformation?.ownerName
                    ?: base.contactInformation?.ownerName,
            ),
            imageUrls = mapped.imageUrls?.takeIf { it.isNotEmpty() } ?: base.imageUrls,
            coordinates = mapped.coordinates ?: base.coordinates,
            mainPrice = mapped.mainPrice ?: base.mainPrice,
            totalArea = mapped.totalArea ?: base.totalArea,
            amenities = mapped.amenities ?: base.amenities,
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.ONTHEMARKET,
        flatDetailUrl = detailUrl ?: "https://www.onthemarket.com/details/$adId/",
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
        val id = item["id"].longOrNull()
            ?: item["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val price = parseGbp(item["short-price"].contentOrNull())
            ?: parseGbp(item["price"].contentOrNull())
        val location = item["location"].asObjectOrNull()
        val lat = location?.get("lat").doubleOrNull()
        val lon = location?.get("lon").doubleOrNull()
        val coordinates = if (lat != null && lon != null) Coordinates(lat, lon) else null
        val address = item["address"].contentOrNull()
        val bedrooms = item["bedrooms"].intOrNull()
        val (rooms, isStudio) = parseBedrooms(bedrooms, item["property-title"].contentOrNull())
        val agent = item["agent"].asObjectOrNull()
        val phones = agent?.get("telephone").contentOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { listOf(it) }
        val agentName = agent?.get("name").contentOrNull()
        val images = parseListImages(item)
        val detailsPath = item["details-url"].contentOrNull()
            ?: item["property-link"].contentOrNull()
        val detailUrl = when {
            detailsPath.isNullOrBlank() -> "https://www.onthemarket.com/details/$id/"
            detailsPath.startsWith("http") -> detailsPath
            else -> "https://www.onthemarket.com${if (detailsPath.startsWith("/")) detailsPath else "/$detailsPath"}"
        }
        val title = item["property-title"].contentOrNull()?.stripHtmlToPlainText()
        val features = item["features"].asArrayOrNull()
            ?.mapNotNull {
                it.contentOrNull()?.stripHtmlToPlainText()?.takeIf { t -> t.isNotBlank() }
            }
            ?.distinct()
            ?.ifEmpty { null }
        val hasCoordsOrPhone = coordinates != null || !phones.isNullOrEmpty()
        val published = listing.gb.GbPublishedAt.fromOnTheMarket(item)

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = hasCoordsOrPhone,
                coordsEnriched = coordinates != null,
            ),
            contactInformation = ContactInformation(phones = phones, ownerName = agentName),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.ONTHEMARKET,
            flatDetailUrl = detailUrl,
            publishedAt = published?.publishedAt,
            publishedAtServer = published?.publishedAtServer,
            publishedAtUi = published?.publishedAtUi,
            imageUrls = images,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = address?.substringAfterLast(',')?.trim(),
            address = address,
            metroStation = null,
            description = title,
            yearBuilt = null,
            totalArea = null,
            livingArea = null,
            kitchenArea = null,
            floor = null,
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
            amenities = features,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = null,
        )
    }

    private fun mapDetailProperty(property: JsonObject, adType: AdType): AppFlat {
        val id = property["id"].longOrNull()
            ?: property["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val price = property["priceRaw"].doubleOrNull()
            ?: parseGbp(property["shortPrice"].contentOrNull())
            ?: parseGbp(property["price"].contentOrNull())
        val location = property["location"].asObjectOrNull()
        val lat = location?.get("lat").doubleOrNull()
        val lon = location?.get("lon").doubleOrNull()
        val coordinates = if (lat != null && lon != null) Coordinates(lat, lon) else null
        val address = property["displayAddress"].contentOrNull()
            ?: property["addressLocality"].contentOrNull()
        val bedrooms = property["bedrooms"].intOrNull()
        val (rooms, isStudio) = parseBedrooms(
            bedrooms,
            property["propertyTitle"].contentOrNull(),
        )
        val agent = property["agent"].asObjectOrNull()
        val phones = agent?.get("telephone").contentOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { listOf(it) }
        val agentName = agent?.get("name").contentOrNull()
        val images = parseDetailImages(property)
        val canonical = property["canonicalUrl"].contentOrNull()
        val detailUrl = when {
            !canonical.isNullOrBlank() && canonical.startsWith("http") -> canonical
            !canonical.isNullOrBlank() -> "https://www.onthemarket.com$canonical"
            else -> "https://www.onthemarket.com/details/$id/"
        }
        val description = property["description"].contentOrNull()?.stripHtmlToPlainText()
            ?: property["summary"].contentOrNull()?.stripHtmlToPlainText()
        val features = property["features"].asArrayOrNull()
            ?.mapNotNull {
                it.contentOrNull()?.stripHtmlToPlainText()?.takeIf { t -> t.isNotBlank() }
            }
            ?.distinct()
            ?.ifEmpty { null }
        val totalArea = property["minimumAreaSqM"].doubleOrNull()
            ?: property["minimumArea"].doubleOrNull()
        val published = listing.gb.GbPublishedAt.fromOnTheMarket(property)

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            contactInformation = ContactInformation(phones = phones, ownerName = agentName),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.ONTHEMARKET,
            flatDetailUrl = detailUrl,
            publishedAt = published?.publishedAt,
            publishedAtServer = published?.publishedAtServer,
            publishedAtUi = published?.publishedAtUi,
            imageUrls = images,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = address?.substringAfterLast(',')?.trim(),
            address = address,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = totalArea,
            livingArea = null,
            kitchenArea = null,
            floor = null,
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
            amenities = features,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = null,
        )
    }

    private fun parseListImages(item: JsonObject): List<String>? = buildList {
        item["cover-image"].asObjectOrNull()?.get("default")?.contentOrNull()?.let { add(it) }
        item["images"].asArrayOrNull()?.forEach { img ->
            img.asObjectOrNull()?.get("default")?.contentOrNull()?.let { add(it) }
        }
    }.distinct().take(20).ifEmpty { null }

    private fun parseDetailImages(property: JsonObject): List<String>? = buildList {
        property["images"].asArrayOrNull()?.forEach { img ->
            val o = img.asObjectOrNull() ?: return@forEach
            o["largeUrl"].contentOrNull()?.let { add(it) }
                ?: o["url"].contentOrNull()?.let { add(it) }
        }
    }.distinct().take(20).ifEmpty { null }

    private fun parseBedrooms(bedrooms: Int?, title: String?): Pair<Int?, Boolean?> {
        if (bedrooms != null) {
            return bedrooms to (bedrooms == 0)
        }
        val t = title?.lowercase().orEmpty()
        if (t.contains("studio")) return 0 to true
        return null to null
    }

    private fun parseGbp(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val match = gbpRe.find(raw.replace('\u00A0', ' ')) ?: return null
        val num = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        return when (match.groupValues.getOrNull(2)?.lowercase()) {
            "k" -> num * 1_000
            "m" -> num * 1_000_000
            else -> num
        }
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
}
