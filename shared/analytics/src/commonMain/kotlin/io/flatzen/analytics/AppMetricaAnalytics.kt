package io.flatzen.analytics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppMetricaAnalytics(
    private val launcher: AppMetricaLauncher,
    private val engine: AppMetricaEngine,
) : Analytics {

    override fun activate() {
        launcher.activate()
    }

    override suspend fun track(event: AnalyticsEvent) {
        if (!AnalyticsConfig.enabled) return
        withContext(Dispatchers.Default) {
            runCatching {
                engine.reportEvent(event.eventName, event.parameters)
            }
        }
    }
}
