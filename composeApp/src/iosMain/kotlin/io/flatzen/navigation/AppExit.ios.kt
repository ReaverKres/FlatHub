package io.flatzen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberExitApp(): () -> Unit = remember { {} }
