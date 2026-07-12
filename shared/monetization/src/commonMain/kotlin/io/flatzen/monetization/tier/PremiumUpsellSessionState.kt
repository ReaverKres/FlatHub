package io.flatzen.monetization.tier

/**
 * In-memory dismiss flag for the premium delay upsell.
 * Resets when the app process restarts.
 */
object PremiumUpsellSessionState {
    var dismissed: Boolean = false
}
