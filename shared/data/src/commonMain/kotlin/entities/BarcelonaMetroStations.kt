package entities

/**
 * Barcelona TMB metro catalog for filter UI + geo line resolution.
 * L1–L5, L9 Nord/Sud, L10, L11 — station names match `barcelona_metro_stations.json`.
 */
object BarcelonaMetroStations {
    private const val ID_BASE = 50_000

    private val L1_LINE = listOf(
        MetroStation(MetroLine.BCN_L1, ID_BASE + 0, "Mercat Nou"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 1, "Plaça de Sants"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 2, "Hostafrancs"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 3, "Espanya"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 4, "Rocafort"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 5, "Urgell"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 6, "Universitat"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 7, "Catalunya"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 8, "Urquinaona"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 9, "Arc de Triomf"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 10, "Marina"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 11, "Glòries"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 12, "El Clot"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 13, "Navas"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 14, "La Sagrera"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 15, "Fabra i Puig"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 16, "Sant Andreu"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 17, "Torras i Bages"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 18, "Trinitat Vella"),
        MetroStation(MetroLine.BCN_L1, ID_BASE + 19, "Baró de Viver"),
    )

    private val L2_LINE = listOf(
        MetroStation(MetroLine.BCN_L2, ID_BASE + 20, "Paral·lel"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 21, "Sant Antoni"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 22, "Universitat"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 23, "Passeig de Gràcia"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 24, "Tetuan"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 25, "Monumental"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 26, "Sagrada Família"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 27, "Encants"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 28, "El Clot"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 29, "Bac de Roda"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 30, "Sant Martí"),
        MetroStation(MetroLine.BCN_L2, ID_BASE + 31, "La Pau"),
    )

    private val L3_LINE = listOf(
        MetroStation(MetroLine.BCN_L3, ID_BASE + 32, "Zona Universitària"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 33, "Palau Reial"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 34, "Maria Cristina"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 35, "les Corts"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 36, "Plaça del Centre"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 37, "Sants Estació"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 38, "Tarragona"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 39, "Espanya"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 40, "El Poble-sec"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 41, "Paral·lel"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 42, "La Rambla | Drassanes"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 43, "Liceu"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 44, "Catalunya"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 45, "Passeig de Gràcia"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 46, "Diagonal"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 47, "Fontana"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 48, "Lesseps"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 49, "Vallcarca"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 50, "Penitents"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 51, "Vall d’Hebron | Sant Genís"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 52, "Montbau"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 53, "Mundet"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 54, "Valldaura"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 55, "Canyelles"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 56, "Roquetes"),
        MetroStation(MetroLine.BCN_L3, ID_BASE + 57, "Trinitat Nova"),
    )

    private val L4_LINE = listOf(
        MetroStation(MetroLine.BCN_L4, ID_BASE + 58, "Trinitat Nova"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 59, "Via Júlia"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 60, "Llucmajor | República"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 61, "Maragall"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 62, "Guinardó | Hospital de Sant Pau"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 63, "Alfons X"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 64, "Joanic"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 65, "Verdaguer"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 66, "Girona"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 67, "Passeig de Gràcia"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 68, "Urquinaona"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 69, "Jaume I"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 70, "Barceloneta"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 71, "Ciutadella | Vila Olímpica"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 72, "Bogatell"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 73, "Llacuna"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 74, "Poblenou"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 75, "Selva de Mar"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 76, "El Maresme | Fòrum"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 77, "Besòs Mar"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 78, "Besòs"),
        MetroStation(MetroLine.BCN_L4, ID_BASE + 79, "La Pau"),
    )

    private val L5_LINE = listOf(
        MetroStation(MetroLine.BCN_L5, ID_BASE + 80, "Badal"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 81, "Plaça de Sants"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 82, "Sants Estació"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 83, "Entença"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 84, "Hospital Clínic"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 85, "Diagonal"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 86, "Verdaguer"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 87, "Sagrada Família"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 88, "Sant Pau | Dos de Maig"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 89, "Camp de l'Arpa"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 90, "La Sagrera"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 91, "Congrés | Indians"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 92, "Maragall"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 93, "Virrei Amat"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 94, "Vilapicina"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 95, "Horta"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 96, "el Carmel"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 97, "El Coll | La Teixonera"),
        MetroStation(MetroLine.BCN_L5, ID_BASE + 98, "Vall d’Hebron | Sant Genís"),
    )

    private val L9N_LINE = listOf(
        MetroStation(MetroLine.BCN_L9N, ID_BASE + 99, "La Sagrera"),
        MetroStation(MetroLine.BCN_L9N, ID_BASE + 100, "Onze de Setembre"),
        MetroStation(MetroLine.BCN_L9N, ID_BASE + 101, "Bon Pastor"),
    )

    private val L9S_LINE = listOf(
        MetroStation(MetroLine.BCN_L9S, ID_BASE + 102, "Mercabarna"),
        MetroStation(MetroLine.BCN_L9S, ID_BASE + 103, "Parc Logístic"),
        MetroStation(MetroLine.BCN_L9S, ID_BASE + 104, "Zona Universitària"),
    )

    private val L10_LINE = listOf(
        MetroStation(MetroLine.BCN_L10, ID_BASE + 105, "La Sagrera"),
        MetroStation(MetroLine.BCN_L10, ID_BASE + 106, "Onze de Setembre"),
        MetroStation(MetroLine.BCN_L10, ID_BASE + 107, "Bon Pastor"),
        MetroStation(MetroLine.BCN_L10, ID_BASE + 108, "Foneria"),
        MetroStation(MetroLine.BCN_L10, ID_BASE + 109, "Foc"),
        MetroStation(MetroLine.BCN_L10, ID_BASE + 110, "Zona Franca"),
        MetroStation(MetroLine.BCN_L10, ID_BASE + 111, "Ecoparc"),
    )

    private val L11_LINE = listOf(
        MetroStation(MetroLine.BCN_L11, ID_BASE + 112, "Trinitat Nova"),
        MetroStation(MetroLine.BCN_L11, ID_BASE + 113, "Casa de l'Aigua"),
        MetroStation(MetroLine.BCN_L11, ID_BASE + 114, "Torre Baró | Vallbona"),
        MetroStation(MetroLine.BCN_L11, ID_BASE + 115, "Ciutat Meridiana"),
    )

    fun allStationsRequest(): List<MetroStation> =
        L1_LINE +
                L2_LINE +
                L3_LINE +
                L4_LINE +
                L5_LINE +
                L9N_LINE +
                L9S_LINE +
                L10_LINE +
                L11_LINE

    fun lineForStationName(name: String): MetroLine? {
        val n = name.trim()
        if (L1_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L1
        if (L2_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L2
        if (L3_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L3
        if (L4_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L4
        if (L5_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L5
        if (L9N_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L9N
        if (L9S_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L9S
        if (L10_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L10
        if (L11_LINE.any { it.name.equals(n, ignoreCase = true) }) return MetroLine.BCN_L11
        return null
    }
}
