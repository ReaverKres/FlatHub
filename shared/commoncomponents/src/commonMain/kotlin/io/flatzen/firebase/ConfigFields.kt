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

    /** Comma-separated FlatPlatform names for Austria, e.g. `IS24_AT,IMMOWELT_AT,WILLHABEN`. Empty = all. */
    EnabledPlatformsAt("enabled_platforms_at"),

    /** Comma-separated FlatPlatform names for Turkey, e.g. `EMLAKJET`. Empty = all. */
    EnabledPlatformsTr("enabled_platforms_tr"),

    /** Comma-separated FlatPlatform names for UAE, e.g. `PROPERTY_FINDER,DUBIZZLE,OPENSOOQ`. Empty = all. */
    EnabledPlatformsAe("enabled_platforms_ae"),

    /** Comma-separated FlatPlatform names for Thailand, e.g. `PROPERTYHUB,LIVINGINSIDER,RENTHUB`. Empty = all. */
    EnabledPlatformsTh("enabled_platforms_th"),

    /** Comma-separated FlatPlatform names for US, e.g. `ZUMPER`. Empty = all. */
    EnabledPlatformsUs("enabled_platforms_us"),

    /** Comma-separated FlatPlatform names for South Korea, e.g. `DABANG,ZIGBANG`. Empty = all. */
    EnabledPlatformsKr("enabled_platforms_kr"),

    /** Comma-separated FlatPlatform names for Japan, e.g. `SUUMO,YAHOO_RE,ATHOME`. Empty = all. */
    EnabledPlatformsJp("enabled_platforms_jp"),

    /** Comma-separated FlatPlatform names for Switzerland, e.g. `FLATFOX`. Empty = all. */
    EnabledPlatformsCh("enabled_platforms_ch"),
}
