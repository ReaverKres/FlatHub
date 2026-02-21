package io.flatzen.navigation

import androidx.compose.runtime.Composable

/**
 * Returns a callback that exits the application.
 * On Android: finishes the Activity.
 * On iOS: no-op (iOS doesn't support programmatic app exit).
 */
@Composable
expect fun rememberExitApp(): () -> Unit
