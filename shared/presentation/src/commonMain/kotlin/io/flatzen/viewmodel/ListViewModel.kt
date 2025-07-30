package io.flatzen.viewmodel

import AppFlat
import androidx.compose.runtime.Immutable
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter.formatInstant
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import repository.fillter.FilterRepository
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository
import repository.realt.RealtRepository
import server_request.OnlinerSearchParams

sealed interface FlatListScreenAction : MviAction {
    class SearchKufarFlats() : FlatListScreenAction
    class SearchOnlinerFlats() : FlatListScreenAction
    class SearchFlats() : FlatListScreenAction
}

@Immutable
data class FlatListScreenState(
    val isLoading: Boolean,
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
    data class AllFlatsLoaded(val allFlats: LCE<List<AppFlat>>) : FlatListEvents
}

class FlatSearchViewModel(
    private val kufarRepository: KufarRepository,
    private val onlinerRepository: OnlinerRepository,
    private val realtRepository: RealtRepository,
    private val filterRepository: FilterRepository
) : BaseMviViewModel<FlatListScreenAction, FlatListScreenState, FlatListEvents, MviEffect>() {

    override fun initialState(): FlatListScreenState = FlatListScreenState(
        isLoading = true,
        flatList = emptyList()
    )

    init {
        viewModelScope.launch {
            filterRepository.updateFilter(CommonFilterRequestModel())
        }

        filterRepository.cashedFilterFlow
            .distinctUntilChanged()
            .onEach { newFilters ->
                onIntent(FlatListScreenAction.SearchFlats())
            }
            .launchIn(viewModelScope)

    }

    override suspend fun handleIntent(
        action: FlatListScreenAction,
        currentState: FlatListScreenState
    ): Flow<FlatListEvents> {
        return when (action) {
            is FlatListScreenAction.SearchFlats -> {
                loadAllFlats()
            }

            is FlatListScreenAction.SearchKufarFlats -> {
                loadKufarFlats()
            }

            is FlatListScreenAction.SearchOnlinerFlats -> {
                loadOnlinerFlats()
            }
        }
    }

    private suspend fun loadAllFlats(): Flow<FlatListEvents> {
        return kufarRepository.searchFlats()
            .zip(onlinerRepository.searchFlats(OnlinerSearchParams())) { kufarList, onlinerList -> kufarList + onlinerList }
            .zip(realtRepository.searchFlats()) { kOn, r -> kOn + r }
            .asLCE()
            .map { FlatListEvents.AllFlatsLoaded(it) }
    }

    private suspend fun loadKufarFlats(): Flow<FlatListEvents> {
        return kufarRepository.searchFlats().asLCE().map {
            FlatListEvents.KufarFlatsLoaded(it)
        }
    }

    private suspend fun loadOnlinerFlats(): Flow<FlatListEvents> {
        return onlinerRepository.searchFlats(OnlinerSearchParams()).asLCE().map {
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
                    currentState.copy(isLoading = true)
                },
                onError = { message, _ ->
                    currentState
                },
                onSuccess = { flatsLoaded(it, currentState) }
            )
        }
    }

    private fun flatsLoaded(
        flats: List<AppFlat>,
        currentState: FlatListScreenState
    ): FlatListScreenState {
        val uiFlatList = appFlatListToUiFlatList(flats.sortedByDescending { it.publishedAt })
        return currentState.copy(
            isLoading = false,
            flatList = uiFlatList
        )
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