package server_request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DomovitaRequest(
    @SerialName("location_sef_alias" ) val location: String,
    @SerialName("page" ) val page: Int = 1,
    @SerialName("limit" ) val limit: Int = 10,
    @SerialName("min_price_usd" ) val min_price_usd: Int? = null,
    @SerialName("max_price_usd" ) val max_price_usd: Int? = null,
    @SerialName("rooms" ) val rooms: List<Int>? = null,
    @SerialName("metro_ids" ) val metro_ids: List<Int>? = null,
    @SerialName("owner_type" ) val owner_type: String? = null // "Собственник" for owner only
)