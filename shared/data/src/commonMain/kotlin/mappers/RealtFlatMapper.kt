package mappers

import entities.AppFlat
import entities.CommercialInfo
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.date.DateConverter
import io.flatzen.commoncomponents.utils.Const.USD_TO_EUR_RATE
import kotlinx.datetime.TimeZone
import mappers.base.ResponseToEntitiesFlatMapper
import server_response.RealtListResponse.RealtListResponseItem.Data.SearchObjects.Body.RealtFlatResponse
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RealtFlatMapper : ResponseToEntitiesFlatMapper<RealtFlatResponse, AppFlat> {

    override fun map(response: RealtFlatResponse): AppFlat {

        var mainPrice: Double? = null
        var secondPrice: Double? = null
        var priceUsdPerSquare: Double? = null
        var priceBynPerSquare: Double? = null
        val priceIsNotZero = response.price != 0.0
        val priceSquareIsNotZero = response.pricePerM2 != 0.0
        if(response.priceCurrency == 840 && priceIsNotZero) {
            mainPrice = response.price
        }
        if(response.priceCurrency == 978 && priceIsNotZero) {
            //TODO получить текущий курс
            mainPrice = response.price?.times(USD_TO_EUR_RATE)
        }
        if(response.priceCurrency == 933 && priceIsNotZero) {
            secondPrice = response.price
        }

        if(response.priceCurrency == 840 && priceSquareIsNotZero) {
            priceUsdPerSquare = response.pricePerM2
        }
        if(response.priceCurrency == 978 && priceSquareIsNotZero) {
            //TODO получить текущий курс
            priceUsdPerSquare = response.pricePerM2?.times(USD_TO_EUR_RATE)
        }
        if(response.priceCurrency == 933 && priceSquareIsNotZero) {
            priceBynPerSquare = response.pricePerM2
        }

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
                    CommercialPropertyType.Land,
                    CommercialPropertyType.Showroom,
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
            commercialInfo = if(response.adType.isCommercial) {
                CommercialInfo(
                    numberOfRooms = response.rooms,
                    propertyType = response.commercialPropertyType
                )
            } else null,
            flatDetailUrl = "https://realt.by${response.code?.let { "/$urlPath/object/$it" } ?: ""}",
            adId = response.code?.toLong() ?: 0L,
            publishedAt = DateConverter.stringToInstant(response.updatedAt.orEmpty()),
            publishedAtServer = response.updatedAt,
            publishedAtUi = DateConverter.formatInstant(
                DateConverter.stringToInstant(response.updatedAt.orEmpty()),
                TimeZone.currentSystemDefault()
            ),
            imageUrls = response.images?.filterNotNull().orEmpty(),
            mainPrice = mainPrice,
            secondPrice = secondPrice,
            secondPriceSquare = priceBynPerSquare,
            mainPriceSquare = priceUsdPerSquare,
            rooms = response.rooms,
            district = response.stateDistrictName,
            address = "${response.streetName}, ${response.buildingNumber}",
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
            isStudio = false,
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
            )
        )
    }
}