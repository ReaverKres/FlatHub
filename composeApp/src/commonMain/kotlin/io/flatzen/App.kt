package io.flatzen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.theme.ThemeMode
import io.flatzen.navigation.MainGraphNavigation
import io.flatzen.navigation.rememberExitApp
import io.flatzen.themes.FlatHubTheme
import org.koin.compose.koinInject
import repository.userpreferences.UserPreferencesRepository

@Composable
fun App() {
    val exitApp = rememberExitApp()
    val userPreferences: UserPreferencesRepository = koinInject()
    val themeMode by userPreferences.observeThemeMode()
        .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)

    FlatHubTheme(themeMode = themeMode) {
        MainGraphNavigation(onExitApp = exitApp)
    }
}
