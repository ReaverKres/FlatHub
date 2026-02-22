package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.CommonFilterRequestModel
import entities.SavedFilter
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.UiDistrict
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.filter.FilterScreenEvent.FilterApplied
import io.flatzen.viewmodel.filter.FilterScreenEvent.FilterDeleted
import io.flatzen.viewmodel.filter.FilterScreenEvent.FilterSaved
import io.flatzen.viewmodel.filter.FilterScreenEvent.FiltersUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.SaveDialogStateUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.SavedAreasDialogStateUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.SavedFilterSelectionUpdated
import io.flatzen.viewmodel.filter.FilterScreenEvent.SavedFiltersLoaded
import io.flatzen.viewmodel.sharedstates.SavedAreasDialogState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import repository.fillter.FilterRepository
import repository.fillter.UserMapAreaRepository
import repository.fillter.areasInFilter
import repository.fillter.lastFilter
import repository.userpreferences.UserPreferencesRepository
import server_request.Currency

// Actions
sealed interface FilterScreenAction : MviAction {
    data class UpdateFilter(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenAction

    data class UpdateSelectedCommercialPropertyType(
        val commercialPropertyType: CommercialPropertyType
    ) : FilterScreenAction

    data class UpdateMetroFilter(val metroStation: UiMetroStation) : FilterScreenAction
    data class UpdateDistrictFilter(val allDistricts: List<UiDistrict>, val district: UiDistrict) :
        FilterScreenAction

    data class UpdateAddressFilter(val addressUiState: Set<AddressUiState>) : FilterScreenAction
    data class UpdateSortOption(val sortOption: FlatSort) :
        FilterScreenAction // Added sort option action

    data object ClearAllFilters : FilterScreenAction
    data object ClearLocationFilters : FilterScreenAction
    data object ClearMetroFilters : FilterScreenAction

    data object ShowSaveFilterDialog : FilterScreenAction
    data object HideSaveFilterDialog : FilterScreenAction

    data object ShowSavedAreaListDialog : FilterScreenAction
    data object HideSavedAreaListDialog : FilterScreenAction

    data class ActivateMapArea(
        val id: String,
        val checked: Boolean,
        val doNetworkCall: Boolean = false,
        val savedAreasDialogIsVisible: Boolean = true
    ) : FilterScreenAction

    data class DeleteMapArea(val id: String, val doNetworkCall: Boolean = false) :
        FilterScreenAction

    data class UpdateFilterName(val name: String) : FilterScreenAction
    data class NotificationEnable(val enabled: Boolean) : FilterScreenAction
    data object SaveFilter : FilterScreenAction
    data object LoadSavedFilters : FilterScreenAction
    data class DeleteSavedFilter(val id: Long) : FilterScreenAction
    data class ToggleSavedFilterSelection(val filterId: Long) : FilterScreenAction

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
    val saveDialogState: SaveDialogState = SaveDialogState(showNotification = true),
    val savedAreasDialogState: SavedAreasDialogState = SavedAreasDialogState(),
) : MviState

// Events
sealed interface FilterScreenEvent : MviEvent {
    data class FiltersUpdated(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenEvent

    data class SavedFiltersLoaded(val filters: List<SavedFilterState>) : FilterScreenEvent
    data class FilterSaved(val filterId: Long) : FilterScreenEvent
    data class FilterDeleted(val filterId: Long) : FilterScreenEvent
    data class FilterApplied(val filter: SavedFilter) : FilterScreenEvent
    data class SaveDialogStateUpdated(val dialogState: SaveDialogState) : FilterScreenEvent
    data class SavedFilterSelectionUpdated(val selectedFilterId: Long?) : FilterScreenEvent
    data class SavedAreasDialogStateUpdated(val dialogState: SavedAreasDialogState) :
        FilterScreenEvent
    data object NavigateToReferralScreen: FilterScreenEvent
}

sealed interface FilterEffect : MviEffect {
    data object NavigateToReferralEffect: FilterEffect
}

class FilterViewModel(
    private val filterRepository: FilterRepository,
    private val userMapAreaRepository: UserMapAreaRepository,
    private val analyticsManager: AnalyticsManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : BaseMviViewModel<FilterScreenAction, FilterScreenState, FilterScreenEvent, MviEffect>() {

    override fun initialState(): FilterScreenState = FilterScreenState(
        filters = FilterState(),
    )

    init {
        filterRepository.cashedFilterFlow.onEach { newFilters ->
            val filterState = mapFilterModelToFilterState(newFilters.commonFilterRequestModel)
            onIntent(FilterScreenAction.UpdateFilter(filterState, false))
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

            is FilterScreenAction.UpdateDistrictFilter -> {
                val allDistricts = if (currentState.filters.districtsArea.isNullOrEmpty()) {
                    action.allDistricts
                } else {
                    currentState.filters.districtsArea
                }
                val updatedFilterState = currentState.filters.copy(
                    districtsArea = allDistricts.map {
                        if (it.nameLocal == action.district.nameLocal) {
                            action.district
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
                    userMapAreas = currentState.filters.userMapAreas?.map { it.copy(isActive = false) },
                    districtsArea = currentState.filters.districtsArea?.map { it.copy(isChecked = false) },
                )
                flowOf(FiltersUpdated(filter))
            }

            is FilterScreenAction.ShowSaveFilterDialog -> {
                flowOf(
                    SaveDialogStateUpdated(
                        currentState.saveDialogState.copy(isVisible = true)
                    )
                )
            }

            is FilterScreenAction.HideSaveFilterDialog -> {
                flowOf(
                    SaveDialogStateUpdated(
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
                    SaveDialogStateUpdated(
                        currentState.saveDialogState.copy(
                            filterName = action.name,
                            isNameValid = isNameValid,
                            errorMessage = errorMessage
                        )
                    )
                )
            }

            is FilterScreenAction.NotificationEnable -> {
                if (userPreferencesRepository.getUserPreferences()
                        .firstOrNull()?.deviceDocumentResponse?.referralStats?.isNotificationAvailable == true
                ) {
                    flowOf(
                        SaveDialogStateUpdated(
                            currentState.saveDialogState.copy(
                                isNotificationEnabled = action.enabled,
                            )
                        )
                    )
                } else {
                    flowOf(FilterScreenEvent.NavigateToReferralScreen)
                }
            }

            is FilterScreenAction.SaveFilter -> {
                val currentFilter = mapFilterStateToFilterModel(currentState.filters)
                val filterId = filterRepository.saveFilter(
                    currentState.saveDialogState.filterName,
                    currentFilter
                )
                flowOf(
                    FilterSaved(filterId),
                    SaveDialogStateUpdated(
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
                var currentAreas = filterRepository.areasInFilter(userMapAreaRepository)
                currentAreas = if (action.checked) {
                    currentAreas.map {
                        if (it.pathId == action.id) it.copy(isActive = true) else it
                    }
                } else {
                    currentAreas.map {
                        if (it.pathId == action.id) it.copy(isActive = false) else it
                    }
                }
                val currentAreasUi = MapAreasUi.mapFromModelToUi(currentAreas)
                val newState = currentState.filters.copy(userMapAreas = currentAreasUi)
                flowOf(
                    FiltersUpdated(newState, action.doNetworkCall),
                    getUpdatedMapAreaDialogState(currentAreasUi, action.savedAreasDialogIsVisible)
                )
            }

            is FilterScreenAction.DeleteMapArea -> {
                userMapAreaRepository.deleteSavedArea(action.id)
                val currentAreasUi =
                    MapAreasUi.mapFromModelToUi(filterRepository.areasInFilter(userMapAreaRepository))
                val newState = currentState.filters.copy(userMapAreas = currentAreasUi)
                flowOf(
                    FiltersUpdated(newState, action.doNetworkCall),
                    getUpdatedMapAreaDialogState(currentAreasUi)
                )
            }

            FilterScreenAction.ShowSavedAreaListDialog -> {
                val currentAreas = filterRepository.areasInFilter(userMapAreaRepository)
                val currentAreasUi = MapAreasUi.mapFromModelToUi(currentAreas)
                flowOf(getUpdatedMapAreaDialogState(currentAreasUi))
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

    override suspend fun onEvent(event: FilterScreenEvent): MviEffect? {
        return when (event) {
            is FilterScreenEvent.NavigateToReferralScreen -> FilterEffect.NavigateToReferralEffect
            else -> super.onEvent(event)
        }
    }

    override suspend fun reduce(
        event: FilterScreenEvent,
        currentState: FilterScreenState
    ): FilterScreenState {
        return when (event) {
            is FilterScreenEvent.NavigateToReferralScreen -> currentState
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

            is SaveDialogStateUpdated -> {
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

    private suspend fun getUpdatedMapAreaDialogState(
        currentAreas: List<MapAreasUi>,
        isVisible: Boolean = true
    ): SavedAreasDialogStateUpdated {
        return SavedAreasDialogStateUpdated(
            SavedAreasDialogState(
                title = "Сохранённые области",
                isVisible = isVisible,
                savedAreas = currentAreas
            )
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