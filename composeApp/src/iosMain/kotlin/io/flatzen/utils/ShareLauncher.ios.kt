package io.flatzen.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun shareLauncher(onShareError: (Exception) -> Unit): ShareLauncher {
    return remember {
        IOSShareLauncher(onShareError)
    }
}

class IOSShareLauncher(
    private val onShareError: (exception: Exception) -> Unit
) : ShareLauncher {
    override fun shareText(text: String, subject: String) {
        try {
            val items = mutableListOf<Any>()
            items.add(text)
            if (subject.isNotBlank()) {
                items.add(subject)
            }
            
            val activityViewController = UIActivityViewController(
                activityItems = items,
                applicationActivities = null
            )
            
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            val presentedViewController = rootViewController?.presentedViewController ?: rootViewController
            
            presentedViewController?.presentViewController(
                viewControllerToPresent = activityViewController,
                animated = true,
                completion = null
            )
        } catch (e: Exception) {
            onShareError(e)
        }
    }
}