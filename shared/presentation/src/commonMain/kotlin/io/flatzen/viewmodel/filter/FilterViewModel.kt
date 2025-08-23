package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.AddressRequestModel
import entities.CommonFilterRequestModel
import entities.City
import entities.Country
import entities.LocationFilter
import entities.MetroStations
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.mergedrepo.MergedRepository

// Actions
sealed interface FilterScreenAction : MviAction {
    data class UpdateFilter(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenAction
    data class UpdateCityFilter(val newFilterState: FilterState) : FilterScreenAction
    data class UpdateMetroFilter(val metroStation: UiMetroStation) : FilterScreenAction
    data class UpdateAddressFilter(val addressUiState: Set<AddressUiState>) : FilterScreenAction
    data object ClearAllFilters : FilterScreenAction
    data object ClearLocationFilters : FilterScreenAction
}

// State
@Immutable
data class FilterScreenState(
    val filters: FilterState,
) : MviState

// Events
sealed interface FilterScreenEvent : MviEvent {
    data class FiltersUpdated(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenEvent
}

class FilterViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository
) : BaseMviViewModel<FilterScreenAction, FilterScreenState, FilterScreenEvent, MviEffect>() {

    override fun initialState(): FilterScreenState = FilterScreenState(
        filters = FilterState(),
    )

    init {
        filterRepository.cashedFilterFlow.onEach { newFilters ->
            val filterState = mapFilterModelToFilterState(newFilters.commonFilterRequestModel)
            onIntent(FilterScreenAction.UpdateFilter(filterState, newFilters.doNetworkCall))
        }.launchIn(viewModelScope)
    }

    override suspend fun handleIntent(
        action: FilterScreenAction,
        currentState: FilterScreenState
    ): Flow<FilterScreenEvent> {
        return when (action) {
            is FilterScreenAction.UpdateFilter -> {
                flowOf(
                    FilterScreenEvent.FiltersUpdated(
                        action.newFilterState,
                        action.doNetworkCall
                    )
                )
            }

            is FilterScreenAction.UpdateAddressFilter -> {
                val newState = currentState.filters.copy(address = action.addressUiState)
                flowOf(FilterScreenEvent.FiltersUpdated(newState))
            }

            is FilterScreenAction.UpdateCityFilter -> {
                flowOf(FilterScreenEvent.FiltersUpdated(action.newFilterState))
            }

            is FilterScreenAction.UpdateMetroFilter -> {
                val updatedFilterState = currentState.filters.copy(
                    metroStationsState = currentState.filters.metroStationsState.map {
                        if (it.name == action.metroStation.name) {
                            action.metroStation
                        } else {
                            it
                        }
                    }
                )
                flowOf(FilterScreenEvent.FiltersUpdated(updatedFilterState))
            }

            is FilterScreenAction.ClearAllFilters -> {
                flowOf(FilterScreenEvent.FiltersUpdated(FilterState()))
            }

            is FilterScreenAction.ClearLocationFilters -> {
                val filter: FilterState = currentState.filters.copy(
                    metroStationsState = MetroStationsMapper.allStationsOrderedForUi(),
                    location = null,
                    address = null,
                )
                flowOf(FilterScreenEvent.FiltersUpdated(filter))
            }
        }
    }

    override suspend fun reduce(
        event: FilterScreenEvent,
        currentState: FilterScreenState
    ): FilterScreenState {
        return when (event) {
            is FilterScreenEvent.FiltersUpdated -> {
                currentState.copy(filters = event.newFilterState).also {
                    val filterModel: CommonFilterRequestModel =
                        mapFilterStateToFilterModel(it.filters)
                    if (filterModel != filterRepository.lastFilter()) {
                        filterRepository.updateFilter(mapFilterStateToFilterModel(it.filters), event.doNetworkCall)
                    }
                }
            }
        }
    }

    private fun mapFilterModelToFilterState(model: CommonFilterRequestModel): FilterState {
        return FilterState(
            priceFrom = model.priceFrom,
            priceTo = model.priceTo,
            currency = model.currency,
            fromOwnerOnly = model.fromOwnerOnly ?: false,
            rooms = model.numberOfRooms ?: emptySet(),
            metroStationsState = MetroStationsMapper.allStationsOrderedForUi().map { uiStation ->
                val sameStationFromRequest = model.metroStations.find { it.name == uiStation.name }
                uiStation.copy(selected = sameStationFromRequest?.selected == true)
            },
            location = model.location?.let {
                LocationUiFilter(
                    selectedCountry = UiCountry(it.country.name),
                    selectedCity = UiCity(
                        it.city.name,
                        LocationUiMapper.displayName(it.city.name)
                    ),
                    availableCities = LocationUiMapper.cities()
                )
            } ?: LocationUiFilter(),
            address = model.addressRequestModel.map { AddressUiState(address = it.address) }
                .toSet(),
        )
    }

    private fun mapFilterStateToFilterModel(filters: FilterState): CommonFilterRequestModel {
        return CommonFilterRequestModel(
            priceFrom = filters.priceFrom,
            priceTo = filters.priceTo,
            currency = filters.currency,
            numberOfRooms = filters.rooms,
            fromOwnerOnly = filters.fromOwnerOnly,
            metroStations = MetroStations.allStationsRequest().map { requestStation ->
                val sameStationFromUi =
                    filters.metroStationsState.find { it.name == requestStation.name }
                requestStation.copy(selected = sameStationFromUi?.selected == true)
            },
            location = filters.location?.let {
                LocationFilter(
                    country = Country.valueOf(filters.location.selectedCountry.code),
                    city = City.valueOf(filters.location.selectedCity.code)
                )
            },
            addressRequestModel = filters.address?.map {
                AddressRequestModel(address = it.address)
            }?.toSet().orEmpty(),
        )
    }
}