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
    ;

    /** Short label for filter UI / chips. */
    fun filterLabel(): String = when (this) {
        USD -> "$"
        EUR -> "€"
        BYR -> "BYN"
        PLN -> "PLN"
        GEL -> "GEL"
        KZT -> "KZT"
    }

    /**
     * Markets that store listing prices in [entities.AppFlat.priceByn]
     * (local currency until price model rename).
     * EUR included so Spain (and any EUR market) shows € as main via localIsMain.
     */
    fun usesLocalPriceField(): Boolean = when (this) {
        PLN, GEL, KZT, BYR, EUR -> true
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
}
