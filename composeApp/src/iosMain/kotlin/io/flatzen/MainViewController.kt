package io.flatzen

import androidx.compose.ui.window.ComposeUIViewController
import io.flatzen.coil.configureSingletonImageLoader

fun MainViewController() = ComposeUIViewController {
    configureSingletonImageLoader()
    // Initialize Koin before any Compose UI renders to avoid "KoinApplication has not been started"
    CommonApplication.initialize()
    App()
}
