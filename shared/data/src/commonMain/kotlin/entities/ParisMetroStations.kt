package entities

/**
 * Paris Métro + RER catalog for filter UI + geo line resolution.
 * Names match paris_metro_stations.json (interchanges duplicated per line).
 */
object ParisMetroStations {
    private const val ID_BASE = 120_000

    private fun stations(line: MetroLine, startId: Int, names: List<String>): List<MetroStation> =
        names.mapIndexed { i, name -> MetroStation(line, ID_BASE + startId + i, name) }

    private val M1_LINE = stations(
        MetroLine.PARIS_M1, 0,
        listOf(
            "La Défense",
            "Esplanade de La Défense",
            "Pont de Neuilly",
            "Les Sablons",
            "Porte Maillot",
            "Argentine",
            "Charles de Gaulle - Étoile",
            "George V",
            "Franklin D. Roosevelt",
            "Champs-Élysées - Clemenceau",
            "Concorde",
            "Tuileries",
            "Palais Royal - Musée du Louvre",
            "Châtelet",
            "Hôtel de Ville",
            "Saint-Paul",
            "Bastille",
            "Gare de Lyon",
            "Reuilly - Diderot",
            "Nation",
            "Porte de Vincennes",
            "Château de Vincennes",
        ),
    )

    private val M2_LINE = stations(
        MetroLine.PARIS_M2, 50,
        listOf(
            "Porte Dauphine", "Victor Hugo", "Charles de Gaulle - Étoile", "Ternes", "Courcelles",
            "Monceau", "Villiers", "Rome", "Place de Clichy", "Blanche", "Pigalle", "Anvers",
            "Barbès - Rochechouart", "La Chapelle", "Stalingrad", "Jaurès", "Colonel Fabien",
            "Belleville", "Ménilmontant", "Père Lachaise", "Philippe Auguste", "Alexandre Dumas",
            "Avron", "Nation",
        ),
    )

    private val M3_LINE = stations(
        MetroLine.PARIS_M3, 100,
        listOf(
            "Pont de Levallois - Bécon",
            "Anatole France",
            "Porte de Champerret",
            "Pereire",
            "Wagram",
            "Malesherbes",
            "Villiers",
            "Europe",
            "Saint-Lazare",
            "Havre - Caumartin",
            "Opéra",
            "Quatre-Septembre",
            "Bourse",
            "Sentier",
            "Réaumur - Sébastopol",
            "Arts et Métiers",
            "Temple",
            "République",
            "Oberkampf",
            "Parmentier",
            "Gambetta",
            "Pelleport",
            "Porte de Bagnolet",
            "Gallieni",
        ),
    )

    private val M4_LINE = stations(
        MetroLine.PARIS_M4, 150,
        listOf(
            "Porte de Clignancourt", "Simplon", "Marcadet - Poissonniers", "Château Rouge",
            "Barbès - Rochechouart", "Gare du Nord", "Gare de l'Est", "Château d'Eau",
            "Strasbourg - Saint-Denis", "Réaumur - Sébastopol", "Étienne Marcel", "Les Halles",
            "Châtelet", "Cité", "Saint-Michel", "Odéon", "Saint-Germain-des-Prés", "Saint-Sulpice",
            "Saint-Placide", "Montparnasse - Bienvenüe", "Raspail", "Denfert-Rochereau", "Alésia",
            "Porte d'Orléans", "Mairie de Montrouge",
        ),
    )

    private val M5_LINE = stations(
        MetroLine.PARIS_M5, 200,
        listOf(
            "Bobigny - Pablo Picasso", "Église de Pantin", "Hoche", "Porte de Pantin", "Ourcq",
            "Laumière", "Jaurès", "Stalingrad", "Gare du Nord", "Gare de l'Est",
            "Jacques Bonsergent", "République", "Oberkampf", "Richard-Lenoir", "Bréguet - Sabin",
            "Bastille", "Quai de la Rapée", "Gare d'Austerlitz", "Saint-Marcel", "Campo-Formio",
            "Place d'Italie",
        ),
    )

    private val M6_LINE = stations(
        MetroLine.PARIS_M6, 250,
        listOf(
            "Charles de Gaulle - Étoile", "Kléber", "Boissière", "Trocadéro", "Passy", "Bir-Hakeim",
            "Dupleix", "La Motte-Picquet - Grenelle", "Cambronne", "Sèvres - Lecourbe", "Pasteur",
            "Montparnasse - Bienvenüe", "Edgar Quinet", "Raspail", "Denfert-Rochereau",
            "Saint-Jacques", "Glacière", "Corvisart", "Place d'Italie", "Nation", "Bercy",
            "Quai de la Gare", "Chevaleret", "Bibliothèque François Mitterrand",
        ),
    )

    private val M7_LINE = stations(
        MetroLine.PARIS_M7, 300,
        listOf(
            "La Courneuve - 8 Mai 1945", "Fort d'Aubervilliers", "Porte de la Villette",
            "Corentin Cariou", "Crimée", "Riquet", "Stalingrad", "Louis Blanc", "Gare de l'Est",
            "Poissonnière", "Cadet", "Le Peletier", "Chaussée d'Antin - La Fayette", "Opéra",
            "Pyramides", "Palais Royal - Musée du Louvre", "Pont Neuf", "Châtelet", "Pont Marie",
            "Sully - Morland", "Jussieu", "Place Monge", "Censier - Daubenton", "Les Gobelins",
            "Place d'Italie", "Tolbiac", "Maison Blanche", "Porte d'Italie",
            "Villejuif - Louis Aragon",
        ),
    )

    private val M8_LINE = stations(
        MetroLine.PARIS_M8, 350,
        listOf(
            "Balard", "Lourmel", "Boucicaut", "Commerce", "La Motte-Picquet - Grenelle",
            "École Militaire", "Invalides", "Concorde", "Madeleine", "Opéra", "Richelieu - Drouot",
            "Grands Boulevards", "Bonne Nouvelle", "Strasbourg - Saint-Denis", "République",
            "Filles du Calvaire", "Chemin Vert", "Saint-Paul", "Bastille", "Ledru-Rollin",
            "Montgallet", "Daumesnil", "Porte Dorée", "Créteil - Université",
        ),
    )

    private val M9_LINE = stations(
        MetroLine.PARIS_M9, 400,
        listOf(
            "Pont de Sèvres", "Billancourt", "Marcel Sembat", "Porte de Saint-Cloud", "Exelmans",
            "Michel-Ange - Molitor", "Jasmin", "Ranelagh", "La Muette", "Trocadéro", "Iéna",
            "Alma - Marceau", "Franklin D. Roosevelt", "Saint-Philippe du Roule", "Miromesnil",
            "Saint-Augustin", "Havre - Caumartin", "Chaussée d'Antin - La Fayette",
            "Richelieu - Drouot", "Grands Boulevards", "Strasbourg - Saint-Denis", "République",
            "Oberkampf", "Saint-Ambroise", "Voltaire", "Charonne", "Nation", "Porte de Montreuil",
            "Mairie de Montreuil",
        ),
    )

    private val M10_LINE = stations(
        MetroLine.PARIS_M10, 450,
        listOf(
            "Boulogne - Pont de Saint-Cloud", "Porte d'Auteuil", "Michel-Ange - Molitor",
            "Javel - André Citroën", "Charles Michels", "La Motte-Picquet - Grenelle", "Ségur",
            "Duroc", "Sèvres - Babylone", "Mabillon", "Odéon", "Cluny - La Sorbonne",
            "Maubert - Mutualité", "Cardinal Lemoine", "Jussieu", "Gare d'Austerlitz",
        ),
    )

    private val M11_LINE = stations(
        MetroLine.PARIS_M11, 500,
        listOf(
            "Châtelet", "Hôtel de Ville", "Rambuteau", "Arts et Métiers", "République", "Goncourt",
            "Belleville", "Pyrénées", "Jourdain", "Place des Fêtes", "Porte des Lilas",
            "Mairie des Lilas",
        ),
    )

    private val M12_LINE = stations(
        MetroLine.PARIS_M12, 550,
        listOf(
            "Mairie d'Issy", "Porte de Versailles", "Convention", "Vaugirard", "Volontaires",
            "Pasteur", "Montparnasse - Bienvenüe", "Notre-Dame-des-Champs", "Sèvres - Babylone",
            "Rue du Bac", "Solférino", "Assemblée Nationale", "Concorde", "Madeleine",
            "Saint-Lazare", "Trinité - d'Estienne d'Orves", "Notre-Dame-de-Lorette", "Pigalle",
            "Abbesses", "Jules Joffrin", "Marcadet - Poissonniers", "Front Populaire",
        ),
    )

    private val M13_LINE = stations(
        MetroLine.PARIS_M13, 600,
        listOf(
            "Saint-Denis - Université",
            "Basilique de Saint-Denis",
            "Garibaldi",
            "Porte de Saint-Ouen",
            "Guy Môquet",
            "Place de Clichy",
            "Liège",
            "Saint-Lazare",
            "Miromesnil",
            "Champs-Élysées - Clemenceau",
            "Invalides",
            "Duroc",
            "Montparnasse - Bienvenüe",
            "Gaîté",
            "Pernety",
            "Plaisance",
            "Porte de Vanves",
            "Châtillon - Montrouge",
        ),
    )

    private val M14_LINE = stations(
        MetroLine.PARIS_M14, 650,
        listOf(
            "Saint-Lazare", "Madeleine", "Pyramides", "Châtelet", "Gare de Lyon", "Bercy",
            "Cour Saint-Émilion", "Bibliothèque François Mitterrand", "Olympiades",
        ),
    )

    private val RER_A_LINE = stations(
        MetroLine.PARIS_RER_A, 700,
        listOf(
            "La Défense", "Neuilly - Porte Maillot", "Charles de Gaulle - Étoile", "Auber",
            "Châtelet - Les Halles", "Gare de Lyon", "Nation", "Vincennes", "Val de Fontenay",
            "Marne-la-Vallée - Chessy", "Boissy-Saint-Léger",
        ),
    )

    private val RER_B_LINE = stations(
        MetroLine.PARIS_RER_B, 750,
        listOf(
            "Gare du Nord", "Châtelet - Les Halles", "Saint-Michel - Notre-Dame", "Luxembourg",
            "Port-Royal", "Denfert-Rochereau", "Cité Universitaire", "Orly Airport",
            "Aéroport Charles de Gaulle 1", "Aéroport Charles de Gaulle 2 TGV",
        ),
    )

    private val allLines = listOf(
        M1_LINE, M2_LINE, M3_LINE, M4_LINE, M5_LINE, M6_LINE, M7_LINE, M8_LINE, M9_LINE,
        M10_LINE, M11_LINE, M12_LINE, M13_LINE, M14_LINE, RER_A_LINE, RER_B_LINE,
    )

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        return allLines.firstOrNull { line -> line.any { it.name.lowercase() == n } }
            ?.firstOrNull()?.line
    }

    fun allStationsRequest(): List<MetroStation> = allLines.flatten()
}
