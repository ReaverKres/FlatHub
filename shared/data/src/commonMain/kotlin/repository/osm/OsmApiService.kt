package repository.osm

import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.DistrictType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import server_response.osm.OsmCityIdResponse
import server_response.osm.OsmDistrictsResponse
import kotlin.random.Random

class OsmApiService(
    private val httpClient: HttpClient,
    private val filterRepository: FilterRepository
) {

    suspend fun findCityId(cityName: String): Long? {
        val query = """
            [out:json][timeout:25];
            (
                relation[name="$cityName"]["boundary"="administrative"];
                relation["name:ru"="$cityName"]["boundary"="administrative"];
            );
            out ids;
        """.trimIndent()

        return try {
            val response = executeQuery<OsmCityIdResponse>(query)
            response.elements?.firstOrNull()?.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCityDistricts(cityId: Long): List<OsmDistricts> {
        val areaId = 3600000000 + cityId
        val query = """
        [out:json][timeout:25];
        area($areaId)->.city;
        (
            relation[boundary="administrative"][admin_level="9"](area.city);
            relation[place="suburb"](area.city);
            way[place="suburb"](area.city);
        );
        out geom;
    """.trimIndent()

        return try {
            val response = executeQuery<OsmDistrictsResponse>(query)
            parseDistricts(response)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend inline fun <reified T> executeQuery(query: String): T {
        val encodedQuery = query.replace("\n", "")
        val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"

        val response: HttpResponse = httpClient.get(url)
        val jsonString = response.bodyAsText()

        return Json.decodeFromString(jsonString)
    }

    private fun parseDistricts(response: OsmDistrictsResponse): List<OsmDistricts> {
        val filter = filterRepository.lastFilter()
        return response.elements?.mapNotNull  { element ->
            val coordinates = mutableListOf<Coordinates>()

            element?.members?.forEach { member ->
                if (member?.type == "way" && member.role == "outer") {
                    member.geometry?.forEach { point ->
                        if (point?.lat != null && point.lon != null) {
                            coordinates.add(Coordinates(point.lat, point.lon))
                        }
                    }
                }
            }

            val nameEn = element?.tags?.nameEn
            val nameRu = element?.tags?.nameRu
            if (nameEn.isNullOrEmpty() && nameRu.isNullOrEmpty() || coordinates.isEmpty()) {
                return@mapNotNull null
            }

            val isChecked = filter.districtsArea.find { it.id == element.id && it.isChecked } != null

            OsmDistricts(
                id = element.id ?: Random.nextLong(),
                nameEn = nameEn ?: nameRu ?: "Unknown",
                nameLocal = nameRu ?: nameEn ?: "Unknown",
                coordinates = coordinates,
                districtType = if(element.tags.boundary == "administrative") DistrictType.ADMINISTRATIVE else DistrictType.SUBURB,
                isChecked = isChecked
            )
        } ?: emptyList()
    }
}