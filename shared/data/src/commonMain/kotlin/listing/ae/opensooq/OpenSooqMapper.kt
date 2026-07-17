package listing.ae.opensooq

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText
import kotlin.time.Instant

/**
 * Maps OpenSooq `__NEXT_DATA__` SERP items + detail JSON-LD → [AppFlat].
 * AED → [AppFlat.priceByn]. See tmp/ae/api/opensooq/NOTES.md.
 */
object OpenSooqMapper {
    private val json = Json { ignoreUnknownKeys = true }
    private val bedroomsRe = Regex("""(\d+)\s*Bedroom""", RegexOption.IGNORE_CASE)
    private val areaRe = Regex("""Area:\s*([\d]+(?:[.,]\d+)?)\s*m2""", RegexOption.IGNORE_CASE)
    private val floorWordRe = Regex(
        """(?i)(Ground|First|Second|Third|Fourth|Fifth|Sixth|Seventh|Eighth|Ninth|Tenth|\d+)(?:st|nd|rd|th)?\s+Floor""",
    )
    private val priceRe = Regex("""([\d,]+(?:\.\d+)?)\s*AED""", RegexOption.IGNORE_CASE)
    private val ldGeoLatRe = Regex(""""latitude"\s*:\s*"?(-?\d+(?:\.\d+)?)""")
    private val ldGeoLngRe = Regex(""""longitude"\s*:\s*"?(-?\d+(?:\.\d+)?)""")
    private val ldRoomsRe = Regex(""""numberOfRooms"\s*:\s*"?(\d+)""")
    private val ldPriceRe = Regex(""""price"\s*:\s*"?([\d.]+)""")
    private val ldDescRe = Regex(""""description"\s*:\s*"((?:\\.|[^"\\])*)"""")

    fun parseSearch(html: String, adType: AdType): List<AppFlat> {
        val next = extractNextData(html) ?: return emptyList()
        val items = next["props"].asObjectOrNull()
            ?.get("pageProps").asObjectOrNull()
            ?.get("serpApiResponse").asObjectOrNull()
            ?.get("listings").asObjectOrNull()
            ?.get("items").asArrayOrNull()
            ?: return emptyList()
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapItem(item, adType) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun mergeDetail(base: AppFlat, html: String): AppFlat {
        val lat = ldGeoLatRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val lng = ldGeoLngRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull()
        val coords =
            if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates
        val rooms = ldRoomsRe.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: base.rooms
        val price = ldPriceRe.find(html)?.groupValues?.get(1)?.toDoubleOrNull() ?: base.priceByn
        val description = ldDescRe.find(html)?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.stripHtmlToPlainText()
            ?.takeIf { it.length > 20 }
            ?: base.description
        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            coordinates = coords,
            rooms = rooms,
            priceByn = price,
            description = description,
        )
    }

    fun listStub(adId: Long, detailUrl: String? = null): AppFlat = AppFlat(
        adId = adId,
        adType = AdType.RENT,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = ContactInformation(phones = null, ownerName = null),
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.OPENSOOQ,
        flatDetailUrl = detailUrl ?: "https://ae.opensooq.com/",
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

    private fun mapItem(item: JsonObject, adType: AdType): AppFlat {
        val id = item["id"].longOrNull()
            ?: item["id"].contentOrNull()?.toLongOrNull()
            ?: error("missing id")
        val priceRaw = item["price_amount"].contentOrNull().orEmpty()
        val price = priceRe.find(priceRaw)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
        val cps = item["cps"].asArrayOrNull()?.mapNotNull { it.contentOrNull() }.orEmpty()
        val highlights = item["highlights"].contentOrNull().orEmpty()
        val starLabels = item["starCps"].asArrayOrNull()?.mapNotNull {
            it.asObjectOrNull()?.get("label").contentOrNull()
        }.orEmpty()
        val roomHints = (cps + starLabels + listOf(highlights)).joinToString(" ")
        val isStudio = roomHints.contains("Studio", ignoreCase = true)
        val rooms = when {
            isStudio -> 0
            else -> bedroomsRe.find(roomHints)?.groupValues?.get(1)?.toIntOrNull()
        }
        val totalArea = (cps + starLabels).firstNotNullOfOrNull { label ->
            areaRe.find(label)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
        }
        val floor = (cps + starLabels).firstNotNullOfOrNull { parseFloor(it) }
        val district = item["nhood_label"].contentOrNull()
            ?: item["nhood_reporting"].contentOrNull()
        val city = item["city_label"].contentOrNull()
            ?: item["city_reporting"].contentOrNull()
        val address = listOfNotNull(city, district).joinToString(", ").ifBlank { null }
        val imageUri = item["image_uri"].contentOrNull()
        val images = imageUri?.let {
            listOf("https://opensooq-images.os-cdn.com/previews/640x480/$it.webp")
        }
        val postUrl = item["post_url"].contentOrNull() ?: "/search/$id"
        val url = if (postUrl.startsWith("http")) postUrl else "https://ae.opensooq.com$postUrl"
        val phone = item["phone_number"].contentOrNull()
            ?.takeIf { it.isNotBlank() && !it.contains("XX", ignoreCase = true) }
        val ownerName = item["member_display_name"].contentOrNull()
            ?: item["shop_name"].contentOrNull()
        val inserted = item["inserted_date"].contentOrNull()
        val publishedAt = parseInsertedDate(inserted)
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: inserted ?: item["posted_at"].contentOrNull()
        val title = item["title"].contentOrNull()?.stripHtmlToPlainText()
        val description = item["masked_description"].contentOrNull()?.stripHtmlToPlainText()
            ?: title

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(
                phones = phone?.let { listOf(it) },
                ownerName = ownerName,
            ),
            coordinates = null,
            commercialInfo = null,
            flatPlatform = FlatPlatform.OPENSOOQ,
            flatDetailUrl = url,
            publishedAt = publishedAt,
            publishedAtServer = inserted,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            priceUsd = null,
            priceByn = price,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = totalArea,
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
            owner = item["user_target_type"].contentOrNull() == "free",
        )
    }

    private fun parseFloor(label: String): Int? {
        val m = floorWordRe.find(label) ?: return null
        val raw = m.groupValues[1]
        raw.toIntOrNull()?.let { return it }
        return when (raw.lowercase()) {
            "ground" -> 0
            "first" -> 1
            "second" -> 2
            "third" -> 3
            "fourth" -> 4
            "fifth" -> 5
            "sixth" -> 6
            "seventh" -> 7
            "eighth" -> 8
            "ninth" -> 9
            "tenth" -> 10
            else -> null
        }
    }

    private fun parseInsertedDate(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        // YYYY-MM-DD or DD-MM-YYYY
        return runCatching {
            val parts = raw.split('-', '/', '.')
            if (parts.size != 3) return null
            val (y, m, d) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            LocalDate(y, m, d).atTime(LocalTime(0, 0)).toInstant(TimeZone.UTC)
        }.getOrNull()
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
