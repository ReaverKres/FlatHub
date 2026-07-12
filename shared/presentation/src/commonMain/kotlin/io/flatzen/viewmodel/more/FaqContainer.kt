package io.flatzen.viewmodel.more

import io.flatzen.commoncomponents.commonentities.more.FaqConfigData
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce

private typealias Ctx = PipelineContext<FaqState, FaqIntent, FaqAction>

class FaqContainer(
    private val configFieldsChecker: ConfigFieldsChecker,
    private val navigator: FlatHubNavigator,
) : Container<FaqState, FaqIntent, FaqAction> {

    override val store = store<FaqState, FaqIntent, FaqAction>(initial = FaqState.Loading) {
        init {
            loadFaqConfig()
        }
        reduce { intent ->
            when (intent) {
                is FaqIntent.LoadData -> loadFaqConfig()
                FaqIntent.NavigateBack -> navigator.navigate(FlatHubCommand.NavigateBack)
            }
        }
    }

    private suspend fun Ctx.loadFaqConfig() {
        updateState { FaqState.Loading }
        withContext(Dispatchers.Default) {
            val result = configFieldsChecker.checkJson<FaqConfigData>(ConfigFields.FaqConfigData)
            if (result != null) {
                updateState { FaqState.Success(result) }
            } else {
                updateState {
                    FaqState.Error(
                        IllegalStateException("FaqConfigData Что то пошло не так")
                    )
                }
            }
        }
    }
}
