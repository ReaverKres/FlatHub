package listing.it.trovacasa

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * TrovaCasa.it city URL slugs — `/case-in-affitto/{slug}`, `/case-in-vendita/{slug}`.
 * See tmp/it/api/trovacasa/NOTES.md.
 */
object TrovacasaCities {
    fun citySlug(city: CityCode?): String? = when (city) {
        CityCode.ROMA -> "roma"
        CityCode.MILANO -> "milano"
        CityCode.NAPOLI -> "napoli"
        CityCode.TORINO -> "torino"
        CityCode.FIRENZE -> "firenze"
        CityCode.BOLOGNA -> "bologna"
        else -> null
    }
}
