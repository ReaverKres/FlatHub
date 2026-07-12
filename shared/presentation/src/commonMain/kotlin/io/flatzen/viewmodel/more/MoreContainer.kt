package io.flatzen.viewmodel.more

import io.flatzen.firebase.RemoteConfigLoadState
import io.flatzen.firebase.RemoteConfigRepository
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.userpreferences.UserPreferencesRepository

private typealias PipeCtx = PipelineContext<MoreState, MoreIntent, MoreAction>

class MoreContainer(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val navigator: FlatHubNavigator,
) : Container<MoreState, MoreIntent, MoreAction> {

    override val store = store<MoreState, MoreIntent, MoreAction>(initial = MoreState.Initial) {
        whileSubscribed {
            remoteConfigRepository.moreConfig.collect { data ->
                updateState {
                    copy(
                        configState = when {
                            data != null -> MoreConfigState.Success(data)
                            remoteConfigRepository.loadState.value is RemoteConfigLoadState.Loading ->
                                MoreConfigState.Loading

                            else -> MoreConfigState.Error(
                                IllegalStateException("MoreConfigData is not loaded yet")
                            )
                        }
                    )
                }
            }
        }
        whileSubscribed {
            checkNotificationAvailableStatus()
        }
        reduce { intent ->
            when (intent) {
                MoreIntent.OpenFaq -> navigator.navigate(FlatHubCommand.OpenFaq)
                MoreIntent.OpenReferral -> navigator.navigate(FlatHubCommand.OpenReferral)
                MoreIntent.OpenPremium -> navigator.navigate(FlatHubCommand.OpenPremium)
            }
        }
    }

    private suspend fun PipeCtx.checkNotificationAvailableStatus() {
        userPreferencesRepository.getUserPreferences().collect { prefs ->
            val available = prefs?.deviceDocumentResponse?.referralStats?.isNotificationAvailable == true
            updateState { copy(isNotificationAvailable = available) }
        }
    }
}
