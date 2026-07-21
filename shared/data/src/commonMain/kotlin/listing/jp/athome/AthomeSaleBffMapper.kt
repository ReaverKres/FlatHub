package listing.jp.athome

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.jp.parseAreaSqm
import listing.jp.stableAdId

/**
 * athome.co.jp sale BFF JSON list mapper (`/csite-bff/sell-living/bukken/list/first-view`).
 */
object AthomeSaleBffMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun mapSearch(raw: String): List<AppFlat> {
        if (raw.trimStart().startsWith("<")) {
            AthomeApiClient.ensureNotBlocked(raw, "Athome sale BFF parse")
            return emptyList()
        }
        val root = runCatching { json.parseToJsonElement(raw).asObjectOrNull() }.getOrNull()
            ?: return emptyList()
        val list = root["data"]?.asObjectOrNull()
            ?.get("bukkenData")?.asObjectOrNull()
            ?.get("bukkenList")?.asArrayOrNull()
            ?: return emptyList()
        return list.mapNotNull { mapItem(it.asObjectOrNull()) }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val merged = AthomeRentHtmlParser.mergeDetail(base, html)
        if (merged.coordinates != null || merged.flatDevInfo.isDetailLoaded) return merged
        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.SALE,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.ATHOME,
        flatDetailUrl = detailUrl ?: "$BASE/kodate/",
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

    private fun mapItem(item: JsonObject?): AppFlat? {
        if (item == null) return null
        val bukkenNo = item["bukkenNo"].contentOrNull() ?: return null
        val adId = bukkenNo.toLongOrNull() ?: stableAdId(bukkenNo)
        val seoRoma = item["seoRoma"].contentOrNull() ?: "kodate"
        val segment = seoRoma.substringBefore('/').ifBlank { "kodate" }
        val detailUrl = "$BASE/$segment/$bukkenNo/"

        val title = item["title"].contentOrNull()
        val location = item["location"].contentOrNull()
        val madori = item["madori"].contentOrNull()
        val mainPrice = parseSalePrice(item["kakaku"]?.asObjectOrNull())
        val priceLabel = formatSalePriceLabel(item["kakaku"]?.asObjectOrNull())
        val metro = item["access"]?.asArrayOrNull()
            ?.mapNotNull {
                it.asObjectOrNull()?.get("accessText")?.contentOrNull()
                    ?.takeIf { t -> t.isNotBlank() }
            }
            ?.firstOrNull()
        val buildingArea = parseAreaSqm(item["buildingFromTo"].contentOrNull())
        val landArea = parseAreaSqm(item["landareaFromTo"].contentOrNull())
        val yearBuilt = item["buildComplete"].contentOrNull()
            ?.let { Regex("""(\d{4})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val images = extractImages(item["images"]).ifEmpty { null }

        val description = listOfNotNull(
            title,
            priceLabel?.let { "価格: $it" },
            item["buildingFromTo"].contentOrNull()?.let { "建物: $it" },
            item["landareaFromTo"].contentOrNull()?.let { "土地: $it" },
            item["buildComplete"].contentOrNull()?.let { "完成: $it" },
        ).joinToString("\n").ifBlank { null }

        return AppFlat(
            adId = adId,
            adType = AdType.SALE,
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
            secondPrice = null,
            totalArea = buildingArea ?: landArea,
            livingArea = buildingArea,
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
            district = location?.substringBefore(" ")?.takeIf { it.isNotBlank() },
            address = location,
            metroStation = metro,
            description = description,
            yearBuilt = yearBuilt,
        )
    }

    private fun parseSalePrice(kakaku: JsonObject?): Double? {
        val first = kakaku?.get("priceText")?.asArrayOrNull()?.firstOrNull()?.asObjectOrNull()
            ?: return null
        return priceEntryYen(first)
    }

    private fun formatSalePriceLabel(kakaku: JsonObject?): String? {
        val entries = kakaku?.get("priceText")?.asArrayOrNull() ?: return null
        val parts = entries.mapNotNull { entry ->
            val obj = entry.asObjectOrNull() ?: return@mapNotNull null
            formatPriceEntry(obj)
        }
        if (parts.isEmpty()) return null
        val buffer = kakaku["bufferText"].contentOrNull().orEmpty()
        return parts.joinToString(buffer.ifBlank { " · " })
    }

    private fun formatPriceEntry(entry: JsonObject): String? {
        val oku = entry["priceOku"].contentOrNull()?.replace(",", "")?.trim().orEmpty()
        val man = entry["priceMan"].contentOrNull()?.replace(",", "")?.trim().orEmpty()
        if (oku.isBlank() && man.isBlank()) return null
        return buildString {
            if (oku.isNotBlank()) append("${oku}億")
            if (man.isNotBlank()) append("${man}万")
            append("円")
        }
    }

    private fun priceEntryYen(entry: JsonObject): Double? {
        val oku = entry["priceOku"].contentOrNull()?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        val man = entry["priceMan"].contentOrNull()?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        if (oku == 0.0 && man == 0.0) return null
        return oku * 100_000_000 + man * 10_000
    }

    private fun extractImages(imagesEl: JsonElement?): List<String> {
        return when (imagesEl) {
            is JsonArray -> imagesEl.mapNotNull { resolveImageUrl(it.asObjectOrNull()) }
            is JsonObject -> listOfNotNull(resolveImageUrl(imagesEl))
            else -> emptyList()
        }.distinct()
    }

    private fun resolveImageUrl(item: JsonObject?): String? {
        if (item == null) return null
        item["thumnailImageUrl"].contentOrNull()?.takeIf { it.isNotBlank() }?.let { path ->
            return if (path.startsWith("http")) path else BASE + path
        }
        item["originalImageUrl"].contentOrNull()?.takeIf { it.isNotBlank() }?.let { path ->
            return when {
                path.startsWith("http") -> path
                path.startsWith("/") -> BASE + path
                else -> "$BASE/$path"
            }
        }
        return null
    }

    private fun parseMadoriRooms(madori: String?): Int? {
        if (madori.isNullOrBlank()) return null
        return Regex("""(\d+)""").find(madori)?.groupValues?.get(1)?.toIntOrNull()
    }

    private const val BASE = AthomeApiClient.BASE
}
