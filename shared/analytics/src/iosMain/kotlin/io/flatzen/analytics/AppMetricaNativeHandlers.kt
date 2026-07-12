package io.flatzen.analytics

object AppMetricaNativeHandlers {
    var activateHandler: ((String, Int, Boolean) -> Unit)? = null
    var reportHandler: ((String, Map<String, Any>) -> Unit)? = null
}
