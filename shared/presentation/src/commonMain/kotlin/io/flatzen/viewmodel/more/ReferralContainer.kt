package io.flatzen.viewmodel.more

import io.flatzen.commoncomponents.extensions.substringBeforeAnyDelimiter
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.notifications.NotificationsService
import io.flatzen.usecases.RegistrationUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import repository.referrals.ReferralError
import repository.referrals.ReferralException
import repository.referrals.ReferralsRepository
import repository.userpreferences.UserPreferencesRepository

private typealias ReferralCtx = PipelineContext<ReferralState, ReferralIntent, ReferralAction>

class ReferralContainer(
    private val registrationUseCase: RegistrationUseCase,
    private val notificationsService: NotificationsService,
    private val prefsRepo: UserPreferencesRepository,
    private val referralRepo: ReferralsRepository,
    private val devicePlatform: DevicePlatform
) : Container<ReferralState, ReferralIntent, ReferralAction> {

    override val store = store<ReferralState, ReferralIntent, ReferralAction>(
        initial = ReferralState.Initial
    ) {
        init {
            updateState { copy(myCode = devicePlatform.deviceId) }
            handleLoad()

        }
        reduce { intent ->
            when (intent) {
                is ReferralIntent.NotificationAvailable -> { /* no state change */
                }

                is ReferralIntent.Load -> handleLoad()
                is ReferralIntent.UpdateInput -> updateState {
                    copy(inputCode = intent.code, submitErrorMessage = null)
                }

                is ReferralIntent.SubmitCode -> handleSubmitCode()
                is ReferralIntent.CopyMyCode -> withState {
                    action(ReferralAction.Copy(myCode))
                }

                is ReferralIntent.HideStatsErrorDialog -> updateState { copy(statsErrorMessage = null) }
            }
        }
    }

    private suspend fun ReferralCtx.handleLoad() {
        val userId = devicePlatform.deviceId
        val isUserNotRegistered = prefsRepo.getUserPreferences()
            .first()?.deviceDocumentResponse?.userId.isNullOrEmpty()
        val statsFlow = if (isUserNotRegistered) {
            flow {
                val token = notificationsService.getOrCreateDeviceToken()
                val user = registrationUseCase.registerUser(token)
                if (user.referralStats != null) emit(user.referralStats!!)
            }.asLCE()
        } else {
            referralRepo.getStats(userId).asLCE()
        }
        statsFlow.collect { lce ->
            when (lce) {
                is LCE.Loading -> updateState { copy(isLoading = true) }
                is LCE.Error -> updateState {
                    copy(
                        isLoading = false,
                        statsErrorMessage = lce.throwable?.cause?.message
                            ?: lce.message?.substringBeforeAnyDelimiter()
                    )
                }

                is LCE.Content -> {
                    if (lce.value.isNotificationAvailable) {
                        launch { prefsRepo.updateReferralStats(lce.value) }
                        store.intent(ReferralIntent.NotificationAvailable)
                    }
                    updateState {
                        copy(
                            isLoading = false,
                            myCode = lce.value.userId,
                            usedReferralCode = lce.value.usedReferralCode,
                            requiredInvites = lce.value.requiredInvites,
                            remainingInvites = lce.value.remainingInvites,
                            remainingInvitesIsVisible = lce.value.remainingInvites != 0,
                            isNotificationAvailable = lce.value.isNotificationAvailable,
                            submitErrorMessage = null
                        )
                    }
                }
            }
        }
    }

    private suspend fun ReferralCtx.handleSubmitCode() {
        withState {
            if (isLoading || codeIsLoading) return@withState
            updateState { copy(codeIsLoading = true) }
            val host = inputCode.trim()
            val invited = devicePlatform.deviceId
            val result = referralRepo.useCode(host, invited)
            result.fold(
                onSuccess = { resp ->
                    updateState {
                        copy(
                            codeIsLoading = false,
                            usedReferralCode = resp.invitedStats.usedReferralCode,
                            requiredInvites = resp.invitedStats.requiredInvites,
                            remainingInvites = resp.invitedStats.remainingInvites,
                            isNotificationAvailable = resp.invitedStats.isNotificationAvailable,
                            inputCode = "",
                            submitErrorMessage = null
                        )
                    }
                },
                onFailure = { t ->
                    val message = when ((t as? ReferralException)?.error) {
                        ReferralError.UserNotFound -> "Пользователь с кодом $host не найден"
                        ReferralError.SameUserIds -> "Нельзя вводить свой собственный код"
                        ReferralError.CodeAlreadyUsed -> "Код уже использован"
                        ReferralError.DuplicateLink -> "Ссылка уже существует"
                        is ReferralError.Unknown, null -> t.cause?.message
                    }
                    updateState { copy(codeIsLoading = false, submitErrorMessage = message) }
                    action(ReferralAction.ShowMessage(message ?: "Ошибка"))
                }
            )
        }
    }
}
