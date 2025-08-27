package io.flatzen.commoncomponents.analytics

/**
 * Represents an analytics event that can be tracked across the application.
 * 
 * @param eventName The name of the event to track
 * @param parameters Optional parameters associated with the event
 */
data class AnalyticsEvent(
    val eventName: String,
    val parameters: Map<String, Any> = emptyMap()
)