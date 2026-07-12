package io.flatzen.analytics

expect class AppMetricaEngine() {
    fun activate(apiKey: String, sessionTimeoutSec: Int, logsEnabled: Boolean)
    fun reportEvent(name: String, parameters: Map<String, Any>)
}
