package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.viewmodel.list.UiFlat
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
@Immutable
data class FavoritesState(
    val isLoading: Boolean,
    val flatList: List<UiFlat>
) : MVIState {
    companion object {
        val Initial = FavoritesState(isLoading = true, flatList = emptyList())
    }
}

// Intent
sealed interface FavoritesIntent : MVIIntent {
    data class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FavoritesIntent
}

// Action — no side effects for Favorites screen
sealed interface FavoritesAction : MVIAction
