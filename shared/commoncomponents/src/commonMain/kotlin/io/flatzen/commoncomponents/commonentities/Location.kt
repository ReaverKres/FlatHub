package io.flatzen.commoncomponents.commonentities

data class LocationCommon(
    val country: Country,
    val selectedCity: City
)

data class City(val cityCode: CityCode, val coordinates: Coordinates)

data class Country(val country: CountryCode, val allCities: List<City>)

enum class CountryCode {
    BY, PL, GE, KZ, ES, DE, AT, TR, AE, TH, US, KR, JP, CH;

    companion object {
        fun fromNetworkIso(iso: String?): CountryCode = when (iso?.uppercase()) {
            "BY" -> BY
            "PL" -> PL
            "GE" -> GE
            "KZ" -> KZ
            "ES" -> ES
            "DE" -> DE
            "AT" -> AT
            "TR" -> TR
            "AE" -> AE
            "TH" -> TH
            "US" -> US
            "KR" -> KR
            "JP" -> JP
            "CH" -> CH
            else -> BY
        }
    }
}

enum class CityCode {
    // Belarus
    MINSK, BREST, VITEBSK, GOMEL, GRODNO, MOGILEV,

    // Poland (major cities)
    WARSZAWA, KRAKOW, WROCLAW, POZNAN, GDANSK, LODZ,
    SZCZECIN, LUBLIN, BYDGOSZCZ, KATOWICE,

    // Georgia (MVP)
    TBILISI, BATUMI, KUTAISI, RUSTAVI,

    // Kazakhstan (MVP)
    ALMATY, ASTANA, SHYMKENT, KARAGANDA,

    // Spain (MVP)
    MADRID, BARCELONA, VALENCIA, SEVILLA, MALAGA, ZARAGOZA,

    // Germany (MVP)
    BERLIN, MUENCHEN, HAMBURG, KOELN, FRANKFURT, STUTTGART, DUESSELDORF, LEIPZIG,

    // Austria (MVP)
    WIEN, GRAZ, LINZ, SALZBURG, INNSBRUCK, KLAGENFURT, VILLACH, WELS, ST_POELTEN, DORNBIRN,

    // Turkey (MVP)
    ISTANBUL, ANKARA, IZMIR, ANTALYA, BURSA, ADANA, GAZIANTEP, KONYA,

    // UAE (MVP)
    DUBAI, ABU_DHABI, SHARJAH, AJMAN, AL_AIN, RAS_AL_KHAIMAH, FUJAIRAH, UMM_AL_QUWAIN,

    // Thailand (MVP)
    BANGKOK, PHUKET, CHIANG_MAI, PATTAYA, HUA_HIN, KOH_SAMUI,

    // United States (MVP top rent markets)
    NEW_YORK, LOS_ANGELES, CHICAGO, HOUSTON, MIAMI, SEATTLE, SAN_FRANCISCO, AUSTIN, BOSTON, DENVER,

    // South Korea (MVP)
    SEOUL, BUSAN, DAEGU, INCHEON, GWANGJU, DAEJEON, ULSAN, SEJONG, SUWON, CHANGWON, JEONJU, CHEONGJU, CHUNCHEON, JEJU,

    // Japan (MVP)
    TOKYO, OSAKA, YOKOHAMA, NAGOYA, SAPPORO, FUKUOKA, KYOTO, KOBE, SENDAI, HIROSHIMA,

    // Switzerland (MVP)
    ZURICH, GENEVA, BASEL, BERN, LAUSANNE, WINTERTHUR, LUZERN, ST_GALLEN, LUGANO, BIEL,
}

/** Countries with a commercial subtype taxonomy (office/retail/…). */
fun CountryCode.hasCommercialPropertyTypeCatalog(): Boolean =
    this == CountryCode.BY || this == CountryCode.AE

/** Prefer SourceCapabilities.supportsCommercialPropertyTypes for UI gating. */
fun CountryCode.supportsCommercialPropertyTypeFilter(): Boolean = hasCommercialPropertyTypeCatalog()

/** US listings store area in square feet; other markets use m² (AE converts on ingest). */
fun CountryCode.usesSquareFeet(): Boolean = this == CountryCode.US

fun CountryCode.defaultCityCode(): CityCode = when (this) {
    CountryCode.BY -> CityCode.MINSK
    CountryCode.PL -> CityCode.WARSZAWA
    CountryCode.GE -> CityCode.TBILISI
    CountryCode.KZ -> CityCode.ALMATY
    CountryCode.ES -> CityCode.MADRID
    CountryCode.DE -> CityCode.BERLIN
    CountryCode.AT -> CityCode.WIEN
    CountryCode.TR -> CityCode.ISTANBUL
    CountryCode.AE -> CityCode.DUBAI
    CountryCode.TH -> CityCode.BANGKOK
    CountryCode.US -> CityCode.NEW_YORK
    CountryCode.KR -> CityCode.SEOUL
    CountryCode.JP -> CityCode.TOKYO
    CountryCode.CH -> CityCode.ZURICH
}

object Location {
    fun mapCityCodeToDomainName(cityCode: CityCode) = when (cityCode) {
        CityCode.MINSK -> "minsk"
        CityCode.BREST -> "brest"
        CityCode.VITEBSK -> "vitebsk"
        CityCode.GOMEL -> "gomel"
        CityCode.GRODNO -> "grodno"
        CityCode.MOGILEV -> "mogilev"
        CityCode.WARSZAWA -> "warszawa"
        CityCode.KRAKOW -> "krakow"
        CityCode.WROCLAW -> "wroclaw"
        CityCode.POZNAN -> "poznan"
        CityCode.GDANSK -> "gdansk"
        CityCode.LODZ -> "lodz"
        CityCode.SZCZECIN -> "szczecin"
        CityCode.LUBLIN -> "lublin"
        CityCode.BYDGOSZCZ -> "bydgoszcz"
        CityCode.KATOWICE -> "katowice"
        CityCode.TBILISI -> "tbilisi"
        CityCode.BATUMI -> "batumi"
        CityCode.KUTAISI -> "kutaisi"
        CityCode.RUSTAVI -> "rustavi"
        CityCode.ALMATY -> "almaty"
        CityCode.ASTANA -> "astana"
        CityCode.SHYMKENT -> "shymkent"
        CityCode.KARAGANDA -> "karaganda"
        CityCode.MADRID -> "madrid"
        CityCode.BARCELONA -> "barcelona"
        CityCode.VALENCIA -> "valencia"
        CityCode.SEVILLA -> "sevilla"
        CityCode.MALAGA -> "malaga"
        CityCode.ZARAGOZA -> "zaragoza"
        CityCode.BERLIN -> "berlin"
        CityCode.MUENCHEN -> "muenchen"
        CityCode.HAMBURG -> "hamburg"
        CityCode.KOELN -> "koeln"
        CityCode.FRANKFURT -> "frankfurt"
        CityCode.STUTTGART -> "stuttgart"
        CityCode.DUESSELDORF -> "duesseldorf"
        CityCode.LEIPZIG -> "leipzig"
        CityCode.WIEN -> "wien"
        CityCode.GRAZ -> "graz"
        CityCode.LINZ -> "linz"
        CityCode.SALZBURG -> "salzburg"
        CityCode.INNSBRUCK -> "innsbruck"
        CityCode.KLAGENFURT -> "klagenfurt"
        CityCode.VILLACH -> "villach"
        CityCode.WELS -> "wels"
        CityCode.ST_POELTEN -> "st-poelten"
        CityCode.DORNBIRN -> "dornbirn"
        CityCode.ISTANBUL -> "istanbul"
        CityCode.ANKARA -> "ankara"
        CityCode.IZMIR -> "izmir"
        CityCode.ANTALYA -> "antalya"
        CityCode.BURSA -> "bursa"
        CityCode.ADANA -> "adana"
        CityCode.GAZIANTEP -> "gaziantep"
        CityCode.KONYA -> "konya"
        CityCode.DUBAI -> "dubai"
        CityCode.ABU_DHABI -> "abu-dhabi"
        CityCode.SHARJAH -> "sharjah"
        CityCode.AJMAN -> "ajman"
        CityCode.AL_AIN -> "al-ain"
        CityCode.RAS_AL_KHAIMAH -> "ras-al-khaimah"
        CityCode.FUJAIRAH -> "fujairah"
        CityCode.UMM_AL_QUWAIN -> "umm-al-quwain"
        CityCode.BANGKOK -> "bangkok"
        CityCode.PHUKET -> "phuket"
        CityCode.CHIANG_MAI -> "chiang-mai"
        CityCode.PATTAYA -> "pattaya"
        CityCode.HUA_HIN -> "hua-hin"
        CityCode.KOH_SAMUI -> "koh-samui"
        CityCode.NEW_YORK -> "new-york"
        CityCode.LOS_ANGELES -> "los-angeles"
        CityCode.CHICAGO -> "chicago"
        CityCode.HOUSTON -> "houston"
        CityCode.MIAMI -> "miami"
        CityCode.SEATTLE -> "seattle"
        CityCode.SAN_FRANCISCO -> "san-francisco"
        CityCode.AUSTIN -> "austin"
        CityCode.BOSTON -> "boston"
        CityCode.DENVER -> "denver"
        CityCode.SEOUL -> "seoul"
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
        CityCode.TOKYO -> "tokyo"
        CityCode.OSAKA -> "osaka"
        CityCode.YOKOHAMA -> "yokohama"
        CityCode.NAGOYA -> "nagoya"
        CityCode.SAPPORO -> "sapporo"
        CityCode.FUKUOKA -> "fukuoka"
        CityCode.KYOTO -> "kyoto"
        CityCode.KOBE -> "kobe"
        CityCode.SENDAI -> "sendai"
        CityCode.HIROSHIMA -> "hiroshima"
        CityCode.ZURICH -> "zurich"
        CityCode.GENEVA -> "geneva"
        CityCode.BASEL -> "basel"
        CityCode.BERN -> "bern"
        CityCode.LAUSANNE -> "lausanne"
        CityCode.WINTERTHUR -> "winterthur"
        CityCode.LUZERN -> "luzern"
        CityCode.ST_GALLEN -> "st-gallen"
        CityCode.LUGANO -> "lugano"
        CityCode.BIEL -> "biel"
    }
}
