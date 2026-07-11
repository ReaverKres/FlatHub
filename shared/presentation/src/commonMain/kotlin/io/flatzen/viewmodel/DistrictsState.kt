package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
@Immutable
data class DistrictsState(
    val isLoading: Boolean,
    val districts: ImmutableList<UiDistrict>
) : MVIState {
    companion object {
        val Initial = DistrictsState(isLoading = true, districts = persistentListOf())
    }
}

// Intent
sealed interface DistrictsIntent : MVIIntent {
    data object LoadDistricts : DistrictsIntent
}

// Action — no side effects for Districts screen
sealed interface DistrictsAction : MVIAction
