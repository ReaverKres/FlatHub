package io.flatzen.commoncomponents.analytics

/**
 * Interface for managing analytics events across the application.
 * This interface provides a unified way to track user events and application behavior.
 */
interface AnalyticsManagerInterface {
    
    /**
     * Registers an analytics event for tracking.
     * 
     * @param event The analytics event to register
     */
    suspend fun registerEvent(event: AnalyticsEvent)
}