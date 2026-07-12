package io.flatzen.analytics

class AppMetricaLauncher(
    private val engine: AppMetricaEngine,
) {
    private var activated = false

    fun activate() {
        if (!AnalyticsConfig.enabled || activated) return
        val apiKey = AnalyticsConfig.appMetricaApiKey
        if (apiKey.isBlank()) return
        engine.activate(
            apiKey = apiKey,
            sessionTimeoutSec = AnalyticsConfig.sessionTimeoutSec,
            logsEnabled = AnalyticsConfig.logsEnabled,
        )
        activated = true
    }
}
