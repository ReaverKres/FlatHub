package server_request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DomovitaRequest(
    @SerialName("location_sef_alias" ) val location: String,
    @SerialName("offset" ) val offset: Int?,
    @SerialName("limit" ) val limit: Int,
    @SerialName("price_max") val priceMax: Double? = null,
    @SerialName("price_min") val priceMin: Double? = null,
    @SerialName("price_currency_text") val currency: String?,
    @SerialName("price_type") val priceType: String? = null,
    @SerialName("rooms" ) val rooms: List<Int>?,
    @SerialName("metro_station_id" ) val metroStationIds: List<Int>?,
    // у домовиты нет сортировки ?
//    @SerialName("sort") val sort: String? = null, // Added sort parameter
//    @SerialName("individual") val isOwner: String? // "Собственник" for owner only
)