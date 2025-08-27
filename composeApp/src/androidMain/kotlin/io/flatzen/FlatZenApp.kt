package io.flatzen

import android.app.Application
import android.util.Log
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import io.flatzen.commoncomponents.config.Config
import io.flatzen.commoncomponents.analytics.AnalyticsManagerInterface
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FlatZenApp : Application(), KoinComponent {
    private val analyticsManager: AnalyticsManagerInterface by inject()
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Config with hardcoded API key for now
        Config.addAppMetricaApiKey("ff1c4b73-6829-46f8-82ff-6d3d94ad1774")
        
        // Initialize AppMetrica
        initAppmetrica()
        
        CommonApplication.initialize {
            androidContext(applicationContext)
        }
        
        // Send app launch analytics event
        sendAppLaunchEvent()
    }
    
    private fun initAppmetrica() {
        val config = AppMetricaConfig
            .newConfigBuilder(Config.appMetricaApiKey)
            .withLogs()
            .build()
        AppMetrica.activate(this, config)
        AppMetrica.enableActivityAutoTracking(this)
    }
    
    private fun sendAppLaunchEvent() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                analyticsManager.registerEvent(
                    AnalyticsEvent(
                        eventName = "app_launch",
                        parameters = mapOf(
                            "app_version" to "1.0",
                            "version_code" to 1
                        )
                    )
                )
            } catch (e: Exception) {
                Log.e("FlatZenApp", "Failed to send app launch event", e)
            }
        }
    }
}
