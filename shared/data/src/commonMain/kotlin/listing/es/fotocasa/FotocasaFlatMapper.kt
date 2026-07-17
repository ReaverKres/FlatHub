package listing.es.fotocasa

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

/**
 * Maps Fotocasa gateway JSON → [AppFlat]. EUR → [AppFlat.priceByn], priceUsd = null.
 * See tmp/es/api/fotocasa/NOTES.md.
 */
object FotocasaFlatMapper {
    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["realEstates"].asArrayOrNull() ?: return emptyList()
        return items.mapNotNull { el ->
            runCatching { mapListItem(el.asObjectOrNull() ?: return@mapNotNull null, adType) }
                .getOrNull()
        }
    }

    fun mapListItem(item: JsonObject, adType: AdType): AppFlat {
        val id = item["id"].longOrNull() ?: error("missing id")
        val detailPath = item["detail"].asObjectOrNull()?.get("es").contentOrNull()
        val detailUrl = when {
            detailPath.isNullOrBlank() -> "https://www.fotocasa.es/"
            detailPath.startsWith("http") -> detailPath
            else -> "https://www.fotocasa.es$detailPath"
        }
        val price = item["transactions"].asArrayOrNull()
            ?.firstOrNull()
            ?.asObjectOrNull()
            ?.get("value")
            .asArrayOrNull()
            ?.firstOrNull()
            .doubleOrNull()
        val features = item["features"].asArrayOrNull().orEmpty()
        val rooms = featureValue(features, "rooms")?.toInt()
        val area = featureValue(features, "surface")
        val floor = featureValue(features, "floor")?.toInt()
        val addressObj = item["address"].asObjectOrNull()
        val location = addressObj?.get("location").asObjectOrNull()
        val district = location?.get("level7").contentOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val ubication = addressObj?.get("ubication").contentOrNull()
        val coordsObj = addressObj?.get("coordinates").asObjectOrNull()
        val lat = coordsObj?.get("latitude").doubleOrNull()
        val lng = coordsObj?.get("longitude").doubleOrNull()
        val coordinates =
            if (lat != null && lng != null) Coordinates(lat, lng) else null
        val images = item["multimedias"].asArrayOrNull()?.mapNotNull { media ->
            val obj = media.asObjectOrNull() ?: return@mapNotNull null
            val typeId = obj["typeId"].intOrNull()
            if (typeId != null && typeId != 2) return@mapNotNull null
            obj["url"].contentOrNull()
        }?.take(20)
        val phone = item["advertiser"].asObjectOrNull()?.get("phone").contentOrNull()
            ?.trim()?.takeIf { it.isNotEmpty() }
        val owner = item["advertiser"].asObjectOrNull()?.get("clientAlias").contentOrNull()
        val description = item["description"].contentOrNull()?.stripHtmlToPlainText()
        val created = item["date"].contentOrNull()
        val publishedAt = created?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        }

        return AppFlat(
            adId = id,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(
                phones = phone?.let { listOf(it) },
                ownerName = owner,
            ),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.FOTOCASA,
            flatDetailUrl = detailUrl,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAtUi,
            imageUrls = images?.ifEmpty { null },
            priceUsd = null,
            priceByn = price,
            rooms = rooms,
            district = district,
            address = ubication,
            metroStation = null,
            description = description,
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

    fun mergeDetail(base: AppFlat, detail: JsonObject): AppFlat {
        val price = detail["prices"].asArrayOrNull()
            ?.firstOrNull()
            ?.asObjectOrNull()
            ?.get("amount")
            .doubleOrNull()
            ?: base.priceByn
        val rooms = detail["rooms"].intOrNull() ?: base.rooms
        val area = detail["surface"].doubleOrNull() ?: base.totalArea
        val floor = detail["floor"].contentOrNull()?.toIntOrNull()
            ?: detail["floor"].intOrNull()
            ?: base.floor
        val phone = detail["phone"].contentOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        val phones = phone?.let { listOf(it) }
            ?: base.contactInformation?.phones
        val loc = detail["location"].asObjectOrNull()
        val lat = loc?.get("latitude").doubleOrNull()
        val lng = loc?.get("longitude").doubleOrNull()
        val coordinates =
            if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates
        val district = loc?.get("level7Name").contentOrNull() ?: base.district
        val street = detail["street"].contentOrNull()
        val barrio = loc?.get("level8Name").contentOrNull()
        val address = listOfNotNull(street, barrio, district)
            .joinToString(", ")
            .ifBlank { base.address }
        val description = detail["description"].contentOrNull()?.stripHtmlToPlainText()
            ?: base.description
        val images = detail["multimedia"].asArrayOrNull()?.mapNotNull { media ->
            media.asObjectOrNull()?.get("url").contentOrNull()
        }?.take(30)?.ifEmpty { null } ?: base.imageUrls
        val created = detail["creationDate"].contentOrNull() ?: base.publishedAtServer
        val publishedAt = created?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: base.publishedAt
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: base.publishedAtUi
        val owner = detail["agency"].asObjectOrNull()?.get("alias").contentOrNull()
            ?: detail["agency"].asObjectOrNull()?.get("name").contentOrNull()
            ?: base.contactInformation?.ownerName

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            priceByn = price,
            rooms = rooms,
            totalArea = area,
            floor = floor,
            coordinates = coordinates,
            district = district,
            address = address,
            description = description,
            imageUrls = images,
            publishedAt = publishedAt,
            publishedAtServer = created,
            publishedAtUi = publishedAtUi,
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
        flatPlatform = FlatPlatform.FOTOCASA,
        flatDetailUrl = "https://www.fotocasa.es/",
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

    private fun featureValue(
        features: List<kotlinx.serialization.json.JsonElement>,
        key: String,
    ): Double? {
        for (el in features) {
            val obj = el.asObjectOrNull() ?: continue
            if (obj["key"].contentOrNull() != key) continue
            return obj["value"].asArrayOrNull()?.firstOrNull().doubleOrNull()
        }
        return null
    }
}
