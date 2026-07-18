package io.flatzen.commoncomponents.commonentities

data class LocationCommon(
    val country: Country,
    val selectedCity: City
)

data class City(val cityCode: CityCode, val coordinates: Coordinates)

data class Country(val country: CountryCode, val allCities: List<City>)

enum class CountryCode {
    BY, PL, GE, KZ, ES, DE, TR, AE, TH;

    companion object {
        fun fromNetworkIso(iso: String?): CountryCode = when (iso?.uppercase()) {
            "BY" -> BY
            "PL" -> PL
            "GE" -> GE
            "KZ" -> KZ
            "ES" -> ES
            "DE" -> DE
            "TR" -> TR
            "AE" -> AE
            "TH" -> TH
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

    // Turkey (MVP)
    ISTANBUL, ANKARA, IZMIR, ANTALYA, BURSA, ADANA, GAZIANTEP, KONYA,

    // UAE (MVP)
    DUBAI, ABU_DHABI, SHARJAH, AJMAN, AL_AIN, RAS_AL_KHAIMAH, FUJAIRAH, UMM_AL_QUWAIN,

    // Thailand (MVP)
    BANGKOK, PHUKET, CHIANG_MAI, PATTAYA, HUA_HIN, KOH_SAMUI,
}

/** Countries with a commercial subtype taxonomy (office/retail/…). */
fun CountryCode.hasCommercialPropertyTypeCatalog(): Boolean =
    this == CountryCode.BY || this == CountryCode.AE

/** Prefer SourceCapabilities.supportsCommercialPropertyTypes for UI gating. */
fun CountryCode.supportsCommercialPropertyTypeFilter(): Boolean = hasCommercialPropertyTypeCatalog()

fun CountryCode.defaultCityCode(): CityCode = when (this) {
    CountryCode.BY -> CityCode.MINSK
    CountryCode.PL -> CityCode.WARSZAWA
    CountryCode.GE -> CityCode.TBILISI
    CountryCode.KZ -> CityCode.ALMATY
    CountryCode.ES -> CityCode.MADRID
    CountryCode.DE -> CityCode.BERLIN
    CountryCode.TR -> CityCode.ISTANBUL
    CountryCode.AE -> CityCode.DUBAI
    CountryCode.TH -> CityCode.BANGKOK
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
    }
}
