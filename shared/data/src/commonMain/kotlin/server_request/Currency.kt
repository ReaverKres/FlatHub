package server_request

import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CountryCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Currency {
    @SerialName("BYR") BYR,
    @SerialName("USD") USD,
    @SerialName("EUR")
    EUR,
    @SerialName("PLN")
    PLN,
    @SerialName("GEL")
    GEL,
    @SerialName("KZT")
    KZT,
    @SerialName("TRY")
    TRY,
    @SerialName("AED")
    AED,
    @SerialName("THB")
    THB,

    @SerialName("KRW")
    KRW,

    @SerialName("JPY")
    JPY,
    ;

    /** Short label for filter UI / chips. */
    fun filterLabel(): String = when (this) {
        USD -> "$"
        EUR -> "€"
        BYR -> "BYN"
        PLN -> "PLN"
        GEL -> "GEL"
        KZT -> "KZT"
        TRY -> "₺"
        AED -> "AED"
        THB -> "฿"
        KRW -> "₩"
        JPY -> "¥"
    }

    /**
     * Whether filter/sort amounts for this currency are stored in [entities.AppFlat.mainPrice].
     * All markets use [entities.AppFlat.mainPrice] for filter and sort; this remains for
     * callers that branch on currency kind (e.g. filter UI labels).
     * USD markets (BY rent, US) store amounts in mainPrice; usesLocalPriceField is false for USD.
     */
    fun usesLocalPriceField(): Boolean = when (this) {
        PLN, GEL, KZT, BYR, EUR, TRY, AED, THB, KRW, JPY -> true
        USD -> false
    }
}

/** Filter price currency for the selected country / ad type. */
fun CountryCode.filterCurrency(adType: AdType): Currency = when (this) {
    CountryCode.BY -> if (adType is AdType.DAILY) Currency.BYR else Currency.USD
    CountryCode.PL -> Currency.PLN
    CountryCode.GE -> Currency.GEL
    CountryCode.KZ -> Currency.KZT
    CountryCode.ES -> Currency.EUR
    CountryCode.DE -> Currency.EUR
    CountryCode.TR -> Currency.TRY
    CountryCode.AE -> Currency.AED
    CountryCode.TH -> Currency.THB
    CountryCode.US -> Currency.USD
    CountryCode.KR -> Currency.KRW
    CountryCode.JP -> Currency.JPY
}
