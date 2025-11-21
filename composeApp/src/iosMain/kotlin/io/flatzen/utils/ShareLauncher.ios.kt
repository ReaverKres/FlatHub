package io.flatzen.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController

@Composable
actual fun shareLauncher(onShareError: (Exception) -> Unit): ShareLauncher {
    return remember {
        IOSShareLauncher(onShareError)
    }
}

class IOSShareLauncher(
    private val onShareError: (exception: Exception) -> Unit
) : ShareLauncher {
    @OptIn(ExperimentalForeignApi::class)
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
            
            activityViewController.popoverPresentationController?.let { popover ->
                val view = presentedViewController?.view
                if (view != null) {
                    popover.sourceView = view
                    val bounds = view.bounds
                    val x = CGRectGetMidX(bounds)
                    val y = CGRectGetMidY(bounds)
                    popover.sourceRect = CGRectMake(x, y, 1.0, 1.0)
                }
            }
            
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