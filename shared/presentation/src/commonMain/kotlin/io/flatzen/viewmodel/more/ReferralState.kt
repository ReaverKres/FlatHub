package io.flatzen.viewmodel.more

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

@Immutable
data class ReferralState(
    val isLoading: Boolean = true,
    val codeIsLoading: Boolean = false,
    val myCode: String = "",
    val inputCode: String = "",
    val usedReferralCode: Boolean = false,
    val requiredInvites: Int = 2,
    val remainingInvites: Int = 2,
    val remainingInvitesIsVisible: Boolean = true,
    val isNotificationAvailable: Boolean = false,
    val submitErrorMessage: String? = null,
    val statsErrorMessage: String? = null
) : MVIState {
    companion object {
        val Initial = ReferralState()
    }
}

sealed interface ReferralIntent : MVIIntent {
    data object Load : ReferralIntent
    data class UpdateInput(val code: String) : ReferralIntent
    data object SubmitCode : ReferralIntent
    data object CopyMyCode : ReferralIntent
    data object HideStatsErrorDialog : ReferralIntent
    data object NotificationAvailable : ReferralIntent
    data object NavigateBack : ReferralIntent
}

sealed interface ReferralAction : MVIAction {
    data class ShowMessage(val text: String) : ReferralAction
    data class Copy(val text: String) : ReferralAction
    data object NotificationAvailable : ReferralAction
}
