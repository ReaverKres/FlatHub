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
    @SerialName("appodealAndroidAppKey") val appodealAndroidAppKey: String? = null,
    @SerialName("appodealIosAppKey") val appodealIosAppKey: String? = null,
    @SerialName("homeFeedListPlacement") val homeFeedListPlacement: String? = null,
    @SerialName("homeFeedGridPlacement") val homeFeedGridPlacement: String? = null,
    @SerialName("swipeCardPlacement") val swipeCardPlacement: String? = null,
    @SerialName("rewardedPremiumPlacement") val rewardedPremiumPlacement: String? = null,
)
