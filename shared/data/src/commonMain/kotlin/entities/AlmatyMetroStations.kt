package entities

/**
 * Almaty metro catalog (single Line A → [MetroLine.BLUE]).
 * Station names match `almaty_metro_stations.json`.
 */
object AlmatyMetroStations {
    private const val ID_BASE = 30_000

    private val LINE_A = listOf(
        "Raiymbek batyr",
        "Zhibek Zholy",
        "Almaly",
        "Abay",
        "Baikonur",
        "Auezov Theater",
        "Alatau",
        "Sayran",
        "Moskva",
        "Saryarqa",
        "Bauyrjan Momyshuly",
    )

    fun allStationsRequest(): List<MetroStation> =
        LINE_A.mapIndexed { index, name ->
            MetroStation(MetroLine.BLUE, ID_BASE + index, name)
        }

    fun lineForStationName(name: String): MetroLine? {
        val normalized = name.trim()
        if (LINE_A.any { it.equals(normalized, ignoreCase = true) }) return MetroLine.BLUE
        return null
    }
}
