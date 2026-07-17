package entities

/**
 * Barcelona metro catalog for filter UI + geo line resolution.
 * L1–L3 → BLUE, L4–L6 → RED, L7+ → GREEN.
 * Station names match `barcelona_metro_stations.json`.
 */
object BarcelonaMetroStations {
    private const val ID_BASE = 50_000

    private val BLUE_LINE = listOf(
        MetroStation(MetroLine.BLUE, ID_BASE + 1, "Arc de Triomf"),
        MetroStation(MetroLine.BLUE, ID_BASE + 3, "Bac de Roda"),
        MetroStation(MetroLine.BLUE, ID_BASE + 8, "Baró de Viver"),
        MetroStation(MetroLine.BLUE, ID_BASE + 14, "Canyelles"),
        MetroStation(MetroLine.BLUE, ID_BASE + 26, "El Poble-sec"),
        MetroStation(MetroLine.BLUE, ID_BASE + 28, "Encants"),
        MetroStation(MetroLine.BLUE, ID_BASE + 31, "Fabra i Puig"),
        MetroStation(MetroLine.BLUE, ID_BASE + 34, "Fontana"),
        MetroStation(MetroLine.BLUE, ID_BASE + 36, "Glòries"),
        MetroStation(MetroLine.BLUE, ID_BASE + 41, "Hostafrancs"),
        MetroStation(MetroLine.BLUE, ID_BASE + 46, "La Rambla | Drassanes"),
        MetroStation(MetroLine.BLUE, ID_BASE + 48, "les Corts"),
        MetroStation(MetroLine.BLUE, ID_BASE + 50, "Lesseps"),
        MetroStation(MetroLine.BLUE, ID_BASE + 51, "Liceu"),
        MetroStation(MetroLine.BLUE, ID_BASE + 55, "Maria Cristina"),
        MetroStation(MetroLine.BLUE, ID_BASE + 56, "Marina"),
        MetroStation(MetroLine.BLUE, ID_BASE + 58, "Mercat Nou"),
        MetroStation(MetroLine.BLUE, ID_BASE + 59, "Montbau"),
        MetroStation(MetroLine.BLUE, ID_BASE + 60, "Monumental"),
        MetroStation(MetroLine.BLUE, ID_BASE + 61, "Mundet"),
        MetroStation(MetroLine.BLUE, ID_BASE + 63, "Navas"),
        MetroStation(MetroLine.BLUE, ID_BASE + 66, "Palau Reial"),
        MetroStation(MetroLine.BLUE, ID_BASE + 70, "Penitents"),
        MetroStation(MetroLine.BLUE, ID_BASE + 72, "Plaça del Centre"),
        MetroStation(MetroLine.BLUE, ID_BASE + 77, "Rocafort"),
        MetroStation(MetroLine.BLUE, ID_BASE + 78, "Roquetes"),
        MetroStation(MetroLine.BLUE, ID_BASE + 80, "Sant Andreu"),
        MetroStation(MetroLine.BLUE, ID_BASE + 81, "Sant Antoni"),
        MetroStation(MetroLine.BLUE, ID_BASE + 83, "Sant Martí"),
        MetroStation(MetroLine.BLUE, ID_BASE + 85, "Santander"),
        MetroStation(MetroLine.BLUE, ID_BASE + 89, "Tarragona"),
        MetroStation(MetroLine.BLUE, ID_BASE + 90, "Tetuan"),
        MetroStation(MetroLine.BLUE, ID_BASE + 91, "Torras i Bages"),
        MetroStation(MetroLine.BLUE, ID_BASE + 94, "Trinitat Vella"),
        MetroStation(MetroLine.BLUE, ID_BASE + 96, "Urgell"),
        MetroStation(MetroLine.BLUE, ID_BASE + 99, "Vallcarca"),
        MetroStation(MetroLine.BLUE, ID_BASE + 100, "Valldaura"),
    )

    private val RED_LINE = listOf(
        MetroStation(MetroLine.RED, ID_BASE + 0, "Alfons X"),
        MetroStation(MetroLine.RED, ID_BASE + 4, "Badal"),
        MetroStation(MetroLine.RED, ID_BASE + 7, "Barceloneta"),
        MetroStation(MetroLine.RED, ID_BASE + 9, "Besòs"),
        MetroStation(MetroLine.RED, ID_BASE + 10, "Besòs Mar"),
        MetroStation(MetroLine.RED, ID_BASE + 11, "Bogatell"),
        MetroStation(MetroLine.RED, ID_BASE + 13, "Camp de l'Arpa"),
        MetroStation(MetroLine.RED, ID_BASE + 17, "Ciutadella | Vila Olímpica"),
        MetroStation(MetroLine.RED, ID_BASE + 19, "Congrés | Indians"),
        MetroStation(MetroLine.RED, ID_BASE + 22, "el Carmel"),
        MetroStation(MetroLine.RED, ID_BASE + 24, "El Coll | La Teixonera"),
        MetroStation(MetroLine.RED, ID_BASE + 25, "El Maresme | Fòrum"),
        MetroStation(MetroLine.RED, ID_BASE + 29, "Entença"),
        MetroStation(MetroLine.RED, ID_BASE + 35, "Girona"),
        MetroStation(MetroLine.RED, ID_BASE + 38, "Guinardó | Hospital de Sant Pau"),
        MetroStation(MetroLine.RED, ID_BASE + 39, "Horta"),
        MetroStation(MetroLine.RED, ID_BASE + 40, "Hospital Clínic"),
        MetroStation(MetroLine.RED, ID_BASE + 42, "Jaume I"),
        MetroStation(MetroLine.RED, ID_BASE + 43, "Joanic"),
        MetroStation(MetroLine.RED, ID_BASE + 44, "La Bonanova"),
        MetroStation(MetroLine.RED, ID_BASE + 49, "Les Tres Torres"),
        MetroStation(MetroLine.RED, ID_BASE + 52, "Llacuna"),
        MetroStation(MetroLine.RED, ID_BASE + 53, "Llucmajor | República"),
        MetroStation(MetroLine.RED, ID_BASE + 62, "Muntaner"),
        MetroStation(MetroLine.RED, ID_BASE + 74, "Poblenou"),
        MetroStation(MetroLine.RED, ID_BASE + 82, "Sant Gervasi"),
        MetroStation(MetroLine.RED, ID_BASE + 84, "Sant Pau | Dos de Maig"),
        MetroStation(MetroLine.RED, ID_BASE + 88, "Selva de Mar"),
        MetroStation(MetroLine.RED, ID_BASE + 102, "Via Júlia"),
        MetroStation(MetroLine.RED, ID_BASE + 103, "Vilapicina"),
        MetroStation(MetroLine.RED, ID_BASE + 104, "Virrei Amat"),
    )

    private val GREEN_LINE = listOf(
        MetroStation(MetroLine.GREEN, ID_BASE + 2, "Avinguda Tibidabo"),
        MetroStation(MetroLine.GREEN, ID_BASE + 5, "Barcelona-Plaça Catalunya"),
        MetroStation(MetroLine.GREEN, ID_BASE + 6, "Barcelona-Plaça Espanya"),
        MetroStation(MetroLine.GREEN, ID_BASE + 12, "Bon Pastor"),
        MetroStation(MetroLine.GREEN, ID_BASE + 15, "Casa de l'Aigua"),
        MetroStation(MetroLine.GREEN, ID_BASE + 16, "Catalunya"),
        MetroStation(MetroLine.GREEN, ID_BASE + 18, "Ciutat Meridiana"),
        MetroStation(MetroLine.GREEN, ID_BASE + 20, "Diagonal"),
        MetroStation(MetroLine.GREEN, ID_BASE + 21, "Ecoparc"),
        MetroStation(MetroLine.GREEN, ID_BASE + 23, "El Clot"),
        MetroStation(MetroLine.GREEN, ID_BASE + 27, "El Putxet"),
        MetroStation(MetroLine.GREEN, ID_BASE + 30, "Espanya"),
        MetroStation(MetroLine.GREEN, ID_BASE + 32, "Foc"),
        MetroStation(MetroLine.GREEN, ID_BASE + 33, "Foneria"),
        MetroStation(MetroLine.GREEN, ID_BASE + 37, "Gràcia"),
        MetroStation(MetroLine.GREEN, ID_BASE + 45, "La Pau"),
        MetroStation(MetroLine.GREEN, ID_BASE + 47, "La Sagrera"),
        MetroStation(MetroLine.GREEN, ID_BASE + 54, "Maragall"),
        MetroStation(MetroLine.GREEN, ID_BASE + 57, "Mercabarna"),
        MetroStation(MetroLine.GREEN, ID_BASE + 64, "Onze de Setembre"),
        MetroStation(MetroLine.GREEN, ID_BASE + 65, "Pàdua"),
        MetroStation(MetroLine.GREEN, ID_BASE + 67, "Paral·lel"),
        MetroStation(MetroLine.GREEN, ID_BASE + 68, "Parc Logístic"),
        MetroStation(MetroLine.GREEN, ID_BASE + 69, "Passeig de Gràcia"),
        MetroStation(MetroLine.GREEN, ID_BASE + 71, "Plaça de Sants"),
        MetroStation(MetroLine.GREEN, ID_BASE + 73, "Plaça Molina"),
        MetroStation(MetroLine.GREEN, ID_BASE + 75, "Provença"),
        MetroStation(MetroLine.GREEN, ID_BASE + 76, "Reina Elisenda"),
        MetroStation(MetroLine.GREEN, ID_BASE + 79, "Sagrada Família"),
        MetroStation(MetroLine.GREEN, ID_BASE + 86, "Sants Estació"),
        MetroStation(MetroLine.GREEN, ID_BASE + 87, "Sarrià"),
        MetroStation(MetroLine.GREEN, ID_BASE + 92, "Torre Baró | Vallbona"),
        MetroStation(MetroLine.GREEN, ID_BASE + 93, "Trinitat Nova"),
        MetroStation(MetroLine.GREEN, ID_BASE + 95, "Universitat"),
        MetroStation(MetroLine.GREEN, ID_BASE + 97, "Urquinaona"),
        MetroStation(MetroLine.GREEN, ID_BASE + 98, "Vall d’Hebron | Sant Genís"),
        MetroStation(MetroLine.GREEN, ID_BASE + 101, "Verdaguer"),
        MetroStation(MetroLine.GREEN, ID_BASE + 105, "Zona Franca"),
        MetroStation(MetroLine.GREEN, ID_BASE + 106, "Zona Universitària"),
    )

    fun allStationsRequest(): List<MetroStation> =
        BLUE_LINE + RED_LINE + GREEN_LINE

    fun lineForStationName(name: String): MetroLine? {
        val n = name.trim()
        if (BLUE_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BLUE
        if (RED_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.RED
        if (GREEN_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.GREEN
        return null
    }
}
