package io.flatzen.viewmodel.list

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
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.mergedrepo.MergedRepository

sealed interface FlatListScreenAction : MviAction {
    class SearchFlats(val isLoadMore: Boolean, val isRefreshing: Boolean = false) :
        FlatListScreenAction

    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FlatListScreenAction
    class LoadFavorites(val favoritesFlats: LCE<List<AppFlat>>) : FlatListScreenAction
    class IsAnyFilterAppliedCheck(val applied: Boolean) : FlatListScreenAction
    data object ScreenVisible : FlatListScreenAction

    // Analytics actions
    class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FlatListScreenAction
}

sealed interface FlatListEvents : MviEvent {
    data class AllFlatsLoaded(
        val allFlats: LCE<List<AppFlat>>,
        val isLoadMore: Boolean,
        val isRefreshing: Boolean
    ) :
        FlatListEvents

    data class FlatUpdateInFavorite(val flat: LCE<AppFlat?>) : FlatListEvents
    data class FavoritesLoaded(val favoriteFlats: LCE<List<AppFlat>>) : FlatListEvents
    class IsAnyFilterApplied(val applied: Boolean) : FlatListEvents
}

class FlatSearchViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val connectionMonitor: ConnectionMonitor,
    private val analyticsManager: AnalyticsManager
) : BaseMviViewModel<FlatListScreenAction, FlatListScreenState, FlatListEvents, MviEffect>() {

    private var noFlatsToLoadMore: Boolean = false

    override fun initialState(): FlatListScreenState = FlatListScreenState(
        isLoading = true,
        isRefreshing = false,
        isLoadingMore = false,
        flatList = emptyList(),
        noFlatsToLoadMore = false,
        isAnyFilterApplied = false
    )

    init {
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
            .launchIn(viewModelScope)

        mergedRepository.getFavoritesFromLocalDb()
            .asLCE()
            .onEach { event -> onIntent(FlatListScreenAction.LoadFavorites(event)) }
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
                flowOf()
            }

            is FlatListScreenAction.IsAnyFilterAppliedCheck -> {
                flowOf(FlatListEvents.IsAnyFilterApplied(action.applied))
            }

            is FlatListScreenAction.SearchFlats -> {
                // Track search flats user action
                viewModelScope.launch {
                        analyticsManager.registerEvent(
                            AnalyticsEvent(
                                eventName = "search_flats",
                                parameters = mapOf(
                                    "is_load_more" to action.isLoadMore,
                                    "is_refreshing" to action.isRefreshing,
                                    "page" to filterRepository.currentAppPage,
                                    "has_network" to connectionMonitor.isNetworkAvailable.first()
                                )
                            )
                        )
                }

                if (connectionMonitor.isNetworkAvailable.first().not()) {
                    return flowOf()
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
                    FlatListEvents.FlatUpdateInFavorite(it)
                }
            }

            is FlatListScreenAction.LoadFavorites -> {
                flowOf(FlatListEvents.FavoritesLoaded(action.favoritesFlats))
            }

            is FlatListScreenAction.TrackScreenView -> {
                // Handle screen view analytics tracking
                viewModelScope.launch {
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
                    .map { FlatListEvents.AllFlatsLoaded(it, isLoadMore, isRefreshing) }
            }

            else -> {
                mergedRepository.searchFlats()
                    .asLCE()
                    .map { FlatListEvents.AllFlatsLoaded(it, isLoadMore, isRefreshing) }
            }
        }
    }

    override suspend fun reduce(
        event: FlatListEvents,
        currentState: FlatListScreenState
    ): FlatListScreenState {
        return when (event) {
            is FlatListEvents.AllFlatsLoaded -> event.allFlats.process(
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

            is FlatListEvents.IsAnyFilterApplied -> {
                currentState.copy(isAnyFilterApplied = event.applied)
            }

            is FlatListEvents.FlatUpdateInFavorite -> event.flat.process(
                onLoading = { currentState },
                onError = { _, _ -> currentState },
                onSuccess = { updatedFlat ->
                    val updatedList = currentState.flatList.map { uiFlat ->
                        if (uiFlat.adId == updatedFlat?.adId && uiFlat.flatPlatform == updatedFlat.flatPlatform) {
                            uiFlat.copy(
                                savedInFavorite = updatedFlat.flatSavedInFavorites
                            )
                        } else {
                            uiFlat
                        }
                    }
                    currentState.copy(flatList = updatedList)
                }
            )

            is FlatListEvents.FavoritesLoaded -> event.favoriteFlats.process(
                onLoading = { currentState },
                onError = { _, _ ->
                    currentState
                },
                onSuccess = { favFlats ->
                    val favIds = favFlats.map { it.adId }.toHashSet()
                    currentState.copy(
                        flatList = currentState.flatList.map { flatOnScreen ->
                            if (flatOnScreen.adId in favIds) {
                                flatOnScreen.copy(savedInFavorite = true)
                            } else {
                                flatOnScreen.copy(savedInFavorite = false)
                            }
                        })
                }
            )
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