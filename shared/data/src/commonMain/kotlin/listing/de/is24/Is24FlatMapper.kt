package listing.de.is24

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
import listing.de.DeRelativeTime
import utils.stripHtmlToPlainText

/**
 * Maps IS24 mobile JSON → [AppFlat]. EUR → [AppFlat.priceByn], priceUsd = null.
 * See tmp/de/api/is24/NOTES.md.
 */
object Is24FlatMapper {
    fun mapSearch(root: JsonObject, adType: AdType): List<AppFlat> {
        val items = root["resultListItems"].asArrayOrNull() ?: return emptyList()
        return items.mapNotNull { el ->
            val wrapper = el.asObjectOrNull() ?: return@mapNotNull null
            if (wrapper["type"].contentOrNull() != "EXPOSE_RESULT") return@mapNotNull null
            val item = wrapper["item"].asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapListItem(item, adType) }.getOrNull()
        }
    }

    fun mapListItem(item: JsonObject, adType: AdType): AppFlat {
        val idStr = item["id"].contentOrNull()
            ?: item["id"].doubleOrNull()?.toLong()?.toString()
            ?: error("missing id")
        val adId = idStr.toLongOrNull() ?: error("non-numeric id $idStr")
        val attrs = item["attributes"].asArrayOrNull().orEmpty()
            .mapNotNull { it.asObjectOrNull()?.get("value").contentOrNull() }
        val price = attrs.firstNotNullOfOrNull { parseEuro(it) }
        val area = attrs.firstNotNullOfOrNull { parseArea(it) }
        val rooms = attrs.firstNotNullOfOrNull { parseRooms(it) }
        val address = item["address"].asObjectOrNull()
        val line = address?.get("line").contentOrNull()
        val lat = address?.get("lat").doubleOrNull()
        val lon = address?.get("lon").doubleOrNull()
        val coordinates =
            if (lat != null && lon != null) Coordinates(lat, lon) else null
        val titlePicture = item["titlePicture"].asObjectOrNull()
        val images = buildList {
            titlePicture?.get("full").contentOrNull()?.let { add(resolveImageUrl(it)) }
            titlePicture?.get("preview").contentOrNull()?.let { add(resolveImageUrl(it)) }
            item["pictures"].asArrayOrNull()?.forEach { pic ->
                pic.asObjectOrNull()?.get("urlScaleAndCrop").contentOrNull()
                    ?.let { add(resolveImageUrl(it)) }
            }
        }.distinct().take(20).ifEmpty { null }
        val district = line?.substringAfterLast(',')?.trim()?.takeIf { it.isNotEmpty() }
        val description = item["title"].contentOrNull()?.stripHtmlToPlainText()
        val publishedRaw = item["published"].contentOrNull()
        val publishedAt = DeRelativeTime.parse(publishedRaw)
        val publishedAtUi = publishedAt?.let {
            DateConverter.formatInstant(it, TimeZone.currentSystemDefault())
        } ?: publishedRaw

        return AppFlat(
            adId = adId,
            adType = adType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coordinates,
            commercialInfo = null,
            flatPlatform = FlatPlatform.IS24,
            flatDetailUrl = "https://www.immobilienscout24.de/expose/$adId",
            publishedAt = publishedAt,
            publishedAtServer = publishedRaw,
            publishedAtUi = publishedAtUi,
            imageUrls = images,
            priceUsd = null,
            priceByn = price,
            rooms = rooms,
            district = district,
            address = line,
            metroStation = null,
            description = description,
            yearBuilt = null,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = rooms == 1 && (description?.contains("Studio", ignoreCase = true) == true),
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
        val targeting = detail["adTargetingParameters"].asObjectOrNull()
        val price = targeting?.get("obj_baseRent").contentOrNull()?.toDoubleOrNull()
            ?: targeting?.get("obj_totalRent").contentOrNull()?.toDoubleOrNull()
            ?: targeting?.get("obj_purchasePrice").contentOrNull()?.toDoubleOrNull()
            ?: base.priceByn
        val rooms = targeting?.get("obj_noRooms").contentOrNull()?.toDoubleOrNull()?.toInt()
            ?: base.rooms
        val area = targeting?.get("obj_livingSpace").contentOrNull()?.toDoubleOrNull()
            ?: base.totalArea
        val floor = targeting?.get("obj_floor").contentOrNull()?.toIntOrNull() ?: base.floor
        val year = targeting?.get("obj_yearConstructed").contentOrNull()?.toIntOrNull()
            ?: base.yearBuilt
        val mapSection = detail["sections"].asArrayOrNull()
            ?.firstOrNull { it.asObjectOrNull()?.get("type").contentOrNull() == "MAP" }
            ?.asObjectOrNull()
        val loc = mapSection?.get("location").asObjectOrNull()
        val lat = loc?.get("lat").doubleOrNull()
        val lng = loc?.get("lng").doubleOrNull()
        val coordinates =
            if (lat != null && lng != null) Coordinates(lat, lng) else base.coordinates
        val address = listOfNotNull(
            mapSection?.get("addressLine1").contentOrNull(),
            mapSection?.get("addressLine2").contentOrNull(),
        ).joinToString(", ").ifBlank { base.address }
        val contact = detail["contact"].asObjectOrNull()
        val phones = contact?.get("phoneNumbers").asArrayOrNull()?.mapNotNull { el ->
            el.asObjectOrNull()?.get("text").contentOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        }?.ifEmpty { null } ?: base.contactInformation?.phones
        val owner = contact?.get("contactData").asObjectOrNull()
            ?.get("agent").asObjectOrNull()
            ?.let { agent ->
                agent["name"].contentOrNull()
                    ?: agent["company"].contentOrNull()
            } ?: base.contactInformation?.ownerName
        val description = detail["sections"].asArrayOrNull()
            ?.firstOrNull { it.asObjectOrNull()?.get("type").contentOrNull() == "DESCRIPTION" }
            ?.asObjectOrNull()
            ?.get("text").contentOrNull()
            ?.stripHtmlToPlainText()
            ?: base.description

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            priceByn = price,
            rooms = rooms,
            totalArea = area,
            floor = floor,
            yearBuilt = year,
            coordinates = coordinates,
            address = address,
            description = description,
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
        flatPlatform = FlatPlatform.IS24,
        flatDetailUrl = "https://www.immobilienscout24.de/expose/$adId",
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

    /** German: `1.250 €`, `380 €`, NBSP before €. */
    fun parseEuro(raw: String): Double? {
        val m = EURO_RE.find(raw.replace('\u00A0', ' ')) ?: return null
        return m.groupValues[1]
            .replace(".", "")
            .replace(',', '.')
            .toDoubleOrNull()
    }

    fun parseArea(raw: String): Double? {
        val m = AREA_RE.find(raw.replace('\u00A0', ' ')) ?: return null
        return m.groupValues[1].replace(',', '.').toDoubleOrNull()
    }

    fun parseRooms(raw: String): Int? {
        val m = ROOMS_RE.find(raw.replace('\u00A0', ' ')) ?: return null
        return m.groupValues[1].toDoubleOrNull()?.toInt()
    }

    fun resolveImageUrl(url: String): String =
        url.replace("%WIDTH%x%HEIGHT%", "800x600")

    private val EURO_RE = Regex("""([\d.]+(?:,\d+)?)\s*€""")
    private val AREA_RE = Regex("""([\d]+(?:,\d+)?)\s*m²""")
    private val ROOMS_RE = Regex("""([\d]+(?:[.,]\d+)?)\s*Zi\.?""")
}
