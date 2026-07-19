package io.flatzen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import io.flatzen.analytics.AnalyticsConfig
import io.flatzen.coil.configureSingletonImageLoader
import io.flatzen.commoncomponents.utils.DevicePlatformImpl
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
        configureSingletonImageLoader(DevicePlatformImpl(this))
        createDefaultNotificationChannel()

        AnalyticsConfig.configure(
            apiKey = "ff1c4b73-6829-46f8-82ff-6d3d94ad1774",
            logsEnabled = true,
        )

        CommonApplication.initialize {
            androidContext(applicationContext)
        }
    }

    private fun createDefaultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "flatzen_general_notifications"
            val name = "Notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
