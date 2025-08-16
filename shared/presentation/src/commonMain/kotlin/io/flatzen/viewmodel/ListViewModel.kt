package io.flatzen.viewmodel

import entities.AppFlat
import androidx.compose.runtime.Immutable
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.network.ConnectionMonitor
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.mergedrepo.MergedRepository

sealed interface FlatListScreenAction : MviAction {
    class SearchFlats(val isLoadMore: Boolean, val isRefreshing: Boolean = false) :
        FlatListScreenAction

    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FlatListScreenAction
    class LoadFavorites(val favoritesFlats: LCE<List<AppFlat>>) : FlatListScreenAction
    data object ScreenVisible : FlatListScreenAction
}

@Immutable
data class FlatListScreenState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
    val noFlatsToLoadMore: Boolean,
    val flatList: List<UiFlat>
) : MviState

@Immutable
data class UiFlat(
    val adId: Long,
    val flatPlatform: FlatPlatform,
    val savedInFavorite: Boolean,
    val imageUrls: List<String>,
    val priceUsd: UiPrice,
    val priceByn: UiPrice,
    val numberOfRooms: Int?,
    val publishedAt: String?,
    val metroStation: String,
    val address: String
)

@Immutable
data class UiPrice(
    val price: Double?,
    val currency: String
)

sealed interface FlatListEvents : MviEvent {
    data class AllFlatsLoaded(
        val allFlats: LCE<List<AppFlat>>,
        val isLoadMore: Boolean,
        val isRefreshing: Boolean
    ) :
        FlatListEvents

    data class FlatUpdateInFavorite(val flat: LCE<AppFlat?>) : FlatListEvents
    data class FavoritesLoaded(val favoriteFlats: LCE<List<AppFlat>>) : FlatListEvents
}

class FlatSearchViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val connectionMonitor: ConnectionMonitor
) : BaseMviViewModel<FlatListScreenAction, FlatListScreenState, FlatListEvents, MviEffect>() {

    private var noFlatsToLoadMore: Boolean = false

    override fun initialState(): FlatListScreenState = FlatListScreenState(
        isLoading = true,
        isRefreshing = false,
        isLoadingMore = false,
        flatList = emptyList(),
        noFlatsToLoadMore = false
    )

    init {
        viewModelScope.launch {
            if (filterRepository.cashedFilterFlow.replayCache.isEmpty()) {
                filterRepository.updateFilter(CommonFilterRequestModel(), true)
            } else {
                val last = filterRepository.lastFilter()
                val lastNet = filterRepository.lastNetworkFilter
                if (lastNet != last) {
                    filterRepository.updateFilter(last, true)
                }
            }
        }

        filterRepository.cashedFilterFlow.onEach { newFilters ->
                noFlatsToLoadMore = false
                if (newFilters.doNetworkCall) {
                    onIntent(FlatListScreenAction.SearchFlats(false))
                }
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
                val last = filterRepository.lastFilter()
                val lastNet = filterRepository.lastNetworkFilter
                if (lastNet != last) {
                    filterRepository.updateFilter(last, true)
                }
                flowOf()
            }
            is FlatListScreenAction.SearchFlats -> {
                if (connectionMonitor.isNetworkAvailable.first().not()) {
                    return flowOf()
                }
                if (noFlatsToLoadMore && action.isRefreshing.not()) {
                    return flowOf()
                }
                if (action.isLoadMore) {
                    filterRepository.currentAppPage++
                } else {
                    filterRepository.currentAppPage = 1
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
                onLoading = { currentState.copy(isLoading = true) },
                onError = { _, _ -> currentState.copy(isLoading = false) },
                onSuccess = { favFlats ->
                    val favIds = favFlats.map { it.adId }.toHashSet()
                    currentState.copy(
                        isLoading = false,
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
        val uiFlatList = appFlatListToUiFlatList(flats)

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

    private fun appFlatListToUiFlatList(appFlatList: List<AppFlat>): List<UiFlat> {
        return appFlatList.map {
            UiFlat(
                adId = it.adId,
                flatPlatform = it.flatPlatform,
                imageUrls = it.imageUrls ?: listOf(),
                savedInFavorite = it.flatSavedInFavorites,
                priceByn = UiPrice(
                    price = it.priceByn,
                    currency = "BYN"
                ),
                priceUsd = UiPrice(
                    price = it.priceUsd,
                    currency = "USD"
                ),
                numberOfRooms = it.rooms,
                publishedAt = it.publishedAtUi,
                address = it.address.orEmpty(),
                metroStation = it.metroStation.orEmpty()
            )
        }
    }
}