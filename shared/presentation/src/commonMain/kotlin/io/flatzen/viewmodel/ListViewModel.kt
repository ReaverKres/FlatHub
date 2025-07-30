package io.flatzen.viewmodel

import AppFlat
import androidx.compose.runtime.Immutable
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.FlatPlatform
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import repository.fillter.FilterRepository
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository
import repository.realt.RealtRepository

sealed interface FlatListScreenAction : MviAction {
    class SearchKufarFlats() : FlatListScreenAction
    class SearchOnlinerFlats() : FlatListScreenAction
    class SearchFlats(val isLoadMore: Boolean) : FlatListScreenAction
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
    data class KufarFlatsLoaded(val kufarFlats: LCE<List<AppFlat>>) : FlatListEvents
    data class OnlinerFlatsLoaded(val onlinerFlats: LCE<List<AppFlat>>) : FlatListEvents
    data class AllFlatsLoaded(val allFlats: LCE<List<AppFlat>>, val isLoadMore: Boolean) :
        FlatListEvents
}

class FlatSearchViewModel(
    private val kufarRepository: KufarRepository,
    private val onlinerRepository: OnlinerRepository,
    private val realtRepository: RealtRepository,
    private val filterRepository: FilterRepository
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
                kufarRepository.clearCashedFlats()
                realtRepository.clearCashedFlats()
                onlinerRepository.clearCashedFlats()
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

            is FlatListScreenAction.SearchKufarFlats -> {
                loadKufarFlats()
            }

            is FlatListScreenAction.SearchOnlinerFlats -> {
                loadOnlinerFlats()
            }
        }
    }

    private suspend fun loadAllFlats(isLoadMore: Boolean): Flow<FlatListEvents> {
        return kufarRepository.searchFlats()
            .zip(onlinerRepository.searchFlats()) { kufarList, onlinerList -> kufarList + onlinerList }
            .zip(realtRepository.searchFlats()) { kOn, r -> kOn + r }
            .asLCE()
            .map { FlatListEvents.AllFlatsLoaded(it, isLoadMore) }
    }

    private suspend fun loadKufarFlats(): Flow<FlatListEvents> {
        return kufarRepository.searchFlats().asLCE().map {
            FlatListEvents.KufarFlatsLoaded(it)
        }
    }

    private suspend fun loadOnlinerFlats(): Flow<FlatListEvents> {
        return onlinerRepository.searchFlats().asLCE().map {
            FlatListEvents.OnlinerFlatsLoaded(it)
        }
    }

    override suspend fun reduce(
        event: FlatListEvents,
        currentState: FlatListScreenState
    ): FlatListScreenState {
        return when (event) {
            is FlatListEvents.KufarFlatsLoaded -> event.kufarFlats.process(
                onLoading = {
                    currentState.copy(isLoading = true)
                },
                onError = { message, _ ->
                    currentState
                },
                onSuccess = { flatsLoaded(it, currentState) }
            )

            is FlatListEvents.OnlinerFlatsLoaded -> event.onlinerFlats.process(
                onLoading = {
                    currentState.copy(isLoading = true)
                },
                onError = { message, _ ->
                    currentState
                },
                onSuccess = { flatsLoaded(it, currentState) }
            )

            is FlatListEvents.AllFlatsLoaded -> event.allFlats.process(
                onLoading = {
                    currentState.copy(isLoading = true, isLoadingMore = event.isLoadMore)
                },
                onError = { message, _ ->
                    currentState
                },
                onSuccess = { flatsLoaded(it, currentState, event.isLoadMore) }
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