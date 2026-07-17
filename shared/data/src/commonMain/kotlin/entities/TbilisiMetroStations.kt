package entities

/**
 * Tbilisi metro catalog for filter UI + geo line resolution.
 * Line 1 (Akhmeteli–Varketili) → [MetroLine.BLUE], Line 2 (Saburtalo) → [MetroLine.RED].
 * Station names match `tbilisi_metro_stations.json`.
 */
object TbilisiMetroStations {
    private const val ID_BASE = 20_000

    /** Akhmeteli–Varketili (north → east). */
    private val LINE_1 = listOf(
        "Akhmeteli Theatre",
        "Sarajishvili",
        "Guramishvili",
        "Ghrmaghele",
        "Didube",
        "Gotsiridze",
        "Nadzaladevi",
        "Station Square",
        "Marjanishvili",
        "Rustaveli",
        "Liberty Square",
        "Avlabari",
        "300 Aragveli",
        "Isani",
        "Samgori",
        "Varketili",
    )

    /** Saburtalo line (transfer → west). Station Square transfer is listed on Line 1 only. */
    private val LINE_2 = listOf(
        "Tsereteli",
        "Technical University",
        "Medical University",
        "Delisi",
        "Vazha-Pshavela",
        "State University",
    )

    /** SS.ge / home.ss.ge `nearbySubwayStations.stationTitle` → canonical English name. */
    private val SS_TITLE_ALIASES: Map<String, String> = mapOf(
        "akhmeteli" to "Akhmeteli Theatre",
        "akhmeteli_theatre" to "Akhmeteli Theatre",
        "akhmetelis_teatri" to "Akhmeteli Theatre",
        "sarajishvili" to "Sarajishvili",
        "guramishvili" to "Guramishvili",
        "ghrmaghele" to "Ghrmaghele",
        "ghrmahele" to "Ghrmaghele",
        "didube" to "Didube",
        "gotsiridze" to "Gotsiridze",
        "nadzaladevi" to "Nadzaladevi",
        "station_square" to "Station Square",
        "sadguris_moedani" to "Station Square",
        "marjanishvili" to "Marjanishvili",
        "rustaveli" to "Rustaveli",
        "liberty_square" to "Liberty Square",
        "tavisuplebis_moedani" to "Liberty Square",
        "avlabari" to "Avlabari",
        "300_aragveli" to "300 Aragveli",
        "samasi_aragveli" to "300 Aragveli",
        "isani" to "Isani",
        "samgori" to "Samgori",
        "varketili" to "Varketili",
        "tsereteli" to "Tsereteli",
        "technical_uni" to "Technical University",
        "technical_university" to "Technical University",
        "medical_uni" to "Medical University",
        "medical_university" to "Medical University",
        "delisi" to "Delisi",
        "vazha_pshavela" to "Vazha-Pshavela",
        "state_university" to "State University",
        "sakhelmtsipo" to "State University",
    )

    fun allStationsRequest(): List<MetroStation> {
        val line1 = LINE_1.mapIndexed { index, name ->
            MetroStation(MetroLine.BLUE, ID_BASE + index, name)
        }
        val line2 = LINE_2.mapIndexed { index, name ->
            MetroStation(MetroLine.RED, ID_BASE + 100 + index, name)
        }
        return line1 + line2
    }

    fun lineForStationName(name: String): MetroLine? {
        val normalized = name.trim()
        if (LINE_1.any { it.equals(normalized, ignoreCase = true) }) return MetroLine.BLUE
        if (LINE_2.any { it.equals(normalized, ignoreCase = true) }) return MetroLine.RED
        return null
    }

    fun canonicalizeStationTitle(raw: String?): String? {
        val title = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        SS_TITLE_ALIASES[title.lowercase()]?.let { return it }
        // Already canonical English name from our catalog.
        if (lineForStationName(title) != null) return title
        // Title-case snake_case fallback: "medical_uni" already covered; try spaced form.
        val spaced = title.replace('_', ' ')
        if (lineForStationName(spaced) != null) return spaced
        return title
    }
}
