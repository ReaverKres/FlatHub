package io.flatzen

import androidx.compose.runtime.Composable
import io.flatzen.navigation.MainGraphNavigation
import io.flatzen.navigation.rememberExitApp
import io.flatzen.themes.ProvideThemeRevealController

@Composable
fun App() {
    val exitApp = rememberExitApp()
    ProvideThemeRevealController {
        MainGraphNavigation(onExitApp = exitApp)
    }
}
