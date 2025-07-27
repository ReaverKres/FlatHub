package io.flatzen.viewmodel

import AppFlat
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository
import server_request.KufarSearchParams
import server_request.OnlinerSearchParams

sealed interface FlatListScreenAction : MviAction {
    class SearchKufarFlats() : FlatListScreenAction
    class SearchOnlinerFlats() : FlatListScreenAction
    class InitialSearchFlats() : FlatListScreenAction
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
    val priceUsd: String,
    val priceByn: String,
    val numberOfRooms: Int?,
    val metroStation: String,
    val address: String
)

sealed interface FlatListEvents : MviEvent {
    data class KufarFlatsLoaded(val kufarFlats: LCE<List<AppFlat>>) : FlatListEvents
    data class OnlinerFlatsLoaded(val onlinerFlats: LCE<List<AppFlat>>) : FlatListEvents
    data class AllFlatsLoaded(val allFlats: LCE<List<AppFlat>>) : FlatListEvents
}

class FlatSearchViewModel(
    private val kufarRepository: KufarRepository,
    private val onlinerRepository: OnlinerRepository
) : BaseMviViewModel<FlatListScreenAction, FlatListScreenState, FlatListEvents, MviEffect>() {

    override fun initialState(): FlatListScreenState = FlatListScreenState(
        isLoading = true,
        flatList = emptyList()
    )

    override suspend fun handleIntent(
        action: FlatListScreenAction,
        currentState: FlatListScreenState
    ): Flow<FlatListEvents> {
        return when (action) {
            is FlatListScreenAction.InitialSearchFlats -> {
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
        return kufarRepository.searchFlats(KufarSearchParams()).zip(
            onlinerRepository.searchFlats(OnlinerSearchParams())
        ) { kufarFlats, onlinerFlats ->
            kufarFlats + onlinerFlats
        }.asLCE().map {
            FlatListEvents.AllFlatsLoaded(it)
        }
    }

    private suspend fun loadKufarFlats(): Flow<FlatListEvents> {
        return kufarRepository.searchFlats(KufarSearchParams()).asLCE().map {
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
                onSuccess = {
                    val uiFlatList = appFlatListToUiFlatList(it)
                    currentState.copy(
                        isLoading = false,
                        flatList = uiFlatList
                    )
                }
            )
            is FlatListEvents.OnlinerFlatsLoaded -> event.onlinerFlats.process(
                onLoading = {
                    currentState.copy(isLoading = true)
                },
                onError = { message, _ ->
                    currentState
                },
                onSuccess = {
                    val uiFlatList = appFlatListToUiFlatList(it)
                    currentState.copy(
                        isLoading = false,
                        flatList = uiFlatList
                    )
                }
            )
            is FlatListEvents.AllFlatsLoaded -> event.allFlats.process(
                onLoading = {
                    currentState.copy(isLoading = true)
                },
                onError = { message, _ ->
                    currentState
                },
                onSuccess = {
                    val uiFlatList = appFlatListToUiFlatList(it)
                    currentState.copy(
                        isLoading = false,
                        flatList = uiFlatList
                    )
                }
            )
        }
    }

    private fun appFlatListToUiFlatList(appFlatList: List<AppFlat>): List<UiFlat> {
        return appFlatList.map {
            UiFlat(
                adId = it.adId,
                flatPlatform = it.flatPlatform,
                imageUrls = it.imageUrls ?: listOf(),
                priceByn = "${it.priceByn} BYN",
                priceUsd = "${it.priceUsd} USD",
                numberOfRooms = it.rooms,
                address = it.address.orEmpty(),
                metroStation = it.metroStation.orEmpty()
            )
        }
    }
}