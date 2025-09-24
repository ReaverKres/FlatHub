package io.flatzen

import android.app.Application
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import io.flatzen.commoncomponents.config.Config
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent

class FlatZenApp : Application(), KoinComponent {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Config with hardcoded API key for now
        Config.addAppMetricaApiKey("ff1c4b73-6829-46f8-82ff-6d3d94ad1774")
        
        // Initialize AppMetrica
        initAppmetrica()
//        ComposeApplication.initialize()
        CommonApplication.initialize {
            androidContext(applicationContext)
        }
    }
    
    private fun initAppmetrica() {
        val config = AppMetricaConfig
            .newConfigBuilder(Config.appMetricaApiKey)
            .withLogs()
            .build()
        AppMetrica.activate(this, config)
        AppMetrica.enableActivityAutoTracking(this)
    }
}
