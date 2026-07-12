package io.flatzen.viewmodel.more

import io.flatzen.commoncomponents.commonentities.more.FaqConfigData
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
sealed interface FaqState : MVIState {
    data object Loading : FaqState

    data class Success(val faqConfigData: FaqConfigData) : FaqState

    data class Error(val exception: Exception?) : FaqState
}

// Intent
sealed interface FaqIntent : MVIIntent {
    data object LoadData : FaqIntent
    data object NavigateBack : FaqIntent
}

// Action — no side effects for FAQ screen
sealed interface FaqAction : MVIAction
