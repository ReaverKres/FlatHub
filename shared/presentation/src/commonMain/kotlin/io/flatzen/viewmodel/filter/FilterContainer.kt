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
import io.flatzen.viewmodel.UiDistrict
import io.flatzen.viewmodel.sharedstates.SavedAreasDialogState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.fillter.FilterRepository
import repository.fillter.UserMapAreaRepository
import repository.fillter.areasInFilter
import repository.fillter.lastFilter
import repository.userpreferences.UserPreferencesRepository
import server_request.Currency

private typealias PipeCtx = PipelineContext<FilterScreenState, FilterScreenAction, FilterEffect>

sealed interface FilterScreenAction : MVIIntent {
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
    data class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FilterScreenAction
}

@Immutable
data class FilterScreenState(
    val filters: FilterState,
    val savedFilters: List<SavedFilterState> = emptyList(),
    val saveDialogState: SaveDialogState = SaveDialogState(showNotification = true),
    val savedAreasDialogState: SavedAreasDialogState = SavedAreasDialogState(),
) : MVIState {
    companion object {
        val Initial = FilterScreenState(filters = FilterState())
    }
}

sealed interface FilterEffect : MVIAction {
    data object NavigateToReferralEffect : FilterEffect
}

class FilterContainer(
    private val filterRepository: FilterRepository,
    private val userMapAreaRepository: UserMapAreaRepository,
    private val analyticsManager: AnalyticsManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : Container<FilterScreenState, FilterScreenAction, FilterEffect> {

    override val store = store<FilterScreenState, FilterScreenAction, FilterEffect>(
        initial = FilterScreenState.Initial
    ) {
        whileSubscribed {
            filterRepository.cashedFilterFlow.collect { newFilters ->
                val filterState = mapFilterModelToFilterState(newFilters.commonFilterRequestModel)
                applyFiltersUpdate(filterState, false)
            }
        }
        whileSubscribed {
            filterRepository.getAllSavedFilters().collect { savedFilters ->
                updateState {
                    copy(savedFilters = savedFilters.map(::mapSavedFilterToSavedFilterState))
                }
            }
        }
        reduce { intent ->
            when (intent) {
                is FilterScreenAction.UpdateFilter -> applyFiltersUpdate(
                    newFilterState = intent.newFilterState,
                    doNetworkCall = intent.doNetworkCall
                )

                is FilterScreenAction.UpdateSelectedCommercialPropertyType -> {
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    val updatedCommercialTypes: List<CommercialPropertyTypeInfo>? =
                        currentState.filters.commercial.commercialPropertyType?.map {
                            if (it.commercialPropertyType == intent.commercialPropertyType) {
                                it.copy(selected = true)
                            } else {
                                it.copy(selected = false)
                            }
                        }
                    val newState = currentState.filters.copy(
                        commercial = currentState.filters.commercial.copy(commercialPropertyType = updatedCommercialTypes)
                    )
                    applyFiltersUpdate(newState, false)
                }

                is FilterScreenAction.UpdateAddressFilter -> {
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    applyFiltersUpdate(
                        newFilterState = currentState.filters.copy(address = intent.addressUiState),
                        doNetworkCall = false
                    )
                }

                is FilterScreenAction.UpdateMetroFilter -> {
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    val updatedFilterState = currentState.filters.copy(
                        metroStationsState = currentState.filters.metroStationsState.map {
                            if (it.name == intent.metroStation.name) intent.metroStation else it
                        }
                    )
                    applyFiltersUpdate(updatedFilterState, false)
                }

                is FilterScreenAction.UpdateDistrictFilter -> {
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    val allDistricts = if (currentState.filters.districtsArea.isNullOrEmpty()) {
                        intent.allDistricts
                    } else {
                        currentState.filters.districtsArea
                    }
                    val updatedFilterState = currentState.filters.copy(
                        districtsArea = allDistricts?.map {
                            if (it.nameLocal == intent.district.nameLocal) intent.district else it
                        }
                    )
                    applyFiltersUpdate(updatedFilterState, false)
                }

                is FilterScreenAction.UpdateSortOption -> {
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    applyFiltersUpdate(
                        newFilterState = currentState.filters.copy(sortOption = intent.sortOption),
                        doNetworkCall = false
                    )
                }

                FilterScreenAction.ClearAllFilters -> applyFiltersUpdate(FilterState(), false)

                FilterScreenAction.ClearMetroFilters -> {
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    val filter = currentState.filters.copy(
                        metroStationsState = MetroStationsMapper.allStationsOrderedForUi()
                    )
                    applyFiltersUpdate(filter, false)
                }

                FilterScreenAction.ClearLocationFilters -> {
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    val filter = currentState.filters.copy(
                        metroStationsState = MetroStationsMapper.allStationsOrderedForUi(),
                        location = null,
                        address = null,
                        userMapAreas = currentState.filters.userMapAreas?.map { it.copy(isActive = false) },
                        districtsArea = currentState.filters.districtsArea?.map { it.copy(isChecked = false) },
                    )
                    applyFiltersUpdate(filter, false)
                }

                FilterScreenAction.ShowSaveFilterDialog -> {
                    updateState {
                        copy(saveDialogState = saveDialogState.copy(isVisible = true))
                    }
                }

                FilterScreenAction.HideSaveFilterDialog -> {
                    updateState {
                        copy(
                            saveDialogState = saveDialogState.copy(
                                isVisible = false,
                                filterName = "",
                                isNameValid = true,
                                errorMessage = null
                            )
                        )
                    }
                }

                is FilterScreenAction.UpdateFilterName -> {
                    val isNameValid = intent.name.length <= 25 && intent.name.isNotBlank()
                    val errorMessage = when {
                        intent.name.isBlank() -> "Название фильтра не может быть пустым"
                        intent.name.length > 25 -> "Название фильтра не должно превышать 25 символов"
                        else -> null
                    }
                    updateState {
                        copy(
                            saveDialogState = saveDialogState.copy(
                                filterName = intent.name,
                                isNameValid = isNameValid,
                                errorMessage = errorMessage
                            )
                        )
                    }
                }

                is FilterScreenAction.NotificationEnable -> {
                    if (userPreferencesRepository.getUserPreferences()
                            .firstOrNull()?.deviceDocumentResponse?.referralStats?.isNotificationAvailable == true
                    ) {
                        updateState {
                            copy(
                                saveDialogState = saveDialogState.copy(
                                    isNotificationEnabled = intent.enabled,
                                )
                            )
                        }
                    } else {
                        action(FilterEffect.NavigateToReferralEffect)
                    }
                }

                FilterScreenAction.SaveFilter -> {
                    var currentState = FilterScreenState.Initial
                    withState {
                        currentState = this
                    }
                    val currentFilter = mapFilterStateToFilterModel(currentState.filters)
                    filterRepository.saveFilter(
                        currentState.saveDialogState.filterName,
                        currentFilter
                    )
                    updateState {
                        copy(
                            saveDialogState = saveDialogState.copy(
                                isVisible = false,
                                filterName = "",
                                isNameValid = true,
                                errorMessage = null
                            )
                        )
                    }
                }

                FilterScreenAction.LoadSavedFilters -> {
                    val savedFilters = filterRepository.getAllSavedFilters().first()
                    updateState {
                        copy(savedFilters = savedFilters.map(::mapSavedFilterToSavedFilterState))
                    }
                }

                is FilterScreenAction.DeleteSavedFilter -> {
                    filterRepository.deleteSavedFilter(intent.id)
                    updateState {
                        copy(savedFilters = savedFilters.filter { it.id != intent.id })
                    }
                }

                is FilterScreenAction.ToggleSavedFilterSelection -> {
                    var selectedFilterId: Long? = null
                    withState {
                        val currentlySelected = savedFilters.find { it.selected }
                        selectedFilterId = if (currentlySelected?.id == intent.filterId) {
                            filterRepository.clearAllSavedFilterSelections()
                            null
                        } else {
                            filterRepository.updateSavedFilterSelection(intent.filterId)
                            filterRepository.getSavedFilterById(intent.filterId)?.let {
                                filterRepository.applySavedFilter(it, false)
                            }
                            intent.filterId
                        }
                    }
                    val selectedId = selectedFilterId
                    updateState {
                        copy(
                            savedFilters = savedFilters.map { filter ->
                                filter.copy(selected = filter.id == selectedId)
                            }
                        )
                    }
                }

                is FilterScreenAction.TrackScreenView -> {
                    launch {
                        try {
                            analyticsManager.registerEvent(
                                AnalyticsEvent(
                                    eventName = AppMetrcica.Events.SCREEN_VIEW,
                                    parameters = mapOf(
                                        AppMetrcica.Parameters.SCREEN_NAME to intent.screenName,
                                        AppMetrcica.Parameters.TIMESTAMP to Clock.System.now()
                                    ) + intent.parameters
                                )
                            )
                        } catch (_: Exception) {
                            // Intentionally ignored for analytics failures.
                        }
                    }
                }

                is FilterScreenAction.ActivateMapArea -> {
                    var currentAreas = filterRepository.areasInFilter(userMapAreaRepository)
                    currentAreas = if (intent.checked) {
                        currentAreas.map {
                            if (it.pathId == intent.id) it.copy(isActive = true) else it
                        }
                    } else {
                        currentAreas.map {
                            if (it.pathId == intent.id) it.copy(isActive = false) else it
                        }
                    }
                    val currentAreasUi = MapAreasUi.mapFromModelToUi(currentAreas)
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    val newState = currentState.filters.copy(userMapAreas = currentAreasUi)
                    applyFiltersUpdate(newState, intent.doNetworkCall)
                    updateState {
                        copy(
                            savedAreasDialogState = getUpdatedMapAreaDialogState(
                                currentAreas = currentAreasUi,
                                isVisible = intent.savedAreasDialogIsVisible
                            )
                        )
                    }
                }

                is FilterScreenAction.DeleteMapArea -> {
                    userMapAreaRepository.deleteSavedArea(intent.id)
                    val currentAreasUi = MapAreasUi.mapFromModelToUi(
                        filterRepository.areasInFilter(userMapAreaRepository)
                    )
                    var currentState = FilterScreenState.Initial
                    withState { currentState = this }
                    val newState = currentState.filters.copy(userMapAreas = currentAreasUi)
                    applyFiltersUpdate(newState, intent.doNetworkCall)
                    updateState {
                        copy(savedAreasDialogState = getUpdatedMapAreaDialogState(currentAreasUi))
                    }
                }

                FilterScreenAction.ShowSavedAreaListDialog -> {
                    val currentAreas = filterRepository.areasInFilter(userMapAreaRepository)
                    val currentAreasUi = MapAreasUi.mapFromModelToUi(currentAreas)
                    updateState {
                        copy(savedAreasDialogState = getUpdatedMapAreaDialogState(currentAreasUi))
                    }
                }

                FilterScreenAction.HideSavedAreaListDialog -> {
                    updateState {
                        copy(
                            savedAreasDialogState = savedAreasDialogState.copy(isVisible = false)
                        )
                    }
                }
            }
        }
    }

    private suspend fun PipeCtx.applyFiltersUpdate(
        newFilterState: FilterState,
        doNetworkCall: Boolean
    ) {
        var currentState = FilterScreenState.Initial
        withState { currentState = this }

        val currency =
            if (currentState.filters.adType == AdType.DAILY) Currency.BYR else Currency.USD
        val updatedFilter = newFilterState.copy(currency = currency)

        var shouldDeselectSaved = false
        currentState.savedFilters.find { it.selected }?.let { selected ->
            val selectedFilterData = filterRepository.getSavedFilterById(selected.id)?.filterData
            val currentFilterData = mapFilterStateToFilterModel(newFilterState)
            if (selectedFilterData != currentFilterData) {
                filterRepository.clearAllSavedFilterSelections()
                shouldDeselectSaved = true
            }
        }

        updateState {
            copy(
                filters = updatedFilter,
                savedFilters = if (shouldDeselectSaved) {
                    savedFilters.map { it.copy(selected = false) }
                } else {
                    savedFilters
                }
            )
        }

        val filterModel: CommonFilterRequestModel = mapFilterStateToFilterModel(updatedFilter)
        if (filterModel != filterRepository.lastFilter()) {
            filterRepository.updateFilter(filterModel, doNetworkCall)
        }
    }

    private fun getUpdatedMapAreaDialogState(
        currentAreas: List<MapAreasUi>,
        isVisible: Boolean = true
    ): SavedAreasDialogState {
        return SavedAreasDialogState(
            title = "Сохранённые области",
            isVisible = isVisible,
            savedAreas = currentAreas
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