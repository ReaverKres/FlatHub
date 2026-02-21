package io.flatzen.navigation

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberExitApp(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            var ctx = context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is Activity) {
                    ctx.finish()
                    return@remember
                }
                ctx = ctx.baseContext
            }
        }
    }
}
