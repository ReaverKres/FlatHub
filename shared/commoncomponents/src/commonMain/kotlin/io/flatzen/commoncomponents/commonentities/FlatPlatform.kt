package io.flatzen.commoncomponents.commonentities

import io.flatzen.commoncomponents.DrawablePath

enum class FlatPlatform(val value: String) {
    // Belarus
    ONLINER("onliner"),
    KUFAR("kufar"),
    REALT("realt"),
    DOMOVITA("domovita"),

    // Poland (MVP)
    OTODOM("otodom"),
    OLX_PL("olx_pl"),
    GRATKA("gratka"),
    MORIZON("morizon"),

    // Georgia (MVP)
    SS_GE("ss_ge"),
    LIVO("livo"),
    BINEBI("binebi"),

    // Kazakhstan (MVP)
    KRISHA("krisha"),
    OLX_KZ("olx_kz"),
    KN("kn"),

    // Spain (MVP)
    FOTOCASA("fotocasa"),
    PISOS("pisos"),

    // Germany (MVP)
    IS24("is24"),
    IMMOWELT("immowelt"),
    KLEINANZEIGEN("kleinanzeigen"),

    // Austria (MVP)
    IS24_AT("is24_at"),
    IMMOWELT_AT("immowelt_at"),
    WILLHABEN("willhaben"),

    // Turkey (MVP)
    EMLAKJET("emlakjet"),

    // UAE (MVP) — Bayut blocked (captcha); OpenSooq is the 3rd GO source
    PROPERTY_FINDER("property_finder"),
    DUBIZZLE("dubizzle"),
    OPENSOOQ("opensooq"),

    // Thailand (MVP) — DDproperty/FazWaz blocked (CF); RentHub rent-only apartments
    PROPERTYHUB("propertyhub"),
    LIVINGINSIDER("livinginsider"),
    RENTHUB("renthub"),

    // United States (MVP) — Zillow/Apartments.com blocked (PerimeterX/Akamai); restore via NOTES
    ZUMPER("zumper"),

    // South Korea (MVP) — Dabang + Zigbang only (Naver/Hogangnono blocked)
    DABANG("dabang"),
    ZIGBANG("zigbang"),

    // Japan (MVP) — SUUMO + Yahoo!不動産 + at home (LIFULL HOME'S blocked)
    SUUMO("suumo"),
    YAHOO_RE("yahoo_re"),
    ATHOME("athome"),

    // Switzerland (MVP) — Flatfox rent-only; Homegate/IS24.ch/Newhome blocked
    FLATFOX("flatfox"),

    // United Kingdom (MVP) — Zoopla deferred (Cloudflare)
    RIGHTMOVE("rightmove"),
    ONTHEMARKET("onthemarket"),
    OPENRENT("openrent"),

    // France (MVP) — Bien'ici only; LeBonCoin/SeLoger/Logic-Immo removed (see FR_PLATFORMS_RESTORE.md)
    BIENICI("bienici"),
}

fun FlatPlatform.marketCountry(): CountryCode = when (this) {
    FlatPlatform.ONLINER,
    FlatPlatform.KUFAR,
    FlatPlatform.REALT,
    FlatPlatform.DOMOVITA,
        -> CountryCode.BY

    FlatPlatform.OTODOM,
    FlatPlatform.OLX_PL,
    FlatPlatform.GRATKA,
    FlatPlatform.MORIZON,
        -> CountryCode.PL

    FlatPlatform.SS_GE,
    FlatPlatform.LIVO,
    FlatPlatform.BINEBI,
        -> CountryCode.GE

    FlatPlatform.KRISHA,
    FlatPlatform.OLX_KZ,
    FlatPlatform.KN,
        -> CountryCode.KZ

    FlatPlatform.FOTOCASA,
    FlatPlatform.PISOS,
        -> CountryCode.ES

    FlatPlatform.IS24,
    FlatPlatform.IMMOWELT,
    FlatPlatform.KLEINANZEIGEN,
        -> CountryCode.DE

    FlatPlatform.IS24_AT,
    FlatPlatform.IMMOWELT_AT,
    FlatPlatform.WILLHABEN,
        -> CountryCode.AT

    FlatPlatform.EMLAKJET,
        -> CountryCode.TR

    FlatPlatform.PROPERTY_FINDER,
    FlatPlatform.DUBIZZLE,
    FlatPlatform.OPENSOOQ,
        -> CountryCode.AE

    FlatPlatform.PROPERTYHUB,
    FlatPlatform.LIVINGINSIDER,
    FlatPlatform.RENTHUB,
        -> CountryCode.TH

    FlatPlatform.ZUMPER,
        -> CountryCode.US

    FlatPlatform.DABANG,
    FlatPlatform.ZIGBANG,
        -> CountryCode.KR

    FlatPlatform.SUUMO,
    FlatPlatform.YAHOO_RE,
    FlatPlatform.ATHOME,
        -> CountryCode.JP

    FlatPlatform.FLATFOX,
        -> CountryCode.CH

    FlatPlatform.RIGHTMOVE,
    FlatPlatform.ONTHEMARKET,
    FlatPlatform.OPENRENT,
        -> CountryCode.GB

    FlatPlatform.BIENICI,
        -> CountryCode.FR
}

fun FlatPlatform.usesSquareFeet(): Boolean = marketCountry().usesSquareFeet()

fun platformsForMarket(country: CountryCode): List<FlatPlatform> =
    FlatPlatform.entries.filter { it.marketCountry() == country }

/**
 * Structured list APIs expose a rooms field — missing value → show "Не указано".
 * Free-text list scrapers (Krisha/kn) often omit rooms → hide the field on the list.
 */
fun FlatPlatform.listExpectsRoomsField(): Boolean = when (this) {
    FlatPlatform.KRISHA, FlatPlatform.KN, FlatPlatform.LIVINGINSIDER, FlatPlatform.RENTHUB -> false
    else -> true
}

fun FlatPlatform.drawablePath(): DrawablePath = when (this) {
    FlatPlatform.ONLINER -> DrawablePath.ONLINER32
    FlatPlatform.KUFAR -> DrawablePath.KUFAR32
    FlatPlatform.REALT -> DrawablePath.REALT32
    FlatPlatform.DOMOVITA -> DrawablePath.DOMOVITA32
    FlatPlatform.OTODOM -> DrawablePath.OTODOM32
    FlatPlatform.OLX_PL -> DrawablePath.OLXPL32
    FlatPlatform.GRATKA -> DrawablePath.GRATKA32
    FlatPlatform.MORIZON -> DrawablePath.MORIZON32
    FlatPlatform.SS_GE -> DrawablePath.SSGE32
    FlatPlatform.LIVO -> DrawablePath.LIVO32
    FlatPlatform.BINEBI -> DrawablePath.BINEBI32
    FlatPlatform.KRISHA -> DrawablePath.KRISHA32
    FlatPlatform.OLX_KZ -> DrawablePath.OLXKZ32
    FlatPlatform.KN -> DrawablePath.KN32
    FlatPlatform.FOTOCASA -> DrawablePath.FOTOCASA32
    FlatPlatform.PISOS -> DrawablePath.PISOS32
    FlatPlatform.IS24 -> DrawablePath.IS2432
    FlatPlatform.IMMOWELT -> DrawablePath.IMMOWELT32
    FlatPlatform.KLEINANZEIGEN -> DrawablePath.KLEINANZEIGEN32
    FlatPlatform.IS24_AT -> DrawablePath.IS24AT32
    FlatPlatform.IMMOWELT_AT -> DrawablePath.IMMOWELTAT32
    FlatPlatform.WILLHABEN -> DrawablePath.WILLHABEN32
    FlatPlatform.EMLAKJET -> DrawablePath.EMLAKJET32
    FlatPlatform.PROPERTY_FINDER -> DrawablePath.PROPERTYFINDER32
    FlatPlatform.DUBIZZLE -> DrawablePath.DUBIZZLE32
    FlatPlatform.OPENSOOQ -> DrawablePath.OPENSOOQ32
    FlatPlatform.PROPERTYHUB -> DrawablePath.PROPERTYHUB32
    FlatPlatform.LIVINGINSIDER -> DrawablePath.LIVINGINSIDER32
    FlatPlatform.RENTHUB -> DrawablePath.RENTHUB32
    FlatPlatform.ZUMPER -> DrawablePath.ZUMPER32
    FlatPlatform.DABANG -> DrawablePath.DABANG32
    FlatPlatform.ZIGBANG -> DrawablePath.ZIGBANG32
    FlatPlatform.SUUMO -> DrawablePath.SUUMO32
    FlatPlatform.YAHOO_RE -> DrawablePath.YAHOO_RE32
    FlatPlatform.ATHOME -> DrawablePath.ATHOME32
    FlatPlatform.FLATFOX -> DrawablePath.FLATFOX32
    FlatPlatform.RIGHTMOVE -> DrawablePath.RIGHTMOVE32
    FlatPlatform.ONTHEMARKET -> DrawablePath.ONTHEMARKET32
    FlatPlatform.OPENRENT -> DrawablePath.OPENRENT32
    FlatPlatform.BIENICI -> DrawablePath.BIENICI32
}
