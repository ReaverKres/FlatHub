package entities

/**
 * Bangkok BTS/MRT/Airport Link catalog for filter UI + geo line resolution.
 * Names match bangkok_metro_stations.json.
 */
object BangkokMetroStations {
    private const val ID_BASE = 70_000

    private val SUKHUMVIT_LINE = listOf(
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 0, "BTS 11 Infantry Regiment"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 1, "BTS Ari"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 2, "BTS Asok"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 3, "BTS Bang Chak"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 4, "BTS Bang Na"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 5, "BTS Bearing"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 6, "BTS Chang Erawan"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 7, "BTS Chit Lom"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 8, "BTS Ekkamai"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 9, "BTS Khu Khot"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 10, "BTS Ladphrao Intersection"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 11, "BTS Mo Chit"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 12, "BTS Nana"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 13, "BTS On Nut"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 14, "BTS Pak Nam"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 15, "BTS Phaholyothin 24"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 16, "BTS Phaya Thai"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 17, "BTS Phra Khanong"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 18, "BTS Phrom Phong"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 19, "BTS Punnawithi"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 20, "BTS Ratchathewi"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 21, "BTS Samrong"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 22, "BTS Saphan Khwai"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 23, "BTS Saphan Mai"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 24, "BTS Siam"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 25, "BTS Thong Lo (Thong Lor)"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 26, "BTS Udom Suk"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 27, "BTS Victory Monument"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 28, "BTS Wat Phra Sri Mahathat"),
        MetroStation(MetroLine.BKK_BTS_SUKHUMVIT, ID_BASE + 29, "BTS Yaek Kor Por Aor"),
    )

    private val SILOM_LINE = listOf(
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 30, "BTS Chong Nonsi"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 31, "BTS Krung Thon Buri"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 32, "BTS National Stadium"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 33, "BTS Ratchadamri"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 34, "BTS Saint Louis"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 35, "BTS Sala Daeng"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 36, "BTS Saphan Taksin"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 37, "BTS Talat Phlu"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 38, "BTS Wongwian Yai"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 39, "BTS Wutthakat"),
        MetroStation(MetroLine.BKK_BTS_SILOM, ID_BASE + 40, "Charoen Nakhon Station"),
    )

    private val MRT_BLUE_LINE = listOf(
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 41, "Krung Thep Aphiwat Station"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 42, "MRT Bang Khae"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 43, "MRT Bang Pho"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 44, "MRT Bang Wa"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 45, "MRT Charan 13"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 46, "MRT Chatuchak Park"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 47, "MRT Fai Chai"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 48, "MRT Hua Lamphong"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 49, "MRT Huai Khwang"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 50, "MRT Itsaraphap"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 51, "MRT Kamphaeng Phet"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 52, "MRT Khlong Toei"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 53, "MRT Lat Phrao"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 54, "MRT Lumphini"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 55, "MRT Phahon Yothin"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 56, "MRT Phetchaburi"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 57, "MRT Phra Ram 9"),
        MetroStation(
            MetroLine.BKK_MRT_BLUE,
            ID_BASE + 58,
            "MRT Queen Sirikit National Convention Center"
        ),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 59, "MRT Si Lom"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 60, "MRT Sirindhorn"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 61, "MRT Sukhumvit"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 62, "MRT Sutthisan"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 63, "MRT Tao Poon"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 64, "MRT Tha Phra"),
        MetroStation(MetroLine.BKK_MRT_BLUE, ID_BASE + 65, "MRT Thailand Cultural Centre"),
    )

    private val MRT_PURPLE_LINE = listOf(
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 66, "Bang Son Station"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 67, "MRT Bang Kapi"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 68, "MRT Bang Kraso"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 69, "MRT Bang Phlu"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 70, "MRT Bang Son"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 71, "MRT Lat Phrao 83"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 72, "MRT Ministry of Public Health"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 73, "MRT Nonthaburi Civic Center"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 74, "MRT Ratchada"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 75, "MRT Sam Yaek Bang Yai"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 76, "MRT Si Dan"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 77, "MRT Si Thepha"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 78, "MRT Talad Bang Yai"),
        MetroStation(MetroLine.BKK_MRT_PURPLE, ID_BASE + 79, "MRT Yaek Lam Sali"),
    )

    private val ARL_LINE = listOf(
        MetroStation(MetroLine.BKK_ARL, ID_BASE + 80, "Airport Link Ban Thap Chang"),
        MetroStation(MetroLine.BKK_ARL, ID_BASE + 81, "Airport Link Hua Mak"),
        MetroStation(MetroLine.BKK_ARL, ID_BASE + 82, "Airport Link Lat Krabang"),
        MetroStation(MetroLine.BKK_ARL, ID_BASE + 83, "Airport Link Makkasan"),
        MetroStation(MetroLine.BKK_ARL, ID_BASE + 84, "Airport Link Ramkhamhaeng"),
        MetroStation(MetroLine.BKK_ARL, ID_BASE + 85, "Airport Link Ratchaprarop"),
    )

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        if (SUKHUMVIT_LINE.any { it.name.lowercase() == n }) return MetroLine.BKK_BTS_SUKHUMVIT
        if (SILOM_LINE.any { it.name.lowercase() == n }) return MetroLine.BKK_BTS_SILOM
        if (MRT_BLUE_LINE.any { it.name.lowercase() == n }) return MetroLine.BKK_MRT_BLUE
        if (MRT_PURPLE_LINE.any { it.name.lowercase() == n }) return MetroLine.BKK_MRT_PURPLE
        if (ARL_LINE.any { it.name.lowercase() == n }) return MetroLine.BKK_ARL
        return null
    }

    fun allStationsRequest(): List<MetroStation> =
        SUKHUMVIT_LINE +
                SILOM_LINE +
                MRT_BLUE_LINE +
                MRT_PURPLE_LINE +
                ARL_LINE
}
