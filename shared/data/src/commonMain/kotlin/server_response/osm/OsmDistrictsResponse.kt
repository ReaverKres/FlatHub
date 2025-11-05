package server_response.osm


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OsmDistrictsResponse(
    @SerialName("elements")
    val elements: List<Element?>?,
    @SerialName("generator")
    val generator: String?,
    @SerialName("osm3s")
    val osm3s: OsmCityIdResponse.Osm3s?,
    @SerialName("version")
    val version: Double?
) {
    @Serializable
    data class Element(
        @SerialName("bounds")
        val bounds: Bounds?,
        @SerialName("id")
        val id: Long?,
        @SerialName("members")
        val members: List<Member?>?,
        @SerialName("tags")
        val tags: Tags?,
        @SerialName("type")
        val type: String?
    ) {
        @Serializable
        data class Bounds(
            @SerialName("maxlat")
            val maxlat: Double?,
            @SerialName("maxlon")
            val maxlon: Double?,
            @SerialName("minlat")
            val minlat: Double?,
            @SerialName("minlon")
            val minlon: Double?
        )

        @Serializable
        data class Member(
            @SerialName("geometry")
            val geometry: List<Geometry?>?,
            @SerialName("lat")
            val lat: Double?,
            @SerialName("lon")
            val lon: Double?,
            @SerialName("ref")
            val ref: Long?,
            @SerialName("role")
            val role: String?,
            @SerialName("type")
            val type: String?
        ) {
            @Serializable
            data class Geometry(
                @SerialName("lat")
                val lat: Double?,
                @SerialName("lon")
                val lon: Double?
            )
        }

        @Serializable
        data class Tags(
            @SerialName("addr:city")
            val addrCity: String?,
            @SerialName("admin_level")
            val adminLevel: String?,
            @SerialName("boundary")
            val boundary: String?,
            @SerialName("int_name")
            val intName: String?,
            @SerialName("name")
            val name: String?,
            @SerialName("name:be")
            val nameBe: String?,
            @SerialName("name:en")
            val nameEn: String?,
            @SerialName("name:ru")
            val nameRu: String?,
            @SerialName("population")
            val population: String?,
            @SerialName("population:date")
            val populationDate: String?,
            @SerialName("population:man")
            val populationMan: String?,
            @SerialName("population:man:date")
            val populationManDate: String?,
            @SerialName("population:woman")
            val populationWoman: String?,
            @SerialName("population:woman:date")
            val populationWomanDate: String?,
            @SerialName("type")
            val type: String?,
            @SerialName("wikidata")
            val wikidata: String?,
            @SerialName("wikipedia")
            val wikipedia: String?
        )
    }
}