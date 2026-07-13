package io.flatzen.monetization.config

import io.flatzen.commoncomponents.commonentities.monetization.MonetizationConfigData
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.monetization.MonetizationDefaults

data class MonetizationRemoteConfig(
    val adsEnabled: Boolean = MonetizationDefaults.ADS_ENABLED,
    val homeGridAdInterval: Int = MonetizationDefaults.HOME_GRID_AD_INTERVAL,
    val homeListAdInterval: Int = MonetizationDefaults.HOME_LIST_AD_INTERVAL,
    val swipeAdInterval: Int = MonetizationDefaults.SWIPE_AD_INTERVAL,
    val swipeAdMinIntervalMinutes: Long = MonetizationDefaults.SWIPE_AD_MIN_INTERVAL_MINUTES,
    val feedDelayMinutes: Long = MonetizationDefaults.FEED_DELAY_MINUTES,
    val premiumFallbackEnabled: Boolean = MonetizationDefaults.PREMIUM_FALLBACK_ENABLED,
    val trialDays: Long = MonetizationDefaults.TRIAL_DAYS,
    val priceWeekUsd: String = MonetizationDefaults.PRICE_WEEK_USD,
    val priceMonthUsd: String = MonetizationDefaults.PRICE_MONTH_USD,
    val priceQuarterUsd: String = MonetizationDefaults.PRICE_QUARTER_USD,
    val appodealAndroidAppKey: String = MonetizationDefaults.APPODEAL_ANDROID_APP_KEY,
    val appodealIosAppKey: String = MonetizationDefaults.APPODEAL_IOS_APP_KEY,
    val homeFeedListPlacement: String = MonetizationDefaults.HOME_FEED_LIST_PLACEMENT,
    val homeFeedGridPlacement: String = MonetizationDefaults.HOME_FEED_GRID_PLACEMENT,
    val swipeCardPlacement: String = MonetizationDefaults.SWIPE_CARD_PLACEMENT,
    val rewardedPremiumPlacement: String = MonetizationDefaults.REWARDED_PREMIUM_PLACEMENT,
)

fun ConfigFieldsChecker.resolveMonetizationConfig(): MonetizationRemoteConfig {
    val jsonConfig = checkJson<MonetizationConfigData>(ConfigFields.MonetizationConfigData)

    return MonetizationRemoteConfig(
        adsEnabled = checkBoolean(ConfigFields.AdsEnabled) ?: MonetizationDefaults.ADS_ENABLED,
        premiumFallbackEnabled = checkBoolean(ConfigFields.PremiumFallbackEnabled)
            ?: MonetizationDefaults.PREMIUM_FALLBACK_ENABLED,
        homeGridAdInterval = jsonConfig?.homeGridAdInterval?.coerceAtLeast(1)
            ?: MonetizationDefaults.HOME_GRID_AD_INTERVAL,
        homeListAdInterval = jsonConfig?.homeListAdInterval?.coerceAtLeast(1)
            ?: MonetizationDefaults.HOME_LIST_AD_INTERVAL,
        swipeAdInterval = jsonConfig?.swipeAdInterval?.coerceAtLeast(1)
            ?: MonetizationDefaults.SWIPE_AD_INTERVAL,
        swipeAdMinIntervalMinutes = jsonConfig?.swipeAdMinIntervalMinutes?.coerceAtLeast(0)
            ?: MonetizationDefaults.SWIPE_AD_MIN_INTERVAL_MINUTES,
        feedDelayMinutes = jsonConfig?.feedDelayMinutes?.coerceAtLeast(0)
            ?: MonetizationDefaults.FEED_DELAY_MINUTES,
        trialDays = jsonConfig?.trialDays?.coerceAtLeast(0)
            ?: MonetizationDefaults.TRIAL_DAYS,
        priceWeekUsd = jsonConfig?.premiumPriceWeekUsd?.takeIf { it.isNotBlank() }
            ?: MonetizationDefaults.PRICE_WEEK_USD,
        priceMonthUsd = jsonConfig?.premiumPriceMonthUsd?.takeIf { it.isNotBlank() }
            ?: MonetizationDefaults.PRICE_MONTH_USD,
        priceQuarterUsd = jsonConfig?.premiumPriceQuarterUsd?.takeIf { it.isNotBlank() }
            ?: MonetizationDefaults.PRICE_QUARTER_USD,
        appodealAndroidAppKey = jsonConfig?.appodealAndroidAppKey.orEmpty(),
        appodealIosAppKey = jsonConfig?.appodealIosAppKey.orEmpty(),
        homeFeedListPlacement = jsonConfig?.homeFeedListPlacement?.takeIf { it.isNotBlank() }
            ?: MonetizationDefaults.HOME_FEED_LIST_PLACEMENT,
        homeFeedGridPlacement = jsonConfig?.homeFeedGridPlacement?.takeIf { it.isNotBlank() }
            ?: MonetizationDefaults.HOME_FEED_GRID_PLACEMENT,
        swipeCardPlacement = jsonConfig?.swipeCardPlacement?.takeIf { it.isNotBlank() }
            ?: MonetizationDefaults.SWIPE_CARD_PLACEMENT,
        rewardedPremiumPlacement = jsonConfig?.rewardedPremiumPlacement?.takeIf { it.isNotBlank() }
            ?: MonetizationDefaults.REWARDED_PREMIUM_PLACEMENT,
    )
}
