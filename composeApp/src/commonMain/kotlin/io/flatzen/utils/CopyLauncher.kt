package io.flatzen.utils

import androidx.compose.runtime.Composable

@Composable
expect fun copyLauncher(
    onCopyError: (exception: Exception) -> Unit = {},
    onCopySuccess: (text: String) -> Unit = {}
): CopyLauncher

interface CopyLauncher {
    fun copyText(text: String)
}