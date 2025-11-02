package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.AddressRequestModel
import entities.CommercialRequestModel
import entities.CommonFilterRequestModel
import entities.LocationFilter
import entities.MetroStations
import entities.SavedFilter
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.filter.FilterScreenEvent.DialogStateUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.FilterApplied
import io.flatzen.viewmodel.filter.FilterScreenEvent.FilterDeleted
import io.flatzen.viewmodel.filter.FilterScreenEvent.FilterSaved
import io.flatzen.viewmodel.filter.FilterScreenEvent.FiltersUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.SavedAreasDialogStateUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.SavedFilterSelectionUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.SavedFiltersLoaded
import io.flatzen.viewmodel.sharedstates.SavedAreasDialogState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import repository.fillter.FilterRepository
import repository.fillter.MapAreaRepository
import repository.fillter.lastFilter
import server_request.Currency

// Actions
sealed interface FilterScreenAction : MviAction {
    data class UpdateFilter(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenAction

    data class UpdateSelectedCommercialPropertyType(
        val commercialPropertyType: CommercialPropertyType
    ) : FilterScreenAction

    data class UpdateMetroFilter(val metroStation: UiMetroStation) : FilterScreenAction
    data class UpdateAddressFilter(val addressUiState: Set<AddressUiState>) : FilterScreenAction
    data class UpdateSortOption(val sortOption: FlatSort) :
        FilterScreenAction // Added sort option action

    data object ClearAllFilters : FilterScreenAction
    data object ClearLocationFilters : FilterScreenAction
    data object ClearMetroFilters : FilterScreenAction

    // Saved filters actions
    data object ShowSaveFilterDialog : FilterScreenAction
    data object HideSaveFilterDialog : FilterScreenAction

    data object ShowSavedAreaListDialog : FilterScreenAction
    data object HideSavedAreaListDialog : FilterScreenAction

    data class ActivateMapArea(val id: String, val checked: Boolean) : FilterScreenAction
    data class DeleteMapArea(val id: String) : FilterScreenAction

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
    val saveDialogState: SaveDialogState = SaveDialogState(),
    val savedAreasDialogState: SavedAreasDialogState = SavedAreasDialogState()
) : MviState

// Events
sealed interface FilterScreenEvent : MviEvent {
    data class FiltersUpdated(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenEvent

    data class SavedFiltersLoaded(val filters: List<SavedFilterState>) : FilterScreenEvent
    data class FilterSaved(val filterId: Long) : FilterScreenEvent
    data class FilterDeleted(val filterId: Long) : FilterScreenEvent
    data class FilterApplied(val filter: SavedFilter) : FilterScreenEvent
    data class DialogStateUpdated(val dialogState: SaveDialogState) : FilterScreenEvent
    data class SavedFilterSelectionUpdated(val selectedFilterId: Long?) : FilterScreenEvent
    data class SavedAreasDialogStateUpdated(val dialogState: SavedAreasDialogState) : FilterScreenEvent
}

class FilterViewModel(
    private val filterRepository: FilterRepository,
    private val mapAreaRepository: MapAreaRepository,
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
                val currency =
                    if (currentState.filters.adType == AdType.DAILY) Currency.BYR else Currency.USD
                val updatedFilter = action.newFilterState.copy(currency = currency)
                events.add(
                    FiltersUpdated(
                        updatedFilter,
                        action.doNetworkCall
                    )
                )

                selectedFilter?.let { selected ->
                    val selectedFilterData =
                        filterRepository.getSavedFilterById(selected.id)?.filterData
                    val currentFilterData = mapFilterStateToFilterModel(action.newFilterState)

                    if (selectedFilterData != currentFilterData) {
                        // Current filter doesn't match selected saved filter, deselect it
                        filterRepository.clearAllSavedFilterSelections()
                        events.add(SavedFilterSelectionUpdated(null))
                    }
                }

                flowOf(*events.toTypedArray())
            }

            is FilterScreenAction.UpdateSelectedCommercialPropertyType -> {
                val updatedCommercialTypes: List<CommercialPropertyTypeInfo>? =
                    currentState.filters.commercial.commercialPropertyType?.map {
                        if (it.commercialPropertyType == action.commercialPropertyType) it.copy(
                            selected = true
                        ) else it.copy(selected = false)
                    }
                val newState = currentState.filters.copy(
                    commercial = currentState.filters.commercial.copy(commercialPropertyType = updatedCommercialTypes)
                )
                flowOf(FiltersUpdated(newState))
            }

            is FilterScreenAction.UpdateAddressFilter -> {
                val newState = currentState.filters.copy(address = action.addressUiState)
                flowOf(FiltersUpdated(newState))
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
                flowOf(FiltersUpdated(updatedFilterState))
            }

            is FilterScreenAction.UpdateSortOption -> {
                val updatedFilterState = currentState.filters.copy(sortOption = action.sortOption)
                flowOf(FiltersUpdated(updatedFilterState))
            }

            is FilterScreenAction.ClearAllFilters -> {
                flowOf(FiltersUpdated(FilterState()))
            }

            is FilterScreenAction.ClearMetroFilters -> {
                val filter: FilterState = currentState.filters.copy(
                    metroStationsState = MetroStationsMapper.allStationsOrderedForUi()
                )
                flowOf(FiltersUpdated(filter))
            }

            is FilterScreenAction.ClearLocationFilters -> {
                val filter: FilterState = currentState.filters.copy(
                    metroStationsState = MetroStationsMapper.allStationsOrderedForUi(),
                    location = null,
                    address = null,
                )
                flowOf(FiltersUpdated(filter))
            }

            // Saved filters actions
            is FilterScreenAction.ShowSaveFilterDialog -> {
                flowOf(
                    DialogStateUpdated(
                        currentState.saveDialogState.copy(isVisible = true)
                    )
                )
            }

            is FilterScreenAction.HideSaveFilterDialog -> {
                flowOf(
                    DialogStateUpdated(
                        currentState.saveDialogState.copy(
                            isVisible = false,
                            filterName = "",
                            isNameValid = true,
                            errorMessage = null
                        )
                    )
                )
            }

            is FilterScreenAction.UpdateFilterName -> {
                val isNameValid = action.name.length <= 25 && action.name.isNotBlank()
                val errorMessage = when {
                    action.name.isBlank() -> "Название фильтра не может быть пустым"
                    action.name.length > 25 -> "Название фильтра не должно превышать 25 символов"
                    else -> null
                }
                flowOf(
                    DialogStateUpdated(
                        currentState.saveDialogState.copy(
                            filterName = action.name,
                            isNameValid = isNameValid,
                            errorMessage = errorMessage
                        )
                    )
                )
            }

            is FilterScreenAction.SaveFilter -> {
                val currentFilter = mapFilterStateToFilterModel(currentState.filters)
                val filterId = filterRepository.saveFilter(
                    currentState.saveDialogState.filterName,
                    currentFilter
                )
                flowOf(
                    FilterSaved(filterId),
                    DialogStateUpdated(
                        currentState.saveDialogState.copy(
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
                flowOf(SavedFiltersLoaded(savedFilterStates))
            }

            is FilterScreenAction.ApplySavedFilter -> {
                val savedFilter = filterRepository.getSavedFilterById(action.filterId)
                savedFilter?.let {
                    filterRepository.applySavedFilter(it, false)
                    flowOf(FilterApplied(it))
                } ?: flowOf()
            }

            is FilterScreenAction.DeleteSavedFilter -> {
                filterRepository.deleteSavedFilter(action.id)
                flowOf(FilterDeleted(action.id))
            }

            is FilterScreenAction.ToggleSavedFilterSelection -> {
                val currentlySelected = currentState.savedFilters.find { it.selected }
                if (currentlySelected?.id == action.filterId) {
                    // Deselect current filter
                    filterRepository.clearAllSavedFilterSelections()
                    flowOf(SavedFilterSelectionUpdated(null))
                } else {
                    // Select new filter and apply it
                    filterRepository.updateSavedFilterSelection(action.filterId)
                    val savedFilter = filterRepository.getSavedFilterById(action.filterId)
                    savedFilter?.let {
                        filterRepository.applySavedFilter(it, false)
                    }
                    flowOf(SavedFilterSelectionUpdated(action.filterId))
                }
            }

            is FilterScreenAction.CheckFilterMatchesSelected -> {
                val selectedFilter = currentState.savedFilters.find { it.selected }
                selectedFilter?.let { selected ->
                    val selectedFilterData =
                        filterRepository.getSavedFilterById(selected.id)?.filterData
                    val currentFilterData = mapFilterStateToFilterModel(action.currentFilter)

                    if (selectedFilterData != currentFilterData) {
                        // Current filter doesn't match selected saved filter, deselect it
                        filterRepository.clearAllSavedFilterSelections()
                        flowOf(SavedFilterSelectionUpdated(null))
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

            is FilterScreenAction.ActivateMapArea -> {
                if(action.checked) {
                    mapAreaRepository.activateMapArea(action.id)
                } else {
                    mapAreaRepository.deactivateMapArea(action.id)
                }
                val currentAreas = MapAreasUi.mapFromModelToUi(mapAreaRepository.getAllSavedAreas().first())
                val newState = currentState.filters.copy(mapAreas = currentAreas)
                flowOf(FiltersUpdated(newState), getUpdatedMapAreaDialogState())
            }
            is FilterScreenAction.DeleteMapArea -> {
                mapAreaRepository.deleteSavedArea(action.id)
                val currentAreas = MapAreasUi.mapFromModelToUi(mapAreaRepository.getAllSavedAreas().first())
                val newState = currentState.filters.copy(mapAreas = currentAreas)
                flowOf(FiltersUpdated(newState), getUpdatedMapAreaDialogState())
            }

            FilterScreenAction.ShowSavedAreaListDialog -> {
                flowOf(getUpdatedMapAreaDialogState())
            }
            FilterScreenAction.HideSavedAreaListDialog -> {
                flowOf(
                    SavedAreasDialogStateUpdated(
                        currentState.savedAreasDialogState.copy(isVisible = false)
                    )
                )
            }
        }
    }

    override suspend fun reduce(
        event: FilterScreenEvent,
        currentState: FilterScreenState
    ): FilterScreenState {
        return when (event) {
            is FiltersUpdated -> {
                currentState.copy(filters = event.newFilterState).also {
                    val filterModel: CommonFilterRequestModel =
                        mapFilterStateToFilterModel(it.filters)
                    if (filterModel != filterRepository.lastFilter()) {
                        filterRepository.updateFilter(
                            mapFilterStateToFilterModel(it.filters),
                            event.doNetworkCall
                        )
                    }
                }
            }

            is SavedFiltersLoaded -> {
                currentState.copy(savedFilters = event.filters)
            }

            is FilterSaved -> {
                currentState
            }

            is FilterDeleted -> {
                currentState.copy(
                    savedFilters = currentState.savedFilters.filter { it.id != event.filterId }
                )
            }

            is FilterApplied -> {
                currentState
            }

            is DialogStateUpdated -> {
                currentState.copy(saveDialogState = event.dialogState)
            }

            is SavedFilterSelectionUpdated -> {
                currentState.copy(
                    savedFilters = currentState.savedFilters.map { filter ->
                        filter.copy(selected = filter.id == event.selectedFilterId)
                    }
                )
            }

            is SavedAreasDialogStateUpdated -> {
                currentState.copy(savedAreasDialogState = event.dialogState)
            }
        }
    }

    private suspend fun getUpdatedMapAreaDialogState(): SavedAreasDialogStateUpdated {
        val mapAreasUi = mapAreaRepository.getAllSavedAreas().let {
            MapAreasUi.mapFromModelToUi(it.first())
        }
        return SavedAreasDialogStateUpdated(
                SavedAreasDialogState(
                    title = "Сохранённые области",
                    isVisible = true,
                    savedAreas = mapAreasUi
                )
            )

    }

    private fun mapFilterModelToFilterState(model: CommonFilterRequestModel): FilterState {
        return FilterState(
            adType = model.adType,
            lastCommercialAdType = model.lastCommercialAdType,
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
            mapAreas = MapAreasUi.mapFromModelToUi(model.mapAreas),
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
                if (it.commercialPropertyType == CommercialPropertyType.All) {
                    it.copy(selected = true)
                } else {
                    it
                }
            }
        }
        return list
    }

    private fun mapFilterStateToFilterModel(filters: FilterState): CommonFilterRequestModel {
        return CommonFilterRequestModel(
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
            mapAreas = MapAreasUi.mapFromUiToModel(filters.mapAreas),
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

    private fun mapSavedFilterToSavedFilterState(savedFilter: SavedFilter): SavedFilterState {
        return SavedFilterState(
            id = savedFilter.id,
            name = savedFilter.name,
            selected = savedFilter.selected,
            createdAt = savedFilter.createdAt
        )
    }
}