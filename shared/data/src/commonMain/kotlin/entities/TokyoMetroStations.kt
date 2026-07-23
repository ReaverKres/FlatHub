package entities

/**
 * Tokyo JR Yamanote / Tokyo Metro / Toei catalog for filter UI + geo line resolution.
 * Names match tokyo_metro_stations.json.
 */
object TokyoMetroStations {
    private const val ID_BASE = 90_000

    private val YAMANOTE_LINE = listOf(
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90000, "JR Yamanote Tokyo"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90001, "JR Yamanote Kanda"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90002, "JR Yamanote Akihabara"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90003, "JR Yamanote Okachimachi"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90004, "JR Yamanote Ueno"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90005, "JR Yamanote Uguisudani"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90006, "JR Yamanote Nippori"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90007, "JR Yamanote Nishi-Nippori"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90008, "JR Yamanote Tabata"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90009, "JR Yamanote Komagome"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90010, "JR Yamanote Sugamo"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90011, "JR Yamanote Otsuka"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90012, "JR Yamanote Ikebukuro"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90013, "JR Yamanote Mejiro"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90014, "JR Yamanote Takadanobaba"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90015, "JR Yamanote Shin-Okubo"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90016, "JR Yamanote Shinjuku"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90017, "JR Yamanote Yoyogi"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90018, "JR Yamanote Harajuku"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90019, "JR Yamanote Shibuya"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90020, "JR Yamanote Ebisu"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90021, "JR Yamanote Meguro"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90022, "JR Yamanote Gotanda"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90023, "JR Yamanote Osaki"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90024, "JR Yamanote Shinagawa"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90025, "JR Yamanote Tamachi"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90026, "JR Yamanote Hamamatsucho"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90027, "JR Yamanote Shinbashi"),
        MetroStation(MetroLine.TOKYO_YAMANOTE, 90028, "JR Yamanote Yurakucho"),
    )

    private val GINZA_LINE = listOf(
        MetroStation(MetroLine.TOKYO_GINZA, 90029, "Metro Ginza Shibuya"),
        MetroStation(MetroLine.TOKYO_GINZA, 90030, "Metro Ginza Omotesando"),
        MetroStation(MetroLine.TOKYO_GINZA, 90031, "Metro Ginza Gaienmae"),
        MetroStation(MetroLine.TOKYO_GINZA, 90032, "Metro Ginza Akasaka-mitsuke"),
        MetroStation(MetroLine.TOKYO_GINZA, 90033, "Metro Ginza Tameike-Sanno"),
        MetroStation(MetroLine.TOKYO_GINZA, 90034, "Metro Ginza Toranomon"),
        MetroStation(MetroLine.TOKYO_GINZA, 90035, "Metro Ginza Shimbashi"),
        MetroStation(MetroLine.TOKYO_GINZA, 90036, "Metro Ginza Ginza"),
        MetroStation(MetroLine.TOKYO_GINZA, 90037, "Metro Ginza Kyobashi"),
        MetroStation(MetroLine.TOKYO_GINZA, 90038, "Metro Ginza Nihombashi"),
        MetroStation(MetroLine.TOKYO_GINZA, 90039, "Metro Ginza Mitsukoshimae"),
        MetroStation(MetroLine.TOKYO_GINZA, 90040, "Metro Ginza Kanda"),
        MetroStation(MetroLine.TOKYO_GINZA, 90041, "Metro Ginza Ueno"),
        MetroStation(MetroLine.TOKYO_GINZA, 90042, "Metro Ginza Asakusa"),
    )

    private val MARUNOUCHI_LINE = listOf(
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90043, "Metro Marunouchi Tokyo"),
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90044, "Metro Marunouchi Otemachi"),
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90045, "Metro Marunouchi Akasaka-mitsuke"),
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90046, "Metro Marunouchi Shinjuku"),
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90047, "Metro Marunouchi Ikebukuro"),
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90048, "Metro Marunouchi Korakuen"),
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90049, "Metro Marunouchi Yotsuya"),
        MetroStation(MetroLine.TOKYO_MARUNOUCHI, 90050, "Metro Marunouchi Nishi-Shinjuku"),
    )

    private val HIBIYA_LINE = listOf(
        MetroStation(MetroLine.TOKYO_HIBIYA, 90051, "Metro Hibiya Ebisu"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90052, "Metro Hibiya Roppongi"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90053, "Metro Hibiya Kamiyacho"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90054, "Metro Hibiya Kasumigaseki"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90055, "Metro Hibiya Hibiya"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90056, "Metro Hibiya Ginza"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90057, "Metro Hibiya Tsukiji"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90058, "Metro Hibiya Minowa"),
        MetroStation(MetroLine.TOKYO_HIBIYA, 90059, "Metro Hibiya Kita-Senju"),
    )

    private val TOZAI_LINE = listOf(
        MetroStation(MetroLine.TOKYO_TOZAI, 90060, "Metro Tozai Nakano"),
        MetroStation(MetroLine.TOKYO_TOZAI, 90061, "Metro Tozai Takadanobaba"),
        MetroStation(MetroLine.TOKYO_TOZAI, 90062, "Metro Tozai Iidabashi"),
        MetroStation(MetroLine.TOKYO_TOZAI, 90063, "Metro Tozai Otemachi"),
        MetroStation(MetroLine.TOKYO_TOZAI, 90064, "Metro Tozai Nihombashi"),
        MetroStation(MetroLine.TOKYO_TOZAI, 90065, "Metro Tozai Monzen-Nakacho"),
        MetroStation(MetroLine.TOKYO_TOZAI, 90066, "Metro Tozai Nishi-Funabashi"),
    )

    private val CHIYODA_LINE = listOf(
        MetroStation(MetroLine.TOKYO_CHIYODA, 90067, "Metro Chiyoda Omotesando"),
        MetroStation(MetroLine.TOKYO_CHIYODA, 90068, "Metro Chiyoda Meiji-Jingumae"),
        MetroStation(MetroLine.TOKYO_CHIYODA, 90069, "Metro Chiyoda Yoyogi-Koen"),
        MetroStation(MetroLine.TOKYO_CHIYODA, 90070, "Metro Chiyoda Nezu"),
        MetroStation(MetroLine.TOKYO_CHIYODA, 90071, "Metro Chiyoda Sendagi"),
        MetroStation(MetroLine.TOKYO_CHIYODA, 90072, "Metro Chiyoda Nishi-Nippori"),
        MetroStation(MetroLine.TOKYO_CHIYODA, 90073, "Metro Chiyoda Ayase"),
    )

    private val YURAKUCHO_LINE = listOf(
        MetroStation(MetroLine.TOKYO_YURAKUCHO, 90074, "Metro Yurakucho Ikebukuro"),
        MetroStation(MetroLine.TOKYO_YURAKUCHO, 90075, "Metro Yurakucho Higashi-Ikebukuro"),
        MetroStation(MetroLine.TOKYO_YURAKUCHO, 90076, "Metro Yurakucho Iidabashi"),
        MetroStation(MetroLine.TOKYO_YURAKUCHO, 90077, "Metro Yurakucho Nagatacho"),
        MetroStation(MetroLine.TOKYO_YURAKUCHO, 90078, "Metro Yurakucho Yurakucho"),
    )

    private val HANZOMON_LINE = listOf(
        MetroStation(MetroLine.TOKYO_HANZOMON, 90079, "Metro Hanzomon Shibuya"),
        MetroStation(MetroLine.TOKYO_HANZOMON, 90080, "Metro Hanzomon Omotesando"),
        MetroStation(MetroLine.TOKYO_HANZOMON, 90081, "Metro Hanzomon Nagatacho"),
        MetroStation(MetroLine.TOKYO_HANZOMON, 90082, "Metro Hanzomon Otemachi"),
        MetroStation(MetroLine.TOKYO_HANZOMON, 90083, "Metro Hanzomon Suitengumae"),
        MetroStation(MetroLine.TOKYO_HANZOMON, 90084, "Metro Hanzomon Oshiage"),
    )

    private val NAMBOKU_LINE = listOf(
        MetroStation(MetroLine.TOKYO_NAMBOKU, 90085, "Metro Namboku Meguro"),
        MetroStation(MetroLine.TOKYO_NAMBOKU, 90086, "Metro Namboku Shirokane-Takanawa"),
        MetroStation(MetroLine.TOKYO_NAMBOKU, 90087, "Metro Namboku Azabu-Juban"),
        MetroStation(MetroLine.TOKYO_NAMBOKU, 90088, "Metro Namboku Roppongi-Itchome"),
        MetroStation(MetroLine.TOKYO_NAMBOKU, 90089, "Metro Namboku Nagatacho"),
        MetroStation(MetroLine.TOKYO_NAMBOKU, 90090, "Metro Namboku Komagome"),
        MetroStation(MetroLine.TOKYO_NAMBOKU, 90091, "Metro Namboku Akabane-Iwabuchi"),
    )

    private val FUKUTOSHIN_LINE = listOf(
        MetroStation(MetroLine.TOKYO_FUKUTOSHIN, 90092, "Metro Fukutoshin Shibuya"),
        MetroStation(MetroLine.TOKYO_FUKUTOSHIN, 90093, "Metro Fukutoshin Meiji-Jingumae"),
        MetroStation(MetroLine.TOKYO_FUKUTOSHIN, 90094, "Metro Fukutoshin Ikebukuro"),
        MetroStation(MetroLine.TOKYO_FUKUTOSHIN, 90095, "Metro Fukutoshin Zoshigaya"),
        MetroStation(MetroLine.TOKYO_FUKUTOSHIN, 90096, "Metro Fukutoshin Nishi-Waseda"),
        MetroStation(MetroLine.TOKYO_FUKUTOSHIN, 90097, "Metro Fukutoshin Higashi-Shinjuku"),
        MetroStation(MetroLine.TOKYO_FUKUTOSHIN, 90098, "Metro Fukutoshin Shinjuku-sanchome"),
    )

    private val ASAKUSA_LINE = listOf(
        MetroStation(MetroLine.TOKYO_ASAKUSA, 90099, "Toei Asakusa Nishi-Magome"),
        MetroStation(MetroLine.TOKYO_ASAKUSA, 90100, "Toei Asakusa Gotanda"),
        MetroStation(MetroLine.TOKYO_ASAKUSA, 90101, "Toei Asakusa Asakusabashi"),
        MetroStation(MetroLine.TOKYO_ASAKUSA, 90102, "Toei Asakusa Asakusa"),
        MetroStation(MetroLine.TOKYO_ASAKUSA, 90103, "Toei Asakusa Oshiage"),
    )

    private val MITA_LINE = listOf(
        MetroStation(MetroLine.TOKYO_MITA, 90104, "Toei Mita Mita"),
        MetroStation(MetroLine.TOKYO_MITA, 90105, "Toei Mita Shirokane-Takanawa"),
        MetroStation(MetroLine.TOKYO_MITA, 90106, "Toei Mita Sugamo"),
        MetroStation(MetroLine.TOKYO_MITA, 90107, "Toei Mita Meguro"),
        MetroStation(MetroLine.TOKYO_MITA, 90108, "Toei Mita Shiba-Koen"),
        MetroStation(MetroLine.TOKYO_MITA, 90109, "Toei Mita Onarimon"),
        MetroStation(MetroLine.TOKYO_MITA, 90110, "Toei Mita Uchisaiwaicho"),
        MetroStation(MetroLine.TOKYO_MITA, 90111, "Toei Mita Hibiya"),
        MetroStation(MetroLine.TOKYO_MITA, 90112, "Toei Mita Otemachi"),
        MetroStation(MetroLine.TOKYO_MITA, 90113, "Toei Mita Kasuga"),
        MetroStation(MetroLine.TOKYO_MITA, 90114, "Toei Mita Hakusan"),
    )

    private val SHINJUKU_LINE = listOf(
        MetroStation(MetroLine.TOKYO_SHINJUKU, 90115, "Toei Shinjuku Shinjuku"),
        MetroStation(MetroLine.TOKYO_SHINJUKU, 90116, "Toei Shinjuku Ichigaya"),
        MetroStation(MetroLine.TOKYO_SHINJUKU, 90117, "Toei Shinjuku Kudanshita"),
        MetroStation(MetroLine.TOKYO_SHINJUKU, 90118, "Toei Shinjuku Jimbocho"),
        MetroStation(MetroLine.TOKYO_SHINJUKU, 90119, "Toei Shinjuku Shinjuku-sanchome"),
        MetroStation(MetroLine.TOKYO_SHINJUKU, 90120, "Toei Shinjuku Akebonobashi"),
        MetroStation(MetroLine.TOKYO_SHINJUKU, 90121, "Toei Shinjuku Wakamatsu-Kawada"),
    )

    private val OEDO_LINE = listOf(
        MetroStation(MetroLine.TOKYO_OEDO, 90122, "Toei Oedo Tocho-mae"),
        MetroStation(MetroLine.TOKYO_OEDO, 90123, "Toei Oedo Shinjuku"),
        MetroStation(MetroLine.TOKYO_OEDO, 90124, "Toei Oedo Roppongi"),
        MetroStation(MetroLine.TOKYO_OEDO, 90125, "Toei Oedo Azabu-Juban"),
        MetroStation(MetroLine.TOKYO_OEDO, 90126, "Toei Oedo Tsukishima"),
        MetroStation(MetroLine.TOKYO_OEDO, 90127, "Toei Oedo Monzen-Nakacho"),
        MetroStation(MetroLine.TOKYO_OEDO, 90128, "Toei Oedo Ryogoku"),
        MetroStation(MetroLine.TOKYO_OEDO, 90129, "Toei Oedo Iidabashi"),
        MetroStation(MetroLine.TOKYO_OEDO, 90130, "Toei Oedo Higashi-Shinjuku"),
    )

    fun lineForStationName(name: String): MetroLine? {
        val n = name.lowercase()
        if (YAMANOTE_LINE.any { it.name.lowercase() == n }) return YAMANOTE_LINE.first().line
        if (GINZA_LINE.any { it.name.lowercase() == n }) return GINZA_LINE.first().line
        if (MARUNOUCHI_LINE.any { it.name.lowercase() == n }) return MARUNOUCHI_LINE.first().line
        if (HIBIYA_LINE.any { it.name.lowercase() == n }) return HIBIYA_LINE.first().line
        if (TOZAI_LINE.any { it.name.lowercase() == n }) return TOZAI_LINE.first().line
        if (CHIYODA_LINE.any { it.name.lowercase() == n }) return CHIYODA_LINE.first().line
        if (YURAKUCHO_LINE.any { it.name.lowercase() == n }) return YURAKUCHO_LINE.first().line
        if (HANZOMON_LINE.any { it.name.lowercase() == n }) return HANZOMON_LINE.first().line
        if (NAMBOKU_LINE.any { it.name.lowercase() == n }) return NAMBOKU_LINE.first().line
        if (FUKUTOSHIN_LINE.any { it.name.lowercase() == n }) return FUKUTOSHIN_LINE.first().line
        if (ASAKUSA_LINE.any { it.name.lowercase() == n }) return ASAKUSA_LINE.first().line
        if (MITA_LINE.any { it.name.lowercase() == n }) return MITA_LINE.first().line
        if (SHINJUKU_LINE.any { it.name.lowercase() == n }) return SHINJUKU_LINE.first().line
        if (OEDO_LINE.any { it.name.lowercase() == n }) return OEDO_LINE.first().line
        return null
    }

    fun allStationsRequest(): List<MetroStation> =
        YAMANOTE_LINE +
                GINZA_LINE +
                MARUNOUCHI_LINE +
                HIBIYA_LINE +
                TOZAI_LINE +
                CHIYODA_LINE +
                YURAKUCHO_LINE +
                HANZOMON_LINE +
                NAMBOKU_LINE +
                FUKUTOSHIN_LINE +
                ASAKUSA_LINE +
                MITA_LINE +
                SHINJUKU_LINE +
                OEDO_LINE
}
