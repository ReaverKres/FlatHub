package io.flatzen.analytics

object AnalyticsConfig {
    var appMetricaApiKey: String = ""
        private set

    var sessionTimeoutSec: Int = 120
    var logsEnabled: Boolean = true
    var enabled: Boolean = true

    fun configure(
        apiKey: String,
        sessionTimeoutSec: Int = 120,
        logsEnabled: Boolean = true,
        enabled: Boolean = true,
    ) {
        appMetricaApiKey = apiKey
        this.sessionTimeoutSec = sessionTimeoutSec
        this.logsEnabled = logsEnabled
        this.enabled = enabled
    }
}
