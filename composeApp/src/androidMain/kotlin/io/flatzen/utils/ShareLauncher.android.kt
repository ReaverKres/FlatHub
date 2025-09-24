package io.flatzen.utils

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun shareLauncher(onShareError: (Exception) -> Unit): ShareLauncher {
    val context = LocalContext.current
    return remember {
        AndroidShareLauncher(context, onShareError)
    }
}

class AndroidShareLauncher(
    private val context: Context,
    private val onShareError: (exception: Exception) -> Unit
) : ShareLauncher {
    override fun shareText(text: String, subject: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                type = "text/plain"
            }
            
            val shareIntent = Intent.createChooser(sendIntent, null)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            onShareError(e)
        }
    }
}