package io.flatzen.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
actual fun copyLauncher(
    onCopyError: (Exception) -> Unit,
    onCopySuccess: (text: String) -> Unit
): CopyLauncher {
    return remember {
        IOSCopyLauncher(onCopyError, onCopySuccess)
    }
}

class IOSCopyLauncher(
    private val onCopyError: (exception: Exception) -> Unit,
    private val onCopySuccess: (text: String) -> Unit
) : CopyLauncher {
    override fun copyText(text: String) {
        try {
            UIPasteboard.generalPasteboard.setString(text)
            onCopySuccess(text)
        } catch (e: Exception) {
            onCopyError(e)
        }
    }
}