package io.flatzen.analytics

import io.flatzen.analytics.AppMetricaIosBridge.configure


/**
 * Wired from Swift via [configure] before [Analytics.activate].
 */
object AppMetricaIosBridge {
    fun configure(
        activate: (apiKey: String, sessionTimeout: Int, logs: Boolean) -> Unit,
        report: (name: String, attributes: Map<String, Any>) -> Unit,
    ) {
        AppMetricaNativeHandlers.activateHandler = activate
        AppMetricaNativeHandlers.reportHandler = report
    }
}
