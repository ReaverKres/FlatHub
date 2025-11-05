package server_response.osm


import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@OptIn(ExperimentalSerializationApi::class)
@JsonIgnoreUnknownKeys
@Serializable
data class OsmDistrictsResponse(
    @SerialName("elements")
    val elements: List<Element?>? = null,
    @SerialName("generator")
    val generator: String? = null,
    @SerialName("osm3s")
    val osm3s: OsmCityIdResponse.Osm3s? = null,
    @SerialName("version")
    val version: Double? = null
) {
    @JsonIgnoreUnknownKeys
    @Serializable
    data class Element(
        @SerialName("bounds")
        val bounds: Bounds? = null,
        @SerialName("id")
        val id: Long? = null,
        @SerialName("members")
        val members: List<Member?>? = null,
        @SerialName("tags")
        val tags: Tags? = null,
        @SerialName("type")
        val type: String? = null
    ) {
        @JsonIgnoreUnknownKeys
        @Serializable
        data class Bounds(
            @SerialName("maxlat")
            val maxlat: Double? = null,
            @SerialName("maxlon")
            val maxlon: Double? = null,
            @SerialName("minlat")
            val minlat: Double? = null,
            @SerialName("minlon")
            val minlon: Double? = null
        )

        @JsonIgnoreUnknownKeys
        @Serializable
        data class Member(
            @SerialName("geometry")
            val geometry: List<Geometry?>? = null,
            @SerialName("lat")
            val lat: Double? = null,
            @SerialName("lon")
            val lon: Double? = null,
            @SerialName("ref")
            val ref: Long? = null,
            @SerialName("role")
            val role: String? = null,
            @SerialName("type")
            val type: String? = null
        ) {
            @JsonIgnoreUnknownKeys
            @Serializable
            data class Geometry(
                @SerialName("lat")
                val lat: Double? = null,
                @SerialName("lon")
                val lon: Double? = null
            )
        }

        @JsonIgnoreUnknownKeys
        @Serializable
        data class Tags(
            @SerialName("addr:city")
            val addrCity: String? = null,
            @SerialName("admin_level")
            val adminLevel: String? = null,
            @SerialName("boundary")
            val boundary: String? = null,
            @SerialName("int_name")
            val intName: String? = null,
            @SerialName("name")
            val name: String? = null,
            @SerialName("name:be")
            val nameBe: String? = null,
            @SerialName("name:en")
            val nameEn: String? = null,
            @SerialName("name:ru")
            val nameRu: String? = null,
            @SerialName("population")
            val population: String? = null,
            @SerialName("population:date")
            val populationDate: String? = null,
            @SerialName("population:man")
            val populationMan: String? = null,
            @SerialName("population:man:date")
            val populationManDate: String? = null,
            @SerialName("population:woman")
            val populationWoman: String? = null,
            @SerialName("population:woman:date")
            val populationWomanDate: String? = null,
            @SerialName("type")
            val type: String? = null,
            @SerialName("wikidata")
            val wikidata: String? = null,
            @SerialName("wikipedia")
            val wikipedia: String? = null
        )
    }
}