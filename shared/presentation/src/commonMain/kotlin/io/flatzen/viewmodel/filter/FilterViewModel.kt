package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.AddressRequestModel
import entities.CommonFilterRequestModel
import entities.City
import entities.Country
import entities.LocationFilter
import entities.MetroStations
import entities.SavedFilter
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
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.mergedrepo.MergedRepository
import io.flatzen.platformtools.notifications.NotificationPermissionProvider
import io.flatzen.platformtools.background.BackgroundWorkManager

// Actions
sealed interface FilterScreenAction : MviAction {
    data class UpdateFilter(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenAction
    data class UpdateCityFilter(val newFilterState: FilterState) : FilterScreenAction
    data class UpdateMetroFilter(val metroStation: UiMetroStation) : FilterScreenAction
    data class UpdateAddressFilter(val addressUiState: Set<AddressUiState>) : FilterScreenAction
    data object ClearAllFilters : FilterScreenAction
    data object ClearLocationFilters : FilterScreenAction
    
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
    
    // Notification actions
    data object ToggleNotificationEnabled : FilterScreenAction
    data class UpdateNotificationInterval(val interval: Int) : FilterScreenAction
    data object ApplyNotificationFilter : FilterScreenAction
    data object DeleteNotificationFilter : FilterScreenAction
    data class UpdateNotificationState(
        val enabled: Boolean,
        val interval: Int?,
        val hasNotificationFilter: Boolean,
        val hasPermission: Boolean
    ) : FilterScreenAction
}

// State
@Immutable
data class FilterScreenState(
    val filters: FilterState,
    val savedFilters: List<SavedFilterState> = emptyList(),
    val dialogState: FilterDialogState = FilterDialogState(),
    val notificationEnabled: Boolean = false,
    val notificationInterval: Int? = null,
    val hasNotificationFilter: Boolean = false,
    val hasNotificationPermission: Boolean = false
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
    
    // Notification events
    data class NotificationStateUpdated(
        val enabled: Boolean,
        val interval: Int?,
        val hasNotificationFilter: Boolean,
        val hasPermission: Boolean
    ) : FilterScreenEvent
    data class NotificationFilterApplied(val filterId: Long) : FilterScreenEvent
    data object NotificationFilterDeleted : FilterScreenEvent
}

class FilterViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val notificationPermissionProvider: NotificationPermissionProvider,
    private val backgroundWorkManager: BackgroundWorkManager
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
        
        // Initialize notification state
        viewModelScope.launch {
            val hasNotificationFilter = filterRepository.hasNotificationFilter()
            val notificationFilter = filterRepository.getNotificationFilter()
            val hasPermission = notificationPermissionProvider.hasPermission()
            
            onIntent(FilterScreenAction.UpdateNotificationState(
                enabled = hasNotificationFilter,
                interval = notificationFilter?.notificationInterval,
                hasNotificationFilter = hasNotificationFilter,
                hasPermission = hasPermission
            ))
        }
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
                val isNameValid = action.name.length <= 15 && action.name.isNotBlank()
                val errorMessage = when {
                    action.name.isBlank() -> "Название фильтра не может быть пустым"
                    action.name.length > 15 -> "Название фильтра не должно превышать 15 символов"
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
            
            // Notification actions
            is FilterScreenAction.UpdateNotificationState -> {
                flowOf(FilterScreenEvent.NotificationStateUpdated(
                    enabled = action.enabled,
                    interval = action.interval,
                    hasNotificationFilter = action.hasNotificationFilter,
                    hasPermission = action.hasPermission
                ))
            }
            
            is FilterScreenAction.ToggleNotificationEnabled -> {
                if (currentState.notificationEnabled) {
                    // Disable notifications - delete notification filter and cancel background work
                    filterRepository.deleteNotificationFilter()
                    backgroundWorkManager.cancelWork()
                    flowOf(
                        FilterScreenEvent.NotificationFilterDeleted,
                        FilterScreenEvent.NotificationStateUpdated(
                            enabled = false,
                            interval = null,
                            hasNotificationFilter = false,
                            hasPermission = currentState.hasNotificationPermission
                        )
                    )
                } else {
                    // Enable notifications - just update state, filter will be applied when user clicks "Apply"
                    flowOf(FilterScreenEvent.NotificationStateUpdated(
                        enabled = true,
                        interval = currentState.notificationInterval ?: 15, // Default to 15 minutes
                        hasNotificationFilter = currentState.hasNotificationFilter,
                        hasPermission = currentState.hasNotificationPermission
                    ))
                }
            }
            
            is FilterScreenAction.UpdateNotificationInterval -> {
                flowOf(FilterScreenEvent.NotificationStateUpdated(
                    enabled = currentState.notificationEnabled,
                    interval = action.interval,
                    hasNotificationFilter = currentState.hasNotificationFilter,
                    hasPermission = currentState.hasNotificationPermission
                ))
            }
            
            is FilterScreenAction.ApplyNotificationFilter -> {
                // Check notification permission first
                val hasPermission = notificationPermissionProvider.hasPermission()
                if (!hasPermission) {
                    val granted = notificationPermissionProvider.requestPermission()
                    if (!granted) {
                        return@handleIntent flowOf(FilterScreenEvent.NotificationStateUpdated(
                            enabled = currentState.notificationEnabled,
                            interval = currentState.notificationInterval,
                            hasNotificationFilter = currentState.hasNotificationFilter,
                            hasPermission = false
                        ))
                    }
                }
                
                // Save notification filter
                val currentFilter = mapFilterStateToFilterModel(currentState.filters)
                val interval = currentState.notificationInterval ?: 15
                val filterId = filterRepository.saveNotificationFilter(
                    "Notification Filter",
                    currentFilter,
                    interval
                )
                
                // Schedule background work
                backgroundWorkManager.schedulePeriodicWork(interval, currentFilter)
                
                flowOf(
                    FilterScreenEvent.NotificationFilterApplied(filterId),
                    FilterScreenEvent.NotificationStateUpdated(
                        enabled = true,
                        interval = interval,
                        hasNotificationFilter = true,
                        hasPermission = true
                    )
                )
            }
            
            is FilterScreenAction.DeleteNotificationFilter -> {
                filterRepository.deleteNotificationFilter()
                backgroundWorkManager.cancelWork()
                flowOf(
                    FilterScreenEvent.NotificationFilterDeleted,
                    FilterScreenEvent.NotificationStateUpdated(
                        enabled = false,
                        interval = null,
                        hasNotificationFilter = false,
                        hasPermission = currentState.hasNotificationPermission
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
            
            // Notification events
            is FilterScreenEvent.NotificationStateUpdated -> {
                currentState.copy(
                    notificationEnabled = event.enabled,
                    notificationInterval = event.interval,
                    hasNotificationFilter = event.hasNotificationFilter,
                    hasNotificationPermission = event.hasPermission
                )
            }
            
            is FilterScreenEvent.NotificationFilterApplied -> {
                currentState
            }
            
            is FilterScreenEvent.NotificationFilterDeleted -> {
                // Remove notification filter from saved filters list and update UI state
                currentState.copy(
                    savedFilters = currentState.savedFilters.filter { !it.isNotification },
                    notificationEnabled = false,
                    hasNotificationFilter = false
                )
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

    private fun mapSavedFilterToSavedFilterState(savedFilter: SavedFilter): SavedFilterState {
        return SavedFilterState(
            id = savedFilter.id,
            name = savedFilter.name,
            selected = savedFilter.selected,
            isNotification = savedFilter.isNotification,
            notificationInterval = savedFilter.notificationInterval,
            createdAt = savedFilter.createdAt
        )
    }
}