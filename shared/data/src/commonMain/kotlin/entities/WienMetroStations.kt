package entities

/**
 * Vienna U-Bahn catalog for filter UI + geo line resolution.
 * U2 → BLUE; U3/U4 → GREEN; U1/U6 → RED. Names match wien_metro_stations.json.
 */
object WienMetroStations {
    private const val ID_BASE = 100_000

    private val BLUE_LINE = listOf(
        MetroStation(MetroLine.BLUE, ID_BASE + 0, "Seestadt"),
        MetroStation(MetroLine.BLUE, ID_BASE + 1, "Hausfeldstraße"),
        MetroStation(MetroLine.BLUE, ID_BASE + 2, "Aspern Nord"),
        MetroStation(MetroLine.BLUE, ID_BASE + 3, "Aspernstraße"),
        MetroStation(MetroLine.BLUE, ID_BASE + 4, "Stadlau"),
        MetroStation(MetroLine.BLUE, ID_BASE + 5, "Hardeggasse"),
        MetroStation(MetroLine.BLUE, ID_BASE + 6, "Donaumarina"),
        MetroStation(MetroLine.BLUE, ID_BASE + 7, "Donaustadtbrücke"),
        MetroStation(MetroLine.BLUE, ID_BASE + 8, "Stadion"),
        MetroStation(MetroLine.BLUE, ID_BASE + 9, "Krieau"),
        MetroStation(MetroLine.BLUE, ID_BASE + 10, "Messe-Prater"),
        MetroStation(MetroLine.BLUE, ID_BASE + 11, "Taborstraße"),
        MetroStation(MetroLine.BLUE, ID_BASE + 12, "Schottenring"),
        MetroStation(MetroLine.BLUE, ID_BASE + 13, "Schottentor"),
        MetroStation(MetroLine.BLUE, ID_BASE + 14, "Rathaus"),
        MetroStation(MetroLine.BLUE, ID_BASE + 15, "Volkstheater"),
        MetroStation(MetroLine.BLUE, ID_BASE + 16, "Museumsquartier"),
    )

    private val GREEN_LINE = listOf(
        MetroStation(MetroLine.GREEN, ID_BASE + 17, "Ottakring"),
        MetroStation(MetroLine.GREEN, ID_BASE + 18, "Kendlerstraße"),
        MetroStation(MetroLine.GREEN, ID_BASE + 19, "Hütteldorfer Straße"),
        MetroStation(MetroLine.GREEN, ID_BASE + 20, "Johnstraße"),
        MetroStation(MetroLine.GREEN, ID_BASE + 21, "Schweglerstraße"),
        MetroStation(MetroLine.GREEN, ID_BASE + 22, "Westbahnhof"),
        MetroStation(MetroLine.GREEN, ID_BASE + 23, "Zieglergasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 24, "Neubaugasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 25, "Herrengasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 26, "Stubentor"),
        MetroStation(MetroLine.GREEN, ID_BASE + 27, "Landstraße"),
        MetroStation(MetroLine.GREEN, ID_BASE + 28, "Rochusgasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 29, "Kardinal-Nagl-Platz"),
        MetroStation(MetroLine.GREEN, ID_BASE + 30, "Schlachthausgasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 31, "Erdberg"),
        MetroStation(MetroLine.GREEN, ID_BASE + 32, "Gasometer"),
        MetroStation(MetroLine.GREEN, ID_BASE + 33, "Zippererstraße"),
        MetroStation(MetroLine.GREEN, ID_BASE + 34, "Enkplatz"),
        MetroStation(MetroLine.GREEN, ID_BASE + 35, "Simmering"),
        MetroStation(MetroLine.GREEN, ID_BASE + 36, "Heiligenstadt"),
        MetroStation(MetroLine.GREEN, ID_BASE + 37, "Spittelau"),
        MetroStation(MetroLine.GREEN, ID_BASE + 38, "Friedensbrücke"),
        MetroStation(MetroLine.GREEN, ID_BASE + 39, "Rossauer Lände"),
        MetroStation(MetroLine.GREEN, ID_BASE + 40, "Stadtpark"),
        MetroStation(MetroLine.GREEN, ID_BASE + 41, "Kettenbrückengasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 42, "Pilgramgasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 43, "Margaretengürtel"),
        MetroStation(MetroLine.GREEN, ID_BASE + 44, "Längenfeldgasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 45, "Meidling Hauptstraße"),
        MetroStation(MetroLine.GREEN, ID_BASE + 46, "Schönbrunn"),
        MetroStation(MetroLine.GREEN, ID_BASE + 47, "Hietzing"),
        MetroStation(MetroLine.GREEN, ID_BASE + 48, "Braunschweiggasse"),
        MetroStation(MetroLine.GREEN, ID_BASE + 49, "Unter St. Veit"),
        MetroStation(MetroLine.GREEN, ID_BASE + 50, "Ober St. Veit"),
        MetroStation(MetroLine.GREEN, ID_BASE + 51, "Hütteldorf"),
    )

    private val RED_LINE = listOf(
        MetroStation(MetroLine.RED, ID_BASE + 52, "Leopoldau"),
        MetroStation(MetroLine.RED, ID_BASE + 53, "Großfeldsiedlung"),
        MetroStation(MetroLine.RED, ID_BASE + 54, "Aderklaaer Straße"),
        MetroStation(MetroLine.RED, ID_BASE + 55, "Rennbahnweg"),
        MetroStation(MetroLine.RED, ID_BASE + 56, "Kagran"),
        MetroStation(MetroLine.RED, ID_BASE + 57, "Kagraner Platz"),
        MetroStation(MetroLine.RED, ID_BASE + 58, "Alte Donau"),
        MetroStation(MetroLine.RED, ID_BASE + 59, "Donauinsel"),
        MetroStation(MetroLine.RED, ID_BASE + 60, "Vorgartenstraße"),
        MetroStation(MetroLine.RED, ID_BASE + 61, "Praterstern"),
        MetroStation(MetroLine.RED, ID_BASE + 62, "Nestroyplatz"),
        MetroStation(MetroLine.RED, ID_BASE + 63, "Schwedenplatz"),
        MetroStation(MetroLine.RED, ID_BASE + 64, "Stephansplatz"),
        MetroStation(MetroLine.RED, ID_BASE + 65, "Karlsplatz"),
        MetroStation(MetroLine.RED, ID_BASE + 66, "Taubstummengasse"),
        MetroStation(MetroLine.RED, ID_BASE + 67, "Südtiroler Platz - Hauptbahnhof"),
        MetroStation(MetroLine.RED, ID_BASE + 68, "Keplerplatz"),
        MetroStation(MetroLine.RED, ID_BASE + 69, "Reumannplatz"),
        MetroStation(MetroLine.RED, ID_BASE + 70, "Troststraße"),
        MetroStation(MetroLine.RED, ID_BASE + 71, "Altes Landgut"),
        MetroStation(MetroLine.RED, ID_BASE + 72, "Alaudagasse"),
        MetroStation(MetroLine.RED, ID_BASE + 73, "Neulaa"),
        MetroStation(MetroLine.RED, ID_BASE + 74, "Oberlaa"),
        MetroStation(MetroLine.RED, ID_BASE + 75, "Floridsdorf"),
        MetroStation(MetroLine.RED, ID_BASE + 76, "Neue Donau"),
        MetroStation(MetroLine.RED, ID_BASE + 77, "Handelskai"),
        MetroStation(MetroLine.RED, ID_BASE + 78, "Dresdner Straße"),
        MetroStation(MetroLine.RED, ID_BASE + 79, "Jägerstraße"),
        MetroStation(MetroLine.RED, ID_BASE + 80, "Josefstädter Straße"),
        MetroStation(MetroLine.RED, ID_BASE + 81, "Alser Straße"),
        MetroStation(MetroLine.RED, ID_BASE + 82, "Michelbeuern AKH"),
        MetroStation(MetroLine.RED, ID_BASE + 83, "Währinger Straße - Volksoper"),
        MetroStation(MetroLine.RED, ID_BASE + 84, "Nußdorfer Straße"),
        MetroStation(MetroLine.RED, ID_BASE + 85, "Thaliastraße"),
        MetroStation(MetroLine.RED, ID_BASE + 86, "Burggasse - Stadthalle"),
        MetroStation(MetroLine.RED, ID_BASE + 87, "Gumpendorfer Straße"),
        MetroStation(MetroLine.RED, ID_BASE + 88, "Niederhofstraße"),
        MetroStation(MetroLine.RED, ID_BASE + 89, "Bahnhof Meidling"),
        MetroStation(MetroLine.RED, ID_BASE + 90, "Tscherttegasse"),
        MetroStation(MetroLine.RED, ID_BASE + 91, "Am Schöpfwerk"),
        MetroStation(MetroLine.RED, ID_BASE + 92, "Erlaaer Straße"),
        MetroStation(MetroLine.RED, ID_BASE + 93, "Siebenhirten"),
    )

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        if (BLUE_LINE.any { it.name.lowercase() == n }) return MetroLine.BLUE
        if (RED_LINE.any { it.name.lowercase() == n }) return MetroLine.RED
        if (GREEN_LINE.any { it.name.lowercase() == n }) return MetroLine.GREEN
        return null
    }

    fun allStationsRequest(): List<MetroStation> =
        BLUE_LINE + GREEN_LINE + RED_LINE
}
