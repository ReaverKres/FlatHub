package io.flatzen

import androidx.compose.ui.window.ComposeUIViewController
import io.flatzen.analytics.AnalyticsConfig
import io.flatzen.coil.configureSingletonImageLoader
import io.flatzen.commoncomponents.utils.DevicePlatformImpl

fun MainViewController() = ComposeUIViewController {
    configureSingletonImageLoader(DevicePlatformImpl())
    AnalyticsConfig.configure(
        apiKey = "ff1c4b73-6829-46f8-82ff-6d3d94ad1774",
        logsEnabled = true,
    )
    CommonApplication.initialize()
    App()
}
