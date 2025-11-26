package server_request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Currency {
    @SerialName("BYR") BYR,
    @SerialName("USD") USD,
    @SerialName("EUR") EUR
}