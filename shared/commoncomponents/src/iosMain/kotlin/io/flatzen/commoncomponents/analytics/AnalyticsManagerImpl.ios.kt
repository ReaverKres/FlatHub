package io.flatzen.commoncomponents.analytics

/**
 * iOS stub implementation of AnalyticsManagerInterface.
 * This is a no-op implementation for iOS platform as requested.
 */
class AnalyticsManagerImpl : AnalyticsManager {
    
    override suspend fun registerEvent(event: AnalyticsEvent) {
        // No-op implementation for iOS
        // Analytics is only implemented for Android platform
        println("iOS Analytics Stub: Event ${event.eventName} with params ${event.parameters}")
    }
}