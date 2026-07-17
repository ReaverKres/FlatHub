package entities

/**
 * Bangkok BTS/MRT/Airport Link catalog for filter UI + geo line resolution.
 * GREEN = BTS (Sukhumvit + Silom), BLUE = MRT Blue, RED = other mass transit.
 * Names match bangkok_metro_stations.json.
 */
object BangkokMetroStations {
    private const val ID_BASE = 70_000

    private val BLUE_LINE = listOf(
        MetroStation(MetroLine.BLUE, ID_BASE + 0, "MRT Bang Khae"),
        MetroStation(MetroLine.BLUE, ID_BASE + 1, "MRT Bang Pho"),
        MetroStation(MetroLine.BLUE, ID_BASE + 2, "MRT Bang Wa"),
        MetroStation(MetroLine.BLUE, ID_BASE + 3, "MRT Charan 13"),
        MetroStation(MetroLine.BLUE, ID_BASE + 4, "MRT Chatuchak Park"),
        MetroStation(MetroLine.BLUE, ID_BASE + 5, "MRT Fai Chai"),
        MetroStation(MetroLine.BLUE, ID_BASE + 6, "MRT Hua Lamphong"),
        MetroStation(MetroLine.BLUE, ID_BASE + 7, "MRT Huai Khwang"),
        MetroStation(MetroLine.BLUE, ID_BASE + 8, "MRT Itsaraphap"),
        MetroStation(MetroLine.BLUE, ID_BASE + 9, "MRT Kamphaeng Phet"),
        MetroStation(MetroLine.BLUE, ID_BASE + 10, "MRT Khlong Toei"),
        MetroStation(MetroLine.BLUE, ID_BASE + 11, "MRT Lat Phrao"),
        MetroStation(MetroLine.BLUE, ID_BASE + 12, "MRT Lumphini"),
        MetroStation(MetroLine.BLUE, ID_BASE + 13, "MRT Phahon Yothin"),
        MetroStation(MetroLine.BLUE, ID_BASE + 14, "MRT Phetchaburi"),
        MetroStation(MetroLine.BLUE, ID_BASE + 15, "MRT Phra Ram 9"),
        MetroStation(MetroLine.BLUE, ID_BASE + 16, "MRT Queen Sirikit National Convention Center"),
        MetroStation(MetroLine.BLUE, ID_BASE + 17, "MRT Si Lom"),
        MetroStation(MetroLine.BLUE, ID_BASE + 18, "MRT Sirindhorn"),
        MetroStation(MetroLine.BLUE, ID_BASE + 19, "MRT Sukhumvit"),
        MetroStation(MetroLine.BLUE, ID_BASE + 20, "MRT Sutthisan"),
        MetroStation(MetroLine.BLUE, ID_BASE + 21, "MRT Tao Poon"),
        MetroStation(MetroLine.BLUE, ID_BASE + 22, "MRT Tha Phra"),
        MetroStation(MetroLine.BLUE, ID_BASE + 23, "MRT Thailand Cultural Centre"),
    )

    private val RED_LINE = listOf(
        MetroStation(MetroLine.RED, ID_BASE + 200, "Airport Link Ban Thap Chang"),
        MetroStation(MetroLine.RED, ID_BASE + 201, "Airport Link Hua Mak"),
        MetroStation(MetroLine.RED, ID_BASE + 202, "Airport Link Lat Krabang"),
        MetroStation(MetroLine.RED, ID_BASE + 203, "Airport Link Makkasan"),
        MetroStation(MetroLine.RED, ID_BASE + 204, "Airport Link Ramkhamhaeng"),
        MetroStation(MetroLine.RED, ID_BASE + 205, "Airport Link Ratchaprarop"),
        MetroStation(MetroLine.RED, ID_BASE + 206, "Bang Son Station"),
        MetroStation(MetroLine.RED, ID_BASE + 207, "Charoen Nakhon Station"),
        MetroStation(MetroLine.RED, ID_BASE + 208, "Krung Thep Aphiwat Station"),
        MetroStation(MetroLine.RED, ID_BASE + 209, "MRT Bang Kapi"),
        MetroStation(MetroLine.RED, ID_BASE + 210, "MRT Bang Kraso"),
        MetroStation(MetroLine.RED, ID_BASE + 211, "MRT Bang Phlu"),
        MetroStation(MetroLine.RED, ID_BASE + 212, "MRT Bang Son"),
        MetroStation(MetroLine.RED, ID_BASE + 213, "MRT Lat Phrao 83"),
        MetroStation(MetroLine.RED, ID_BASE + 214, "MRT Ministry of Public Health"),
        MetroStation(MetroLine.RED, ID_BASE + 215, "MRT Nonthaburi Civic Center"),
        MetroStation(MetroLine.RED, ID_BASE + 216, "MRT Ratchada"),
        MetroStation(MetroLine.RED, ID_BASE + 217, "MRT Sam Yaek Bang Yai"),
        MetroStation(MetroLine.RED, ID_BASE + 218, "MRT Si Dan"),
        MetroStation(MetroLine.RED, ID_BASE + 219, "MRT Si Thepha"),
        MetroStation(MetroLine.RED, ID_BASE + 220, "MRT Talad Bang Yai"),
        MetroStation(MetroLine.RED, ID_BASE + 221, "MRT Yaek Lam Sali"),
    )

    private val GREEN_LINE = listOf(
        MetroStation(MetroLine.GREEN, ID_BASE + 400, "BTS 11 Infantry Regiment"),
        MetroStation(MetroLine.GREEN, ID_BASE + 401, "BTS Ari"),
        MetroStation(MetroLine.GREEN, ID_BASE + 402, "BTS Asok"),
        MetroStation(MetroLine.GREEN, ID_BASE + 403, "BTS Bang Chak"),
        MetroStation(MetroLine.GREEN, ID_BASE + 404, "BTS Bang Na"),
        MetroStation(MetroLine.GREEN, ID_BASE + 405, "BTS Bearing"),
        MetroStation(MetroLine.GREEN, ID_BASE + 406, "BTS Chang Erawan"),
        MetroStation(MetroLine.GREEN, ID_BASE + 407, "BTS Chit Lom"),
        MetroStation(MetroLine.GREEN, ID_BASE + 408, "BTS Chong Nonsi"),
        MetroStation(MetroLine.GREEN, ID_BASE + 409, "BTS Ekkamai"),
        MetroStation(MetroLine.GREEN, ID_BASE + 410, "BTS Khu Khot"),
        MetroStation(MetroLine.GREEN, ID_BASE + 411, "BTS Krung Thon Buri"),
        MetroStation(MetroLine.GREEN, ID_BASE + 412, "BTS Ladphrao Intersection"),
        MetroStation(MetroLine.GREEN, ID_BASE + 413, "BTS Mo Chit"),
        MetroStation(MetroLine.GREEN, ID_BASE + 414, "BTS Nana"),
        MetroStation(MetroLine.GREEN, ID_BASE + 415, "BTS National Stadium"),
        MetroStation(MetroLine.GREEN, ID_BASE + 416, "BTS On Nut"),
        MetroStation(MetroLine.GREEN, ID_BASE + 417, "BTS Pak Nam"),
        MetroStation(MetroLine.GREEN, ID_BASE + 418, "BTS Phaholyothin 24"),
        MetroStation(MetroLine.GREEN, ID_BASE + 419, "BTS Phaya Thai"),
        MetroStation(MetroLine.GREEN, ID_BASE + 420, "BTS Phra Khanong"),
        MetroStation(MetroLine.GREEN, ID_BASE + 421, "BTS Phrom Phong"),
        MetroStation(MetroLine.GREEN, ID_BASE + 422, "BTS Punnawithi"),
        MetroStation(MetroLine.GREEN, ID_BASE + 423, "BTS Ratchadamri"),
        MetroStation(MetroLine.GREEN, ID_BASE + 424, "BTS Ratchathewi"),
        MetroStation(MetroLine.GREEN, ID_BASE + 425, "BTS Saint Louis"),
        MetroStation(MetroLine.GREEN, ID_BASE + 426, "BTS Sala Daeng"),
        MetroStation(MetroLine.GREEN, ID_BASE + 427, "BTS Samrong"),
        MetroStation(MetroLine.GREEN, ID_BASE + 428, "BTS Saphan Khwai"),
        MetroStation(MetroLine.GREEN, ID_BASE + 429, "BTS Saphan Mai"),
        MetroStation(MetroLine.GREEN, ID_BASE + 430, "BTS Saphan Taksin"),
        MetroStation(MetroLine.GREEN, ID_BASE + 431, "BTS Siam"),
        MetroStation(MetroLine.GREEN, ID_BASE + 432, "BTS Talat Phlu"),
        MetroStation(MetroLine.GREEN, ID_BASE + 433, "BTS Thong Lo (Thong Lor)"),
        MetroStation(MetroLine.GREEN, ID_BASE + 434, "BTS Udom Suk"),
        MetroStation(MetroLine.GREEN, ID_BASE + 435, "BTS Victory Monument"),
        MetroStation(MetroLine.GREEN, ID_BASE + 436, "BTS Wat Phra Sri Mahathat"),
        MetroStation(MetroLine.GREEN, ID_BASE + 437, "BTS Wongwian Yai"),
        MetroStation(MetroLine.GREEN, ID_BASE + 438, "BTS Wutthakat"),
        MetroStation(MetroLine.GREEN, ID_BASE + 439, "BTS Yaek Kor Por Aor"),
    )

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        if (BLUE_LINE.any { it.name.lowercase() == n }) return MetroLine.BLUE
        if (RED_LINE.any { it.name.lowercase() == n }) return MetroLine.RED
        if (GREEN_LINE.any { it.name.lowercase() == n }) return MetroLine.GREEN
        return null
    }

    fun allStationsRequest(): List<MetroStation> =
        BLUE_LINE + RED_LINE + GREEN_LINE
}
