package listing.kr

import io.flatzen.commoncomponents.commonentities.CityCode

/** City slugs for Dabang / Zigbang APIs. Sibling agents flesh out per-platform mapping. */
object KrCities {
    fun slug(city: CityCode?): String = when (city) {
        null, CityCode.SEOUL -> "seoul"
        CityCode.BUSAN -> "busan"
        CityCode.DAEGU -> "daegu"
        CityCode.INCHEON -> "incheon"
        CityCode.GWANGJU -> "gwangju"
        CityCode.DAEJEON -> "daejeon"
        CityCode.ULSAN -> "ulsan"
        CityCode.SEJONG -> "sejong"
        CityCode.SUWON -> "suwon"
        CityCode.CHANGWON -> "changwon"
        CityCode.JEONJU -> "jeonju"
        CityCode.CHEONGJU -> "cheongju"
        CityCode.CHUNCHEON -> "chuncheon"
        CityCode.JEJU -> "jeju"
        else -> "seoul"
    }
}
