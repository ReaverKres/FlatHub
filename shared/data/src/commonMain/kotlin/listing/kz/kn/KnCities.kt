package listing.kz.kn

import io.flatzen.commoncomponents.commonentities.CityCode

/** kn.kz path city slugs. See tmp/kz/api/kn/NOTES.md. */
object KnCities {
    fun cityAlias(city: CityCode?): String = when (city) {
        null, CityCode.ALMATY -> "almaty"
        CityCode.ASTANA -> "astana"
        CityCode.SHYMKENT -> "shymkent"
        CityCode.KARAGANDA -> "karaganda"
        else -> "almaty"
    }
}
