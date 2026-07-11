package io.flatzen.monetization.config

import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.monetization.MonetizationDefaults

data class MonetizationRemoteConfig(
    val adsEnabled: Boolean = MonetizationDefaults.ADS_ENABLED,
    val homeListAdInterval: Int = MonetizationDefaults.HOME_LIST_AD_INTERVAL,
    val swipeAdInterval: Int = MonetizationDefaults.SWIPE_AD_INTERVAL,
    val feedDelayMinutes: Long = MonetizationDefaults.FEED_DELAY_MINUTES,
    val premiumFallbackEnabled: Boolean = MonetizationDefaults.PREMIUM_FALLBACK_ENABLED,
    val trialDays: Long = MonetizationDefaults.TRIAL_DAYS,
    val priceWeekUsd: String = MonetizationDefaults.PRICE_WEEK_USD,
    val priceMonthUsd: String = MonetizationDefaults.PRICE_MONTH_USD,
    val priceQuarterUsd: String = MonetizationDefaults.PRICE_QUARTER_USD,
    val applovinSdkKey: String = MonetizationDefaults.APPLOVIN_SDK_KEY,
    val bannerAdUnit: String = MonetizationDefaults.APPLOVIN_BANNER_AD_UNIT,
    val interstitialAdUnit: String = MonetizationDefaults.APPLOVIN_INTERSTITIAL_AD_UNIT,
    val rewardedAdUnit: String = MonetizationDefaults.APPLOVIN_REWARDED_AD_UNIT,
)

fun ConfigFieldsChecker.resolveMonetizationConfig(): MonetizationRemoteConfig {
    return MonetizationRemoteConfig(
        adsEnabled = checkBoolean(ConfigFields.AdsEnabled) ?: MonetizationDefaults.ADS_ENABLED,
        homeListAdInterval = checkLong(ConfigFields.HomeListAdInterval)?.toInt()?.coerceAtLeast(1)
            ?: MonetizationDefaults.HOME_LIST_AD_INTERVAL,
        swipeAdInterval = checkLong(ConfigFields.SwipeAdInterval)?.toInt()?.coerceAtLeast(1)
            ?: MonetizationDefaults.SWIPE_AD_INTERVAL,
        feedDelayMinutes = checkLong(ConfigFields.FeedDelayMinutes)?.coerceAtLeast(0)
            ?: MonetizationDefaults.FEED_DELAY_MINUTES,
        premiumFallbackEnabled = checkBoolean(ConfigFields.PremiumFallbackEnabled)
            ?: MonetizationDefaults.PREMIUM_FALLBACK_ENABLED,
        trialDays = checkLong(ConfigFields.TrialDays)?.coerceAtLeast(0)
            ?: MonetizationDefaults.TRIAL_DAYS,
        priceWeekUsd = checkString(ConfigFields.PremiumPriceWeekUsd)
            ?: MonetizationDefaults.PRICE_WEEK_USD,
        priceMonthUsd = checkString(ConfigFields.PremiumPriceMonthUsd)
            ?: MonetizationDefaults.PRICE_MONTH_USD,
        priceQuarterUsd = checkString(ConfigFields.PremiumPriceQuarterUsd)
            ?: MonetizationDefaults.PRICE_QUARTER_USD,
        applovinSdkKey = checkString(ConfigFields.ApplovinSdkKey)
            ?: MonetizationDefaults.APPLOVIN_SDK_KEY,
        bannerAdUnit = checkString(ConfigFields.ApplovinBannerAdUnit)
            ?: MonetizationDefaults.APPLOVIN_BANNER_AD_UNIT,
        interstitialAdUnit = checkString(ConfigFields.ApplovinInterstitialAdUnit)
            ?: MonetizationDefaults.APPLOVIN_INTERSTITIAL_AD_UNIT,
        rewardedAdUnit = checkString(ConfigFields.ApplovinRewardedAdUnit)
            ?: MonetizationDefaults.APPLOVIN_REWARDED_AD_UNIT,
    )
}
