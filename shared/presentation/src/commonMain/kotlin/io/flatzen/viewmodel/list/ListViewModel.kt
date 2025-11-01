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
import io.flatzen.viewmodel.list.FlatListEvents.AllFlatsLoaded
import io.flatzen.viewmodel.list.FlatListEvents.DbFlatsLoaded
import io.flatzen.viewmodel.list.FlatListEvents.ErrorDialogHidden
import io.flatzen.viewmodel.list.FlatListEvents.ErrorDialogShowed
import io.flatzen.viewmodel.list.FlatListEvents.FlatUpdateInFavorite
import io.flatzen.viewmodel.list.FlatListEvents.InfoDialogShowed
import io.flatzen.viewmodel.list.FlatListEvents.IsAnyFilterApplied
import io.flatzen.viewmodel.list.FlatListEvents.ScrollToTopEffect
import io.flatzen.viewmodel.list.FlatListEvents.ViewToggled
import io.flatzen.viewmodel.sharedstates.DialogType
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
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
    data object ScrollToTop : FlatListScreenAction
    class SearchFlats(
        val isLoadMore: Boolean,
        val isLoadMoreForce: Boolean = false,
        val isRefreshing: Boolean = false
    ) :
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
    object HideNetworkErrorDialog : FlatListScreenAction
}

sealed interface FlatListEvents : MviEvent {
    data object ScrollToTopEffect : FlatListEvents
    data class AllFlatsLoaded(
        val allFlats: LCE<List<AppFlat>>,
        val isLoadMore: Boolean,
        val isRefreshing: Boolean
    ) :
        FlatListEvents

    data class FlatUpdateInFavorite(
        val flat: LCE<AppFlat?>,
        val flatPlatform: FlatPlatform,
        val adId: Long
    ) : FlatListEvents

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

sealed interface FlatListEffect : MviEffect {
    data object ScrollToTop : FlatListEffect
}

class FlatSearchViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val connectionMonitor: ConnectionMonitor,
    private val analyticsManager: AnalyticsManager,
    private val configFieldsChecker: ConfigFieldsChecker
) : BaseMviViewModel<FlatListScreenAction, FlatListScreenState, FlatListEvents, FlatListEffect>() {

    private var noFlatsToLoadMore: Boolean = false
    private var isNetworkAvailable: Boolean = true

    override fun initialState(): FlatListScreenState = FlatListScreenState(
        isLoading = true,
        isRefreshing = false,
        isLoadingMore = false,
        flatList = emptyList(),
        noFlatsToLoadMore = false,
        isAnyFilterApplied = false,
        currentSearchPage = 1
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
            is FlatListScreenAction.ScrollToTop -> {
                flowOf(ScrollToTopEffect)
            }

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

                if (action.isLoadMoreForce.not() && noFlatsToLoadMore && action.isRefreshing.not()) {
                    return flowOf()
                }
                if (action.isRefreshing || action.isLoadMore.not()) {
                    filterRepository.currentAppPage = 1
                    mergedRepository.clearCashedFlats()
                }
                if (action.isLoadMore) {
                    filterRepository.currentAppPage++
                }
                if (action.isLoadMore.not()) {
                    onIntent(FlatListScreenAction.ScrollToTop)
                }
                loadAllFlats(action.isLoadMore, action.isRefreshing)
            }

            is FlatListScreenAction.ClickOnFavorite -> {
                mergedRepository.saveFlatToFavorite(action.flatPlatform, action.adId).asLCE().map {
                    FlatUpdateInFavorite(it, action.flatPlatform, action.adId)
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
                    .asLCE()  // Convert Flow<MergedFlatResponse> to LCE<MergedFlatResponse>
                    .flatMapConcat { lceResult ->
                        when (lceResult) {
                            is LCE.Content -> {
                                val response = lceResult.value
                                val flatsLce = LCE.Content(response.flats) as LCE<List<AppFlat>>
                                val contentEvent =
                                    AllFlatsLoaded(flatsLce, isLoadMore, isRefreshing)

                                // Check if there are errors to display
                                if (response.errors.isNotEmpty() &&
                                    response.errors.any { it.errorMessages.isNotEmpty() }
                                ) {
                                    val dialogEvent = ErrorDialogShowed(
                                        dialogType = DialogType.NetworkError,
                                        title = "Произошла ошибка",
                                        networkErrorInfo = response.errors
                                    )
                                    flowOf(contentEvent, dialogEvent)
                                } else {
                                    flowOf(contentEvent)
                                }
                            }

                            is LCE.Error -> {
                                val errorLce =
                                    LCE.Error<List<AppFlat>>(lceResult.message, lceResult.throwable)
                                flowOf(AllFlatsLoaded(errorLce, isLoadMore, isRefreshing))
                            }

                            is LCE.Loading -> {
                                val loadingLce = LCE.Loading<List<AppFlat>>()
                                flowOf(AllFlatsLoaded(loadingLce, isLoadMore, isRefreshing))
                            }
                        }
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

            is FlatUpdateInFavorite -> {
                val detailFlatAccessible = currentState.flatList.find {
                    it.adId == event.adId && it.flatPlatform == event.flatPlatform
                }?.isDetailDataLoaded == true
                event.flat.process(
                    onLoading = {
                        if (detailFlatAccessible) {
                            currentState
                        } else {
                            val updatedList = currentState.flatList.map { uiFlat ->
                                if (uiFlat.adId == event.adId && event.flatPlatform == uiFlat.flatPlatform) {
                                    uiFlat.copy(
                                        saveInFavoriteInProgress = true
                                    )
                                } else {
                                    uiFlat
                                }
                            }
                            currentState.copy(flatList = updatedList)
                        }
                    },
                    onError = { _, _ -> currentState },
                    onSuccess = { updatedFlat ->
                        val updatedList = currentState.flatList.map { uiFlat ->
                            if (uiFlat.adId == updatedFlat?.adId && uiFlat.flatPlatform == updatedFlat.flatPlatform) {
                                uiFlat.copy(
                                    savedInFavorite = updatedFlat.savedInFavorites,
                                    saveInFavoriteInProgress = false
                                )
                            } else {
                                uiFlat
                            }
                        }
                        currentState.copy(flatList = updatedList)
                    }
                )
            }

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
                                        isViewed = flatFromDb.isViewed,
                                        imageUrls = flatFromDb.imageUrls ?: flatOnScreen.imageUrls
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
                val hasError = event.networkErrorInfo.flatMap { it.errorMessages }.isNotEmpty()
                currentState.copy(
                    errorDialogState = SearchErrorDialogState(
                        isVisible = hasError,
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

            is ScrollToTopEffect -> currentState
        }
    }

    private fun flatsLoaded(
        flats: List<AppFlat>,
        currentState: FlatListScreenState,
        isLoadMore: Boolean = false,
        isRefreshing: Boolean
    ): FlatListScreenState {
        val uiFlatList = UiFlat.appFlatListToUiFlatList(flats)

        return when {
            isLoadMore -> {
                if (uiFlatList.isEmpty()) noFlatsToLoadMore = true
                currentState.copy(
                    noFlatsToLoadMore = noFlatsToLoadMore,
                    isRefreshing = false,
                    isLoading = false,
                    isLoadingMore = false,
                    flatList = currentState.flatList + uiFlatList,
                    currentSearchPage = filterRepository.currentAppPage
                )
            }

            uiFlatList.isEmpty() -> {
                noFlatsToLoadMore = true
                currentState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    flatList = uiFlatList,
                    noFlatsToLoadMore = noFlatsToLoadMore,
                    currentSearchPage = filterRepository.currentAppPage
                )
            }

            else -> {
                currentState.copy(
                    noFlatsToLoadMore = false,
                    isRefreshing = false,
                    isLoading = false,
                    isLoadingMore = false,
                    flatList = uiFlatList,
                    currentSearchPage = filterRepository.currentAppPage
                )
            }
        }
    }

    override suspend fun onEvent(event: FlatListEvents): FlatListEffect? {
        return when (event) {
            ScrollToTopEffect -> FlatListEffect.ScrollToTop
            else -> super.onEvent(event)
        }
    }
}