package io.flatzen.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun copyLauncher(
    onCopyError: (Exception) -> Unit,
    onCopySuccess: (text: String) -> Unit
): CopyLauncher {
    val context = LocalContext.current
    return remember {
        AndroidCopyLauncher(context, onCopyError, onCopySuccess)
    }
}

class AndroidCopyLauncher(
    private val context: Context,
    private val onCopyError: (exception: Exception) -> Unit,
    private val onCopySuccess: (text: String) -> Unit
) : CopyLauncher {
    override fun copyText(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", text)
            clipboard.setPrimaryClip(clip)
            onCopySuccess(text)
        } catch (e: Exception) {
            onCopyError(e)
        }
    }
}