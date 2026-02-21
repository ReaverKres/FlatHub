package io.flatzen.viewmodel

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
sealed interface SplashState : MVIState {
    data object Loading : SplashState
    data object Success : SplashState
    data class Error(val exception: Exception?) : SplashState
}

// Intent — no user intents, load on init
sealed interface SplashIntent : MVIIntent

// Action — no side effects for Splash screen
sealed interface SplashAction : MVIAction
