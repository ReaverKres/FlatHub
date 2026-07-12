package io.flatzen.analytics

actual class AppMetricaEngine actual constructor() {
    actual fun activate(apiKey: String, sessionTimeoutSec: Int, logsEnabled: Boolean) {
        AppMetricaNativeHandlers.activateHandler?.invoke(apiKey, sessionTimeoutSec, logsEnabled)
    }

    actual fun reportEvent(name: String, parameters: Map<String, Any>) {
        AppMetricaNativeHandlers.reportHandler?.invoke(name, parameters)
    }
}
