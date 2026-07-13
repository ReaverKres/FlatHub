package io.flatzen.monetization.tier

import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * In-memory monetization session state. Resets when the app process restarts.
 */
object MonetizationSessionState {
    var premiumUpsellDismissed: Boolean = false

    private var lastAdShownEpochMs: Long = 0L

    fun canShowAd(minIntervalMinutes: Long): Boolean {
        if (minIntervalMinutes <= 0) return true
        if (lastAdShownEpochMs == 0L) return true
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastAdShownEpochMs
        return elapsed >= minIntervalMinutes.minutes.inWholeMilliseconds
    }

    fun millisUntilCanShowAd(minIntervalMinutes: Long): Long {
        if (minIntervalMinutes <= 0) return 0L
        if (lastAdShownEpochMs == 0L) return 0L
        val required = minIntervalMinutes.minutes.inWholeMilliseconds
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastAdShownEpochMs
        return (required - elapsed).coerceAtLeast(0L)
    }

    fun recordAdShown() {
        lastAdShownEpochMs = Clock.System.now().toEpochMilliseconds()
    }
}
