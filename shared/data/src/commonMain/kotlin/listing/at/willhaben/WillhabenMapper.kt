package listing.at.willhaben

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.longOrNull
import listing.de.DeRelativeTime
import utils.stripHtmlToPlainText

/**
 * Maps willhaben `__NEXT_DATA__` SERP + detail → [AppFlat]. EUR → [AppFlat.mainPrice].
 * See tmp/at/api/willhaben/NOTES.md.
 */
object WillhabenMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val euroRe = Regex("""([\d.]+(?:,\d+)?)\s*€""")
    private val areaRe = Regex("""([\d]+(?:[.,]\d+)?)\s*m²""", RegexOption.IGNORE_CASE)
    private val roomsRe = Regex("""(\d+)\s*Zimmer""", RegexOption.IGNORE_CASE)

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val next = extractNextData(html) ?: return emptyList()
        val pageProps = next["props"].asObjectOrNull()?.get("pageProps").asObjectOrNull()
            ?: return emptyList()
        val searchResult = pageProps["searchResult"].asObjectOrNull()
            ?: pageProps["initialSearchResult"].asObjectOrNull()
            ?: return emptyList()
        val summaries = searchResult["advertSummaryList"].asObjectOrNull()
            ?.get("advertSummary").asArrayOrNull()
            ?: searchResult["advertSummary"].asArrayOrNull()
            ?: return emptyList()
        return summaries.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapSummary(item, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val next = extractNextData(html) ?: return base
        val details = next["props"].asObjectOrNull()
            ?.get("pageProps").asObjectOrNull()
            ?.get("advertDetails").asObjectOrNull()
            ?: return base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
        val attrs = attributeMap(details["attributes"])
        val description = attrs["BODY_DYN"]
            ?: attrs["DESCRIPTION"]
            ?: details["description"].contentOrNull()
        val phones = details["organisationDetails"].asObjectOrNull()
            ?.get("orgPhone").contentOrNull()
            ?.let { listOf(it.replace(Regex("""[\s/-]"""), "")) }
            ?: base.contactInformation?.phones
        val coords = parseCoordinates(attrs["COORDINATES"]) ?: base.coordinates
        val price = parseEuro(attrs["PRICE"] ?: attrs["PRICE_FOR_DISPLAY"]) ?: base.mainPrice
        val rooms = attrs["NO_OF_ROOMS"]?.toDoubleOrNull()?.toInt()
            ?: roomsRe.find(attrs["HEADING"].orEmpty())?.groupValues?.get(1)?.toIntOrNull()
            ?: base.rooms
        val area = parseArea(
            attrs["ESTATE_SIZE"] ?: attrs["LIVING_AREA"] ?: attrs["HEADING"],
        ) ?: base.totalArea
        val images = details["advertImageList"].asObjectOrNull()
            ?.get("advertImage").asArrayOrNull()
            ?.mapNotNull { img ->
                img.asObjectOrNull()?.get("mainImageUrl").contentOrNull()
                    ?: img.asObjectOrNull()?.get("thumbnailImageUrl").contentOrNull()
            }
            ?.distinct()
            ?.take(20)
            ?.ifEmpty { null }
            ?: base.imageUrls

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description?.stripHtmlToPlainText()?.takeIf { it.length > 20 }
                ?: base.description,
            mainPrice = price,
            rooms = rooms,
            totalArea = area,
            coordinates = coords,
            address = attrs["LOCATION"] ?: base.address,
            district = attrs["LOCATION"]?.substringBefore(',')?.trim() ?: base.district,
            imageUrls = images,
            contactInformation = ContactInformation(
                phones = phones,
                ownerName = details["organisationDetails"].asObjectOrNull()
                    ?.get("orgName").contentOrNull()
                    ?: base.contactInformation?.ownerName,
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
        flatPlatform = FlatPlatform.WILLHABEN,
        flatDetailUrl = detailUrl ?: "https://www.willhaben.at/iad/object?adId=$adId",
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

    private fun mapSummary(item: JsonObject, adType: AdType): AppFlat {
        val adId = item["id"].longOrNull()
            ?: item["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val attrs = attributeMap(item["attributes"])
        val heading = attrs["HEADING"]
        val price = parseEuro(attrs["PRICE"] ?: attrs["PRICE_FOR_DISPLAY"])
            ?: heading?.let { euroRe.find(it)?.groupValues?.get(1) }?.let { parseEuro(it) }
        val rooms = attrs["NO_OF_ROOMS"]?.toDoubleOrNull()?.toInt()
            ?: heading?.let { roomsRe.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val area = parseArea(attrs["ESTATE_SIZE"] ?: attrs["LIVING_AREA"] ?: heading)
        val coords = parseCoordinates(attrs["COORDINATES"])
        val seoUrl = attrs["SEO_URL"]
        val detailUrl = when {
            seoUrl.isNullOrBlank() -> "https://www.willhaben.at/iad/object?adId=$adId"
            seoUrl.startsWith("http") -> seoUrl
            else -> "https://www.willhaben.at$seoUrl"
        }
        val publishedRaw = attrs["PUBLISHED_String"]
        val publishedAt = DeRelativeTime.parse(publishedRaw)
        val images = item["advertImageList"].asObjectOrNull()
            ?.get("advertImage").asArrayOrNull()
            ?.mapNotNull { img ->
                img.asObjectOrNull()?.get("mainImageUrl").contentOrNull()
                    ?: img.asObjectOrNull()?.get("thumbnailImageUrl").contentOrNull()
            }
            ?.distinct()
            ?.take(12)
            ?.ifEmpty { null }

        return AppFlat(
            adId = adId,
            adType = adType,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = false,
                coordsEnriched = coords != null,
            ),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.WILLHABEN,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = publishedRaw,
            publishedAtUi = publishedRaw,
            imageUrls = images,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = attrs["LOCATION"]?.substringBefore(',')?.trim(),
            address = attrs["LOCATION"],
            metroStation = null,
            description = heading,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = heading?.contains("Studio", ignoreCase = true) == true,
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

    private fun attributeMap(attributes: JsonElement?): Map<String, String> {
        val root = attributes.asObjectOrNull() ?: return emptyMap()
        val list = root["attribute"].asArrayOrNull()
        if (list != null) {
            return list.mapNotNull { el ->
                val obj = el.asObjectOrNull() ?: return@mapNotNull null
                val name = obj["name"].contentOrNull() ?: return@mapNotNull null
                val value = obj["values"].asArrayOrNull()?.firstOrNull()?.contentOrNull()
                    ?: obj["value"].contentOrNull()
                    ?: return@mapNotNull null
                name to value
            }.toMap()
        }
        return root.entries.mapNotNull { (key, value) ->
            val text = value.contentOrNull()
                ?: value.asArrayOrNull()?.firstOrNull()?.contentOrNull()
            text?.let { key to it }
        }.toMap()
    }

    private fun parseEuro(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace('\u00A0', ' ')
        euroRe.find(cleaned)?.groupValues?.get(1)?.let {
            return it.replace(".", "").replace(',', '.').toDoubleOrNull()
        }
        return cleaned.replace(Regex("""[^\d,.]"""), "")
            .replace(".", "")
            .replace(',', '.')
            .toDoubleOrNull()
    }

    private fun parseArea(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        areaRe.find(raw.replace('\u00A0', ' '))?.groupValues?.get(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()
            ?.let { return it }
        return null
    }

    private fun parseCoordinates(raw: String?): Coordinates? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(',', ';', ' ')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        return Coordinates(lat, lng)
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
