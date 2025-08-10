package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import entities.CommonFilterRequestModel
import entities.City
import entities.Country
import entities.LocationFilter
import entities.MetroLine
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.states.FilterState
import io.flatzen.states.LocationUiFilter
import io.flatzen.states.MetroLineState
import io.flatzen.states.UiCity
import io.flatzen.states.UiCountry
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import repository.fillter.FilterRepository
import repository.kufar.KufarRepository
import repository.mergedrepo.MergedRepository
import repository.onliner.OnlinerRepository
import repository.realt.RealtRepository

// Actions
sealed interface FilterScreenAction : MviAction {
    data class UpdateFilter(val newFilterState: FilterState) : FilterScreenAction
    object ClearFilters : FilterScreenAction
}

// State
@Immutable
data class FilterScreenState(
    val filters: FilterState,
) : MviState

// Events
sealed interface FilterScreenEvent : MviEvent {
    data class FiltersUpdated(val newFilterState: FilterState) : FilterScreenEvent
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
            val filterState = mapFilterModelToFilterState(newFilters)
            onIntent(FilterScreenAction.UpdateFilter(filterState))
        }.launchIn(viewModelScope)
    }

    override suspend fun handleIntent(
        action: FilterScreenAction,
        currentState: FilterScreenState
    ): Flow<FilterScreenEvent> {
        return when (action) {
            is FilterScreenAction.UpdateFilter -> {
                flowOf(FilterScreenEvent.FiltersUpdated(action.newFilterState))
            }

            is FilterScreenAction.ClearFilters -> {
                flowOf(FilterScreenEvent.FiltersUpdated(FilterState()))
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
                    if (filterModel != filterRepository.cashedFilterFlow.replayCache.firstOrNull()) {
                        mergedRepository.searchFlats()

                        filterRepository.updateFilter(
                            mapFilterStateToFilterModel(it.filters)
                        )
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
            metroLineState = model.metroLine.mapNotNull { line ->
                MetroLineState.entries.find { it.name == line.name }
            },
            location = model.location?.let {
                LocationUiFilter(
                    country = UiCountry(it.country.name),
                    city = UiCity(it.city.name)
                )
            } ?: LocationUiFilter(),
            selectedMetroStationIds = model.selectedMetroStationIds
        )
    }

    private fun mapFilterStateToFilterModel(filters: FilterState): CommonFilterRequestModel {
        return CommonFilterRequestModel(
            priceFrom = filters.priceFrom,
            priceTo = filters.priceTo,
            currency = filters.currency,
            numberOfRooms = filters.rooms,
            fromOwnerOnly = filters.fromOwnerOnly,
            metroLine = filters.metroLineState.mapNotNull { line ->
                MetroLine.entries.find { it.name == line.name }
            },
            location = filters.location?.let {
                LocationFilter(
                    country = Country.valueOf(filters.location.country.code),
                    city = City.valueOf(filters.location.city.code)
                )
            },
            selectedMetroStationIds = filters.selectedMetroStationIds
        )
    }
}