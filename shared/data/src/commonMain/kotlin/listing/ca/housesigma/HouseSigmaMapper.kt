package listing.ca.housesigma

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import listing.ca.isCaSaleDeal
import listing.ca.stableCaAdId
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.doubleOrNull
import listing.core.intOrNull
import kotlin.time.Instant

/**
 * Maps HouseSigma encrypted list cards + map markers → [AppFlat].
 * See tmp/ca/api/housesigma/NOTES.md.
 */
object HouseSigmaMapper {
    private const val SITE_ORIGIN = "https://housesigma.com"
    private val priceSuffixRe = Regex("""([\d.]+)\s*([KkMm])?$""")
    private val idListingQueryRe = Regex("""[?&]id_listing=([^&]+)""")
    private val provinceQueryRe = Regex("""[?&]province=([^&]+)""")

    fun extractIdListing(detailUrl: String): String? =
        idListingQueryRe.find(detailUrl)?.groupValues?.get(1)

    fun extractProvince(detailUrl: String): String? =
        provinceQueryRe.find(detailUrl)?.groupValues?.get(1)?.uppercase()

    fun mergeDetail(base: AppFlat, detail: JsonObject): AppFlat {
        val house = detail["house"]?.asObjectOrNull()
        val picture = detail["picture"]?.asObjectOrNull()
        val photos = picture?.get("photo_list")?.asArrayOrNull()
            ?.mapNotNull { it.contentOrNull()?.takeIf { url -> url.isNotBlank() } }
            ?.distinct()
            ?.ifEmpty { null }
        val houseMapped = house?.let {
            runCatching { mapHouse(it, base.adType?.let { t -> t is AdType.SALE } ?: false) }
                .getOrNull()
        }

        val description = buildString {
            house?.get("text")?.asObjectOrNull()?.get("rooms_long")?.contentOrNull()
                ?.let { append(it) }
            detail["key_facts_v2"]?.asObjectOrNull()?.entries?.forEach { (_, value) ->
                val fact = value.asObjectOrNull() ?: return@forEach
                val name =
                    fact["name"]?.contentOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
                val factValue =
                    fact["value"]?.contentOrNull()?.takeIf { it.isNotBlank() && it != "-" }
                        ?: return@forEach
                if (isNotEmpty()) append("\n")
                append("$name: $factValue")
            }
        }.takeIf { it.isNotBlank() }

        val dates = house?.let { publishedDatesFromHouse(it) } ?: base.publishedAt?.let {
            Triple(base.publishedAt, base.publishedAtServer, base.publishedAtUi)
        }

        val phone = house?.get("agent_user")?.asObjectOrNull()
            ?.get("phone")?.contentOrNull()
            ?.filter { ch -> ch.isDigit() || ch == '+' }
            ?.takeIf { it.length >= 10 }

        return base.copy(
            flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            description = description?.takeIf { it.length > (base.description?.length ?: 0) }
                ?: base.description,
            imageUrls = photos ?: base.imageUrls,
            mainPrice = houseMapped?.mainPrice ?: base.mainPrice,
            rooms = houseMapped?.rooms ?: base.rooms,
            coordinates = houseMapped?.coordinates ?: base.coordinates,
            address = houseMapped?.address ?: base.address,
            district = houseMapped?.district ?: base.district,
            publishedAt = dates?.first ?: base.publishedAt,
            publishedAtServer = dates?.second ?: base.publishedAtServer,
            publishedAtUi = dates?.third ?: base.publishedAtUi,
            contactInformation = ContactInformation(
                phones = phone?.let { listOf(it) } ?: base.contactInformation?.phones,
                ownerName = house?.get("brokerage")?.contentOrNull()
                    ?: base.contactInformation?.ownerName,
            ),
        )
    }

    fun parseList(root: JsonObject, adType: AdType): List<AppFlat> {
        val list = root["houseList"]?.asArrayOrNull() ?: return emptyList()
        val isSale = adType.isCaSaleDeal()
        return list.mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            runCatching { mapHouse(item, isSale) }.getOrNull()
        }.distinctBy { it.adId }
    }

    fun parseMapListing(root: JsonObject, adType: AdType): List<AppFlat> {
        val list =
            root["data"]?.asObjectOrNull()?.get("list")?.asArrayOrNull() ?: return emptyList()
        val isSale = adType.isCaSaleDeal()
        return list.mapNotNull { element ->
            val item = element.asObjectOrNull() ?: return@mapNotNull null
            if (item["type"]?.contentOrNull() != "marker") return@mapNotNull null
            runCatching { mapMarker(item, isSale) }.getOrNull()
        }.distinctBy { it.adId }
    }

    private fun mapHouse(item: JsonObject, isSale: Boolean): AppFlat? {
        val idListing = item["id_listing"]?.contentOrNull() ?: return null
        val adId = stableCaAdId(idListing)

        val map = item["map"]?.asObjectOrNull()
        val lat = map?.get("lat")?.doubleOrNull()
        val lng = map?.get("lon")?.doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null

        val price = item["price_int"]?.intOrNull()?.toDouble()
            ?: item["price_int"]?.doubleOrNull()
            ?: parseLabelPrice(item["marker_label"]?.contentOrNull().orEmpty())

        val bedroom = item["bedroom"]?.intOrNull()
        val rooms = bedroom?.takeIf { it > 0 }

        val photoUrl = item["photo_url"]?.contentOrNull()?.takeIf { it.isNotBlank() }
        val imageUrls = photoUrl?.let { listOf(it) }

        val address = item["address"]?.contentOrNull()?.takeIf { it.isNotBlank() }
        val municipality = item["municipality_name"]?.contentOrNull()
        val community = item["community_name"]?.contentOrNull()
        val district = community?.takeIf { it.isNotBlank() } ?: municipality

        val houseType = item["house_type_name"]?.contentOrNull()
        val houseStyle = item["house_style"]?.contentOrNull()
        val roomsText = item["text"]?.asObjectOrNull()?.get("rooms_long")?.contentOrNull()
        val description = buildString {
            houseType?.let { append(it) }
            houseStyle?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
            roomsText?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) append("\n")
                append(it)
            }
        }.takeIf { it.isNotBlank() }

        val dates = publishedDatesFromHouse(item)
        val mappedAdType = if (isSale) AdType.SALE else AdType.RENT
        val provinceCode = item["province"]?.contentOrNull()?.uppercase() ?: "ON"
        val seoMunicipality = item["seo_municipality"]?.contentOrNull()
        val seoSuffix = item["seo_suffix"]?.contentOrNull()
        val detailUrl = buildDetailUrl(idListing, provinceCode, seoMunicipality, seoSuffix)

        return AppFlat(
            adId = adId,
            adType = mappedAdType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.HOUSESIGMA,
            flatDetailUrl = detailUrl,
            publishedAt = dates?.first,
            publishedAtServer = dates?.second,
            publishedAtUi = dates?.third,
            imageUrls = imageUrls,
            secondPrice = null,
            mainPrice = price,
            rooms = rooms,
            district = district,
            address = address,
            metroStation = null,
            description = description,
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
            owner = false,
        )
    }

    private fun publishedDatesFromHouse(item: JsonObject): Triple<Instant?, String?, String?>? {
        val dateAdded = item["date_added"]?.contentOrNull()?.takeIf { it.isNotBlank() }
            ?: item["list_dates"]?.asObjectOrNull()?.get("date_added")?.contentOrNull()
            ?: item["text"]?.asObjectOrNull()?.get("date_preview")?.contentOrNull()
            ?: return null
        return runCatching {
            val instant = DateConverter.parseDomovitaDate(dateAdded)
            Triple(
                instant,
                dateAdded,
                DateConverter.formatInstant(instant, TimeZone.currentSystemDefault()),
            )
        }.getOrNull()
    }

    private fun buildDetailUrl(
        idListing: String,
        province: String,
        seoMunicipality: String?,
        seoSuffix: String?,
    ): String {
        val base = when {
            seoMunicipality != null && seoSuffix != null ->
                "$SITE_ORIGIN/on/$seoMunicipality/$seoSuffix/"

            else -> "$SITE_ORIGIN/on/map/"
        }
        val separator = if (base.contains('?')) "&" else "?"
        return "$base${separator}id_listing=$idListing&province=$province"
    }

    private fun mapMarker(item: JsonObject, isSale: Boolean): AppFlat? {
        val idListing = item["ids"]?.asArrayOrNull()?.firstOrNull()?.contentOrNull()
            ?: return null
        val adId = idListing.toLongOrNull() ?: stableCaAdId(idListing)

        val location = item["location"]?.asObjectOrNull()
        val lat = location?.get("lat")?.doubleOrNull()
        val lng = location?.get("lon")?.doubleOrNull()
        val coords = if (lat != null && lng != null) Coordinates(lat, lng) else null

        val label = item["label"]?.contentOrNull().orEmpty()
        val price = parseLabelPrice(label)
        val mappedAdType = if (isSale) AdType.SALE else AdType.RENT

        return AppFlat(
            adId = adId,
            adType = mappedAdType,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.HOUSESIGMA,
            flatDetailUrl = buildDetailUrl(idListing, "ON", null, null),
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = null,
            secondPrice = null,
            mainPrice = price,
            rooms = null,
            district = null,
            address = label.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } },
            metroStation = null,
            description = label,
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
            owner = false,
        )
    }

    /** Parses `D 1.1M`, `TC 3K`, `TF 0.88M` style map labels. */
    internal fun parseLabelPrice(label: String): Double? {
        if (label.isBlank() || label.all { it.isDigit() }) return null
        val token = label.substringAfterLast(' ').trim()
        val match = priceSuffixRe.find(token) ?: return null
        val amount = match.groupValues[1].toDoubleOrNull() ?: return null
        return when (match.groupValues[2].uppercase()) {
            "K" -> amount * 1_000
            "M" -> amount * 1_000_000
            else -> amount
        }
    }
}
