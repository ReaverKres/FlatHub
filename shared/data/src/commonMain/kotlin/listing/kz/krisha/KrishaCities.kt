package listing.kz.krisha

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * Krisha path aliases. See tmp/kz/api/krisha/NOTES.md.
 */
object KrishaCities {
    fun cityAlias(city: CityCode?): String = when (city) {
        null, CityCode.ALMATY -> "almaty"
        CityCode.ASTANA -> "astana"
        CityCode.SHYMKENT -> "shymkent"
        CityCode.KARAGANDA -> "karaganda"
        else -> "almaty"
    }
}
