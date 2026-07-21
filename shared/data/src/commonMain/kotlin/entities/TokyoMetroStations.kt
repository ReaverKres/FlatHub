package entities

/**
 * Tokyo JR / Tokyo Metro / Toei catalog for filter UI + geo line resolution.
 * BLUE = JR hubs (Yamanote + major lines); GREEN = Tokyo Metro; RED = Toei Subway.
 * Names match tokyo_metro_stations.json.
 */
object TokyoMetroStations {
    private const val ID_BASE = 90000

    private val BLUE_LINE = listOf(
        MetroStation(MetroLine.BLUE, 90000, "JR Yamanote Tokyo"),
        MetroStation(MetroLine.BLUE, 90001, "JR Yamanote Kanda"),
        MetroStation(MetroLine.BLUE, 90002, "JR Yamanote Akihabara"),
        MetroStation(MetroLine.BLUE, 90003, "JR Yamanote Okachimachi"),
        MetroStation(MetroLine.BLUE, 90004, "JR Yamanote Ueno"),
        MetroStation(MetroLine.BLUE, 90005, "JR Yamanote Uguisudani"),
        MetroStation(MetroLine.BLUE, 90006, "JR Yamanote Nippori"),
        MetroStation(MetroLine.BLUE, 90007, "JR Yamanote Nishi-Nippori"),
        MetroStation(MetroLine.BLUE, 90008, "JR Yamanote Tabata"),
        MetroStation(MetroLine.BLUE, 90009, "JR Yamanote Komagome"),
        MetroStation(MetroLine.BLUE, 90010, "JR Yamanote Sugamo"),
        MetroStation(MetroLine.BLUE, 90011, "JR Yamanote Otsuka"),
        MetroStation(MetroLine.BLUE, 90012, "JR Yamanote Ikebukuro"),
        MetroStation(MetroLine.BLUE, 90013, "JR Yamanote Mejiro"),
        MetroStation(MetroLine.BLUE, 90014, "JR Yamanote Takadanobaba"),
        MetroStation(MetroLine.BLUE, 90015, "JR Yamanote Shin-Okubo"),
        MetroStation(MetroLine.BLUE, 90016, "JR Yamanote Shinjuku"),
        MetroStation(MetroLine.BLUE, 90017, "JR Yamanote Yoyogi"),
        MetroStation(MetroLine.BLUE, 90018, "JR Yamanote Harajuku"),
        MetroStation(MetroLine.BLUE, 90019, "JR Yamanote Shibuya"),
        MetroStation(MetroLine.BLUE, 90020, "JR Yamanote Ebisu"),
        MetroStation(MetroLine.BLUE, 90021, "JR Yamanote Meguro"),
        MetroStation(MetroLine.BLUE, 90022, "JR Yamanote Gotanda"),
        MetroStation(MetroLine.BLUE, 90023, "JR Yamanote Osaki"),
        MetroStation(MetroLine.BLUE, 90024, "JR Yamanote Shinagawa"),
        MetroStation(MetroLine.BLUE, 90025, "JR Yamanote Tamachi"),
        MetroStation(MetroLine.BLUE, 90026, "JR Yamanote Hamamatsucho"),
        MetroStation(MetroLine.BLUE, 90027, "JR Yamanote Shinbashi"),
        MetroStation(MetroLine.BLUE, 90028, "JR Yamanote Yurakucho"),
        MetroStation(MetroLine.BLUE, 90029, "JR Chuo Tokyo"),
        MetroStation(MetroLine.BLUE, 90030, "JR Chuo Shinjuku"),
        MetroStation(MetroLine.BLUE, 90031, "JR Keihin-Tohoku Shinagawa"),
        MetroStation(MetroLine.BLUE, 90032, "JR Keihin-Tohoku Ueno"),
        MetroStation(MetroLine.BLUE, 90033, "JR Sobu Ryogoku"),
        MetroStation(MetroLine.BLUE, 90034, "JR Sobu Kinshicho"),
        MetroStation(MetroLine.BLUE, 90035, "JR Joban Ayase"),
    )

    private val GREEN_LINE = listOf(
        MetroStation(MetroLine.GREEN, 90200, "Metro Ginza Shibuya"),
        MetroStation(MetroLine.GREEN, 90201, "Metro Ginza Omotesando"),
        MetroStation(MetroLine.GREEN, 90202, "Metro Ginza Gaienmae"),
        MetroStation(MetroLine.GREEN, 90203, "Metro Ginza Akasaka-mitsuke"),
        MetroStation(MetroLine.GREEN, 90204, "Metro Ginza Tameike-Sanno"),
        MetroStation(MetroLine.GREEN, 90205, "Metro Ginza Toranomon"),
        MetroStation(MetroLine.GREEN, 90206, "Metro Ginza Shimbashi"),
        MetroStation(MetroLine.GREEN, 90207, "Metro Ginza Ginza"),
        MetroStation(MetroLine.GREEN, 90208, "Metro Ginza Kyobashi"),
        MetroStation(MetroLine.GREEN, 90209, "Metro Ginza Nihombashi"),
        MetroStation(MetroLine.GREEN, 90210, "Metro Ginza Mitsukoshimae"),
        MetroStation(MetroLine.GREEN, 90211, "Metro Ginza Kanda"),
        MetroStation(MetroLine.GREEN, 90212, "Metro Ginza Ueno"),
        MetroStation(MetroLine.GREEN, 90213, "Metro Ginza Asakusa"),
        MetroStation(MetroLine.GREEN, 90214, "Metro Marunouchi Tokyo"),
        MetroStation(MetroLine.GREEN, 90215, "Metro Marunouchi Otemachi"),
        MetroStation(MetroLine.GREEN, 90216, "Metro Marunouchi Akasaka-mitsuke"),
        MetroStation(MetroLine.GREEN, 90217, "Metro Marunouchi Shinjuku"),
        MetroStation(MetroLine.GREEN, 90218, "Metro Marunouchi Ikebukuro"),
        MetroStation(MetroLine.GREEN, 90219, "Metro Marunouchi Korakuen"),
        MetroStation(MetroLine.GREEN, 90220, "Metro Marunouchi Yotsuya"),
        MetroStation(MetroLine.GREEN, 90221, "Metro Marunouchi Nishi-Shinjuku"),
        MetroStation(MetroLine.GREEN, 90222, "Metro Hibiya Ebisu"),
        MetroStation(MetroLine.GREEN, 90223, "Metro Hibiya Roppongi"),
        MetroStation(MetroLine.GREEN, 90224, "Metro Hibiya Kamiyacho"),
        MetroStation(MetroLine.GREEN, 90225, "Metro Hibiya Kasumigaseki"),
        MetroStation(MetroLine.GREEN, 90226, "Metro Hibiya Hibiya"),
        MetroStation(MetroLine.GREEN, 90227, "Metro Hibiya Ginza"),
        MetroStation(MetroLine.GREEN, 90228, "Metro Hibiya Tsukiji"),
        MetroStation(MetroLine.GREEN, 90229, "Metro Hibiya Minowa"),
        MetroStation(MetroLine.GREEN, 90230, "Metro Hibiya Kita-Senju"),
        MetroStation(MetroLine.GREEN, 90231, "Metro Tozai Nakano"),
        MetroStation(MetroLine.GREEN, 90232, "Metro Tozai Takadanobaba"),
        MetroStation(MetroLine.GREEN, 90233, "Metro Tozai Iidabashi"),
        MetroStation(MetroLine.GREEN, 90234, "Metro Tozai Otemachi"),
        MetroStation(MetroLine.GREEN, 90235, "Metro Tozai Nihombashi"),
        MetroStation(MetroLine.GREEN, 90236, "Metro Tozai Monzen-Nakacho"),
        MetroStation(MetroLine.GREEN, 90237, "Metro Tozai Nishi-Funabashi"),
        MetroStation(MetroLine.GREEN, 90238, "Metro Chiyoda Omotesando"),
        MetroStation(MetroLine.GREEN, 90239, "Metro Chiyoda Meiji-Jingumae"),
        MetroStation(MetroLine.GREEN, 90240, "Metro Chiyoda Yoyogi-Koen"),
        MetroStation(MetroLine.GREEN, 90241, "Metro Chiyoda Nezu"),
        MetroStation(MetroLine.GREEN, 90242, "Metro Chiyoda Sendagi"),
        MetroStation(MetroLine.GREEN, 90243, "Metro Chiyoda Nishi-Nippori"),
        MetroStation(MetroLine.GREEN, 90244, "Metro Chiyoda Ayase"),
        MetroStation(MetroLine.GREEN, 90245, "Metro Yurakucho Ikebukuro"),
        MetroStation(MetroLine.GREEN, 90246, "Metro Yurakucho Higashi-Ikebukuro"),
        MetroStation(MetroLine.GREEN, 90247, "Metro Yurakucho Iidabashi"),
        MetroStation(MetroLine.GREEN, 90248, "Metro Yurakucho Nagatacho"),
        MetroStation(MetroLine.GREEN, 90249, "Metro Yurakucho Yurakucho"),
        MetroStation(MetroLine.GREEN, 90250, "Metro Fukutoshin Shibuya"),
        MetroStation(MetroLine.GREEN, 90251, "Metro Fukutoshin Meiji-Jingumae"),
        MetroStation(MetroLine.GREEN, 90252, "Metro Namboku Meguro"),
        MetroStation(MetroLine.GREEN, 90253, "Metro Namboku Shirokane-Takanawa"),
        MetroStation(MetroLine.GREEN, 90254, "Metro Namboku Azabu-Juban"),
        MetroStation(MetroLine.GREEN, 90255, "Metro Namboku Roppongi-Itchome"),
        MetroStation(MetroLine.GREEN, 90256, "Metro Namboku Nagatacho"),
        MetroStation(MetroLine.GREEN, 90257, "Metro Namboku Komagome"),
        MetroStation(MetroLine.GREEN, 90258, "Metro Namboku Akabane-Iwabuchi"),
        MetroStation(MetroLine.GREEN, 90259, "Metro Hanzomon Shibuya"),
        MetroStation(MetroLine.GREEN, 90260, "Metro Hanzomon Omotesando"),
        MetroStation(MetroLine.GREEN, 90261, "Metro Hanzomon Nagatacho"),
        MetroStation(MetroLine.GREEN, 90262, "Metro Hanzomon Otemachi"),
        MetroStation(MetroLine.GREEN, 90263, "Metro Hanzomon Suitengumae"),
        MetroStation(MetroLine.GREEN, 90264, "Metro Hanzomon Oshiage"),
    )

    private val RED_LINE = listOf(
        MetroStation(MetroLine.RED, 90400, "Toei Asakusa Nishi-Magome"),
        MetroStation(MetroLine.RED, 90401, "Toei Asakusa Gotanda"),
        MetroStation(MetroLine.RED, 90402, "Toei Asakusa Asakusabashi"),
        MetroStation(MetroLine.RED, 90403, "Toei Asakusa Asakusa"),
        MetroStation(MetroLine.RED, 90404, "Toei Asakusa Oshiage"),
        MetroStation(MetroLine.RED, 90405, "Toei Oedo Tocho-mae"),
        MetroStation(MetroLine.RED, 90406, "Toei Oedo Shinjuku"),
        MetroStation(MetroLine.RED, 90407, "Toei Oedo Roppongi"),
        MetroStation(MetroLine.RED, 90408, "Toei Oedo Azabu-Juban"),
        MetroStation(MetroLine.RED, 90409, "Toei Oedo Tsukishima"),
        MetroStation(MetroLine.RED, 90410, "Toei Oedo Monzen-Nakacho"),
        MetroStation(MetroLine.RED, 90411, "Toei Oedo Ryogoku"),
        MetroStation(MetroLine.RED, 90412, "Toei Oedo Iidabashi"),
        MetroStation(MetroLine.RED, 90413, "Toei Oedo Higashi-Shinjuku"),
        MetroStation(MetroLine.RED, 90414, "Toei Shinjuku Shinjuku"),
        MetroStation(MetroLine.RED, 90415, "Toei Shinjuku Ichigaya"),
        MetroStation(MetroLine.RED, 90416, "Toei Mita Mita"),
        MetroStation(MetroLine.RED, 90417, "Toei Mita Shirokane-Takanawa"),
        MetroStation(MetroLine.RED, 90418, "Toei Mita Sugamo"),
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
