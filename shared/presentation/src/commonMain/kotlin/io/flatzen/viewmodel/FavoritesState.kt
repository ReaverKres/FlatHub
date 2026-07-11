package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.viewmodel.list.UiFlat
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
@Immutable
data class FavoritesState(
    val isLoading: Boolean,
    val flatList: ImmutableList<UiFlat>
) : MVIState {
    companion object {
        val Initial = FavoritesState(isLoading = true, flatList = persistentListOf())
    }
}

// Intent
sealed interface FavoritesIntent : MVIIntent {
    data class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FavoritesIntent
}

// Action — no side effects for Favorites screen
sealed interface FavoritesAction : MVIAction
