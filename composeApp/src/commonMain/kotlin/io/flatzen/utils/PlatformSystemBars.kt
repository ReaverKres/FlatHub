package io.flatzen.utils

import androidx.compose.runtime.Composable

/** Syncs status / navigation bar icon contrast with the active theme. */
@Composable
expect fun PlatformSystemBars(isDark: Boolean)
