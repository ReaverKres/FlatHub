package io.flatzen.viewmodel.list

import core.NetworkErrorInfo
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.list.FlatListEvents.*
import io.flatzen.viewmodel.sharedstates.DialogType
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.mergedrepo.MergedRepository
import repository.userpreferences.UserPreferencesRepository

sealed interface FlatListScreenAction : MviAction {
    class SearchFlats(val isLoadMore: Boolean, val isRefreshing: Boolean = false) :
        FlatListScreenAction

    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FlatListScreenAction

    //    class LoadFavorites(val favoritesFlats: LCE<List<AppFlat>>) : FlatListScreenAction
    class LoadDbFlats(val dbFlats: LCE<List<AppFlat>>) : FlatListScreenAction
    class IsAnyFilterAppliedCheck(val applied: Boolean) : FlatListScreenAction
    data object ScreenVisible : FlatListScreenAction

    // Analytics actions
    class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FlatListScreenAction

    // View toggle actions
    data object ToggleView : FlatListScreenAction
    class SetListView(val isListView: Boolean) : FlatListScreenAction
    object HideNetworkErrorDialog: FlatListScreenAction
}

sealed interface FlatListEvents : MviEvent {
    data class AllFlatsLoaded(
        val allFlats: LCE<List<AppFlat>>,
        val isLoadMore: Boolean,
        val isRefreshing: Boolean
    ) :
        FlatListEvents

    data class FlatUpdateInFavorite(val flat: LCE<AppFlat?>) : FlatListEvents
    data class DbFlatsLoaded(val dbFlats: LCE<List<AppFlat>>) : FlatListEvents
    class IsAnyFilterApplied(val applied: Boolean) : FlatListEvents
    class ViewToggled(val isListView: Boolean) : FlatListEvents
    class InfoDialogShowed(val dialogType: DialogType, val title: String, val description: String) :
        FlatListEvents

    class ErrorDialogShowed(
        val dialogType: DialogType,
        val title: String,
        val networkErrorInfo: List<NetworkErrorInfo>
    ) : FlatListEvents

    class ErrorDialogHidden() : FlatListEvents
}

class FlatSearchViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val connectionMonitor: ConnectionMonitor,
    private val analyticsManager: AnalyticsManager,
    private val configFieldsChecker: ConfigFieldsChecker
) : BaseMviViewModel<FlatListScreenAction, FlatListScreenState, FlatListEvents, MviEffect>() {

    private var noFlatsToLoadMore: Boolean = false
    private var isNetworkAvailable: Boolean = true

    override fun initialState(): FlatListScreenState = FlatListScreenState(
        isLoading = true,
        isRefreshing = false,
        isLoadingMore = false,
        flatList = emptyList(),
        noFlatsToLoadMore = false,
        isAnyFilterApplied = false
    )

    init {
        connectionMonitor.isNetworkAvailable.onEach {
            isNetworkAvailable = it
        }
            .flowOn(Dispatchers.Default)
            .launchIn(viewModelScope)

        filterRepository.cashedFilterFlow.onEach { newFilters ->
            noFlatsToLoadMore = false
            if (newFilters.doNetworkCall) {
                onIntent(FlatListScreenAction.SearchFlats(false))
            }
            onIntent(
                FlatListScreenAction.IsAnyFilterAppliedCheck(
                    newFilters.commonFilterRequestModel != CommonFilterRequestModel()
                )
            )
        }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)

        mergedRepository.getAllFlatsFromLocalDb()
            .asLCE()
            .onEach { event -> onIntent(FlatListScreenAction.LoadDbFlats(event)) }
            .flowOn(Dispatchers.IO)
            .launchIn(viewModelScope)
    }

    override suspend fun handleIntent(
        action: FlatListScreenAction,
        currentState: FlatListScreenState
    ): Flow<FlatListEvents> {
        return when (action) {
            is FlatListScreenAction.ScreenVisible -> {
                val lastNet = filterRepository.lastNetworkFilter
                // First, check and apply selected saved filter if exists
                val selectedFilter = filterRepository.getSelectedSavedFilter()
                if (selectedFilter != null && selectedFilter.filterData != lastNet) {
                    // Apply the selected saved filter
                    filterRepository.applySavedFilter(selectedFilter, true)
                } else {
                    // No saved filter selected, use default or cached filter
                    if (filterRepository.cashedFilterFlow.replayCache.isEmpty()) {
                        filterRepository.updateFilter(CommonFilterRequestModel(), true)
                    } else {
                        val last = filterRepository.lastFilter()
                        if (lastNet != last) {
                            filterRepository.updateFilter(last, true)
                        }
                    }
                }

                // Load user preferences for view type
                userPreferencesRepository.getUserPreferences().first()?.let { preferences ->
                    return flowOf(ViewToggled(preferences.isListView))
                }

                flowOf()
            }

            is FlatListScreenAction.IsAnyFilterAppliedCheck -> {
                flowOf(IsAnyFilterApplied(action.applied))
            }

            is FlatListScreenAction.SearchFlats -> {
                // Track search flats user action
                viewModelScope.launch(Dispatchers.IO) {
                    analyticsManager.registerEvent(
                        AnalyticsEvent(
                            eventName = "search_flats",
                            parameters = mapOf(
                                "is_load_more" to action.isLoadMore,
                                "is_refreshing" to action.isRefreshing,
                                "page" to filterRepository.currentAppPage
                            )
                        )
                    )
                }

                if (connectionMonitor.isNetworkAvailable.first().not()) {
                    return flowOf()
                }

                if (configFieldsChecker.checkBoolean(ConfigFields.FreeVersionAvailable)
                        ?.not() == true
                ) {
                    return flowOf(
                        InfoDialogShowed(
                            dialogType = DialogType.ForceUpdate,
                            title = "Доступна новая версия",
                            description = "Текущая версия неработоспособна, пожалуйста обновите приложение"
                        )
                    )
                }

                if (noFlatsToLoadMore && action.isRefreshing.not()) {
                    return flowOf()
                }
                if (action.isRefreshing || action.isLoadMore.not()) {
                    filterRepository.currentAppPage = 1
                    mergedRepository.clearCashedFlats()
                }
                if (action.isLoadMore) {
                    filterRepository.currentAppPage++
                }
                loadAllFlats(action.isLoadMore, action.isRefreshing)
            }

            is FlatListScreenAction.ClickOnFavorite -> {
                mergedRepository.saveFlatToFavorite(action.flatPlatform, action.adId).asLCE().map {
                    FlatUpdateInFavorite(it)
                }
            }

            is FlatListScreenAction.LoadDbFlats -> {
                flowOf(DbFlatsLoaded(action.dbFlats))
            }

            is FlatListScreenAction.TrackScreenView -> {
                // Handle screen view analytics tracking
                viewModelScope.launch(Dispatchers.IO) {
                    analyticsManager.registerEvent(
                        AnalyticsEvent(
                            eventName = AppMetrcica.Events.SCREEN_VIEW,
                            parameters = mapOf(
                                AppMetrcica.Parameters.SCREEN_NAME to action.screenName,
                                AppMetrcica.Parameters.TIMESTAMP to Clock.System.now()
                            ) + action.parameters
                        )
                    )
                }
                flowOf()
            }

            // Handle view toggle actions
            is FlatListScreenAction.ToggleView -> {
                val newIsListView = !currentState.isListView
                viewModelScope.launch(Dispatchers.IO) {
                    userPreferencesRepository.saveUserPreferences(newIsListView)
                }
                flowOf(ViewToggled(newIsListView))
            }

            is FlatListScreenAction.SetListView -> {
                viewModelScope.launch(Dispatchers.IO) {
                    userPreferencesRepository.saveUserPreferences(action.isListView)
                }
                flowOf(ViewToggled(action.isListView))
            }

            is FlatListScreenAction.HideNetworkErrorDialog -> {
                flowOf(ErrorDialogHidden())
            }
        }
    }

    private suspend fun loadAllFlats(
        isLoadMore: Boolean,
        isRefreshing: Boolean
    ): Flow<FlatListEvents> {
        return when {
            connectionMonitor.isNetworkAvailable.first().not() -> {
                mergedRepository.getAllFlatsFromLocalDb()
                    .asLCE()
                    .map { AllFlatsLoaded(it, isLoadMore, isRefreshing) }
            }

            else -> {
                mergedRepository.searchFlats()
                    .flatMapConcat { response ->
                        val flats = LCE.Content(response.flats) as LCE<List<AppFlat>>
                        val errors = response.errors

                        // Create both events
                        val contentEvent = AllFlatsLoaded(flats, isLoadMore, isRefreshing)
                        val dialogEvent = ErrorDialogShowed(
                            dialogType = DialogType.NetworkError,
                            title = "Произошла ошибка",
                            networkErrorInfo = errors
                        )

                        // Emit both events sequentially
                        flowOf(contentEvent, dialogEvent)
                    }
            }
        }
    }

    override suspend fun reduce(
        event: FlatListEvents,
        currentState: FlatListScreenState
    ): FlatListScreenState {
        return when (event) {
            is AllFlatsLoaded -> event.allFlats.process(
                onLoading = {
                    currentState.copy(
                        isLoading = true,
                        isLoadingMore = event.isLoadMore,
                        isRefreshing = event.isRefreshing
                    )
                },
                onError = { message, _ ->
                    currentState.copy(isRefreshing = false)
                },
                onSuccess = { flatsLoaded(it, currentState, event.isLoadMore, event.isRefreshing) }
            )

            is IsAnyFilterApplied -> {
                currentState.copy(isAnyFilterApplied = event.applied)
            }

            is FlatUpdateInFavorite -> event.flat.process(
                onLoading = { currentState },
                onError = { _, _ -> currentState },
                onSuccess = { updatedFlat ->
                    val updatedList = currentState.flatList.map { uiFlat ->
                        if (uiFlat.adId == updatedFlat?.adId && uiFlat.flatPlatform == updatedFlat.flatPlatform) {
                            uiFlat.copy(
                                savedInFavorite = updatedFlat.savedInFavorites
                            )
                        } else {
                            uiFlat
                        }
                    }
                    currentState.copy(flatList = updatedList)
                }
            )

            is DbFlatsLoaded -> event.dbFlats.process(
                onLoading = { currentState },
                onError = { _, _ ->
                    currentState
                },
                onSuccess = { dbFlats ->
                    if (isNetworkAvailable) {
                        currentState.copy(
                            flatList = currentState.flatList.map { flatOnScreen ->
                                val flatFromDb = dbFlats.find { it.adId == flatOnScreen.adId }
                                if (flatFromDb != null) {
                                    flatOnScreen.copy(
                                        savedInFavorite = flatFromDb.savedInFavorites,
                                        isViewed = flatFromDb.isViewed
                                    )
                                } else {
                                    flatOnScreen
                                }
                            })
                    } else {
                        flatsLoaded(
                            flats = dbFlats,
                            currentState = currentState,
                            isLoadMore = false,
                            isRefreshing = false
                        )
                    }
                }
            )

            is ViewToggled -> {
                currentState.copy(isListView = event.isListView)
            }

            is InfoDialogShowed -> {
                currentState.copy(
                    infoDialogState = InfoDialogState(
                        isVisible = true,
                        dialogType = event.dialogType,
                        title = event.title,
                        description = event.description
                    )
                )
            }

            is ErrorDialogShowed -> {
                currentState.copy(
                    errorDialogState = SearchErrorDialogState(
                        isVisible = true,
                        dialogType = DialogType.NetworkError,
                        title = event.title,
                        errorInfo = event.networkErrorInfo.map {
                            SearchErrorDialogState.ErrorInfo(
                                platform = it.platform,
                                errorMessages = it.errorMessages
                            )
                        }
                    )
                )
            }
            is ErrorDialogHidden -> {
                currentState.copy(
                    errorDialogState = null
                )
            }
        }
    }

    private fun flatsLoaded(
        flats: List<AppFlat>,
        currentState: FlatListScreenState,
        isLoadMore: Boolean = false,
        isRefreshing: Boolean
    ): FlatListScreenState {
        val uiFlatList = UiFlat.appFlatListToUiFlatList(flats)

        return if (currentState.flatList.isNotEmpty() && uiFlatList.isEmpty()) {
            noFlatsToLoadMore = true
            currentState.copy(
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                flatList = currentState.flatList,
                noFlatsToLoadMore = true
            )
        } else if (isLoadMore) {
            currentState.copy(
                noFlatsToLoadMore = false,
                isRefreshing = false,
                isLoading = false,
                isLoadingMore = false,
                flatList = currentState.flatList + uiFlatList
            )
        } else {
            currentState.copy(
                noFlatsToLoadMore = false,
                isRefreshing = false,
                isLoading = false,
                isLoadingMore = false,
                flatList = uiFlatList
            )
        }
    }
}