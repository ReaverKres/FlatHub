package io.flatzen.viewmodel.notifications

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State — stateless, no UI state to hold
data object ToggleNotificationsState : MVIState

// Intent
sealed interface ToggleNotificationsIntent : MVIIntent {
    data class ToggleNotifications(val filterName: String, val enabled: Boolean) : ToggleNotificationsIntent
}

// Action — one-time side effects
sealed interface ToggleNotificationsAction : MVIAction {
    data object ShowSettingsDialog : ToggleNotificationsAction
}
