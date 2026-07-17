package io.flatzen.viewmodel.more

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.more.MoreConfigData
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
@Immutable
data class MoreState(
    val configState: MoreConfigState = MoreConfigState.Loading,
    val isNotificationAvailable: Boolean = false
) : MVIState {
    companion object {
        val Initial = MoreState()
    }
}

sealed interface MoreConfigState {
    data object Loading : MoreConfigState
    data class Success(val moreConfigData: MoreConfigData) : MoreConfigState
    data class Error(val exception: Exception?) : MoreConfigState
}

// Intent — no user intents, load on init
sealed interface MoreIntent : MVIIntent {
    data object OpenFaq : MoreIntent
    data object OpenReferral : MoreIntent
    data object OpenPremium : MoreIntent
    data object OpenLanguage : MoreIntent
    data object NavigateBack : MoreIntent
}

// Action — no side effects for More screen
sealed interface MoreAction : MVIAction
