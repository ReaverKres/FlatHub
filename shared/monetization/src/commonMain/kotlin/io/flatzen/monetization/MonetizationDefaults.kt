package io.flatzen.monetization

import io.flatzen.monetization.MonetizationDefaults.DEBUG_PREMIUM_SCREEN_TOGGLE


/**
 * Product IDs must match Play Console / App Store Connect.
 * See docs/12-console-setup.md
 */
object MonetizationDefaults {
    const val PRODUCT_WEEK = "flatzen_premium_week"
    const val PRODUCT_MONTH = "flatzen_premium_month"
    const val PRODUCT_QUARTER = "flatzen_premium_quarter"

    const val PRICE_WEEK_USD = "1.00"
    const val PRICE_MONTH_USD = "2.50"
    const val PRICE_QUARTER_USD = "6.00"

    const val HOME_GRID_AD_INTERVAL = 16
    const val HOME_LIST_AD_INTERVAL = 14
    const val SWIPE_AD_INTERVAL = 10
    const val SWIPE_AD_MIN_INTERVAL_MINUTES = 1L
    const val FEED_DELAY_MINUTES = 60L
    const val TRIAL_DAYS = 0L

    /** When true — treat everyone as Premium and skip store billing. */
    const val PREMIUM_FALLBACK_ENABLED = false

    /**
     * Debug: show Active / Purchase toggle on PremiumScreen to preview both UI branches
     * without changing real subscription status. Turn off before release.
     */
    const val DEBUG_PREMIUM_SCREEN_TOGGLE = false

    /**
     * Debug: force PremiumScreen branch when [DEBUG_PREMIUM_SCREEN_TOGGLE] is true.
     * `null` = follow real [io.flatzen.monetization.billing.SubscriptionStatus];
     * `true` = active Premium UI; `false` = purchase UI.
     */
    val DEBUG_PREMIUM_SCREEN_FORCE_ACTIVE: Boolean? = null

    const val ADS_ENABLED = true

    const val REWARDED_PREMIUM_HOURS = 1L

    /** Empty app keys = ads SDK not initialized (safe no-op). Fill via Remote Config. */
    const val APPODEAL_ANDROID_APP_KEY = ""
    const val APPODEAL_IOS_APP_KEY = ""

    const val HOME_FEED_LIST_PLACEMENT = "home_feed_list"
    const val HOME_FEED_GRID_PLACEMENT = "home_feed_grid"
    const val SWIPE_CARD_PLACEMENT = "swipe_card"
    const val REWARDED_PREMIUM_PLACEMENT = "rewarded_premium"
}
