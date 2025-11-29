package api

import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType

fun CommonFilterRequestModel.toSubscriptionDto(): CommonFilterRequestDto {
    return CommonFilterRequestDto(
        name = this.name,
        adType = this.adType.toDto(),
        commercial = this.commercial?.commercialPropertyType
            ?.toDto()
            ?.let { CommercialDto(commercialPropertyType = it) },
        lastCommercialAdType = this.lastCommercialAdType,
        priceFull = this.priceFull,
        pricePerSquare = this.pricePerSquare,
        totalArea = this.totalArea,
        priceType = this.priceType,
        currency = this.currency,
        addressRequestModel = this.addressRequestModel,
        numberOfRooms = this.numberOfRooms,
        metroStations = this.metroStations,
        districtsArea = this.districtsArea,
        location = this.location,
        userMapAreas = this.userMapAreas,
        roomOnly = this.roomOnly,
        fromOwnerOnly = this.fromOwnerOnly,
        withPhotoOnly = this.withPhotoOnly,
        sortOption = this.sortOption,
        bookingDatesFilter = this.bookingDatesFilter,
        isNotificationEnabled = this.isNotificationEnabled
    )
}

private fun AdType.toDto(): AdTypeDto =
    when (this) {
        AdType.RENT -> AdTypeDto(type = "RENT")
        AdType.SALE -> AdTypeDto(type = "SALE")
        AdType.DAILY -> AdTypeDto(type = "DAILY")
        is AdType.COMMERCIAL -> AdTypeDto(
            type = "COMMERCIAL",
            commercialAdType = when (this.commercialAdType) {
                CommercialAdType.RENT -> "RENT"
                CommercialAdType.SALE -> "SALE"
            }
        )
    }

private fun CommercialPropertyType.toDto(): CommercialPropertyTypeDto =
    when (this) {
        CommercialPropertyType.Office -> CommercialPropertyTypeDto("Office")
        CommercialPropertyType.Retail -> CommercialPropertyTypeDto("Retail")
        CommercialPropertyType.Services -> CommercialPropertyTypeDto("Services")
        CommercialPropertyType.Industrial -> CommercialPropertyTypeDto("Industrial")
        CommercialPropertyType.Warehouses -> CommercialPropertyTypeDto("Warehouses")
        CommercialPropertyType.Other -> CommercialPropertyTypeDto("Other")
        is CommercialPropertyType.All -> CommercialPropertyTypeDto("Other")
    }

