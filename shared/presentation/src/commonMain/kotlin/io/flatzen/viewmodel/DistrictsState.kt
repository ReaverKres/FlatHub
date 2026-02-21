package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
@Immutable
data class DistrictsState(
    val isLoading: Boolean,
    val districts: List<UiDistrict>
) : MVIState {
    companion object {
        val Initial = DistrictsState(isLoading = true, districts = emptyList())
    }
}

// Intent
sealed interface DistrictsIntent : MVIIntent {
    data object LoadDistricts : DistrictsIntent
}

// Action — no side effects for Districts screen
sealed interface DistrictsAction : MVIAction
