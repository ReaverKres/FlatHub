package io.flatzen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import io.flatzen.di.container
import io.flatzen.navigation.MainGraphNavigation
import io.flatzen.navigation.rememberExitApp
import io.flatzen.themes.ProvideThemeRevealController
import io.flatzen.viewmodel.SplashContainer
import io.flatzen.viewmodel.SplashState
import pro.respawn.flowmvi.compose.dsl.subscribe

@Composable
fun App() {
    val splashContainer: SplashContainer = container()
    val splashState by splashContainer.store.subscribe { }

    if (splashState is SplashState.Loading) {
        return
    }

    val exitApp = rememberExitApp()
    ProvideThemeRevealController {
        MainGraphNavigation(onExitApp = exitApp)
    }
}
