package server_response.osm


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OsmCityIdResponse(
    @SerialName("elements")
    val elements: List<Element?>?,
    @SerialName("generator")
    val generator: String?,
    @SerialName("osm3s")
    val osm3s: Osm3s?,
    @SerialName("version")
    val version: Double?
) {
    @Serializable
    data class Element(
        @SerialName("id")
        val id: Long?,
        @SerialName("type")
        val type: String?
    )

    @Serializable
    data class Osm3s(
        @SerialName("copyright")
        val copyright: String?,
        @SerialName("timestamp_osm_base")
        val timestampOsmBase: String?
    )
}