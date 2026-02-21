package io.flatzen.viewmodel.more

import io.flatzen.commoncomponents.commonentities.more.MoreConfigData
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import repository.userpreferences.UserPreferencesRepository

private typealias PipeCtx = PipelineContext<MoreState, MoreIntent, MoreAction>

class MoreContainer(
    private val configFieldsChecker: ConfigFieldsChecker,
    private val userPreferencesRepository: UserPreferencesRepository
) : Container<MoreState, MoreIntent, MoreAction> {

    override val store = store<MoreState, MoreIntent, MoreAction>(initial = MoreState.Initial) {
        init {
            loadMoreConfig()
        }
        whileSubscribed {
            checkNotificationAvailableStatus()
        }
        reduce { intent ->
            // No user intents for More screen
        }
    }

    private suspend fun PipeCtx.loadMoreConfig() {
        updateState { copy(configState = MoreConfigState.Loading) }
        withContext(Dispatchers.Default) {
            val result = configFieldsChecker.checkJson<MoreConfigData>(
                ConfigFields.MoreConfigData
            )
            if (result != null) {
                updateState { copy(configState = MoreConfigState.Success(result)) }
            } else {
                updateState {
                    copy(
                        configState = MoreConfigState.Error(
                            IllegalStateException("MoreConfigData Что то пошло не так")
                        )
                    )
                }
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
