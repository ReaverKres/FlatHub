package io.flatzen

import io.flatzen.viewmodel.SplashScreenViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.flatzen.viewmodel.SplashUiState
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    val splashScreenViewModel by viewModel<SplashScreenViewModel>()
    var isSplashVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Remove when https://issuetracker.google.com/issues/364713509 is fixed
            LaunchedEffect(isSystemInDarkTheme()) {
                enableEdgeToEdge()
            }
            LaunchedEffect(Unit) {
                splashScreenViewModel.uiState.collect {
                    isSplashVisible = it != SplashUiState.Loading
                }
            }
            splashScreen.setKeepOnScreenCondition {
                isSplashVisible
            }
            App()
        }
    }
}