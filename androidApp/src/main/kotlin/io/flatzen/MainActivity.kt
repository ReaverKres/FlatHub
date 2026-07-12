package io.flatzen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.flatzen.di.container
import io.flatzen.monetization.billing.CurrentActivityHolder
import io.flatzen.viewmodel.SplashContainer
import io.flatzen.viewmodel.SplashState
import pro.respawn.flowmvi.compose.dsl.subscribe

class MainActivity : ComponentActivity() {
    private var isSplashVisible by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        CurrentActivityHolder.activity = this
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        splashScreen.setKeepOnScreenCondition {
            isSplashVisible
        }

        setContent {
            val splashContainer: SplashContainer = container()
            val splashState by splashContainer.store.subscribe { }
            LaunchedEffect(splashState) {
                isSplashVisible = splashState is SplashState.Loading
            }

            App()
        }
    }

    override fun onDestroy() {
        if (CurrentActivityHolder.activity === this) {
            CurrentActivityHolder.activity = null
        }
        super.onDestroy()
    }
}
