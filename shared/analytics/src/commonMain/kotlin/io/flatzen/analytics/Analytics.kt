package io.flatzen.analytics

/**
 * Platform-agnostic analytics facade. Swap [AppMetricaAnalytics] for another implementation if needed.
 */
interface Analytics {
    fun activate()
    suspend fun track(event: AnalyticsEvent)
}
