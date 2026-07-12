package io.flatzen.analytics

import android.content.Context
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class AppMetricaEngine actual constructor() : KoinComponent {
    private val context: Context by inject()

    actual fun activate(apiKey: String, sessionTimeoutSec: Int, logsEnabled: Boolean) {
        val builder = AppMetricaConfig.newConfigBuilder(apiKey)
            .withSessionTimeout(sessionTimeoutSec)
        if (logsEnabled) {
            builder.withLogs()
        }
        AppMetrica.activate(context.applicationContext, builder.build())
    }

    actual fun reportEvent(name: String, parameters: Map<String, Any>) {
        AppMetrica.reportEvent(name, parameters)
    }
}
