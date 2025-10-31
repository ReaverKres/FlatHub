package mappers

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import mappers.base.ResponseToEntitiesFlatMapper
import server_response.RealtListResponse.RealtListResponseItem.Data.SearchObjects.Body.RealtFlatResponse
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RealtFlatMapper : ResponseToEntitiesFlatMapper<RealtFlatResponse, AppFlat> {

    override fun map(response: RealtFlatResponse): AppFlat {
        val urlPath = when {
            response.adType == AdType.RENT -> "rent-flat-for-long"
            response.adType == AdType.DAILY -> "rent-flat-for-day"
            response.adType.isCommercial -> {
                when (response.commercialPropertyType) {
                    CommercialPropertyType.All -> null
                    CommercialPropertyType.Industrial -> if (response.adType == AdType.COMMERCIAL(
                            CommercialAdType.RENT)) "rent-proizvodstvo" else "sale-proizvodstvo"
                    CommercialPropertyType.Office -> if (response.adType == AdType.COMMERCIAL(
                            CommercialAdType.RENT)) "rent-offices" else "sale-offices"
                    CommercialPropertyType.Other -> null
                    CommercialPropertyType.Retail -> if (response.adType == AdType.COMMERCIAL(
                            CommercialAdType.RENT)) "rent-shops" else "sale-shops"
                    CommercialPropertyType.Services -> if (response.adType == AdType.COMMERCIAL(
                            CommercialAdType.RENT)) "rent-services" else "sale-services"
                    CommercialPropertyType.Warehouses -> if (response.adType == AdType.COMMERCIAL(
                            CommercialAdType.RENT)) "rent-warehouses" else "sale-warehouses"
                    null -> null
                }
            }
            else -> "sale-flats"
        }
        return AppFlat(
            flatPlatform = FlatPlatform.REALT,
            flatDevInfo = FlatDevInfo(
                isDetailData = true,
                isDetailLoaded = true
            ),
            flatDetailUrl = "https://realt.by${response.code?.let { "/$urlPath/object/$it" } ?: ""}",
            adId = response.code?.toLong() ?: 0L,
            publishedAt = DateConverter.stringToInstant(response.updatedAt.orEmpty()),
            publishedAtServer = response.updatedAt,
            publishedAtUi = DateConverter.formatInstant(
                DateConverter.stringToInstant(response.updatedAt.orEmpty()),
                TimeZone.currentSystemDefault()
            ),
            imageUrls = response.images?.filterNotNull().orEmpty(),
            priceUsd = response.price?.toDouble().takeIf {
                response.priceCurrency == 840
            },
            priceByn = response.price?.toDouble().takeIf {
                response.priceCurrency == 933
            },
            rooms = response.rooms,
            district = response.stateDistrictName,
            address = listOfNotNull(response.streetName, response.buildingNumber).joinToString(" "),
            coordinates = response.location?.let { latLng ->
                if (latLng.size >= 2) {
                    Coordinates(
                        latitude = latLng[1]!!,
                        longitude = latLng[0]!!
                    )
                } else null
            },
            metroStation = response.metroStationName,
            description = response.description,
            yearBuilt = response.buildingYear,
            totalArea = response.areaTotal,
            livingArea = response.areaLiving,
            kitchenArea = response.areaKitchen,
            floor = response.storey,
            totalFloors = response.storeys,
            sleepingPlaces = response.numberOfBeds,
            isStudio = response.rooms == 1 && (response.areaTotal ?: 0.0) < 35.0,
            bathroomType = null,
//                when (r?.code) {
//                    1 -> "Раздельный"
//                    2 -> "Совмещённый"
//                    else -> null
//                }
            balcony = when (response?.balconyType) {
                2 -> "Есть"
                27 -> "Нет"
                else -> null
            },
            repairType = null,
            condition = null,
            windowDirections = emptyList(), // в ответе Realt нет такого поля
            buildingImprovements = emptyList(), // в ответе Realt нет такого поля
            prepaymentType = null, // в ответе Realt нет такого поля
            amenities = emptyList(), // в ответе Realt нет такого поля
            kitchenEquipment = emptyList(), // в ответе Realt нет такого поля
            forWhom = emptyList(), // в ответе Realt нет такого поля
            parkingInfo = null,
            owner = response.agencyUuid == null && response.agencyName == null,
            contactInformation = ContactInformation(
                phones = response.contactPhones?.filterNotNull(),
                ownerName = response.contactName
            ),
            commercialInfo = null
        )
    }
}