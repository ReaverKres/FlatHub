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
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.ads.AdService
import io.flatzen.monetization.ads.AppodealConsentStartup
import io.flatzen.monetization.billing.CurrentActivityHolder
import io.flatzen.viewmodel.SplashContainer
import io.flatzen.viewmodel.SplashState
import org.koin.android.ext.android.inject
import pro.respawn.flowmvi.compose.dsl.subscribe

class MainActivity : ComponentActivity() {
    private var isSplashVisible by mutableStateOf(true)
    private val adService: AdService by inject()
    private val configFieldsChecker: ConfigFieldsChecker by inject()

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
                if (splashState is SplashState.Success) {
                    val consentEnabled = configFieldsChecker
                        .checkBoolean(ConfigFields.ConsentManagerEnabled)
                        ?: MonetizationDefaults.CONSENT_MANAGER_ENABLED
                    AppodealConsentStartup.start(
                        activity = this@MainActivity,
                        adService = adService,
                        consentManagerEnabled = consentEnabled,
                    )
                }
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
