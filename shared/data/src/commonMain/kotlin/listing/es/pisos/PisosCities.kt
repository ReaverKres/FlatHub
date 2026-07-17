package listing.es.pisos

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * pisos.com city path slugs. Prefer capital-level paths from tmp/es/api/pisos/NOTES.md.
 */
object PisosCities {
    fun citySlug(city: CityCode?): String = when (city) {
        null, CityCode.MADRID -> "madrid_capital_zona_urbana"
        CityCode.BARCELONA -> "barcelona_capital"
        CityCode.VALENCIA -> "valencia_capital_zona_urbana"
        CityCode.SEVILLA -> "sevilla_capital"
        CityCode.MALAGA -> "malaga_capital_zona_urbana"
        CityCode.ZARAGOZA -> "zaragoza_capital"
        else -> "madrid_capital_zona_urbana"
    }
}
