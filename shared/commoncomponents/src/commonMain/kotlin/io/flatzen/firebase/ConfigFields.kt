package io.flatzen.firebase

enum class ConfigFields(val param: String) {
    FreeVersionAvailable("freeversion2"),
    MoreConfigData("moreConfigData"),
    FaqConfigData("faqConfigData"),

    AdsEnabled("adsEnabled"),
    PremiumFallbackEnabled("premiumFallbackEnabled"),
    ConsentManagerEnabled("consentManagerEnabled"),
    MonetizationConfigData("monetizationConfigData"),
}
