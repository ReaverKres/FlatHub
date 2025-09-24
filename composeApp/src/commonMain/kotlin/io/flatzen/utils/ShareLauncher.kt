package io.flatzen.utils

import androidx.compose.runtime.Composable

@Composable
expect fun shareLauncher(
    onShareError: (exception: Exception) -> Unit = {}
): ShareLauncher

interface ShareLauncher {
    fun shareText(text: String, subject: String = "")
}