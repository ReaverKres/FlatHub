package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import entities.CommonFilterRequestModel
import entities.SavedFilter
import io.flatzen.analytics.Analytics
import io.flatzen.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.mappers.MetroStationsMapper
import io.flatzen.monetization.tier.UserTier
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import io.flatzen.viewmodel.UiDistrict
import io.flatzen.viewmodel.sharedstates.SavedAreasDialogState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import listing.core.ListingSourceRegistry
import listing.core.SourceCapabilities
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.updateStateImmediate
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.fillter.FilterRepository
import repository.fillter.UserMapAreaRepository
import repository.fillter.areasInFilter
import repository.fillter.lastFilter
import repository.osm.CityDistrictsCatalog
import repository.userpreferences.UserPreferencesRepository
import server_request.filterCurrency
import kotlin.time.Clock

private typealias PipeCtx = PipelineContext<FilterScreenState, FilterScreenAction, FilterEffect>

sealed interface FilterScreenAction : MVIIntent {
    data class UpdateFilter(val newFilterState: FilterState, val doNetworkCall: Boolean = false) :
        FilterScreenAction

    /** Text-field typing (price/area/rooms): UI state via [updateStateImmediate]. */
    data class UpdateFilterTyping(val newFilterState: FilterState) : FilterScreenAction

    data class UpdateSelectedCommercialPropertyType(
        val commercialPropertyType: CommercialPropertyType
    ) : FilterScreenAction

    data class UpdateMetroFilter(val metroStation: UiMetroStation) : FilterScreenAction
    data class UpdateMetroLine(val line: MetroLineState, val selected: Boolean) : FilterScreenAction
    data class UpdateWithAnyMetro(val enabled: Boolean) : FilterScreenAction
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

    data object NavigateBack : FilterScreenAction
    data object OpenLocation : FilterScreenAction
    data object OpenCity : FilterScreenAction
    data object OpenMetro : FilterScreenAction
    data object OpenDistricts : FilterScreenAction
    data class SelectCountry(val countryCode: CountryCode) : FilterScreenAction
    data class SelectCity(val cityCode: CityCode) : FilterScreenAction

    data object OpenPremiumForAddress : FilterScreenAction
    data class AddAddress(val address: String) : FilterScreenAction
}

@Immutable
data class FilterScreenState(
    val filters: FilterState,
    val savedFilters: List<SavedFilterState> = emptyList(),
    val saveDialogState: SaveDialogState = SaveDialogState(showNotification = true),
    val savedAreasDialogState: SavedAreasDialogState = SavedAreasDialogState(),
    val sourceCapabilities: SourceCapabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
        supportsCommercialPropertyTypes = false,
    ),
    /** Geo catalog: city has metro stations (not SourceCapabilities). */
    val hasMetroFilter: Boolean = false,
    /** Geo catalog: city has districts (after CityDistrictsCatalog.loadIfNeeded). */
    val hasDistrictsFilter: Boolean = false,
) : MVIState {
    companion object {
        val Initial = FilterScreenState(filters = FilterState())
    }
}

sealed interface FilterEffect : MVIAction {
    data class ShowToastEffect(val messageKey: LocalizationKeys) : FilterEffect
}

class FilterContainer(
    private val filterRepository: FilterRepository,
    private val userMapAreaRepository: UserMapAreaRepository,
    private val analytics: Analytics,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val userTierProvider: UserTierProvider,
    private val navigator: FlatHubNavigator,
    private val listingSourceRegistry: ListingSourceRegistry,
) : Container<FilterScreenState, FilterScreenAction, FilterEffect> {

    override val store = store<FilterScreenState, FilterScreenAction, FilterEffect>(
        initial = FilterScreenState.Initial
    ) {
        whileSubscribed {
            CityDistrictsCatalog.loadIfNeeded()
            updateState {
                val city = filters.location?.selectedCity
                copy(
                    hasMetroFilter = MetroStationsMapper.hasStations(city?.code),
                    hasDistrictsFilter = city?.displayName
                        ?.let { CityDistrictsCatalog.hasDistrictsForCity(it) } == true,
                )
            }
        }
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
                is FilterScreenAction.UpdateFilter ->
                    applyFiltersUpdate(intent.newFilterState, intent.doNetworkCall)

                is FilterScreenAction.UpdateFilterTyping ->
                    applyFiltersTypingUpdate(intent.newFilterState)

                is FilterScreenAction.UpdateSelectedCommercialPropertyType ->
                    onUpdateSelectedCommercialPropertyType(intent)

                is FilterScreenAction.UpdateAddressFilter -> onUpdateAddressFilter(intent)
                is FilterScreenAction.UpdateMetroFilter -> onUpdateMetroFilter(intent)
                is FilterScreenAction.UpdateMetroLine -> onUpdateMetroLine(intent)
                is FilterScreenAction.UpdateWithAnyMetro -> onUpdateWithAnyMetro(intent)
                is FilterScreenAction.UpdateDistrictFilter -> onUpdateDistrictFilter(intent)
                is FilterScreenAction.UpdateSortOption -> onUpdateSortOption(intent)
                FilterScreenAction.ClearAllFilters -> onClearAllFilters()
                FilterScreenAction.ClearMetroFilters -> onClearMetroFilters()
                FilterScreenAction.ClearLocationFilters -> onClearLocationFilters()
                FilterScreenAction.ShowSaveFilterDialog -> onShowSaveFilterDialog()
                FilterScreenAction.HideSaveFilterDialog -> onHideSaveFilterDialog()
                is FilterScreenAction.UpdateFilterName -> onUpdateFilterName(intent)
                is FilterScreenAction.NotificationEnable -> onNotificationEnable(intent)
                FilterScreenAction.SaveFilter -> onSaveFilter()
                FilterScreenAction.LoadSavedFilters -> onLoadSavedFilters()
                is FilterScreenAction.DeleteSavedFilter -> onDeleteSavedFilter(intent)
                is FilterScreenAction.ToggleSavedFilterSelection ->
                    onToggleSavedFilterSelection(intent)

                is FilterScreenAction.TrackScreenView -> onTrackScreenView(intent)
                is FilterScreenAction.ActivateMapArea -> onActivateMapArea(intent)
                is FilterScreenAction.DeleteMapArea -> onDeleteMapArea(intent)
                FilterScreenAction.ShowSavedAreaListDialog -> onShowSavedAreaListDialog()
                FilterScreenAction.HideSavedAreaListDialog -> onHideSavedAreaListDialog()
                FilterScreenAction.NavigateBack -> onNavigateBack()
                FilterScreenAction.OpenLocation -> navigator.navigate(FlatHubCommand.OpenLocation)
                FilterScreenAction.OpenCity -> navigator.navigate(FlatHubCommand.OpenCitySelect)
                FilterScreenAction.OpenMetro -> navigator.navigate(FlatHubCommand.OpenMetroSelect)
                FilterScreenAction.OpenDistricts ->
                    navigator.navigate(FlatHubCommand.OpenDistrictSelect)

                FilterScreenAction.OpenPremiumForAddress ->
                    navigator.navigate(FlatHubCommand.OpenPremium)

                is FilterScreenAction.AddAddress -> onAddAddress(intent)
                is FilterScreenAction.SelectCountry -> onSelectCountry(intent)
                is FilterScreenAction.SelectCity -> onSelectCity(intent)
            }
        }
    }

    private suspend fun PipeCtx.screenState(): FilterScreenState {
        var current = FilterScreenState.Initial
        withState { current = this }
        return current
    }

    private suspend fun PipeCtx.onUpdateSelectedCommercialPropertyType(
        intent: FilterScreenAction.UpdateSelectedCommercialPropertyType,
    ) {
        val current = screenState()
        val updatedCommercialTypes = current.filters.commercial.commercialPropertyType?.map {
            it.copy(selected = it.commercialPropertyType == intent.commercialPropertyType)
        }
        applyFiltersUpdate(
            current.filters.copy(
                commercial = current.filters.commercial.copy(
                    commercialPropertyType = updatedCommercialTypes,
                ),
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onUpdateAddressFilter(
        intent: FilterScreenAction.UpdateAddressFilter,
    ) {
        applyFiltersUpdate(
            screenState().filters.copy(address = intent.addressUiState),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onUpdateMetroFilter(intent: FilterScreenAction.UpdateMetroFilter) {
        if (intent.metroStation.selected && requirePremiumForLocationFilter()) return
        val current = screenState()
        applyFiltersUpdate(
            current.filters.copy(
                withAnyMetro = false,
                metroStationsState = current.filters.metroStationsState.map {
                    if (it.name == intent.metroStation.name) intent.metroStation else it
                },
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onUpdateMetroLine(intent: FilterScreenAction.UpdateMetroLine) {
        if (intent.selected && requirePremiumForLocationFilter()) return
        val current = screenState()
        applyFiltersUpdate(
            current.filters.copy(
                withAnyMetro = false,
                metroStationsState = current.filters.metroStationsState.map { station ->
                    if (station.line == intent.line) {
                        station.copy(selected = intent.selected)
                    } else {
                        station
                    }
                },
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onUpdateWithAnyMetro(
        intent: FilterScreenAction.UpdateWithAnyMetro,
    ) {
        if (intent.enabled && requirePremiumForLocationFilter()) return
        val current = screenState()
        applyFiltersUpdate(
            current.filters.copy(
                withAnyMetro = intent.enabled,
                metroStationsState = if (intent.enabled) {
                    current.filters.metroStationsState.map { it.copy(selected = false) }
                } else {
                    current.filters.metroStationsState
                },
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onUpdateDistrictFilter(
        intent: FilterScreenAction.UpdateDistrictFilter,
    ) {
        if (intent.district.isChecked && requirePremiumForLocationFilter()) return
        val current = screenState()
        val allDistricts = if (current.filters.districtsArea.isNullOrEmpty()) {
            intent.allDistricts
        } else {
            current.filters.districtsArea
        }
        applyFiltersUpdate(
            current.filters.copy(
                districtsArea = allDistricts?.map {
                    if (it.nameLocal == intent.district.nameLocal) intent.district else it
                },
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onUpdateSortOption(intent: FilterScreenAction.UpdateSortOption) {
        applyFiltersUpdate(
            screenState().filters.copy(sortOption = intent.sortOption),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onClearAllFilters() {
        val location = screenState().filters.location ?: LocationUiFilter.networkDefault()
        applyFiltersUpdate(
            FilterState(
                location = location,
                metroStationsState = MetroStationsMapper.stationsForCity(
                    location.selectedCity.code,
                ),
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onClearMetroFilters() {
        val current = screenState()
        applyFiltersUpdate(
            current.filters.copy(
                withAnyMetro = false,
                metroStationsState = MetroStationsMapper.stationsForCity(
                    current.filters.location?.selectedCity?.code,
                ),
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onClearLocationFilters() {
        val current = screenState()
        applyFiltersUpdate(
            current.filters.copy(
                withAnyMetro = false,
                metroStationsState = MetroStationsMapper.allStationsOrderedForUi(),
                location = null,
                address = null,
                userMapAreas = current.filters.userMapAreas?.map { it.copy(isActive = false) },
                districtsArea = current.filters.districtsArea?.map { it.copy(isChecked = false) },
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onShowSaveFilterDialog() {
        updateState { copy(saveDialogState = saveDialogState.copy(isVisible = true)) }
    }

    private suspend fun PipeCtx.onHideSaveFilterDialog() {
        updateState {
            copy(
                saveDialogState = saveDialogState.copy(
                    isVisible = false,
                    filterName = "",
                    isNameValid = true,
                    errorMessage = null,
                ),
            )
        }
    }

    private fun PipeCtx.onUpdateFilterName(intent: FilterScreenAction.UpdateFilterName) {
        val isNameValid = intent.name.length <= 25 && intent.name.isNotBlank()
        val errorMessage = when {
            intent.name.isBlank() -> LocalizationKeys.FILTER_NAME_EMPTY_ERROR
            intent.name.length > 25 -> LocalizationKeys.FILTER_NAME_LENGTH_ERROR
            else -> null
        }
        updateStateImmediate {
            copy(
                saveDialogState = saveDialogState.copy(
                    filterName = intent.name,
                    isNameValid = isNameValid,
                    errorMessage = errorMessage,
                ),
            )
        }
    }

    private suspend fun PipeCtx.onNotificationEnable(
        intent: FilterScreenAction.NotificationEnable,
    ) {
        val available = userPreferencesRepository.getUserPreferences()
            .firstOrNull()?.deviceDocumentResponse?.referralStats?.isNotificationAvailable == true
        if (!available) {
            navigator.navigate(FlatHubCommand.OpenReferral)
            return
        }
        updateState {
            copy(saveDialogState = saveDialogState.copy(isNotificationEnabled = intent.enabled))
        }
    }

    private suspend fun PipeCtx.onSaveFilter() {
        val current = screenState()
        filterRepository.saveFilter(
            current.saveDialogState.filterName,
            mapFilterStateToFilterModel(current.filters),
        )
        updateState {
            copy(
                saveDialogState = saveDialogState.copy(
                    isVisible = false,
                    filterName = "",
                    isNameValid = true,
                    errorMessage = null,
                ),
            )
        }
    }

    private suspend fun PipeCtx.onLoadSavedFilters() {
        val savedFilters = filterRepository.getAllSavedFilters().first()
        updateState {
            copy(savedFilters = savedFilters.map(::mapSavedFilterToSavedFilterState))
        }
    }

    private suspend fun PipeCtx.onDeleteSavedFilter(intent: FilterScreenAction.DeleteSavedFilter) {
        filterRepository.deleteSavedFilter(intent.id)
        updateState { copy(savedFilters = savedFilters.filter { it.id != intent.id }) }
    }

    private suspend fun PipeCtx.onToggleSavedFilterSelection(
        intent: FilterScreenAction.ToggleSavedFilterSelection,
    ) {
        var selectedFilterId: Long? = null
        var showPaidLocationToast = false
        withState {
            val currentlySelected = savedFilters.find { it.selected }
            selectedFilterId = if (currentlySelected?.id == intent.filterId) {
                filterRepository.clearAllSavedFilterSelections()
                null
            } else {
                filterRepository.getSavedFilterById(intent.filterId)?.let { saved ->
                    val filterState = mapFilterModelToFilterState(saved.filterData)
                    if (userTierProvider.currentTier() == UserTier.FREE &&
                        filterState.hasPaidLocationFilters()
                    ) {
                        val stripped = filterState.stripPaidLocationFilters()
                        filterRepository.updateFilter(
                            mapFilterStateToFilterModel(stripped),
                            false,
                        )
                        filterRepository.updateSavedFilterSelection(intent.filterId)
                        showPaidLocationToast = true
                    } else {
                        filterRepository.applySavedFilter(saved, false)
                    }
                }
                intent.filterId
            }
        }
        val selectedId = selectedFilterId
        updateState {
            copy(
                savedFilters = savedFilters.map { filter ->
                    filter.copy(selected = filter.id == selectedId)
                },
            )
        }
        if (showPaidLocationToast) {
            action(FilterEffect.ShowToastEffect(LocalizationKeys.PREMIUM_LOCATION_TOAST))
        }
    }

    private fun PipeCtx.onTrackScreenView(intent: FilterScreenAction.TrackScreenView) {
        launch {
            try {
                analytics.track(
                    AnalyticsEvent(
                        eventName = AppMetrcica.Events.SCREEN_VIEW,
                        parameters = mapOf(
                            AppMetrcica.Parameters.SCREEN_NAME to intent.screenName,
                            AppMetrcica.Parameters.TIMESTAMP to Clock.System.now(),
                        ) + intent.parameters,
                    ),
                )
            } catch (_: Exception) {
                // Intentionally ignored for analytics failures.
            }
        }
    }

    private suspend fun PipeCtx.onActivateMapArea(intent: FilterScreenAction.ActivateMapArea) {
        if (intent.checked && userTierProvider.currentTier() == UserTier.FREE) {
            navigator.navigate(FlatHubCommand.OpenPremium)
            return
        }
        var currentAreas = filterRepository.areasInFilter(userMapAreaRepository)
        currentAreas = currentAreas.map {
            if (it.pathId == intent.id) it.copy(isActive = intent.checked) else it
        }
        val currentAreasUi = MapAreasUi.mapFromModelToUi(currentAreas)
        applyFiltersUpdate(
            screenState().filters.copy(userMapAreas = currentAreasUi),
            intent.doNetworkCall,
        )
        updateState {
            copy(
                savedAreasDialogState = getUpdatedMapAreaDialogState(
                    currentAreas = currentAreasUi,
                    isVisible = intent.savedAreasDialogIsVisible,
                ),
            )
        }
    }

    private suspend fun PipeCtx.onDeleteMapArea(intent: FilterScreenAction.DeleteMapArea) {
        userMapAreaRepository.deleteSavedArea(intent.id)
        val currentAreasUi = MapAreasUi.mapFromModelToUi(
            filterRepository.areasInFilter(userMapAreaRepository),
        )
        applyFiltersUpdate(
            screenState().filters.copy(userMapAreas = currentAreasUi),
            intent.doNetworkCall,
        )
        updateState {
            copy(savedAreasDialogState = getUpdatedMapAreaDialogState(currentAreasUi))
        }
    }

    private suspend fun PipeCtx.onShowSavedAreaListDialog() {
        val currentAreasUi = MapAreasUi.mapFromModelToUi(
            filterRepository.areasInFilter(userMapAreaRepository),
        )
        updateState {
            copy(savedAreasDialogState = getUpdatedMapAreaDialogState(currentAreasUi))
        }
    }

    private suspend fun PipeCtx.onHideSavedAreaListDialog() {
        updateState {
            copy(savedAreasDialogState = savedAreasDialogState.copy(isVisible = false))
        }
    }

    private suspend fun PipeCtx.onNavigateBack() {
        forceFilterNetworkReload()
        navigator.navigate(FlatHubCommand.NavigateBack)
    }

    private suspend fun PipeCtx.onAddAddress(intent: FilterScreenAction.AddAddress) {
        if (userTierProvider.currentTier() != UserTier.PREMIUM) {
            navigator.navigate(FlatHubCommand.OpenPremium)
            return
        }
        if (intent.address.isBlank()) return
        val current = screenState()
        val addresses = current.filters.address?.toSet().orEmpty() +
                AddressUiState(intent.address.trim())
        applyFiltersUpdate(
            current.filters.copy(address = addresses),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onSelectCountry(intent: FilterScreenAction.SelectCountry) {
        val current = screenState()
        if (current.filters.location?.selectedCountry?.code == intent.countryCode) return
        val cities = LocationUiMapper.cities(intent.countryCode)
        val defaultCity = LocationUiMapper.defaultCity(intent.countryCode)
        applyFiltersUpdate(
            current.filters.copy(
                location = LocationUiFilter(
                    selectedCountry = UiCountry(
                        code = intent.countryCode,
                        name = LocationUiMapper.countryDisplayName(intent.countryCode),
                    ),
                    selectedCity = defaultCity,
                    availableCities = cities,
                ),
                districtsArea = null,
                metroStationsState = MetroStationsMapper.stationsForCity(defaultCity.code),
                withAnyMetro = false,
                priceFull = null,
                pricePerSquare = null,
            ),
            doNetworkCall = false,
        )
    }

    private suspend fun PipeCtx.onSelectCity(intent: FilterScreenAction.SelectCity) {
        val current = screenState()
        val cityChanged = current.filters.location?.selectedCity?.code != intent.cityCode
        val country = LocationUiMapper.countryForCity(intent.cityCode)
        applyFiltersUpdate(
            current.filters.copy(
                location = current.filters.location?.copy(
                    selectedCountry = UiCountry(
                        code = country,
                        name = LocationUiMapper.countryDisplayName(country),
                    ),
                    selectedCity = LocationUiMapper.findSelectedCity(intent.cityCode),
                    availableCities = LocationUiMapper.cities(country),
                ),
                districtsArea = if (cityChanged) null else current.filters.districtsArea,
                metroStationsState = if (cityChanged) {
                    MetroStationsMapper.stationsForCity(intent.cityCode)
                } else {
                    current.filters.metroStationsState
                },
                withAnyMetro = if (cityChanged) false else current.filters.withAnyMetro,
            ),
            doNetworkCall = false,
        )
        navigator.navigate(FlatHubCommand.NavigateBack)
    }

    /** @return true if navigation to Premium was triggered (caller should skip applying the filter). */
    private fun requirePremiumForLocationFilter(): Boolean {
        if (userTierProvider.currentTier() == UserTier.PREMIUM) return false
        navigator.navigate(FlatHubCommand.OpenPremium)
        return true
    }

    private suspend fun PipeCtx.forceFilterNetworkReload() {
        val filterModel = mapFilterStateToFilterModel(screenState().filters)
        filterRepository.updateFilter(filterModel, doNetworkCall = true)
    }

    private suspend fun PipeCtx.applyFiltersUpdate(
        newFilterState: FilterState,
        doNetworkCall: Boolean,
    ) {
        CityDistrictsCatalog.loadIfNeeded()
        val currentState = screenState()
        val country = newFilterState.location?.selectedCountry?.code
            ?: currentState.filters.location?.selectedCountry?.code
            ?: CountryCode.BY
        val caps = listingSourceRegistry.capabilitiesFor(country)
        val sanitized = newFilterState.sanitizeForCountryCapabilities(country, caps)
        val currency = country.filterCurrency(sanitized.adType)
        val updatedFilter = sanitized.copy(currency = currency)

        var shouldDeselectSaved = false
        currentState.savedFilters.find { it.selected }?.let { selected ->
            val selectedFilterData = filterRepository.getSavedFilterById(selected.id)?.filterData
            val currentFilterData = mapFilterStateToFilterModel(updatedFilter)
            if (selectedFilterData != currentFilterData) {
                filterRepository.clearAllSavedFilterSelections()
                shouldDeselectSaved = true
            }
        }

        updateState {
            copy(
                filters = updatedFilter,
                sourceCapabilities = caps,
                hasMetroFilter = MetroStationsMapper.hasStations(
                    updatedFilter.location?.selectedCity?.code,
                ),
                hasDistrictsFilter = updatedFilter.location?.selectedCity?.displayName
                    ?.let { CityDistrictsCatalog.hasDistrictsForCity(it) } == true,
                savedFilters = if (shouldDeselectSaved) {
                    savedFilters.map { it.copy(selected = false) }
                } else {
                    savedFilters
                },
            )
        }

        val filterModel: CommonFilterRequestModel = mapFilterStateToFilterModel(updatedFilter)
        if (filterModel != filterRepository.lastFilter()) {
            filterRepository.updateFilter(filterModel, doNetworkCall)
        }
    }

    /**
     * Price/area/rooms TextField typing — same persistence as [applyFiltersUpdate],
     * but UI state via [updateStateImmediate] to avoid Compose TextField jank.
     */
    private suspend fun PipeCtx.applyFiltersTypingUpdate(newFilterState: FilterState) {
        val currentState = screenState()
        val country = newFilterState.location?.selectedCountry?.code
            ?: currentState.filters.location?.selectedCountry?.code
            ?: CountryCode.BY
        val currency = country.filterCurrency(newFilterState.adType)
        val updatedFilter = newFilterState.copy(currency = currency)

        var shouldDeselectSaved = false
        currentState.savedFilters.find { it.selected }?.let { selected ->
            val selectedFilterData = filterRepository.getSavedFilterById(selected.id)?.filterData
            val currentFilterData = mapFilterStateToFilterModel(updatedFilter)
            if (selectedFilterData != currentFilterData) {
                filterRepository.clearAllSavedFilterSelections()
                shouldDeselectSaved = true
            }
        }

        updateStateImmediate {
            copy(
                filters = updatedFilter,
                savedFilters = if (shouldDeselectSaved) {
                    savedFilters.map { it.copy(selected = false) }
                } else {
                    savedFilters
                },
            )
        }

        val filterModel = mapFilterStateToFilterModel(updatedFilter)
        if (filterModel != filterRepository.lastFilter()) {
            filterRepository.updateFilter(filterModel, doNetworkCall = false)
        }
    }

    private fun getUpdatedMapAreaDialogState(
        currentAreas: List<MapAreasUi>,
        isVisible: Boolean = true,
    ): SavedAreasDialogState {
        return SavedAreasDialogState(
            title = LocalizationKeys.MAP_SAVED_AREAS,
            isVisible = isVisible,
            savedAreas = currentAreas,
        )
    }

    private fun mapSavedFilterToSavedFilterState(savedFilter: SavedFilter): SavedFilterState {
        return SavedFilterState(
            id = savedFilter.id,
            name = savedFilter.name,
            selected = savedFilter.selected,
            createdAt = savedFilter.createdAt,
        )
    }
}
