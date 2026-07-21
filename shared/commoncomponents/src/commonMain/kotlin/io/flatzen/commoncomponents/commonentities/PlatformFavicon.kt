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
    FlatPlatform.FOTOCASA ->
        "https://frtassets.fotocasa.es/statics/img/favicon-96x96.png"
    FlatPlatform.PISOS -> "https://www.pisos.com/favicon.ico"
    FlatPlatform.IS24 -> "https://www.immobilienscout24.de/favicon.ico"
    FlatPlatform.IMMOWELT -> "https://www.immowelt.de/favicon.ico"
    FlatPlatform.KLEINANZEIGEN -> "https://www.kleinanzeigen.de/favicon.ico"
    FlatPlatform.EMLAKJET -> "https://www.emlakjet.com/favicon.ico"
    FlatPlatform.PROPERTY_FINDER -> "https://www.propertyfinder.ae/favicon.ico"
    FlatPlatform.DUBIZZLE -> "https://dubai.dubizzle.com/favicon.ico"
    FlatPlatform.OPENSOOQ -> "https://ae.opensooq.com/favicon.ico"
    FlatPlatform.PROPERTYHUB -> "https://propertyhub.in.th/favicon.ico"
    FlatPlatform.LIVINGINSIDER -> "https://www.livinginsider.com/favicon.ico"
    FlatPlatform.RENTHUB -> "https://www.renthub.in.th/favicon.ico"
    FlatPlatform.ZUMPER -> "https://www.zumper.com/favicon.ico"
    FlatPlatform.DABANG -> "https://www.dabangapp.com/static/favicon.ico"
    FlatPlatform.ZIGBANG -> "https://www.zigbang.com/favicon.ico"
    FlatPlatform.SUUMO -> "https://suumo.jp/front/img/favicon.ico"
    FlatPlatform.YAHOO_RE -> "https://realestate.yahoo.co.jp/favicon.ico"
    FlatPlatform.ATHOME -> "https://www.athome.co.jp/favicon.ico"
    FlatPlatform.FLATFOX ->
        "https://flatfox.ch/public/flatfox_website/favicons/favicon-32x32.4955d0c30454.png"
}
