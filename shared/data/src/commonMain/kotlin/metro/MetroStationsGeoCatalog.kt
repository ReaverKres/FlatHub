package metro

import entities.MetroLine
import entities.MetroStationGeo
import entities.MetroStationGeoDto
import entities.MetroStationNames
import entities.MetroStations
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.data.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.concurrent.Volatile

private const val MINSK_METRO_GEO_RESOURCE = "files/minsk_metro_stations.json"
private const val WARSAW_METRO_GEO_RESOURCE = "files/warsaw_metro_stations.json"

/** Synthetic metroId base for Warsaw stations (not used by BY site APIs). */
private const val WARSAW_METRO_ID_BASE = 10_000

object MetroStationsGeoCatalog {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var stations: List<MetroStationGeo> = emptyList()

    suspend fun loadIfNeeded() {
        if (stations.isNotEmpty()) return
        mutex.withLock {
            if (stations.isNotEmpty()) return
            stations = loadStations()
        }
    }

    fun allStations(): List<MetroStationGeo> = stations

    fun findNearestWithinMeters(
        coordinates: Coordinates,
        maxMeters: Double = 1_000.0,
        preferredLines: Set<MetroLine> = emptySet(),
    ): MetroStationGeo? {
        if (stations.isEmpty()) return null

        val candidates = stations.map { station ->
            station to distanceMeters(coordinates, station.coordinates)
        }.filter { it.second <= maxMeters }

        if (candidates.isEmpty()) return null

        val preferred = if (preferredLines.isNotEmpty()) {
            candidates.filter { it.first.line in preferredLines }
        } else {
            emptyList()
        }

        return (preferred.ifEmpty { candidates }).minByOrNull { it.second }?.first
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadStations(): List<MetroStationGeo> {
        val minsk = loadMinskStations()
        val warsaw = loadWarsawStations()
        return minsk + warsaw
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadMinskStations(): List<MetroStationGeo> {
        val text = Res.readBytes(MINSK_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalogByNormalizedName = MetroStations.allStationsRequest()
            .groupBy { normalizeMetroName(it.name) }

        return dtos.mapNotNull { dto ->
            if (dto.coordinates.size < 2) return@mapNotNull null
            val resolved =
                resolveStation(dto.name, catalogByNormalizedName) ?: return@mapNotNull null
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = resolved.name,
                line = resolved.line,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = resolved.metroId,
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadWarsawStations(): List<MetroStationGeo> {
        val text = Res.readBytes(WARSAW_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.WarsawMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.WarsawMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (WARSAW_METRO_ID_BASE + index),
            )
        }
    }

    private fun resolveStation(
        jsonName: String,
        catalogByNormalizedName: Map<String, List<entities.MetroStation>>,
    ): entities.MetroStation? {
        val normalized = normalizeMetroName(jsonName)
        aliasToCanonicalName[normalized]?.let { canonical ->
            val preferredLine = aliasPreferredLine[normalized]
            val matches = catalogByNormalizedName[normalizeMetroName(canonical)].orEmpty()
            if (preferredLine != null) {
                matches.firstOrNull { it.line == preferredLine }?.let { return it }
            }
            return matches.firstOrNull()
        }
        return catalogByNormalizedName[normalized]?.firstOrNull()
    }
}

internal fun normalizeMetroName(name: String): String =
    name.lowercase()
        .replace('ё', 'е')
        .replace(" ", "")
        .replace("-", "")
        .replace("(", "")
        .replace(")", "")
        .replace(".", "")

/**
 * JSON names that don't exactly match [MetroStationNames] after normalize.
 */
private val aliasToCanonicalName: Map<String, String> = mapOf(
    normalizeMetroName("Юбилейная площадь") to MetroStationNames.FRUNZENSKAYA,
    normalizeMetroName("Фрунзенская") to MetroStationNames.FRUNZENSKAYA,
    normalizeMetroName("Каменная Горка") to MetroStationNames.KAMENNAYA_GORKA,
    normalizeMetroName("Ковальская Слобода") to MetroStationNames.KOVALSKAYA_SLOBODA,
    normalizeMetroName("Слуцкий гостинец") to MetroStationNames.SLUTSKIY_GOSTINEC,
)

private val aliasPreferredLine: Map<String, MetroLine> = mapOf(
    normalizeMetroName("Юбилейная площадь") to MetroLine.GREEN,
    normalizeMetroName("Фрунзенская") to MetroLine.RED,
)
