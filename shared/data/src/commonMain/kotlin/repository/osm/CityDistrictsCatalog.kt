package repository.osm

import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.DistrictType
import io.flatzen.data.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.concurrent.Volatile

private const val DISTRICTS_RESOURCE = "files/by_city_districts.json"

@Serializable
private data class CityDistrictDto(
    val id: Long,
    val nameEn: String,
    val nameLocal: String,
    val coordinates: List<Coordinates>,
    val districtType: DistrictType,
)

object CityDistrictsCatalog {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var byCity: Map<String, List<OsmDistricts>> = emptyMap()

    suspend fun loadIfNeeded() {
        if (byCity.isNotEmpty()) return
        mutex.withLock {
            if (byCity.isNotEmpty()) return
            byCity = loadCatalog()
        }
    }

    fun districtsForCity(cityName: String): List<OsmDistricts> {
        byCity[cityName]?.let { return it }
        val normalized = normalizeCityName(cityName)
        return byCity.entries
            .firstOrNull { normalizeCityName(it.key) == normalized }
            ?.value
            .orEmpty()
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadCatalog(): Map<String, List<OsmDistricts>> {
        val text = Res.readBytes(DISTRICTS_RESOURCE).decodeToString()
        val raw = json.decodeFromString<Map<String, List<CityDistrictDto>>>(text)
        return raw.mapValues { (_, districts) ->
            districts.map { dto ->
                OsmDistricts(
                    id = dto.id,
                    nameEn = dto.nameEn,
                    nameLocal = dto.nameLocal,
                    coordinates = dto.coordinates,
                    districtType = dto.districtType,
                    isChecked = false,
                )
            }
        }
    }

    private fun normalizeCityName(name: String): String =
        name.lowercase()
            .replace('ё', 'е')
            .trim()
}
