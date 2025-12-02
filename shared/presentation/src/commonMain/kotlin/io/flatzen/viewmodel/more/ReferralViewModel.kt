package io.flatzen.viewmodel.more

import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import repository.referrals.ReferralError
import repository.referrals.ReferralException
import repository.referrals.ReferralsRepository
import server_response.flathub.ReferralStatsResponse

data class ReferralUiState(
    val isLoading: Boolean = true,
    val codeIsLoading: Boolean = false,
    val myCode: String = "",
    val inputCode: String = "",
    val usedReferralCode: Boolean = false,
    val requiredInvites: Int = 3,
    val remainingInvites: Int = 3,
    val isNotificationAvailable: Boolean = false,
    val submitErrorMessage: String? = null,
    val statsErrorMessage: String? = null
) : MviState

sealed interface ReferralAction : MviAction {
    data object HideStatsErrorDialog : ReferralAction
    data object Load : ReferralAction
    data class UpdateInput(val code: String) : ReferralAction
    data object SubmitCode : ReferralAction
    data object CopyMyCode : ReferralAction
}

sealed interface ReferralEvent : MviEvent {
    data class StatsLoaded(val stats: LCE<ReferralStatsResponse>) : ReferralEvent
    data object SubmitLoading: ReferralEvent
    data class SubmitSucceeded(val host: ReferralStatsResponse, val invited: ReferralStatsResponse) : ReferralEvent
    data class SubmitFailed(val message: String?) : ReferralEvent
    data class InputUpdated(val code: String) : ReferralEvent
    data class Copied(val code: String) : ReferralEvent
    data object StatsErrorDialogHidden : ReferralEvent
}

sealed interface ReferralEffect : MviEffect {
    data class ShowMessage(val text: String) : ReferralEffect
    data class Copy(val text: String) : ReferralEffect
}

class ReferralViewModel(
    private val referralRepo: ReferralsRepository,
    private val devicePlatform: DevicePlatform
) : BaseMviViewModel<ReferralAction, ReferralUiState, ReferralEvent, ReferralEffect>() {

    override fun initialState(): ReferralUiState = ReferralUiState()

    init {
        onIntent(ReferralAction.Load)
    }

    override suspend fun handleIntent(action: ReferralAction, currentState: ReferralUiState): Flow<ReferralEvent> {
        return when (action) {
            ReferralAction.Load -> {
                val userId = devicePlatform.deviceId
                referralRepo.getStats(userId).asLCE().map {
                    ReferralEvent.StatsLoaded(it)
                }
            }
            is ReferralAction.UpdateInput -> flow { emit(ReferralEvent.InputUpdated(action.code)) }
            ReferralAction.CopyMyCode -> flow { emit(ReferralEvent.Copied(currentState.myCode)) }
            ReferralAction.SubmitCode -> {
                if (currentState.isLoading || currentState.codeIsLoading) {
                    return flowOf()
                }
                flow {
                    emit(ReferralEvent.SubmitLoading)
                    val host = currentState.inputCode.trim()
                    val invited = devicePlatform.deviceId
                    val result = referralRepo.useCode(host, invited)
                    result.onSuccess { resp ->
                        emit(ReferralEvent.SubmitSucceeded(resp.hostStats, resp.invitedStats))
                    }.onFailure { t ->
                        val message = when ((t as? ReferralException)?.error) {
                            ReferralError.SameUserIds -> "Нельзя вводить свой собственный код"
                            ReferralError.CodeAlreadyUsed -> "Код уже использован"
                            ReferralError.DuplicateLink -> "Ссылка уже существует"
                            is ReferralError.Unknown, null -> t.cause?.message
                        }
                        emit(ReferralEvent.SubmitFailed(message))
                    }
                }
            }
            is ReferralAction.HideStatsErrorDialog -> {
                flowOf()
            }
        }
    }

    override suspend fun onEvent(event: ReferralEvent): ReferralEffect? {
        return when (event) {
            is ReferralEvent.Copied -> ReferralEffect.Copy(event.code)
            is ReferralEvent.SubmitFailed -> ReferralEffect.ShowMessage(event.message ?: "Ошибка")
            else -> null
        }
    }

    override suspend fun reduce(event: ReferralEvent, currentState: ReferralUiState): ReferralUiState {
        return when (event) {
            is ReferralEvent.SubmitLoading -> {
                currentState.copy(codeIsLoading = true)
            }

            is ReferralEvent.StatsLoaded -> event.stats.process(
                onLoading = {
                    currentState.copy(
                        codeIsLoading = false,
                        isLoading = false,
                    )
                },
                onError = { message, throwable ->
                    currentState.copy(
                        codeIsLoading = false,
                        isLoading = false,
                        statsErrorMessage = throwable.cause?.message
                    )
                },
                onSuccess = {
                    currentState.copy(
                        codeIsLoading = false,
                        isLoading = false,
                        myCode = it.userId,
                        usedReferralCode = it.usedReferralCode,
                        requiredInvites = it.requiredInvites,
                        remainingInvites = it.remainingInvites,
                        isNotificationAvailable = it.isNotificationAvailable,
                        submitErrorMessage = null
                    )
                }
            )
            is ReferralEvent.SubmitSucceeded -> currentState.copy(
                isLoading = false,
                usedReferralCode = event.invited.usedReferralCode,
                requiredInvites = event.invited.requiredInvites,
                remainingInvites = event.invited.remainingInvites,
                isNotificationAvailable = event.invited.isNotificationAvailable,
                inputCode = "",
                submitErrorMessage = null
            )
            is ReferralEvent.SubmitFailed -> currentState.copy(isLoading = false, submitErrorMessage = event.message)
            is ReferralEvent.InputUpdated -> currentState.copy(inputCode = event.code, submitErrorMessage = null)
            is ReferralEvent.Copied -> currentState
            is ReferralEvent.StatsErrorDialogHidden -> currentState.copy(statsErrorMessage = null)
        }
    }
}


