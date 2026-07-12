package io.flatzen.viewmodel

import io.flatzen.firebase.RemoteConfigRepository
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce

private typealias SplashCtx = PipelineContext<SplashState, SplashIntent, SplashAction>

class SplashContainer(
    private val remoteConfigRepository: RemoteConfigRepository,
) : Container<SplashState, SplashIntent, SplashAction> {

    override val store = store(initial = SplashState.Loading) {
        init {
            remoteConfigRepository.awaitFirstLoadAttempt()
            updateState { SplashState.Success }
        }
        reduce { intent ->
            // No user intents for Splash screen
        }
    }
}
