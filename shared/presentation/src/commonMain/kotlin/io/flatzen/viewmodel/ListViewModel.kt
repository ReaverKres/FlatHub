package io.flatzen.viewmodel

import AppFlat
import androidx.compose.runtime.Immutable
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
import repository.kufar.KufarRepository
import server_request.KufarSearchParams

sealed interface FlatListScreenAction : MviAction {
    class SearchKufarFlats() : FlatListScreenAction
}

@Immutable
data class FlatListScreenState(
    val isLoading: Boolean,
    val flatList: List<UiFlat>
) : MviState

@Immutable
data class UiFlat(
    val adId: Long,
    val imageUrls: List<String>,
    val priceUsd: String,
    val priceByn: String,
    val numberOfRooms: Int,
    val metroStation: String,
    val address: String
)

sealed interface FlatListEvents : MviEvent {
    data class KufarFlatsLoaded(val kufarFlats: LCE<List<AppFlat>>) : FlatListEvents
}

class FlatSearchViewModel(
    private val kufarRepository: KufarRepository
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
            is FlatListScreenAction.SearchKufarFlats -> {
                loadKufarFlats()
            }
        }
    }

    private suspend fun loadKufarFlats(): Flow<FlatListEvents> {
        return kufarRepository.searchFlats(KufarSearchParams()).asLCE().map {
            FlatListEvents.KufarFlatsLoaded(it)
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
        }
    }

    private fun appFlatListToUiFlatList(appFlatList: List<AppFlat>): List<UiFlat> {
        return appFlatList.map {
            UiFlat(
                adId = it.adId,
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