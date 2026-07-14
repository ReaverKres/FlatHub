package io.flatzen.viewmodel.filter

import entities.AddressRequestModel
import entities.CommercialRequestModel
import entities.CommonFilterRequestModel
import entities.LocationFilter
import entities.MetroStations
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.viewmodel.UiDistrict

fun mapFilterStateToFilterModel(filters: FilterState): CommonFilterRequestModel {
    return CommonFilterRequestModel(
        name = filters.name,
        adType = filters.adType,
        lastCommercialAdType = filters.lastCommercialAdType,
        priceFull = filters.priceFull,
        pricePerSquare = filters.pricePerSquare,
        totalArea = filters.totalArea,
        currency = filters.currency,
        numberOfRooms = filters.rooms,
        fromOwnerOnly = filters.fromOwnerOnly,
        withPhotoOnly = filters.withPhotoOnly,
        roomOnly = filters.roomOnly,
        isNotificationEnabled = filters.isNotificationEnabled,
        metroStations = MetroStations.allStationsRequest().map { requestStation ->
            val sameStationFromUi =
                filters.metroStationsState.find { it.name == requestStation.name }
            requestStation.copy(selected = sameStationFromUi?.selected == true)
        },
        withAnyMetro = filters.withAnyMetro,
        location = filters.location?.let {
            LocationFilter(
                country = filters.location.selectedCountry.code,
                city = filters.location.selectedCity.code
            )
        },
        districtsArea = UiDistrict.mapFromUiToModel(filters.districtsArea),
        userMapAreas = MapAreasUi.mapFromUiToModel(filters.userMapAreas),
        addressRequestModel = filters.address?.map {
            AddressRequestModel(address = it.address)
        }?.toSet().orEmpty(),
        sortOption = filters.sortOption,
        commercial = CommercialRequestModel(
            roomRange = filters.commercial.roomRange,
            commercialPropertyType = filters.commercial.commercialPropertyType?.find {
                it.selected
            }?.commercialPropertyType
        ),
        bookingDatesFilter = filters.bookingDatesFilter
    )
}


fun mapFilterModelToFilterState(model: CommonFilterRequestModel): FilterState {
    return FilterState(
        name = model.name,
        adType = model.adType,
        lastCommercialAdType = model.lastCommercialAdType,
        priceFull = model.priceFull,
        pricePerSquare = model.pricePerSquare,
        totalArea = model.totalArea,
        currency = model.currency,
        fromOwnerOnly = model.fromOwnerOnly ?: false,
        withPhotoOnly = model.withPhotoOnly,
        roomOnly = model.roomOnly,
        isNotificationEnabled = model.isNotificationEnabled,
        rooms = model.numberOfRooms ?: emptySet(),
        metroStationsState = MetroStationsMapper.allStationsOrderedForUi().map { uiStation ->
            val sameStationFromRequest = model.metroStations.find { it.name == uiStation.name }
            uiStation.copy(selected = sameStationFromRequest?.selected == true)
        },
        withAnyMetro = model.withAnyMetro,
        location = model.location?.let {
            LocationUiFilter(
                selectedCountry = UiCountry(it.country),
                selectedCity = LocationUiMapper.findSelectedCity(it.city),
                availableCities = LocationUiMapper.cities()
            )
        } ?: LocationUiFilter(),
        districtsArea = UiDistrict.mapFromModelToUi(model.districtsArea),
        userMapAreas = MapAreasUi.mapFromModelToUi(model.userMapAreas),
        address = model.addressRequestModel.map { AddressUiState(address = it.address) }
            .toSet(),
        sortOption = model.sortOption,
        commercial = CommercialFilters(
            roomRange = model.commercial?.roomRange,
            commercialPropertyType = getCommercialPropertiesTypeInfo(model)
        ),
        bookingDatesFilter = model.bookingDatesFilter
    )
}

private fun getCommercialPropertiesTypeInfo(model: CommonFilterRequestModel): List<CommercialPropertyTypeInfo> {
    var list = CommercialPropertyType.allInstances.map {
        val selected = model.commercial?.commercialPropertyType == it
        CommercialPropertyTypeInfo(
            selected = selected,
            commercialPropertyType = it,
            commercialPropertyTypeName = CommercialPropertyTypeInfo.commercialPropertyTypeName(
                it
            )
        )
    }
    if (list.find { it.selected } == null) {
        list = list.map {
            if (it.commercialPropertyType == CommercialPropertyType.Office) {
                it.copy(selected = true)
            } else {
                it
            }
        }
    }
    return list
}