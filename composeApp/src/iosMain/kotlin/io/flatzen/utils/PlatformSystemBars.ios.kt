package io.flatzen.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIStatusBarStyleDarkContent
import platform.UIKit.UIStatusBarStyleLightContent
import platform.UIKit.setStatusBarStyle

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformSystemBars(isDark: Boolean) {
    SideEffect {
        val style = if (isDark) {
            UIStatusBarStyleLightContent
        } else {
            UIStatusBarStyleDarkContent
        }
        UIApplication.sharedApplication.setStatusBarStyle(style, animated = true)
    }
}
