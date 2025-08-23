package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import entities.AppFlat
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import repository.mergedrepo.MergedRepository

sealed interface FavoritesScreenAction : MviAction {
    class LoadFavorites(val favoritesFlats: LCE<List<AppFlat>>) : FavoritesScreenAction
    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long): FavoritesScreenAction
}

@Immutable
data class FavoritesScreenState(
    val isLoading: Boolean,
    val flatList: List<UiFlat>
) : MviState

sealed interface FavoritesEvents : MviEvent {
    data class FavoritesLoaded(val allFlats: LCE<List<AppFlat>>) : FavoritesEvents
    data class FlatUpdateInFavorite(val flat: LCE<AppFlat?>) : FavoritesEvents
}

class FavoritesViewModel(
    private val mergedRepository: MergedRepository,
) : BaseMviViewModel<FavoritesScreenAction, FavoritesScreenState, FavoritesEvents, MviEffect>() {

    override fun initialState(): FavoritesScreenState = FavoritesScreenState(
        isLoading = true,
        flatList = emptyList(),
    )

    init {
        mergedRepository.getFavoritesFromLocalDb()
            .asLCE()
            .onEach { event -> onIntent(FavoritesScreenAction.LoadFavorites(event)) }
            .launchIn(viewModelScope)
    }

    override suspend fun handleIntent(
        action: FavoritesScreenAction,
        currentState: FavoritesScreenState
    ): Flow<FavoritesEvents> {
        return when (action) {
           is FavoritesScreenAction.LoadFavorites -> {
               flowOf(FavoritesEvents.FavoritesLoaded(action.favoritesFlats))
           }
            is FavoritesScreenAction.ClickOnFavorite -> {
                mergedRepository.saveFlatToFavorite(action.flatPlatform, action.adId)
                    .asLCE()
                    .map { FavoritesEvents.FlatUpdateInFavorite(it) }
            }
        }
    }

    override suspend fun reduce(
        event: FavoritesEvents,
        currentState: FavoritesScreenState
    ): FavoritesScreenState {
        return when (event) {
            is FavoritesEvents.FavoritesLoaded -> event.allFlats.process(
                onLoading = { currentState.copy(isLoading = true) },
                onError = { _, _ -> currentState.copy(isLoading = false) },
                onSuccess = { flats ->
                    val uiFlats = appFlatListToUiFlatList(flats)
                    currentState.copy(isLoading = false, flatList = uiFlats)
                }
            )
            is FavoritesEvents.FlatUpdateInFavorite -> event.flat.process(
                onLoading = { currentState },
                onError = { _, _ -> currentState },
                onSuccess = { updatedFlat ->
                    val updatedList = if (updatedFlat == null) currentState.flatList else {
                        currentState.flatList.filterNot { it.adId == updatedFlat.adId && it.flatPlatform == updatedFlat.flatPlatform }
                    }
                    currentState.copy(flatList = updatedList)
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
                metroStation = it.metroStation.orEmpty(),
                coordinates = it.coordinates?.let {
                    UiCoordinates(it.latitude, it.longitude)
                }
            )
        }
    }
}

