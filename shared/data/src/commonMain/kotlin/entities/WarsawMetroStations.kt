package entities

/**
 * Warsaw metro catalog for filter UI + geo line resolution.
 * M1 → [MetroLine.BLUE], M2 → [MetroLine.RED] (matches Warsaw line colours).
 * Station names match `warsaw_metro_stations.json`.
 */
object WarsawMetroStations {
    private const val ID_BASE = 10_000

    private val M1 = listOf(
        "Kabaty",
        "Natolin",
        "Imielin",
        "Stokłosy",
        "Ursynów",
        "Służew",
        "Wilanowska",
        "Wierzbno",
        "Racławicka",
        "Pole Mokotowskie",
        "Politechnika",
        "Centrum",
        "Świętokrzyska",
        "Ratusz Arsenał",
        "Dworzec Gdański",
        "Plac Wilsona",
        "Marymont",
        "Słodowiec",
        "Stare Bielany",
        "Wawrzyszew",
        "Młociny",
    )

    private val M2 = listOf(
        "Bemowo",
        "Ulrychów",
        "Księcia Janusza",
        "Młynów",
        "Płocka",
        "Rondo Daszyńskiego",
        "Rondo ONZ",
        "Świętokrzyska",
        "Nowy Świat-Uniwersytet",
        "Centrum Nauki Kopernik",
        "Stadion Narodowy",
        "Dworzec Wileński",
        "Szwedzka",
        "Targówek Mieszkaniowy",
        "Trocka",
        "Zacisze",
        "Kondratowicza",
        "Bródno",
    )

    fun allStationsRequest(): List<MetroStation> {
        val m1 = M1.mapIndexed { index, name ->
            MetroStation(MetroLine.BLUE, ID_BASE + index, name)
        }
        val m2 = M2.mapIndexed { index, name ->
            MetroStation(MetroLine.RED, ID_BASE + 100 + index, name)
        }
        return m1 + m2
    }

    fun lineForStationName(name: String): MetroLine? {
        val normalized = name.trim()
        if (M1.any { it.equals(normalized, ignoreCase = true) }) return MetroLine.BLUE
        if (M2.any { it.equals(normalized, ignoreCase = true) }) return MetroLine.RED
        return null
    }
}
