package io.flatzen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import io.flatzen.commoncomponents.config.Config
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent

class FlatZenApp : Application(), KoinComponent {

    companion object {
        var instance: FlatZenApp? = null
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createDefaultNotificationChannel()
        initKmpNotifier()
        CommonApplication.initialize {
            androidContext(applicationContext)
        }
        
        // Initialize Config with hardcoded API key for now
        Config.addAppMetricaApiKey("ff1c4b73-6829-46f8-82ff-6d3d94ad1774")
        // Initialize AppMetrica
        initAppmetrica()
    }
    
    private fun initAppmetrica() {
        val config = AppMetricaConfig
            .newConfigBuilder(Config.appMetricaApiKey)
            .withLogs()
            .build()
        AppMetrica.activate(this, config)
        AppMetrica.enableActivityAutoTracking(this)
    }

    private fun createDefaultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default"
            val name = "Default"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun initKmpNotifier() {
        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.mipmap.ic_launcher,
                showPushNotification = true
            )
        )
    }
}
