package io.flatzen.commoncomponents.commonentities

/**
 * Favicon URL for listing platforms. Prefer remote favicon for new markets;
 * BY platforms still have bundled drawables in UI for offline reliability.
 */
fun FlatPlatform.faviconUrl(): String = when (this) {
    FlatPlatform.ONLINER -> "https://www.onliner.by/favicon.ico"
    FlatPlatform.KUFAR -> "https://www.kufar.by/favicon.ico"
    FlatPlatform.REALT -> "https://realt.by/favicon.ico"
    FlatPlatform.DOMOVITA -> "https://domovita.by/favicon.ico"
    FlatPlatform.OTODOM -> "https://www.otodom.pl/favicon.ico"
    FlatPlatform.OLX_PL -> "https://www.olx.pl/favicon.ico"
    FlatPlatform.GRATKA -> "https://gratka.pl/favicon.ico"
    FlatPlatform.MORIZON -> "https://www.morizon.pl/favicon.ico"
}
