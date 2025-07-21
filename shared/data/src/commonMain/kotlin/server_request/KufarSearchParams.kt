package server_request

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class KufarSearchParams(
    @SerialName("cat")  val categoryId: Int = 1010,
    @SerialName("cur")  val currency: Currency = Currency.BYR,
    @SerialName("gtsy") val geoTag: String = "country-belarus~province-minsk~locality-minsk",
    @SerialName("lang") val language: Language = Language.RU,
    @SerialName("size") val pageSize: Int = 30,
    @SerialName("typ")  val dealType: DealType = DealType.LET,
    @SerialName("sort")  val sort: Sort = Sort.NEW_FIRST
)

enum class Sort(val paramName: String) {
    NEW_FIRST("lst.d")
}

@Serializable
enum class Currency {
    @SerialName("BYR") BYR,
    @SerialName("USD") USD,
    @SerialName("EUR") EUR
}

@Serializable
enum class Language {
    @SerialName("ru") RU,
    @SerialName("en") EN,
    @SerialName("be") BE
}

@Serializable
enum class DealType {
    @SerialName("let") LET,      // long-term rent
    @SerialName("sale") SALE,    // purchase
    @SerialName("day") DAY       // short-term rent
}