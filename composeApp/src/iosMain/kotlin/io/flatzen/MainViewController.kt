package io.flatzen

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController { 
    // Initialize Koin before any Compose UI renders to avoid "KoinApplication has not been started"
    CommonApplication.initialize()
    App() 
}
