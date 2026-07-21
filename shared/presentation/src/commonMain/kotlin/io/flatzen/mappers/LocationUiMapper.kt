package io.flatzen.mappers

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.CountryCode

object LocationUiMapper {
    /**
     * @param latinName English/Latin name used for search (always searchable).
     * @param districtsCatalogKey Key in `*_city_districts.json` — do not localize.
     */
    data class UiCityItem(
        val code: CityCode,
        val latinName: String,
        val districtsCatalogKey: String,
        val coordinates: Coordinates,
    ) {
        /** Legacy alias for districts / filter chips until UI resolves localized labels. */
        val displayName: String get() = districtsCatalogKey
    }

    data class UiCountryItem(
        val code: CountryCode,
        val latinName: String,
    )

    val minskUiItem =
        UiCityItem(CityCode.MINSK, "Minsk", "Минск", Coordinates(53.902147, 27.561388))
    val warszawaUiItem =
        UiCityItem(CityCode.WARSZAWA, "Warsaw", "Warszawa", Coordinates(52.2297, 21.0122))
    val tbilisiUiItem =
        UiCityItem(CityCode.TBILISI, "Tbilisi", "Tbilisi", Coordinates(41.7151, 44.8271))
    val almatyUiItem =
        UiCityItem(CityCode.ALMATY, "Almaty", "Алматы", Coordinates(43.2389, 76.9455))
    val madridUiItem =
        UiCityItem(CityCode.MADRID, "Madrid", "Madrid", Coordinates(40.4168, -3.7038))
    val berlinUiItem =
        UiCityItem(CityCode.BERLIN, "Berlin", "Berlin", Coordinates(52.5200, 13.4050))
    val istanbulUiItem =
        UiCityItem(CityCode.ISTANBUL, "Istanbul", "İstanbul", Coordinates(41.0082, 28.9784))
    val dubaiUiItem = UiCityItem(CityCode.DUBAI, "Dubai", "Dubai", Coordinates(25.2048, 55.2708))
    val bangkokUiItem =
        UiCityItem(CityCode.BANGKOK, "Bangkok", "Bangkok", Coordinates(13.7563, 100.5018))
    val newYorkUiItem =
        UiCityItem(CityCode.NEW_YORK, "New York", "New York", Coordinates(40.7128, -74.0060))
    val seoulUiItem =
        UiCityItem(CityCode.SEOUL, "Seoul", "Seoul", Coordinates(37.5665, 126.9780))
    val tokyoUiItem =
        UiCityItem(CityCode.TOKYO, "Tokyo", "Tokyo", Coordinates(35.6762, 139.6503))
    val zurichUiItem =
        UiCityItem(CityCode.ZURICH, "Zurich", "Zürich", Coordinates(47.3769, 8.5417))
    val wienUiItem =
        UiCityItem(CityCode.WIEN, "Vienna", "Wien", Coordinates(48.2082, 16.3738))

    fun countries(): List<UiCountryItem> = listOf(
        UiCountryItem(CountryCode.BY, "Belarus"),
        UiCountryItem(CountryCode.PL, "Poland"),
        UiCountryItem(CountryCode.GE, "Georgia"),
        UiCountryItem(CountryCode.KZ, "Kazakhstan"),
        UiCountryItem(CountryCode.ES, "Spain"),
        UiCountryItem(CountryCode.DE, "Germany"),
        UiCountryItem(CountryCode.AT, "Austria"),
        UiCountryItem(CountryCode.TR, "Turkey"),
        UiCountryItem(CountryCode.AE, "UAE"),
        UiCountryItem(CountryCode.TH, "Thailand"),
        UiCountryItem(CountryCode.US, "United States"),
        UiCountryItem(CountryCode.KR, "South Korea"),
        UiCountryItem(CountryCode.JP, "Japan"),
        UiCountryItem(CountryCode.CH, "Switzerland"),
    )

    fun defaultCity(country: CountryCode): UiCityItem = when (country) {
        CountryCode.PL -> warszawaUiItem
        CountryCode.GE -> tbilisiUiItem
        CountryCode.KZ -> almatyUiItem
        CountryCode.ES -> madridUiItem
        CountryCode.DE -> berlinUiItem
        CountryCode.AT -> wienUiItem
        CountryCode.TR -> istanbulUiItem
        CountryCode.AE -> dubaiUiItem
        CountryCode.TH -> bangkokUiItem
        CountryCode.US -> newYorkUiItem
        CountryCode.KR -> seoulUiItem
        CountryCode.JP -> tokyoUiItem
        CountryCode.CH -> zurichUiItem
        CountryCode.BY -> minskUiItem
    }

    fun cities(country: CountryCode = CountryCode.BY): List<UiCityItem> = when (country) {
        CountryCode.BY -> listOf(
            minskUiItem,
            UiCityItem(CityCode.BREST, "Brest", "Брест", Coordinates(52.093825, 23.684889)),
            UiCityItem(CityCode.VITEBSK, "Vitebsk", "Витебск", Coordinates(55.184217, 30.202878)),
            UiCityItem(CityCode.GOMEL, "Gomel", "Гомель", Coordinates(52.424160, 31.014281)),
            UiCityItem(CityCode.GRODNO, "Grodno", "Гродно", Coordinates(53.677839, 23.829529)),
            UiCityItem(CityCode.MOGILEV, "Mogilev", "Могилёв", Coordinates(53.894548, 30.330654)),
        )

        CountryCode.PL -> listOf(
            warszawaUiItem,
            UiCityItem(CityCode.KRAKOW, "Krakow", "Kraków", Coordinates(50.0647, 19.9450)),
            UiCityItem(CityCode.WROCLAW, "Wroclaw", "Wrocław", Coordinates(51.1079, 17.0385)),
            UiCityItem(CityCode.POZNAN, "Poznan", "Poznań", Coordinates(52.4064, 16.9252)),
            UiCityItem(CityCode.GDANSK, "Gdansk", "Gdańsk", Coordinates(54.3520, 18.6466)),
            UiCityItem(CityCode.LODZ, "Lodz", "Łódź", Coordinates(51.7592, 19.4560)),
            UiCityItem(CityCode.SZCZECIN, "Szczecin", "Szczecin", Coordinates(53.4285, 14.5528)),
            UiCityItem(CityCode.LUBLIN, "Lublin", "Lublin", Coordinates(51.2465, 22.5684)),
            UiCityItem(CityCode.BYDGOSZCZ, "Bydgoszcz", "Bydgoszcz", Coordinates(53.1235, 18.0084)),
            UiCityItem(CityCode.KATOWICE, "Katowice", "Katowice", Coordinates(50.2649, 19.0238)),
        )

        CountryCode.GE -> listOf(
            tbilisiUiItem,
            UiCityItem(CityCode.BATUMI, "Batumi", "Batumi", Coordinates(41.6168, 41.6367)),
            UiCityItem(CityCode.KUTAISI, "Kutaisi", "Kutaisi", Coordinates(42.2679, 42.6946)),
            UiCityItem(CityCode.RUSTAVI, "Rustavi", "Rustavi", Coordinates(41.5495, 44.9932)),
        )

        CountryCode.KZ -> listOf(
            almatyUiItem,
            UiCityItem(CityCode.ASTANA, "Astana", "Астана", Coordinates(51.1694, 71.4491)),
            UiCityItem(CityCode.SHYMKENT, "Shymkent", "Шымкент", Coordinates(42.3417, 69.5901)),
            UiCityItem(CityCode.KARAGANDA, "Karaganda", "Қарағанды", Coordinates(49.8047, 73.1094)),
        )

        CountryCode.ES -> listOf(
            madridUiItem,
            UiCityItem(CityCode.BARCELONA, "Barcelona", "Barcelona", Coordinates(41.3874, 2.1686)),
            UiCityItem(CityCode.VALENCIA, "Valencia", "Valencia", Coordinates(39.4699, -0.3763)),
            UiCityItem(CityCode.SEVILLA, "Sevilla", "Sevilla", Coordinates(37.3891, -5.9845)),
            UiCityItem(CityCode.MALAGA, "Malaga", "Málaga", Coordinates(36.7213, -4.4214)),
            UiCityItem(CityCode.ZARAGOZA, "Zaragoza", "Zaragoza", Coordinates(41.6488, -0.8891)),
        )

        CountryCode.DE -> listOf(
            berlinUiItem,
            UiCityItem(CityCode.MUENCHEN, "Munich", "München", Coordinates(48.1351, 11.5820)),
            UiCityItem(CityCode.HAMBURG, "Hamburg", "Hamburg", Coordinates(53.5511, 9.9937)),
            UiCityItem(CityCode.KOELN, "Cologne", "Köln", Coordinates(50.9375, 6.9603)),
            UiCityItem(
                CityCode.FRANKFURT,
                "Frankfurt",
                "Frankfurt am Main",
                Coordinates(50.1109, 8.6821)
            ),
            UiCityItem(CityCode.STUTTGART, "Stuttgart", "Stuttgart", Coordinates(48.7758, 9.1829)),
            UiCityItem(
                CityCode.DUESSELDORF,
                "Dusseldorf",
                "Düsseldorf",
                Coordinates(51.2277, 6.7735)
            ),
            UiCityItem(CityCode.LEIPZIG, "Leipzig", "Leipzig", Coordinates(51.3397, 12.3731)),
        )

        CountryCode.AT -> listOf(
            wienUiItem,
            UiCityItem(CityCode.GRAZ, "Graz", "Graz", Coordinates(47.0707, 15.4395)),
            UiCityItem(CityCode.LINZ, "Linz", "Linz", Coordinates(48.3069, 14.2858)),
            UiCityItem(CityCode.SALZBURG, "Salzburg", "Salzburg", Coordinates(47.8095, 13.0550)),
            UiCityItem(CityCode.INNSBRUCK, "Innsbruck", "Innsbruck", Coordinates(47.2692, 11.4041)),
            UiCityItem(
                CityCode.KLAGENFURT,
                "Klagenfurt",
                "Klagenfurt",
                Coordinates(46.6247, 14.3053)
            ),
            UiCityItem(CityCode.VILLACH, "Villach", "Villach", Coordinates(46.6111, 13.8558)),
            UiCityItem(CityCode.WELS, "Wels", "Wels", Coordinates(48.1575, 14.0289)),
            UiCityItem(
                CityCode.ST_POELTEN,
                "St. Pölten",
                "St. Pölten",
                Coordinates(48.2047, 15.6256)
            ),
            UiCityItem(CityCode.DORNBIRN, "Dornbirn", "Dornbirn", Coordinates(47.4124, 9.7438)),
        )

        CountryCode.TR -> listOf(
            istanbulUiItem,
            UiCityItem(CityCode.ANKARA, "Ankara", "Ankara", Coordinates(39.9334, 32.8597)),
            UiCityItem(CityCode.IZMIR, "Izmir", "İzmir", Coordinates(38.4237, 27.1428)),
            UiCityItem(CityCode.ANTALYA, "Antalya", "Antalya", Coordinates(36.8969, 30.7133)),
            UiCityItem(CityCode.BURSA, "Bursa", "Bursa", Coordinates(40.1885, 29.0610)),
            UiCityItem(CityCode.ADANA, "Adana", "Adana", Coordinates(37.0000, 35.3213)),
            UiCityItem(CityCode.GAZIANTEP, "Gaziantep", "Gaziantep", Coordinates(37.0662, 37.3833)),
            UiCityItem(CityCode.KONYA, "Konya", "Konya", Coordinates(37.8746, 32.4932)),
        )

        CountryCode.AE -> listOf(
            dubaiUiItem,
            UiCityItem(CityCode.ABU_DHABI, "Abu Dhabi", "Abu Dhabi", Coordinates(24.4539, 54.3773)),
            UiCityItem(CityCode.SHARJAH, "Sharjah", "Sharjah", Coordinates(25.3463, 55.4209)),
            UiCityItem(CityCode.AJMAN, "Ajman", "Ajman", Coordinates(25.4052, 55.5136)),
            UiCityItem(CityCode.AL_AIN, "Al Ain", "Al Ain", Coordinates(24.2075, 55.7447)),
            UiCityItem(
                CityCode.RAS_AL_KHAIMAH,
                "Ras Al Khaimah",
                "Ras Al Khaimah",
                Coordinates(25.7895, 55.9432)
            ),
            UiCityItem(CityCode.FUJAIRAH, "Fujairah", "Fujairah", Coordinates(25.1288, 56.3265)),
            UiCityItem(
                CityCode.UMM_AL_QUWAIN,
                "Umm Al Quwain",
                "Umm Al Quwain",
                Coordinates(25.5647, 55.5552)
            ),
        )

        CountryCode.TH -> listOf(
            bangkokUiItem,
            UiCityItem(CityCode.PHUKET, "Phuket", "Phuket", Coordinates(7.8804, 98.3923)),
            UiCityItem(
                CityCode.CHIANG_MAI,
                "Chiang Mai",
                "Chiang Mai",
                Coordinates(18.7883, 98.9853)
            ),
            UiCityItem(CityCode.PATTAYA, "Pattaya", "Pattaya", Coordinates(12.9236, 100.8825)),
            UiCityItem(CityCode.HUA_HIN, "Hua Hin", "Hua Hin", Coordinates(12.5684, 99.9577)),
            UiCityItem(CityCode.KOH_SAMUI, "Koh Samui", "Koh Samui", Coordinates(9.5120, 100.0136)),
        )

        CountryCode.US -> listOf(
            newYorkUiItem,
            UiCityItem(
                CityCode.LOS_ANGELES,
                "Los Angeles",
                "Los Angeles",
                Coordinates(34.0522, -118.2437)
            ),
            UiCityItem(CityCode.CHICAGO, "Chicago", "Chicago", Coordinates(41.8781, -87.6298)),
            UiCityItem(CityCode.HOUSTON, "Houston", "Houston", Coordinates(29.7604, -95.3698)),
            UiCityItem(CityCode.MIAMI, "Miami", "Miami", Coordinates(25.7617, -80.1918)),
            UiCityItem(CityCode.SEATTLE, "Seattle", "Seattle", Coordinates(47.6062, -122.3321)),
            UiCityItem(
                CityCode.SAN_FRANCISCO,
                "San Francisco",
                "San Francisco",
                Coordinates(37.7749, -122.4194)
            ),
            UiCityItem(CityCode.AUSTIN, "Austin", "Austin", Coordinates(30.2672, -97.7431)),
            UiCityItem(CityCode.BOSTON, "Boston", "Boston", Coordinates(42.3601, -71.0589)),
            UiCityItem(CityCode.DENVER, "Denver", "Denver", Coordinates(39.7392, -104.9903)),
        )

        CountryCode.KR -> listOf(
            // districtsCatalogKey "Seoul" matches future seoul_city_districts.json catalog key
            seoulUiItem,
            UiCityItem(CityCode.BUSAN, "Busan", "Busan", Coordinates(35.1796, 129.0756)),
            UiCityItem(CityCode.DAEGU, "Daegu", "Daegu", Coordinates(35.8714, 128.6014)),
            UiCityItem(CityCode.INCHEON, "Incheon", "Incheon", Coordinates(37.4563, 126.7052)),
            UiCityItem(CityCode.GWANGJU, "Gwangju", "Gwangju", Coordinates(35.1595, 126.8526)),
            UiCityItem(CityCode.DAEJEON, "Daejeon", "Daejeon", Coordinates(36.3504, 127.3845)),
            UiCityItem(CityCode.ULSAN, "Ulsan", "Ulsan", Coordinates(35.5384, 129.3114)),
            UiCityItem(CityCode.SEJONG, "Sejong", "Sejong", Coordinates(36.4800, 127.2890)),
            UiCityItem(CityCode.SUWON, "Suwon", "Suwon", Coordinates(37.2636, 127.0286)),
            UiCityItem(CityCode.CHANGWON, "Changwon", "Changwon", Coordinates(35.2284, 128.6811)),
            UiCityItem(CityCode.JEONJU, "Jeonju", "Jeonju", Coordinates(35.8242, 127.1480)),
            UiCityItem(CityCode.CHEONGJU, "Cheongju", "Cheongju", Coordinates(36.6424, 127.4890)),
            UiCityItem(
                CityCode.CHUNCHEON,
                "Chuncheon",
                "Chuncheon",
                Coordinates(37.8813, 127.7298)
            ),
            UiCityItem(CityCode.JEJU, "Jeju", "Jeju", Coordinates(33.4996, 126.5312)),
        )

        CountryCode.JP -> listOf(
            // districtsCatalogKey "Tokyo" matches jp_city_districts.json catalog key
            tokyoUiItem,
            UiCityItem(CityCode.OSAKA, "Osaka", "Osaka", Coordinates(34.6937, 135.5023)),
            UiCityItem(CityCode.YOKOHAMA, "Yokohama", "Yokohama", Coordinates(35.4437, 139.6380)),
            UiCityItem(CityCode.NAGOYA, "Nagoya", "Nagoya", Coordinates(35.1815, 136.9066)),
            UiCityItem(CityCode.SAPPORO, "Sapporo", "Sapporo", Coordinates(43.0618, 141.3545)),
            UiCityItem(CityCode.FUKUOKA, "Fukuoka", "Fukuoka", Coordinates(33.5904, 130.4017)),
            UiCityItem(CityCode.KYOTO, "Kyoto", "Kyoto", Coordinates(35.0116, 135.7681)),
            UiCityItem(CityCode.KOBE, "Kobe", "Kobe", Coordinates(34.6901, 135.1955)),
            UiCityItem(CityCode.SENDAI, "Sendai", "Sendai", Coordinates(38.2682, 140.8694)),
            UiCityItem(
                CityCode.HIROSHIMA,
                "Hiroshima",
                "Hiroshima",
                Coordinates(34.3853, 132.4553)
            ),
        )

        CountryCode.CH -> listOf(
            zurichUiItem,
            UiCityItem(CityCode.GENEVA, "Geneva", "Genève", Coordinates(46.2044, 6.1432)),
            UiCityItem(CityCode.BASEL, "Basel", "Basel", Coordinates(47.5596, 7.5886)),
            UiCityItem(CityCode.BERN, "Bern", "Bern", Coordinates(46.9480, 7.4474)),
            UiCityItem(CityCode.LAUSANNE, "Lausanne", "Lausanne", Coordinates(46.5197, 6.6323)),
            UiCityItem(
                CityCode.WINTERTHUR,
                "Winterthur",
                "Winterthur",
                Coordinates(47.5005, 8.7245),
            ),
            UiCityItem(CityCode.LUZERN, "Lucerne", "Luzern", Coordinates(47.0502, 8.3093)),
            UiCityItem(
                CityCode.ST_GALLEN,
                "St. Gallen",
                "St. Gallen",
                Coordinates(47.4245, 9.3767),
            ),
            UiCityItem(CityCode.LUGANO, "Lugano", "Lugano", Coordinates(46.0037, 8.9511)),
            UiCityItem(CityCode.BIEL, "Biel", "Biel/Bienne", Coordinates(47.1368, 7.2468)),
        )
    }

    fun cities(): List<UiCityItem> = cities(CountryCode.BY)

    fun allCities(): List<UiCityItem> =
        CountryCode.entries.flatMap { cities(it) }

    fun findSelectedCity(cityCode: CityCode): UiCityItem =
        allCities().find { it.code == cityCode } ?: minskUiItem

    fun countryForCity(cityCode: CityCode): CountryCode =
        when (cityCode) {
            in cities(CountryCode.PL).map { it.code } -> CountryCode.PL
            in cities(CountryCode.GE).map { it.code } -> CountryCode.GE
            in cities(CountryCode.KZ).map { it.code } -> CountryCode.KZ
            in cities(CountryCode.ES).map { it.code } -> CountryCode.ES
            in cities(CountryCode.DE).map { it.code } -> CountryCode.DE
            in cities(CountryCode.AT).map { it.code } -> CountryCode.AT
            in cities(CountryCode.TR).map { it.code } -> CountryCode.TR
            in cities(CountryCode.AE).map { it.code } -> CountryCode.AE
            in cities(CountryCode.TH).map { it.code } -> CountryCode.TH
            in cities(CountryCode.US).map { it.code } -> CountryCode.US
            in cities(CountryCode.KR).map { it.code } -> CountryCode.KR
            in cities(CountryCode.JP).map { it.code } -> CountryCode.JP
            in cities(CountryCode.CH).map { it.code } -> CountryCode.CH
            else -> CountryCode.BY
        }

    /** Latin/English catalog name (not localized). Prefer UI string resources for display. */
    fun countryLatinName(code: CountryCode): String =
        countries().find { it.code == code }?.latinName ?: code.name

    fun countryDisplayName(code: CountryCode): String = countryLatinName(code)

    fun matchesQuery(
        query: String,
        latinName: String,
        localizedName: String,
    ): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return latinName.lowercase().contains(q) || localizedName.lowercase().contains(q)
    }
}
