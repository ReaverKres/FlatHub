package io.flatzen.firebase

enum class ConfigFields(val param: String) {
    FreeVersionAvailable("freeVersionAvailable"),
    MoreConfigData("moreConfigData"),
    FaqConfigData("faqConfigData"),

    AdsEnabled("adsEnabled"),
    HomeListAdInterval("homeListAdInterval"),
    SwipeAdInterval("swipeAdInterval"),
    FeedDelayMinutes("feedDelayMinutes"),
    PremiumFallbackEnabled("premiumFallbackEnabled"),
    TrialDays("trialDays"),
    PremiumPriceWeekUsd("premiumPriceWeekUsd"),
    PremiumPriceMonthUsd("premiumPriceMonthUsd"),
    PremiumPriceQuarterUsd("premiumPriceQuarterUsd"),
    ApplovinSdkKey("applovinSdkKey"),
    ApplovinBannerAdUnit("applovinBannerAdUnit"),
    ApplovinInterstitialAdUnit("applovinInterstitialAdUnit"),
    ApplovinRewardedAdUnit("applovinRewardedAdUnit"),
}
