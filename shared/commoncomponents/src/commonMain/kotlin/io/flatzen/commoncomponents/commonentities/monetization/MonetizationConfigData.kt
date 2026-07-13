package io.flatzen.commoncomponents.commonentities.monetization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MonetizationConfigData(
    @SerialName("homeGridAdInterval") val homeGridAdInterval: Int? = null,
    @SerialName("homeListAdInterval") val homeListAdInterval: Int? = null,
    @SerialName("swipeAdInterval") val swipeAdInterval: Int? = null,
    @SerialName("swipeAdMinIntervalMinutes") val swipeAdMinIntervalMinutes: Long? = null,
    @SerialName("feedDelayMinutes") val feedDelayMinutes: Long? = null,
    @SerialName("trialDays") val trialDays: Long? = null,
    @SerialName("premiumPriceWeekUsd") val premiumPriceWeekUsd: String? = null,
    @SerialName("premiumPriceMonthUsd") val premiumPriceMonthUsd: String? = null,
    @SerialName("premiumPriceQuarterUsd") val premiumPriceQuarterUsd: String? = null,
    @SerialName("applovinSdkKey") val applovinSdkKey: String? = null,
    @SerialName("applovinBannerAdUnit") val applovinBannerAdUnit: String? = null,
    @SerialName("applovinInterstitialAdUnit") val applovinInterstitialAdUnit: String? = null,
    @SerialName("applovinRewardedAdUnit") val applovinRewardedAdUnit: String? = null,
)
