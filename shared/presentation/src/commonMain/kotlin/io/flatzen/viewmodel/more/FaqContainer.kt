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

private typealias Ctx = PipelineContext<FaqState, FaqIntent, FaqAction>

class FaqContainer(
    private val remoteConfigRepository: RemoteConfigRepository,
    private val navigator: FlatHubNavigator,
) : Container<FaqState, FaqIntent, FaqAction> {

    override val store = store<FaqState, FaqIntent, FaqAction>(initial = FaqState.Loading) {
        whileSubscribed {
            remoteConfigRepository.faqConfig.collect { data ->
                when {
                    data != null && data.faqItems.isNotEmpty() ->
                        updateState { FaqState.Success(data) }

                    remoteConfigRepository.loadState.value is RemoteConfigLoadState.Loading ->
                        updateState { FaqState.Loading }

                    else ->
                        updateState {
                            FaqState.Error(
                                IllegalStateException("FaqConfigData is empty or not loaded yet")
                            )
                        }
                }
            }
        }
        reduce { intent ->
            when (intent) {
                FaqIntent.LoadData -> { /* config updates reactively */
                }
                FaqIntent.NavigateBack -> navigator.navigate(FlatHubCommand.NavigateBack)
            }
        }
    }
}
