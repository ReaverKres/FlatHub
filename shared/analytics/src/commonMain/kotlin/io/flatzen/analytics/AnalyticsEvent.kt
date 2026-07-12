package io.flatzen.analytics

data class AnalyticsEvent(
    val eventName: String,
    val parameters: Map<String, Any> = emptyMap(),
)
