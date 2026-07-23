package entities

/**
 * Vienna U-Bahn catalog — real lines U1/U2/U3/U4/U6.
 * Names match wien_metro_stations.json (interchanges duplicated per line).
 */
object WienMetroStations {
    private const val ID_BASE = 100_000

    private fun stations(line: MetroLine, startId: Int, names: List<String>): List<MetroStation> =
        names.mapIndexed { i, name -> MetroStation(line, ID_BASE + startId + i, name) }

    private val U1_LINE = stations(
        MetroLine.WIEN_U1, 0,
        listOf(
            "Leopoldau", "Großfeldsiedlung", "Aderklaaer Straße", "Rennbahnweg", "Kagran",
            "Kagraner Platz", "Alte Donau", "Donauinsel", "Vorgartenstraße", "Praterstern",
            "Nestroyplatz", "Schwedenplatz", "Stephansplatz", "Karlsplatz", "Taubstummengasse",
            "Südtiroler Platz - Hauptbahnhof", "Keplerplatz", "Reumannplatz", "Troststraße",
            "Altes Landgut", "Alaudagasse", "Neulaa", "Oberlaa",
        ),
    )

    private val U2_LINE = stations(
        MetroLine.WIEN_U2, 100,
        listOf(
            "Seestadt", "Hausfeldstraße", "Aspern Nord", "Aspernstraße", "Stadlau",
            "Hardeggasse", "Donaumarina", "Donaustadtbrücke", "Stadion", "Krieau",
            "Messe-Prater", "Taborstraße", "Schottenring", "Schottentor", "Rathaus",
            "Volkstheater", "Museumsquartier", "Karlsplatz",
        ),
    )

    private val U3_LINE = stations(
        MetroLine.WIEN_U3, 200,
        listOf(
            "Ottakring", "Kendlerstraße", "Hütteldorfer Straße", "Johnstraße", "Schweglerstraße",
            "Westbahnhof", "Zieglergasse", "Neubaugasse", "Herrengasse", "Stephansplatz",
            "Stubentor", "Landstraße", "Rochusgasse", "Kardinal-Nagl-Platz", "Schlachthausgasse",
            "Erdberg", "Gasometer", "Zippererstraße", "Enkplatz", "Simmering",
        ),
    )

    private val U4_LINE = stations(
        MetroLine.WIEN_U4, 300,
        listOf(
            "Heiligenstadt", "Spittelau", "Friedensbrücke", "Rossauer Lände", "Schottenring",
            "Schwedenplatz", "Landstraße", "Stadtpark", "Karlsplatz", "Kettenbrückengasse",
            "Pilgramgasse", "Margaretengürtel", "Längenfeldgasse", "Meidling Hauptstraße",
            "Schönbrunn", "Hietzing", "Braunschweiggasse", "Unter St. Veit", "Ober St. Veit",
            "Hütteldorf",
        ),
    )

    private val U6_LINE = stations(
        MetroLine.WIEN_U6, 400,
        listOf(
            "Floridsdorf", "Neue Donau", "Handelskai", "Dresdner Straße", "Jägerstraße",
            "Spittelau", "Nußdorfer Straße", "Währinger Straße - Volksoper", "Michelbeuern AKH",
            "Alser Straße", "Josefstädter Straße", "Thaliastraße", "Burggasse - Stadthalle",
            "Westbahnhof", "Gumpendorfer Straße", "Längenfeldgasse", "Niederhofstraße",
            "Bahnhof Meidling", "Tscherttegasse", "Am Schöpfwerk", "Erlaaer Straße", "Siebenhirten",
        ),
    )

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        if (U1_LINE.any { it.name.lowercase() == n }) return MetroLine.WIEN_U1
        if (U2_LINE.any { it.name.lowercase() == n }) return MetroLine.WIEN_U2
        if (U3_LINE.any { it.name.lowercase() == n }) return MetroLine.WIEN_U3
        if (U4_LINE.any { it.name.lowercase() == n }) return MetroLine.WIEN_U4
        if (U6_LINE.any { it.name.lowercase() == n }) return MetroLine.WIEN_U6
        return null
    }

    fun allStationsRequest(): List<MetroStation> =
        U1_LINE + U2_LINE + U3_LINE + U4_LINE + U6_LINE
}
