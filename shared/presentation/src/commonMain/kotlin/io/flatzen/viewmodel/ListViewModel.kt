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
import repository.mergedrepo.MergedRepository

sealed interface FlatListScreenAction : MviAction {
    class SearchFlats(val isLoadMore: Boolean) : FlatListScreenAction
    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long): FlatListScreenAction
}

@Immutable
data class FlatListScreenState(
    val isLoading: Boolean,
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
    data class AllFlatsLoaded(val allFlats: LCE<List<AppFlat>>, val isLoadMore: Boolean) :
        FlatListEvents
    data class FlatUpdateInFavorite(val flat: LCE<AppFlat?>) : FlatListEvents
}

class FlatSearchViewModel(
    private val mergedRepository: MergedRepository,
    private val filterRepository: FilterRepository,
    private val connectionMonitor: ConnectionMonitor
) : BaseMviViewModel<FlatListScreenAction, FlatListScreenState, FlatListEvents, MviEffect>() {

    private var noFlatsToLoadMore: Boolean = false

    override fun initialState(): FlatListScreenState = FlatListScreenState(
        isLoading = true,
        isLoadingMore = false,
        flatList = emptyList(),
        noFlatsToLoadMore = false
    )

    init {
        viewModelScope.launch {
            filterRepository.updateFilter(CommonFilterRequestModel())
        }

        filterRepository.cashedFilterFlow
            .distinctUntilChanged()
            .onEach { newFilters ->
                noFlatsToLoadMore = false
                onIntent(FlatListScreenAction.SearchFlats(false))
            }
            .launchIn(viewModelScope)

    }

    override suspend fun handleIntent(
        action: FlatListScreenAction,
        currentState: FlatListScreenState
    ): Flow<FlatListEvents> {
        return when (action) {
            is FlatListScreenAction.SearchFlats -> {
                if(currentState.flatList.isNotEmpty() &&
                    connectionMonitor.isNetworkAvailable.first().not()
                    ) { return flowOf() }
                if(noFlatsToLoadMore) {
                    return flowOf()
                }
                if (action.isLoadMore) {
                    filterRepository.currentAppPage++
                } else {
                    filterRepository.currentAppPage = 1
                }
                loadAllFlats(action.isLoadMore)
            }
            is FlatListScreenAction.ClickOnFavorite -> {
                mergedRepository.saveFlatToFavorite(action.flatPlatform, action.adId).asLCE().map {
                    FlatListEvents.FlatUpdateInFavorite(it)
                }
            }
        }
    }

    private suspend fun loadAllFlats(isLoadMore: Boolean): Flow<FlatListEvents> {
        return when {
            connectionMonitor.isNetworkAvailable.first().not() -> {
                mergedRepository.getAllFlatsFromLocalDb()
                    .asLCE()
                    .map { FlatListEvents.AllFlatsLoaded(it, isLoadMore) }
            }
            else -> {
                mergedRepository.searchFlats()
                    .asLCE()
                    .map { FlatListEvents.AllFlatsLoaded(it, isLoadMore) }
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
                    currentState.copy(isLoading = true, isLoadingMore = event.isLoadMore)
                },
                onError = { message, _ ->
                    currentState
                },
                onSuccess = { flatsLoaded(it, currentState, event.isLoadMore) }
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
        }
    }

    private fun flatsLoaded(
        flats: List<AppFlat>,
        currentState: FlatListScreenState,
        isLoadMore: Boolean = false
    ): FlatListScreenState {
        val uiFlatList = appFlatListToUiFlatList(flats.sortedByDescending { it.publishedAt })

        return if(currentState.flatList.isNotEmpty() && uiFlatList.isEmpty()){
            noFlatsToLoadMore = true
            currentState.copy(
                isLoading = false,
                isLoadingMore = false,
                flatList = currentState.flatList,
                noFlatsToLoadMore = true
            )
        } else if (isLoadMore) {
            currentState.copy(
                noFlatsToLoadMore = false,
                isLoading = false,
                isLoadingMore = false,
                flatList = currentState.flatList + uiFlatList
            )
        } else {
            currentState.copy(
                noFlatsToLoadMore = false,
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