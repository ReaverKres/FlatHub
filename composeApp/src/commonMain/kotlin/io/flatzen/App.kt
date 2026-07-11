package io.flatzen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import io.flatzen.navigation.MainGraphNavigation
import io.flatzen.navigation.rememberExitApp

@Composable
fun App() {
    val exitApp = rememberExitApp()

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        MainGraphNavigation(onExitApp = exitApp)
    }
}
