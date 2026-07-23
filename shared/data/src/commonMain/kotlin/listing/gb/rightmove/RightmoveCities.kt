package listing.gb.rightmove

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Rightmove `locationIdentifier` values (`TYPE^id` from typeahead).
 * See tmp/gb/api/rightmove/NOTES.md and `typeahead_cities.json`.
 */
object RightmoveCities {
    fun locationIdentifier(city: CityCode?): String = when (city) {
        null, CityCode.LONDON -> "REGION^87490"
        CityCode.MANCHESTER -> "REGION^904"
        CityCode.BIRMINGHAM -> "REGION^162"
        CityCode.LEEDS -> "REGION^787"
        CityCode.GLASGOW -> "REGION^550"
        CityCode.EDINBURGH -> "REGION^475"
        CityCode.BRISTOL -> "REGION^219"
        CityCode.LIVERPOOL -> "REGION^813"
        CityCode.NEWCASTLE -> "REGION^984"
        CityCode.SHEFFIELD -> "REGION^1195"
        else -> locationIdentifier(CityCode.LONDON)
    }
}
