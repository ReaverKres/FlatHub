package entities

/**
 * Toronto TTC subway catalog for filter UI + geo line resolution.
 * Names match toronto_metro_stations.json.
 */
object TorontoMetroStations {
    private const val ID_BASE = 130_000

    private fun stations(line: MetroLine, startId: Int, names: List<String>): List<MetroStation> =
        names.mapIndexed { i, name -> MetroStation(line, ID_BASE + startId + i, name) }

    private val L1_LINE = stations(
        MetroLine.TORONTO_L1, 0,
        listOf(
            "Vaughan Metropolitan Centre", "Highway 407", "Pioneer Village", "Finch West",
            "Downsview Park", "Sheppard West", "Wilson", "Yorkdale", "Lawrence West",
            "Glencairn", "Eglinton West", "St Clair West", "Dupont", "St George", "Spadina",
            "St Patrick", "Osgoode", "St Andrew", "Union", "King", "Queen", "Dundas",
            "College", "Wellesley", "Bloor-Yonge", "Rosedale", "Summerhill", "St Clair",
            "Davisville", "Eglinton", "Lawrence", "York Mills", "Sheppard-Yonge",
            "North York Centre", "Finch",
        ),
    )

    private val L2_LINE = stations(
        MetroLine.TORONTO_L2, 50,
        listOf(
            "Kipling", "Islington", "Royal York", "Old Mill", "Jane", "Runnymede",
            "High Park", "Keele", "Dundas West", "Lansdowne", "Dufferin", "Ossington",
            "Christie", "Bathurst", "St George", "Bloor-Yonge", "Broadview", "Chester",
            "Pape", "Donlands", "Greenwood", "Coxwell", "Woodbine", "Main Street",
            "Victoria Park", "Warden", "Kennedy",
        ),
    )

    private val L3_LINE = stations(
        MetroLine.TORONTO_L3, 100,
        listOf(
            "Kennedy", "Lawrence East", "Ellesmere", "Midland", "McCowan", "Scarborough Centre",
        ),
    )

    private val L4_LINE = stations(
        MetroLine.TORONTO_L4, 130,
        listOf(
            "Sheppard-Yonge", "Bayview", "Bessarion", "Leslie", "Don Mills",
        ),
    )

    private val allLines = listOf(L1_LINE, L2_LINE, L3_LINE, L4_LINE)

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        return allLines.firstOrNull { line -> line.any { it.name.lowercase() == n } }
            ?.firstOrNull()?.line
    }

    fun allStationsRequest(): List<MetroStation> = allLines.flatten()
}
