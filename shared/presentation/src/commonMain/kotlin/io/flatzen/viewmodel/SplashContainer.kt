package io.flatzen.viewmodel

import io.flatzen.firebase.ConfigManager
import kotlinx.coroutines.flow.last
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce

private typealias SplashCtx = PipelineContext<SplashState, SplashIntent, SplashAction>

class SplashContainer(
    private val configManager: ConfigManager
) : Container<SplashState, SplashIntent, SplashAction> {

    override val store = store(initial = SplashState.Loading) {
        init {
            configManager.init()
            fetchRemoteConfig()
        }
        reduce { intent ->
            // No user intents for Splash screen
        }
    }

    private suspend fun SplashCtx.fetchRemoteConfig() {
        val result = configManager.fetchAndActivate().last()
        if (result.isSuccess) {
            updateState { SplashState.Success }
        } else {
            updateState { SplashState.Error(result.exception) }
        }
    }
}
