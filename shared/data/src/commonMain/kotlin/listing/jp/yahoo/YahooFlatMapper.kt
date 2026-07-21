package listing.jp.yahoo

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.JsonObject
import listing.core.asArrayOrNull
import listing.core.asObjectOrNull
import listing.core.contentOrNull
import listing.core.intOrNull
import listing.jp.parseAreaSqm
import listing.jp.parseYenLabel
import listing.jp.stableAdId

object YahooFlatMapper {
    fun mapRentSearch(root: JsonObject): List<AppFlat> {
        val buildings = root["properties"].asArrayOrNull() ?: return emptyList()
        return buildings.flatMap { buildingEl ->
            val building = buildingEl.asObjectOrNull() ?: return@flatMap emptyList()
            mapBuilding(building)
        }.distinctBy { it.adId }
    }

    fun listStub(adId: Long, adType: AdType = AdType.RENT): AppFlat = AppFlat(
        adId = adId,
        adType = adType,
        flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
        contactInformation = null,
        coordinates = null,
        commercialInfo = null,
        flatPlatform = FlatPlatform.YAHOO_RE,
        flatDetailUrl = when (adType) {
            is AdType.SALE -> "$BASE/used/mansion/detail_corp/"
            else -> "$BASE/rent/detail/"
        },
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

    private fun mapBuilding(building: JsonObject): List<AppFlat> {
        val coords = parseCoords(building["CoordinatesWgs"].contentOrNull())
        val location = building["LocationView"].asObjectOrNull()
        val district = location?.get("GeoName")?.contentOrNull()
        val address = location?.get("AddressName")?.contentOrNull()
        val buildingName = building["BuildingName"].contentOrNull()
        val totalFloors = building["TotalFloorNum"].intOrNull()
        val externalImage = building["ExternalImageUrl"].contentOrNull()
        val transports = building["Transports"].asArrayOrNull()
        val metro = transports?.firstOrNull()?.asObjectOrNull()?.get("StationName")?.contentOrNull()
        val groupProps = building["GroupProperties"].asArrayOrNull()
        if (groupProps.isNullOrEmpty()) {
            return emptyList()
        }
        return groupProps.mapNotNull { gpEl ->
            val gp = gpEl.asObjectOrNull() ?: return@mapNotNull null
            mapGroupProperty(
                gp = gp,
                buildingName = buildingName,
                coords = coords,
                district = district,
                address = address,
                metro = metro,
                totalFloors = totalFloors,
                externalImage = externalImage,
            )
        }
    }

    private fun mapGroupProperty(
        gp: JsonObject,
        buildingName: String?,
        coords: Coordinates?,
        district: String?,
        address: String?,
        metro: String?,
        totalFloors: Int?,
        externalImage: String?,
    ): AppFlat? {
        val propertyId = gp["PropertyId"].contentOrNull() ?: return null
        val adId = stableAdId(propertyId)
        val detailUrl = "$BASE/rent/detail/$propertyId/"
        val mainPrice = parseYenLabel(gp["PriceLabel"].contentOrNull())
        val mgmtFee = parseYenLabel(gp["MonthlyManagementCostLabel"].contentOrNull())
        val area = parseAreaSqm(gp["MonopolyAreaLabel"].contentOrNull())
        val floor = gp["FloorNum"].contentOrNull()?.filter { it.isDigit() }?.toIntOrNull()
        val images = buildList {
            gp["RoomLayoutImageUrl"].contentOrNull()?.let { add(it) }
            externalImage?.let { add(it) }
        }.distinct().ifEmpty { null }
        val secondaryParts = listOfNotNull(
            gp["SecurityDepositLabel"].contentOrNull()?.takeIf { it.isNotBlank() }
                ?.let { "敷金: $it" },
            gp["KeyMoneyLabel"].contentOrNull()?.takeIf { it.isNotBlank() }?.let { "礼金: $it" },
            mgmtFee?.let { "管理費: ${it.toInt()}円" },
        )
        val description =
            listOfNotNull(buildingName, secondaryParts.joinToString(" · ").ifBlank { null })
                .joinToString("\n")
                .ifBlank { null }

        return AppFlat(
            adId = adId,
            adType = AdType.RENT,
            flatDevInfo = FlatDevInfo(isDetailData = false, isDetailLoaded = false),
            contactInformation = ContactInformation(phones = null, ownerName = null),
            coordinates = coords,
            commercialInfo = null,
            flatPlatform = FlatPlatform.YAHOO_RE,
            flatDetailUrl = detailUrl,
            publishedAt = null,
            publishedAtServer = null,
            publishedAtUi = null,
            imageUrls = images,
            mainPrice = mainPrice,
            secondPrice = mgmtFee,
            totalArea = area,
            livingArea = null,
            kitchenArea = null,
            floor = floor,
            totalFloors = totalFloors,
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
            rooms = gp["DetailRoomLayout"].intOrNull(),
            district = district,
            address = address,
            metroStation = metro,
            description = description,
            yearBuilt = null,
        )
    }

    private fun parseCoords(raw: String?): Coordinates? {
        if (raw.isNullOrBlank()) return null
        val parts = raw.split(',')
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        return Coordinates(lat, lng)
    }

    private const val BASE = YahooApiClient.BASE
}
