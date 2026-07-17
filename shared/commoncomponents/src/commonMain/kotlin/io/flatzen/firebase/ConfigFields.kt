package io.flatzen.firebase

enum class ConfigFields(val param: String) {
    FreeVersionAvailable("freeVersionAvailable2"),
    MoreConfigData("moreConfigData"),
    FaqConfigData("faqConfigData"),

    AdsEnabled("adsEnabled"),
    PremiumFallbackEnabled("premiumFallbackEnabled"),
    ConsentManagerEnabled("consentManagerEnabled"),
    MonetizationConfigData("monetizationConfigData"),

    /** Comma-separated FlatPlatform names for Poland, e.g. `OTODOM,OLX_PL,GRATKA,MORIZON`. Empty = all. */
    EnabledPlatformsPl("enabled_platforms_pl"),

    /** Comma-separated FlatPlatform names for Georgia, e.g. `SS_GE,LIVO`. Empty = all. */
    EnabledPlatformsGe("enabled_platforms_ge"),

    /** Comma-separated FlatPlatform names for Kazakhstan, e.g. `KRISHA,OLX_KZ,KN`. Empty = all. */
    EnabledPlatformsKz("enabled_platforms_kz"),

    /** Comma-separated FlatPlatform names for Spain, e.g. `FOTOCASA,PISOS`. Empty = all. */
    EnabledPlatformsEs("enabled_platforms_es"),

    /** Comma-separated FlatPlatform names for Germany, e.g. `IS24,IMMOWELT,KLEINANZEIGEN`. Empty = all. */
    EnabledPlatformsDe("enabled_platforms_de"),
}
