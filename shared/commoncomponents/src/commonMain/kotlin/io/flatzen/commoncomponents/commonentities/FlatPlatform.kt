package io.flatzen.commoncomponents.commonentities

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

    // Turkey (MVP)
    EMLAKJET("emlakjet"),
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

    FlatPlatform.EMLAKJET,
        -> CountryCode.TR
}

fun platformsForMarket(country: CountryCode): List<FlatPlatform> =
    FlatPlatform.entries.filter { it.marketCountry() == country }

/**
 * Structured list APIs expose a rooms field — missing value → show "Не указано".
 * Free-text list scrapers (Krisha/kn) often omit rooms → hide the field on the list.
 */
fun FlatPlatform.listExpectsRoomsField(): Boolean = when (this) {
    FlatPlatform.KRISHA, FlatPlatform.KN -> false
    else -> true
}
