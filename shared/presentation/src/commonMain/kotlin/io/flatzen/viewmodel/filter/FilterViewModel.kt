package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.AddressRequestModel
import entities.CommonFilterRequestModel
import entities.LocationFilter
import entities.MetroStations
import entities.SavedFilter
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import repository.fillter.FilterRepository
import repository.fillter.lastFilter

// Actions
sealed interface FilterScreenAction : MviAction {
    data class UpdateFilter(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenAction
    data class UpdateMetroFilter(val metroStation: UiMetroStation) : FilterScreenAction
    data class UpdateAddressFilter(val addressUiState: Set<AddressUiState>) : FilterScreenAction
    data class UpdateSortOption(val sortOption: FlatSort) : FilterScreenAction // Added sort option action
    data object ClearAllFilters : FilterScreenAction
    data object ClearLocationFilters : FilterScreenAction
    data object ClearMetroFilters : FilterScreenAction

    // Saved filters actions
    data object ShowSaveFilterDialog : FilterScreenAction
    data object HideSaveFilterDialog : FilterScreenAction
    data class UpdateFilterName(val name: String) : FilterScreenAction
    data object SaveFilter : FilterScreenAction
    data object LoadSavedFilters : FilterScreenAction
    data class ApplySavedFilter(val filterId: Long) : FilterScreenAction
    data class DeleteSavedFilter(val id: Long) : FilterScreenAction
    data class ToggleSavedFilterSelection(val filterId: Long) : FilterScreenAction
    data class CheckFilterMatchesSelected(val currentFilter: FilterState) : FilterScreenAction
    
    // Analytics actions
    class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FilterScreenAction
}

// State
@Immutable
data class FilterScreenState(
    val filters: FilterState,
    val savedFilters: List<SavedFilterState> = emptyList(),
    val dialogState: FilterDialogState = FilterDialogState()
) : MviState

// Events
sealed interface FilterScreenEvent : MviEvent {
    data class FiltersUpdated(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenEvent
    data class SavedFiltersLoaded(val filters: List<SavedFilterState>) : FilterScreenEvent
    data class FilterSaved(val filterId: Long) : FilterScreenEvent
    data class FilterDeleted(val filterId: Long) : FilterScreenEvent
    data class FilterApplied(val filter: SavedFilter) : FilterScreenEvent
    data class DialogStateUpdated(val dialogState: FilterDialogState) : FilterScreenEvent
    data class SavedFilterSelectionUpdated(val selectedFilterId: Long?) : FilterScreenEvent
}

class FilterViewModel(
    private val filterRepository: FilterRepository,
    private val analyticsManager: AnalyticsManager
) : BaseMviViewModel<FilterScreenAction, FilterScreenState, FilterScreenEvent, MviEffect>() {

    override fun initialState(): FilterScreenState = FilterScreenState(
        filters = FilterState(),
    )

    init {
        filterRepository.cashedFilterFlow.onEach { newFilters ->
            val filterState = mapFilterModelToFilterState(newFilters.commonFilterRequestModel)
            onIntent(FilterScreenAction.UpdateFilter(filterState, newFilters.doNetworkCall))
        }.launchIn(viewModelScope)

        // Load saved filters
        filterRepository.getAllSavedFilters().onEach { savedFilters ->
            onIntent(FilterScreenAction.LoadSavedFilters)
        }.launchIn(viewModelScope)
    }

    override suspend fun handleIntent(
        action: FilterScreenAction,
        currentState: FilterScreenState
    ): Flow<FilterScreenEvent> {
        return when (action) {
            is FilterScreenAction.UpdateFilter -> {
                // Check if current filter still matches selected saved filter
                val selectedFilter = currentState.savedFilters.find { it.selected }
                val events = mutableListOf<FilterScreenEvent>()
                events.add(FilterScreenEvent.FiltersUpdated(action.newFilterState, action.doNetworkCall))
                
                selectedFilter?.let { selected ->
                    val selectedFilterData = filterRepository.getSavedFilterById(selected.id)?.filterData
                    val currentFilterData = mapFilterStateToFilterModel(action.newFilterState)
                    
                    if (selectedFilterData != currentFilterData) {
                        // Current filter doesn't match selected saved filter, deselect it
                        filterRepository.clearAllSavedFilterSelections()
                        events.add(FilterScreenEvent.SavedFilterSelectionUpdated(null))
                    }
                }
                
                flowOf(*events.toTypedArray())
            }

            is FilterScreenAction.UpdateAddressFilter -> {
                val newState = currentState.filters.copy(address = action.addressUiState)
                flowOf(FilterScreenEvent.FiltersUpdated(newState))
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

            is FilterScreenAction.UpdateSortOption -> {
                val updatedFilterState = currentState.filters.copy(sortOption = action.sortOption)
                flowOf(FilterScreenEvent.FiltersUpdated(updatedFilterState)) // Trigger network call
            }

            is FilterScreenAction.ClearAllFilters -> {
                flowOf(FilterScreenEvent.FiltersUpdated(FilterState()))
            }

            is FilterScreenAction.ClearMetroFilters -> {
                val filter: FilterState = currentState.filters.copy(
                    metroStationsState = MetroStationsMapper.allStationsOrderedForUi()
                )
                flowOf(FilterScreenEvent.FiltersUpdated(filter))
            }

            is FilterScreenAction.ClearLocationFilters -> {
                val filter: FilterState = currentState.filters.copy(
                    metroStationsState = MetroStationsMapper.allStationsOrderedForUi(),
                    location = null,
                    address = null,
                )
                flowOf(FilterScreenEvent.FiltersUpdated(filter))
            }

            // Saved filters actions
            is FilterScreenAction.ShowSaveFilterDialog -> {
                flowOf(FilterScreenEvent.DialogStateUpdated(
                    currentState.dialogState.copy(isVisible = true)
                ))
            }

            is FilterScreenAction.HideSaveFilterDialog -> {
                flowOf(FilterScreenEvent.DialogStateUpdated(
                    currentState.dialogState.copy(
                        isVisible = false,
                        filterName = "",
                        isNameValid = true,
                        errorMessage = null
                    )
                ))
            }

            is FilterScreenAction.UpdateFilterName -> {
                val isNameValid = action.name.length <= 25 && action.name.isNotBlank()
                val errorMessage = when {
                    action.name.isBlank() -> "Название фильтра не может быть пустым"
                    action.name.length > 25 -> "Название фильтра не должно превышать 25 символов"
                    else -> null
                }
                flowOf(FilterScreenEvent.DialogStateUpdated(
                    currentState.dialogState.copy(
                        filterName = action.name,
                        isNameValid = isNameValid,
                        errorMessage = errorMessage
                    )
                ))
            }

            is FilterScreenAction.SaveFilter -> {
                val currentFilter = mapFilterStateToFilterModel(currentState.filters)
                val filterId = filterRepository.saveFilter(
                    currentState.dialogState.filterName,
                    currentFilter
                )
                flowOf(
                    FilterScreenEvent.FilterSaved(filterId),
                    FilterScreenEvent.DialogStateUpdated(
                        currentState.dialogState.copy(
                            isVisible = false,
                            filterName = "",
                            isNameValid = true,
                            errorMessage = null
                        )
                    )
                )
            }

            is FilterScreenAction.LoadSavedFilters -> {
                // This action is triggered from the flow observation
                // We need to get the current value from the flow
                val savedFilters = filterRepository.getAllSavedFilters().first()
                val savedFilterStates = savedFilters.map { mapSavedFilterToSavedFilterState(it) }
                flowOf(FilterScreenEvent.SavedFiltersLoaded(savedFilterStates))
            }

            is FilterScreenAction.ApplySavedFilter -> {
                val savedFilter = filterRepository.getSavedFilterById(action.filterId)
                savedFilter?.let {
                    filterRepository.applySavedFilter(it, false)
                    flowOf(FilterScreenEvent.FilterApplied(it))
                } ?: flowOf()
            }

            is FilterScreenAction.DeleteSavedFilter -> {
                filterRepository.deleteSavedFilter(action.id)
                flowOf(FilterScreenEvent.FilterDeleted(action.id))
            }
            
            is FilterScreenAction.ToggleSavedFilterSelection -> {
                val currentlySelected = currentState.savedFilters.find { it.selected }
                if (currentlySelected?.id == action.filterId) {
                    // Deselect current filter
                    filterRepository.clearAllSavedFilterSelections()
                    flowOf(FilterScreenEvent.SavedFilterSelectionUpdated(null))
                } else {
                    // Select new filter and apply it
                    filterRepository.updateSavedFilterSelection(action.filterId)
                    val savedFilter = filterRepository.getSavedFilterById(action.filterId)
                    savedFilter?.let {
                        filterRepository.applySavedFilter(it, false)
                    }
                    flowOf(FilterScreenEvent.SavedFilterSelectionUpdated(action.filterId))
                }
            }

            is FilterScreenAction.CheckFilterMatchesSelected -> {
                val selectedFilter = currentState.savedFilters.find { it.selected }
                selectedFilter?.let { selected ->
                    val selectedFilterData = filterRepository.getSavedFilterById(selected.id)?.filterData
                    val currentFilterData = mapFilterStateToFilterModel(action.currentFilter)
                    
                    if (selectedFilterData != currentFilterData) {
                        // Current filter doesn't match selected saved filter, deselect it
                        filterRepository.clearAllSavedFilterSelections()
                        flowOf(FilterScreenEvent.SavedFilterSelectionUpdated(null))
                    } else {
                        flowOf() // No change needed
                    }
                } ?: flowOf()
            }
            
            is FilterScreenAction.TrackScreenView -> {
                // Handle screen view analytics tracking
                viewModelScope.launch {
                    try {
                        analyticsManager.registerEvent(
                            AnalyticsEvent(
                                eventName = AppMetrcica.Events.SCREEN_VIEW,
                                parameters = mapOf(
                                    AppMetrcica.Parameters.SCREEN_NAME to action.screenName,
                                    AppMetrcica.Parameters.TIMESTAMP to Clock.System.now()
                                ) + action.parameters
                            )
                        )
                    } catch (e: Exception) {
                        // Log error but don't break the flow
                    }
                }
                flowOf()
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
            is FilterScreenEvent.SavedFiltersLoaded -> {
                currentState.copy(savedFilters = event.filters)
            }
            is FilterScreenEvent.FilterSaved -> {
                currentState
            }
            is FilterScreenEvent.FilterDeleted -> {
                currentState.copy(
                    savedFilters = currentState.savedFilters.filter { it.id != event.filterId }
                )
            }
            is FilterScreenEvent.FilterApplied -> {
                currentState
            }
            is FilterScreenEvent.DialogStateUpdated -> {
                currentState.copy(dialogState = event.dialogState)
            }
            is FilterScreenEvent.SavedFilterSelectionUpdated -> {
                currentState.copy(
                    savedFilters = currentState.savedFilters.map { filter ->
                        filter.copy(selected = filter.id == event.selectedFilterId)
                    }
                )
            }
        }
    }

    private fun mapFilterModelToFilterState(model: CommonFilterRequestModel): FilterState {
        return FilterState(
            adType = model.adType,
            priceFull = model.priceFull,
            pricePerSquare = model.pricePerSquare,
            totalArea = model.totalArea,
            currency = model.currency,
            fromOwnerOnly = model.fromOwnerOnly ?: false,
            withPhotoOnly = model.withPhotoOnly,
            roomOnly = model.roomOnly,
            rooms = model.numberOfRooms ?: emptySet(),
            metroStationsState = MetroStationsMapper.allStationsOrderedForUi().map { uiStation ->
                val sameStationFromRequest = model.metroStations.find { it.name == uiStation.name }
                uiStation.copy(selected = sameStationFromRequest?.selected == true)
            },
            location = model.location?.let {
                LocationUiFilter(
                    selectedCountry = UiCountry(it.country),
                    selectedCity = LocationUiMapper.findSelectedCity(it.city),
                    availableCities = LocationUiMapper.cities()
                )
            } ?: LocationUiFilter(),
            address = model.addressRequestModel.map { AddressUiState(address = it.address) }
                .toSet(),
            sortOption = model.sortOption // Added sort option mapping
        )
    }

    private fun mapFilterStateToFilterModel(filters: FilterState): CommonFilterRequestModel {
        return CommonFilterRequestModel(
            adType = filters.adType,
            priceFull = filters.priceFull,
            pricePerSquare = filters.pricePerSquare,
            totalArea = filters.totalArea,
            currency = filters.currency,
            numberOfRooms = filters.rooms,
            fromOwnerOnly = filters.fromOwnerOnly,
            withPhotoOnly= filters.withPhotoOnly,
            roomOnly = filters.roomOnly,
            metroStations = MetroStations.allStationsRequest().map { requestStation ->
                val sameStationFromUi =
                    filters.metroStationsState.find { it.name == requestStation.name }
                requestStation.copy(selected = sameStationFromUi?.selected == true)
            },
            location = filters.location?.let {
                LocationFilter(
                    country = filters.location.selectedCountry.code,
                    city = filters.location.selectedCity.code
                )
            },
            addressRequestModel = filters.address?.map {
                AddressRequestModel(address = it.address)
            }?.toSet().orEmpty(),
            sortOption = filters.sortOption // Added sort option mapping
        )
    }

    private fun mapSavedFilterToSavedFilterState(savedFilter: SavedFilter): SavedFilterState {
        return SavedFilterState(
            id = savedFilter.id,
            name = savedFilter.name,
            selected = savedFilter.selected,
            createdAt = savedFilter.createdAt
        )
    }
}