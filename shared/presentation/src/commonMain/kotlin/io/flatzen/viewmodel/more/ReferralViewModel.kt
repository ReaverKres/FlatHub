package io.flatzen.viewmodel.more

import io.flatzen.commoncomponents.extensions.substringBeforeAnyDelimiter
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.notifications.NotificationsService
import io.flatzen.usecases.RegistrationUseCase
import io.flatzen.viewmodel.base.BaseMviViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import repository.referrals.ReferralError
import repository.referrals.ReferralException
import repository.referrals.ReferralsRepository
import repository.userpreferences.UserPreferencesRepository
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

private sealed interface PrivateReferralAction : ReferralAction {
    data object NotificationAvailable : PrivateReferralAction
    data object UpdateUserId : PrivateReferralAction
}

sealed interface ReferralEvent : MviEvent {
    data class StatsLoaded(val stats: LCE<ReferralStatsResponse>) : ReferralEvent
    data object SubmitLoading : ReferralEvent
    data class SubmitSucceeded(
        val host: ReferralStatsResponse,
        val invited: ReferralStatsResponse
    ) : ReferralEvent

    data class SubmitFailed(val message: String?) : ReferralEvent
    data class InputUpdated(val code: String) : ReferralEvent
    data class Copied(val code: String) : ReferralEvent
    data object StatsErrorDialogHidden : ReferralEvent
    data object NotificationAvailable : ReferralEvent
    data object UserIdUpdated : ReferralEvent
}

sealed interface ReferralEffect : MviEffect {
    data class ShowMessage(val text: String) : ReferralEffect
    data class Copy(val text: String) : ReferralEffect
    data object NotificationAvailable : ReferralEffect
}

class ReferralViewModel(
    private val registrationUseCase: RegistrationUseCase,
    private val notificationsService: NotificationsService,
    private val prefsRepo: UserPreferencesRepository,
    private val referralRepo: ReferralsRepository,
    private val devicePlatform: DevicePlatform
) : BaseMviViewModel<ReferralAction, ReferralUiState, ReferralEvent, ReferralEffect>() {

    override fun initialState(): ReferralUiState = ReferralUiState()

    init {
        onIntent(PrivateReferralAction.UpdateUserId)
        onIntent(ReferralAction.Load)
    }

    override suspend fun handleIntent(
        action: ReferralAction,
        currentState: ReferralUiState
    ): Flow<ReferralEvent> {
        return when (action) {
            PrivateReferralAction.UpdateUserId -> {
                flowOf(ReferralEvent.UserIdUpdated)
            }

            PrivateReferralAction.NotificationAvailable -> {
                flowOf(ReferralEvent.NotificationAvailable)
            }

            ReferralAction.Load -> {
                val userId = devicePlatform.deviceId
                val isUserNotRegistered = prefsRepo.getUserPreferences()
                    .first()?.deviceDocumentResponse?.userId.isNullOrEmpty()
                if (isUserNotRegistered) {
                    flow {
                        val token = notificationsService.getOrCreateDeviceToken()
                        val user = registrationUseCase.registerUser(token)
                        if (user.referralStats != null) {
                            emit(user.referralStats!!)
                        }
                    }.asLCE().map { stats ->
                        ReferralEvent.StatsLoaded(stats)
                    }
                } else {
                    referralRepo.getStats(userId).asLCE().map {
                        ReferralEvent.StatsLoaded(it)
                    }
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
                            ReferralError.UserNotFound -> "Пользователь с кодом $host не найден"
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
                flowOf(ReferralEvent.StatsErrorDialogHidden)
            }
        }
    }

    override suspend fun onEvent(event: ReferralEvent): ReferralEffect? {
        return when (event) {
            is ReferralEvent.Copied -> ReferralEffect.Copy(event.code)
            is ReferralEvent.SubmitFailed -> ReferralEffect.ShowMessage(event.message ?: "Ошибка")
            is ReferralEvent.NotificationAvailable -> ReferralEffect.NotificationAvailable
            else -> null
        }
    }

    override suspend fun reduce(
        event: ReferralEvent,
        currentState: ReferralUiState
    ): ReferralUiState {
        return when (event) {
            is ReferralEvent.UserIdUpdated -> {
                currentState.copy(myCode = devicePlatform.deviceId)
            }

            is ReferralEvent.NotificationAvailable -> currentState
            is ReferralEvent.SubmitLoading -> {
                currentState.copy(codeIsLoading = true)
            }

            is ReferralEvent.StatsLoaded -> event.stats.process(
                onLoading = {
                    currentState.copy(
                        isLoading = true,
                    )
                },
                onError = { message, throwable ->
                    currentState.copy(
                        isLoading = false,
                        statsErrorMessage = throwable.cause?.message
                            ?: message?.substringBeforeAnyDelimiter()
                    )
                },
                onSuccess = {
                    if (it.isNotificationAvailable) {
                        viewModelScope.launch {
                            prefsRepo.updateReferralStats(it)
                            onIntent(PrivateReferralAction.NotificationAvailable)
                        }
                    }
                    currentState.copy(
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
                codeIsLoading = false,
                usedReferralCode = event.invited.usedReferralCode,
                requiredInvites = event.invited.requiredInvites,
                remainingInvites = event.invited.remainingInvites,
                isNotificationAvailable = event.invited.isNotificationAvailable,
                inputCode = "",
                submitErrorMessage = null
            )

            is ReferralEvent.SubmitFailed -> currentState.copy(
                codeIsLoading = false,
                submitErrorMessage = event.message
            )

            is ReferralEvent.InputUpdated -> currentState.copy(
                inputCode = event.code,
                submitErrorMessage = null
            )

            is ReferralEvent.Copied -> currentState
            is ReferralEvent.StatsErrorDialogHidden -> currentState.copy(statsErrorMessage = null)
        }
    }
}


