package server_response.osm


import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class OsmCityIdResponse(
    @SerialName("elements")
    val elements: List<Element?>? = null,
    @SerialName("generator")
    val generator: String? = null,
    @SerialName("osm3s")
    val osm3s: Osm3s? = null,
    @SerialName("version")
    val version: Double? = null
) {

    @JsonIgnoreUnknownKeys
    @Serializable
    data class Element(
        @SerialName("id")
        val id: Long? = null,
        @SerialName("type")
        val type: String? = null
    )

    @JsonIgnoreUnknownKeys
    @Serializable
    data class Osm3s(
        @SerialName("copyright")
        val copyright: String? = null,
        @SerialName("timestamp_osm_base")
        val timestampOsmBase: String? = null
    )
}