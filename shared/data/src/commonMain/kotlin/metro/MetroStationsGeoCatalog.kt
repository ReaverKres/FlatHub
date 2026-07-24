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
private const val TBILISI_METRO_GEO_RESOURCE = "files/tbilisi_metro_stations.json"
private const val ALMATY_METRO_GEO_RESOURCE = "files/almaty_metro_stations.json"
private const val MADRID_METRO_GEO_RESOURCE = "files/madrid_metro_stations.json"
private const val BARCELONA_METRO_GEO_RESOURCE = "files/barcelona_metro_stations.json"
private const val BERLIN_METRO_GEO_RESOURCE = "files/berlin_metro_stations.json"
private const val BANGKOK_METRO_GEO_RESOURCE = "files/bangkok_metro_stations.json"
private const val SEOUL_METRO_GEO_RESOURCE = "files/seoul_metro_stations.json"
private const val TOKYO_METRO_GEO_RESOURCE = "files/tokyo_metro_stations.json"
private const val WIEN_METRO_GEO_RESOURCE = "files/wien_metro_stations.json"
private const val LONDON_METRO_GEO_RESOURCE = "files/london_metro_stations.json"
private const val PARIS_METRO_GEO_RESOURCE = "files/paris_metro_stations.json"
private const val TORONTO_METRO_GEO_RESOURCE = "files/toronto_metro_stations.json"

/** Synthetic metroId base for Warsaw stations (not used by BY site APIs). */
private const val WARSAW_METRO_ID_BASE = 10_000

/** Synthetic metroId base for Tbilisi stations (not used by GE site APIs). */
private const val TBILISI_METRO_ID_BASE = 20_000

/** Synthetic metroId base for Almaty stations (not used by KZ site APIs). */
private const val ALMATY_METRO_ID_BASE = 30_000

/** Synthetic metroId base for Madrid stations (not used by ES site APIs). */
private const val MADRID_METRO_ID_BASE = 40_000

/** Synthetic metroId base for Barcelona stations (not used by ES site APIs). */
private const val BARCELONA_METRO_ID_BASE = 50_000

/** Synthetic metroId base for Berlin stations (not used by DE site APIs). */
private const val BERLIN_METRO_ID_BASE = 60_000

/** Synthetic metroId base for Bangkok stations (not used by TH site APIs). */
private const val BANGKOK_METRO_ID_BASE = 70_000

/** Synthetic metroId base for Seoul stations (not used by KR site APIs). */
private const val SEOUL_METRO_ID_BASE = 80_000

/** Synthetic metroId base for Tokyo stations (not used by JP site APIs). */
private const val TOKYO_METRO_ID_BASE = 90_000

/** Synthetic metroId base for Vienna stations (not used by AT site APIs). */
private const val WIEN_METRO_ID_BASE = 100_000

/** Synthetic metroId base for London stations (not used by GB site APIs). */
private const val LONDON_METRO_ID_BASE = 110_000

/** Synthetic metroId base for Paris stations (not used by FR site APIs). */
private const val PARIS_METRO_ID_BASE = 120_000

/** Synthetic metroId base for Toronto stations (not used by CA site APIs). */
private const val TORONTO_METRO_ID_BASE = 130_000

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
        val tbilisi = loadTbilisiStations()
        val almaty = loadAlmatyStations()
        val madrid = loadMadridStations()
        val barcelona = loadBarcelonaStations()
        val berlin = loadBerlinStations()
        val bangkok = loadBangkokStations()
        val seoul = loadSeoulStations()
        val tokyo = loadTokyoStations()
        val wien = loadWienStations()
        val london = loadLondonStations()
        val paris = loadParisStations()
        val toronto = loadTorontoStations()
        return minsk + warsaw + tbilisi + almaty + madrid + barcelona + berlin + bangkok + seoul + tokyo + wien + london + paris + toronto
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

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadTbilisiStations(): List<MetroStationGeo> {
        val text = Res.readBytes(TBILISI_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.TbilisiMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.TbilisiMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (TBILISI_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadAlmatyStations(): List<MetroStationGeo> {
        val text = Res.readBytes(ALMATY_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.AlmatyMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.AlmatyMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (ALMATY_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadMadridStations(): List<MetroStationGeo> {
        val text = Res.readBytes(MADRID_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.MadridMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.MadridMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (MADRID_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadBarcelonaStations(): List<MetroStationGeo> {
        val text = Res.readBytes(BARCELONA_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.BarcelonaMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.BarcelonaMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (BARCELONA_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadBerlinStations(): List<MetroStationGeo> {
        val text = Res.readBytes(BERLIN_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.BerlinMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.BerlinMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (BERLIN_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadBangkokStations(): List<MetroStationGeo> {
        val text = Res.readBytes(BANGKOK_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.BangkokMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.BangkokMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.GREEN,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (BANGKOK_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadSeoulStations(): List<MetroStationGeo> {
        val text = Res.readBytes(SEOUL_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.SeoulMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.SeoulMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (SEOUL_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadTokyoStations(): List<MetroStationGeo> {
        val text = Res.readBytes(TOKYO_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.TokyoMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.TokyoMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (TOKYO_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadWienStations(): List<MetroStationGeo> {
        val text = Res.readBytes(WIEN_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.WienMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.WienMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (WIEN_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadLondonStations(): List<MetroStationGeo> {
        val text = Res.readBytes(LONDON_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.LondonMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.LondonMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.BLUE,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (LONDON_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadParisStations(): List<MetroStationGeo> {
        val text = Res.readBytes(PARIS_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.ParisMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.ParisMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.PARIS_M1,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (PARIS_METRO_ID_BASE + index),
            )
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadTorontoStations(): List<MetroStationGeo> {
        val text = Res.readBytes(TORONTO_METRO_GEO_RESOURCE).decodeToString()
        val dtos = json.decodeFromString<List<MetroStationGeoDto>>(text)
        val catalog = entities.TorontoMetroStations.allStationsRequest()
            .groupBy { it.name.lowercase() }
        return dtos.mapIndexedNotNull { index, dto ->
            if (dto.coordinates.size < 2) return@mapIndexedNotNull null
            val fromCatalog = catalog[dto.name.lowercase()]?.firstOrNull()
            MetroStationGeo(
                jsonName = dto.name,
                canonicalName = dto.name,
                line = fromCatalog?.line
                    ?: entities.TorontoMetroStations.lineForStationName(dto.name)
                    ?: MetroLine.TORONTO_L1,
                coordinates = Coordinates(dto.latitude, dto.longitude),
                metroId = fromCatalog?.metroId ?: (TORONTO_METRO_ID_BASE + index),
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
