package listing.ge.livo

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import listing.core.longOrNull
import utils.stripHtmlToPlainText
import kotlin.time.Instant

object LivoFlatMapper {
    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["data"].asObjectOrNull()
            ?.get("data").asArrayOrNull()
            ?: root["data"].asArrayOrNull()
            ?: return emptyList()
        return items.mapNotNull { el ->
            val item = el.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapItem(item, adType, detailLoaded = false) }.getOrNull()
        }
    }

    fun mapDetail(root: JsonObject, base: AppFlat): AppFlat {
        val item = root["data"].asObjectOrNull() ?: root
        return runCatching {
            mapItem(
                item,
                base.getAdTypeNonNull(),
                detailLoaded = true,
                base = base
            )
        }
            .getOrDefault(
                base.copy(flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true)),
            )
    }

    private fun mapItem(
        item: JsonObject,
        adType: AdType,
        detailLoaded: Boolean,
        base: AppFlat? = null,
    ): AppFlat {
        val id = item["id"].longOrNull() ?: error("missing id")
        // Public page is /ka/statement/{id} (slug path 404s).
        val detailUrl = "https://livo.ge/ka/statement/$id"
        val priceObj = item["price"].asObjectOrNull()
        // currency id 1 = GEL, 2 = USD (observed)
        val priceGel = priceObj?.get("1").asObjectOrNull()?.get("price_total").doubleOrNull()
        val priceUsd = priceObj?.get("2").asObjectOrNull()?.get("price_total").doubleOrNull()
        val priceGelSq = priceObj?.get("1").asObjectOrNull()?.get("price_square").doubleOrNull()
        val priceUsdSq = priceObj?.get("2").asObjectOrNull()?.get("price_square").doubleOrNull()
        val lat = item["lat"].doubleOrNull()
        val lng = item["lng"].doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else base?.coordinates
        val images = item["images"].asArrayOrNull()?.mapNotNull { img ->
            val o = img.asObjectOrNull() ?: return@mapNotNull null
            o["large"].contentOrNull() ?: o["thumb"].contentOrNull()
        } ?: base?.imageUrls
        val rooms = item["room"].intOrNull()
            ?: item["room"].contentOrNull()?.toIntOrNull()
            ?: item["bedroom"].intOrNull()
            ?: item["bedroom"].contentOrNull()?.toIntOrNull()
        val created = item["last_updated"].contentOrNull()
        val publishedAt = created?.let { parseInstant(it) } ?: base?.publishedAt
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: base?.publishedAtUi
        val title = item["dynamic_title"].contentOrNull()
        val comment = item["comment"].contentOrNull()
        val description = item["description"].contentOrNull()
            ?: comment
            ?: title
            ?: base?.description
        val phonesFromText = extractPhones(comment ?: description)
        val ownerName = item["user_title"].contentOrNull() ?: base?.contactInformation?.ownerName
        val street = item["address"].contentOrNull()
        val district = item["urban_name"].contentOrNull()
            ?: item["district_name"].contentOrNull()
            ?: base?.district
        val city = item["city_name"].contentOrNull()
        val address = listOfNotNull(street, district, city).joinToString(", ")
            .ifBlank { base?.address }

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = detailLoaded, isDetailLoaded = detailLoaded),
            contactInformation = ContactInformation(
                phones = phonesFromText
                    ?: base?.contactInformation?.phones,
                ownerName = ownerName,
            ),
            coordinates = coords,
            commercialInfo = null,
            savedInFavorites = base?.savedInFavorites == true,
            isViewed = base?.isViewed == true,
            dislike = base?.dislike == true,
            flatPlatform = FlatPlatform.LIVO,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created ?: base?.publishedAtServer,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            priceUsd = priceUsd ?: base?.priceUsd,
            priceByn = priceGel ?: base?.priceByn,
            priceUsdSquare = priceUsdSq ?: base?.priceUsdSquare,
            priceBynSquare = priceGelSq ?: base?.priceBynSquare,
            rooms = rooms ?: base?.rooms,
            district = district,
            address = address,
            metroStation = base?.metroStation,
            description = description.stripHtmlToPlainText(),
            yearBuilt = base?.yearBuilt,
            totalArea = item["area"].doubleOrNull() ?: base?.totalArea,
            livingArea = base?.livingArea,
            kitchenArea = base?.kitchenArea,
            floor = item["floor"].intOrNull() ?: base?.floor,
            totalFloors = item["total_floors"].intOrNull() ?: base?.totalFloors,
            sleepingPlaces = null,
            isStudio = rooms == 0,
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = null,
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            amenities = base?.amenities,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = null,
        )
    }

    private fun parseInstant(raw: String): Instant? {
        val normalized = raw.trim().replace(' ', 'T').let {
            if (it.endsWith('Z') || '+' in it || it.count { ch -> ch == '-' } >= 3) it else "${it}Z"
        }
        return runCatching { Instant.parse(normalized) }.getOrNull()
    }

    private fun extractPhones(text: String?): List<String>? {
        if (text.isNullOrBlank()) return null
        val found = Regex("""(?:\+?995)?[\s-]?(5\d{2})[\s-]?(\d{2})[\s-]?(\d{2})[\s-]?(\d{2})""")
            .findAll(text)
            .map { m ->
                val digits =
                    (m.groupValues[1] + m.groupValues[2] + m.groupValues[3] + m.groupValues[4])
                "+995$digits"
            }
            .distinct()
            .toList()
        return found.ifEmpty { null }
    }
}
