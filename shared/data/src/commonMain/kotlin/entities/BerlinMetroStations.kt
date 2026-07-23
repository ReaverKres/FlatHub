package entities

/**
 * Berlin U-/S-Bahn catalog for filter UI + geo line resolution.
 * Names match berlin_metro_stations.json.
 */
object BerlinMetroStations {
    private const val ID_BASE = 60000

    private val U1_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 0, "Warschauer Straße"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 1, "Schlesisches Tor"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 2, "Görlitzer Bahnhof"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 3, "Kottbusser Tor"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 4, "Prinzenstraße"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 5, "Hallesches Tor"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 6, "Mehringdamm"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 7, "Gleisdreieck"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 8, "Viktoria-Luise-Platz"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 9, "Bayerischer Platz"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 10, "Rathaus Schöneberg"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 11, "Innsbrucker Platz"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 12, "Fehrbelliner Platz"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 13, "Wittenbergplatz"),
        MetroStation(MetroLine.BERLIN_U1, ID_BASE + 14, "Uhlandstraße"),
    )

    private val U2_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 15, "Pankow"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 16, "Vinetastraße"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 17, "Schönhauser Allee"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 18, "Senefelderplatz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 19, "Rosa-Luxemburg-Platz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 20, "Alexanderplatz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 21, "Klosterstraße"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 22, "Märkisches Museum"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 23, "Spittelmarkt"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 24, "Hausvogteiplatz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 25, "Stadtmitte"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 26, "Mohrenstraße"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 27, "U Potsdamer Platz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 28, "Mendelssohn-Bartholdy-Park"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 29, "Gleisdreieck"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 30, "Bülowstraße"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 31, "Nollendorfplatz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 32, "Wittenbergplatz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 33, "Zoologischer Garten"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 34, "Sophie-Charlotte-Platz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 35, "Bismarckstraße"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 36, "Deutsche Oper"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 37, "Ernst-Reuter-Platz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 38, "Neu-Westend"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 39, "Theodor-Heuss-Platz"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 40, "Olympia-Stadion"),
        MetroStation(MetroLine.BERLIN_U2, ID_BASE + 41, "Ruhleben"),
    )

    private val U3_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 42, "Nollendorfplatz"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 43, "Viktoria-Luise-Platz"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 44, "Bayerischer Platz"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 45, "Rüdesheimer Platz"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 46, "Breitenbachplatz"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 47, "Heidelberger Platz"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 48, "Oskar-Helene-Heim"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 49, "Onkel Toms Hütte"),
        MetroStation(MetroLine.BERLIN_U3, ID_BASE + 50, "Krumme Lanke"),
    )

    private val U4_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U4, ID_BASE + 51, "Nollendorfplatz"),
        MetroStation(MetroLine.BERLIN_U4, ID_BASE + 52, "Viktoria-Luise-Platz"),
        MetroStation(MetroLine.BERLIN_U4, ID_BASE + 53, "Bayerischer Platz"),
        MetroStation(MetroLine.BERLIN_U4, ID_BASE + 54, "Rathaus Schöneberg"),
        MetroStation(MetroLine.BERLIN_U4, ID_BASE + 55, "Innsbrucker Platz"),
    )

    private val U5_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 56, "Hönow"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 57, "Kaulsdorf-Nord"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 58, "Kienberg (Gärten der Welt)"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 59, "Cottbusser Platz"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 60, "Elsterwerdaer Platz"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 61, "Wuhletal"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 62, "Biesdorf-Süd"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 63, "Tierpark"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 64, "Friedrichsfelde"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 65, "Magdalenenstraße (Campus für Demokratie)"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 66, "Frankfurter Allee"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 67, "Samariterstraße"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 68, "Frankfurter Tor"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 69, "Weberwiese"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 70, "Strausberger Platz"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 71, "Schillingstraße"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 72, "Alexanderplatz"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 73, "Museumsinsel"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 74, "Unter den Linden"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 75, "Bundestag"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 76, "Brandenburger Tor"),
        MetroStation(MetroLine.BERLIN_U5, ID_BASE + 77, "Hauptbahnhof"),
    )

    private val U6_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 78, "Alt-Tegel"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 79, "Borsigwerke"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 80, "Holzhauser Straße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 81, "Otisstraße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 82, "Scharnweberstraße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 83, "Kurt-Schumacher-Platz"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 84, "Afrikanische Straße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 85, "Rehberge"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 86, "Leopoldplatz"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 87, "Wedding"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 88, "Seestraße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 89, "Amrumer Straße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 90, "Westhafen"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 91, "Beusselstraße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 92, "Jungfernheide"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 93, "Messe Nord/ZOB"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 94, "Kaiserdamm"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 95, "Sophie-Charlotte-Platz"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 96, "Bismarckstraße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 97, "Wilmersdorfer Straße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 98, "Adenauerplatz"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 99, "Konstanzer Straße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 100, "Fehrbelliner Platz"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 101, "Berliner Straße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 102, "Bayerischer Platz"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 103, "Platz der Luftbrücke"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 104, "Paradestraße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 105, "Tempelhof"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 106, "Alt-Tempelhof"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 107, "Kaiserin-Augusta-Straße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 108, "Ullsteinstraße"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 109, "Westphalweg"),
        MetroStation(MetroLine.BERLIN_U6, ID_BASE + 110, "Alt-Mariendorf"),
    )

    private val U7_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 111, "Rathaus Spandau"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 112, "Altstadt Spandau"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 113, "Zitadelle"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 114, "Haselhorst"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 115, "Paulsternstraße"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 116, "Rohrdamm"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 117, "Siemensdamm"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 118, "Halemweg"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 119, "Jakob-Kaiser-Platz"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 120, "Jungfernheide"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 121, "Mierendorffplatz"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 122, "Richard-Wagner-Platz"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 123, "Bismarckstraße"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 124, "Wilmersdorfer Straße"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 125, "Adenauerplatz"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 126, "Konstanzer Straße"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 127, "Fehrbelliner Platz"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 128, "Blissestraße"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 129, "Berliner Straße"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 130, "Bayerischer Platz"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 131, "Südstern"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 132, "Hermannplatz"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 133, "Karl-Marx-Straße"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 134, "Rathaus Neukölln"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 135, "Grenzallee"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 136, "Blaschkoallee"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 137, "Parchimer Allee"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 138, "Britz-Süd"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 139, "Johannisthaler Chaussee"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 140, "Lipschitzallee"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 141, "Wutzkyallee"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 142, "Zwickauer Damm"),
        MetroStation(MetroLine.BERLIN_U7, ID_BASE + 143, "Rudow"),
    )

    private val U8_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 144, "Wittenau"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 145, "Karl-Bonhoeffer-Nervenklinik"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 146, "Lindauer Allee"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 147, "Paracelsus-Bad"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 148, "Residenzstraße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 149, "Franz-Neumann-Platz"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 150, "Pankstraße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 151, "Osloer Straße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 152, "Voltastraße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 153, "Bernauer Straße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 154, "Rosenthaler Platz"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 155, "Weinmeisterstraße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 156, "Alexanderplatz"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 157, "Jannowitzbrücke"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 158, "Heinrich-Heine-Straße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 159, "Moritzplatz"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 160, "Kottbusser Tor"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 161, "Schönleinstraße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 162, "Hermannplatz"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 163, "Boddinstraße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 164, "Leinestraße"),
        MetroStation(MetroLine.BERLIN_U8, ID_BASE + 165, "Hermannstraße"),
    )

    private val U9_LINE = listOf(
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 166, "Osloer Straße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 167, "Nauener Platz"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 168, "Leopoldplatz"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 169, "Amrumer Straße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 170, "Westhafen"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 171, "Birkenstraße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 172, "Turmstraße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 173, "Hansaplatz"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 174, "Ernst-Reuter-Platz"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 175, "Zoologischer Garten"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 176, "Kurfürstendamm"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 177, "Spichernstraße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 178, "Güntzelstraße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 179, "Berliner Straße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 180, "Bundesplatz"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 181, "Friedrich-Wilhelm-Platz"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 182, "Walther-Schreiber-Platz"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 183, "Schloßstraße"),
        MetroStation(MetroLine.BERLIN_U9, ID_BASE + 184, "Rathaus Steglitz"),
    )

    private val S1_LINE = listOf(
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 185, "Wannsee"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 186, "Nikolassee"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 187, "Schlachtensee"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 188, "Mexikoplatz"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 189, "Schöneberg"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 190, "Südkreuz (Nord-Süd)"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 191, "Yorckstraße"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 192, "Potsdamer Platz"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 193, "S Friedrichstraße"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 194, "Hackescher Markt"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 195, "Oranienburger Straße"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 196, "Nordbahnhof"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 197, "Gesundbrunnen"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 198, "Wittenau"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 199, "Karl-Bonhoeffer-Nervenklinik"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 200, "Waidmannslust"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 201, "Schönholz"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 202, "Wilhelmsruh"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 203, "Pankow"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 204, "Buch"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 205, "Bernau"),
        MetroStation(MetroLine.BERLIN_S1, ID_BASE + 206, "Oranienburg"),
    )

    private val S2_LINE = listOf(
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 207, "Blankenburg"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 208, "Pankow"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 209, "Schönhauser Allee"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 210, "Bornholmer Straße"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 211, "Gesundbrunnen"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 212, "Nordbahnhof"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 213, "S Friedrichstraße"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 214, "Hackescher Markt"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 215, "Alexanderplatz"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 216, "Jannowitzbrücke"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 217, "Ostbahnhof"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 218, "Treptower Park"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 219, "Plänterwald"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 220, "Baumschulenweg"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 221, "Schöneweide"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 222, "Adlershof"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 223, "Grünau"),
        MetroStation(MetroLine.BERLIN_S2, ID_BASE + 224, "Lichtenrade"),
    )

    private val S3_LINE = listOf(
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 225, "Spandau"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 226, "Stresow"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 227, "Westkreuz"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 228, "Charlottenburg"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 229, "Savignyplatz"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 230, "Zoologischer Garten"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 231, "Hauptbahnhof"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 232, "S Friedrichstraße"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 233, "Hackescher Markt"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 234, "Alexanderplatz"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 235, "Jannowitzbrücke"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 236, "Ostbahnhof"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 237, "Ostkreuz"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 238, "Karlshorst"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 239, "Köpenick"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 240, "Hirschgarten"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 241, "Friedrichshagen"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 242, "Rahnsdorf"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 243, "Wilhelmshagen"),
        MetroStation(MetroLine.BERLIN_S3, ID_BASE + 244, "Erkner"),
    )

    private val S5_LINE = listOf(
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 245, "Westkreuz"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 246, "Charlottenburg"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 247, "Zoologischer Garten"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 248, "Hauptbahnhof"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 249, "S Friedrichstraße"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 250, "Hackescher Markt"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 251, "Alexanderplatz"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 252, "Jannowitzbrücke"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 253, "Ostbahnhof"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 254, "Lichtenberg"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 255, "Friedrichsfelde Ost"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 256, "Wuhletal"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 257, "Nöldnerplatz"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 258, "Landsberger Allee"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 259, "Gehrenseestraße"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 260, "Mahlsdorf"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 261, "Hoppegarten"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 262, "Strausberg"),
        MetroStation(MetroLine.BERLIN_S5, ID_BASE + 263, "Strausberg Nord"),
    )

    private val S7_LINE = listOf(
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 264, "Ahrensfelde"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 265, "Marzahn"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 266, "Springpfuhl"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 267, "Lichtenberg"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 268, "Friedrichsfelde Ost"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 269, "Ostkreuz"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 270, "Jannowitzbrücke"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 271, "Alexanderplatz"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 272, "S Friedrichstraße"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 273, "Hauptbahnhof"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 274, "Zoologischer Garten"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 275, "Charlottenburg"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 276, "Westkreuz"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 277, "Grunewald"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 278, "Olympiastadion"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 279, "Pichelsberg"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 280, "Nikolassee"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 281, "Wannsee"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 282, "Sundgauer Straße"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 283, "Lankwitz"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 284, "Lichterfelde Ost"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 285, "Lichterfelde Süd"),
        MetroStation(MetroLine.BERLIN_S7, ID_BASE + 286, "Potsdam Hauptbahnhof"),
    )

    private val S9_LINE = listOf(
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 287, "Spandau"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 288, "Staaken"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 289, "Westkreuz"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 290, "Hauptbahnhof"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 291, "Ostbahnhof"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 292, "Ostkreuz"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 293, "Schöneweide"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 294, "Adlershof"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 295, "Grünau"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 296, "Altglienicke"),
        MetroStation(MetroLine.BERLIN_S9, ID_BASE + 297, "Flughafen BER"),
    )

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        if (U1_LINE.any { it.name.lowercase() == n }) return U1_LINE.first().line
        if (U2_LINE.any { it.name.lowercase() == n }) return U2_LINE.first().line
        if (U3_LINE.any { it.name.lowercase() == n }) return U3_LINE.first().line
        if (U4_LINE.any { it.name.lowercase() == n }) return U4_LINE.first().line
        if (U5_LINE.any { it.name.lowercase() == n }) return U5_LINE.first().line
        if (U6_LINE.any { it.name.lowercase() == n }) return U6_LINE.first().line
        if (U7_LINE.any { it.name.lowercase() == n }) return U7_LINE.first().line
        if (U8_LINE.any { it.name.lowercase() == n }) return U8_LINE.first().line
        if (U9_LINE.any { it.name.lowercase() == n }) return U9_LINE.first().line
        if (S1_LINE.any { it.name.lowercase() == n }) return S1_LINE.first().line
        if (S2_LINE.any { it.name.lowercase() == n }) return S2_LINE.first().line
        if (S3_LINE.any { it.name.lowercase() == n }) return S3_LINE.first().line
        if (S5_LINE.any { it.name.lowercase() == n }) return S5_LINE.first().line
        if (S7_LINE.any { it.name.lowercase() == n }) return S7_LINE.first().line
        if (S9_LINE.any { it.name.lowercase() == n }) return S9_LINE.first().line
        return null
    }

    fun allStationsRequest(): List<MetroStation> =
        U1_LINE +
                U2_LINE +
                U3_LINE +
                U4_LINE +
                U5_LINE +
                U6_LINE +
                U7_LINE +
                U8_LINE +
                U9_LINE +
                S1_LINE +
                S2_LINE +
                S3_LINE +
                S5_LINE +
                S7_LINE +
                S9_LINE
}
