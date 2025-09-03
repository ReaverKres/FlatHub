package io.flatzen.commoncomponents.analytics

import io.appmetrica.analytics.AppMetrica
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of AnalyticsManagerInterface using AppMetrica SDK.
 */
class AnalyticsManagerImpl : AnalyticsManager {

    override suspend fun registerEvent(event: AnalyticsEvent) {
        withContext(Dispatchers.IO) {
            try {
                AppMetrica.reportEvent(event.eventName, event.parameters)
                println("Event registered: ${event.eventName} with params: ${event.parameters}")
            } catch (e: Exception) {
                println("Failed to register event: ${event.eventName} \n error ${e.message}")
            }
        }
    }
}