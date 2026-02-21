package io.flatzen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import io.flatzen.di.container
import io.flatzen.navigation.DeepLinkRouter
import io.flatzen.viewmodel.SplashContainer
import io.flatzen.viewmodel.SplashState
import pro.respawn.flowmvi.compose.dsl.subscribe

class MainActivity : ComponentActivity() {
    private var isSplashVisible by mutableStateOf(true)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
        intent.data?.toString()?.let { DeepLinkRouter.emitDeepLink(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        NotifierManager.onCreateOrOnNewIntent(intent)
        intent.data?.toString()?.let { DeepLinkRouter.emitDeepLink(it) }

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
}