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
    // Root /favicon.ico is 404; use Otodom CDN asset.
    FlatPlatform.OTODOM ->
        "https://statics.otodom.pl/static/otodompl/naspersclassifieds-regional/" +
                "verticalsre-atlas-web-otodompl/static/img/app-icon.png"
    FlatPlatform.OLX_PL -> "https://www.olx.pl/favicon.ico"
    FlatPlatform.GRATKA -> "https://gratka.pl/favicon.ico"
    FlatPlatform.MORIZON -> "https://www.morizon.pl/favicon.ico"
    FlatPlatform.SS_GE -> "https://home.ss.ge/favicon.ico"
    FlatPlatform.LIVO -> "https://livo.ge/favicon.ico"
    FlatPlatform.BINEBI -> "https://binebi.ge/favicon.ico"
    FlatPlatform.KRISHA ->
        "https://krisha.kz/static/frontend/favicons/apple-touch-icon.png"

    FlatPlatform.OLX_KZ -> "https://www.olx.kz/favicon.ico"
    FlatPlatform.KN -> "https://www.kn.kz/favicon/favicon-32x32.png"
    FlatPlatform.IDEALISTA -> "https://www.idealista.com/favicon.ico"
    FlatPlatform.FOTOCASA ->
        "https://frtassets.fotocasa.es/statics/img/favicon-96x96.png"

    FlatPlatform.PISOS -> "https://www.pisos.com/favicon.ico"
}
