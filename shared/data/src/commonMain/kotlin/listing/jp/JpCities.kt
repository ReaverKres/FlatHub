package listing.jp

import io.flatzen.commoncomponents.commonentities.CityCode

/** Shared prefecture / region IDs for JP listing sources. See tmp/jp/api site NOTES.md files. */
object JpCities {
    data class CityIds(
        /** JIS prefecture code (2-digit), e.g. 13 = Tokyo */
        val prefCode: String,
        /** SUUMO macro region `ar` query param */
        val suumoAr: String,
        /** at home URL slug / BFF `prefectureRoman` */
        val athomeSlug: String,
        /** Yahoo!不動産 macro region `lc` path segment */
        val yahooLc: String,
        /** Yahoo municipality JIS code when searching a specific city (null = prefecture-wide) */
        val yahooGeo: String? = null,
    )

    fun ids(city: CityCode?): CityIds = when (city) {
        null, CityCode.TOKYO -> CityIds("13", "030", "tokyo", "03")
        CityCode.OSAKA -> CityIds("27", "060", "osaka", "06", "27100")
        CityCode.YOKOHAMA -> CityIds("14", "030", "kanagawa", "03", "14100")
        CityCode.NAGOYA -> CityIds("23", "020", "aichi", "05", "23100")
        CityCode.SAPPORO -> CityIds("01", "010", "hokkaido", "01", "01100")
        CityCode.FUKUOKA -> CityIds("40", "080", "fukuoka", "09", "40130")
        CityCode.KYOTO -> CityIds("26", "060", "kyoto", "06", "26100")
        CityCode.KOBE -> CityIds("28", "060", "hyogo", "06", "28100")
        CityCode.SENDAI -> CityIds("04", "040", "miyagi", "02", "04100")
        CityCode.HIROSHIMA -> CityIds("34", "070", "hiroshima", "07", "34100")
        else -> ids(CityCode.TOKYO)
    }
}
