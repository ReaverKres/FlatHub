package entities

/**
 * Seoul Subway Lines 1-9 catalog for filter UI + geo line resolution.
 * Names match seoul_metro_stations.json.
 */
object SeoulMetroStations {
    private const val ID_BASE = 80_000

    private val LINE_1 = listOf(
        MetroStation(MetroLine.SEOUL_1, 80000, "Line 1 Seoul Station"),
        MetroStation(MetroLine.SEOUL_1, 80001, "Line 1 City Hall"),
        MetroStation(MetroLine.SEOUL_1, 80002, "Line 1 Jonggak"),
        MetroStation(MetroLine.SEOUL_1, 80003, "Line 1 Jongno 3-ga"),
        MetroStation(MetroLine.SEOUL_1, 80004, "Line 1 Jongno 5-ga"),
        MetroStation(MetroLine.SEOUL_1, 80005, "Line 1 Dongdaemun"),
        MetroStation(MetroLine.SEOUL_1, 80006, "Line 1 Cheongnyangni"),
        MetroStation(MetroLine.SEOUL_1, 80007, "Line 1 Yongsan"),
        MetroStation(MetroLine.SEOUL_1, 80008, "Line 1 Noryangjin"),
        MetroStation(MetroLine.SEOUL_1, 80009, "Line 1 Daerim"),
        MetroStation(MetroLine.SEOUL_1, 80010, "Line 1 Guro"),
        MetroStation(MetroLine.SEOUL_1, 80011, "Line 1 Yeongdeungpo"),
        MetroStation(MetroLine.SEOUL_1, 80012, "Line 1 Sindorim"),
        MetroStation(MetroLine.SEOUL_1, 80013, "Line 1 Sinchon"),
        MetroStation(MetroLine.SEOUL_1, 80014, "Line 1 Gwangmyeong"),
        MetroStation(MetroLine.SEOUL_1, 80015, "Line 1 Incheon"),
        MetroStation(MetroLine.SEOUL_1, 80016, "Line 1 Seokgye"),
        MetroStation(MetroLine.SEOUL_1, 80017, "Line 1 Wangsimni"),
    )

    private val LINE_2 = listOf(
        MetroStation(MetroLine.SEOUL_2, 80018, "Line 2 City Hall"),
        MetroStation(MetroLine.SEOUL_2, 80019, "Line 2 Euljiro 1-ga"),
        MetroStation(MetroLine.SEOUL_2, 80020, "Line 2 Euljiro 3-ga"),
        MetroStation(MetroLine.SEOUL_2, 80021, "Line 2 Euljiro 4-ga"),
        MetroStation(MetroLine.SEOUL_2, 80022, "Line 2 Sindang"),
        MetroStation(MetroLine.SEOUL_2, 80023, "Line 2 Wangsimni"),
        MetroStation(MetroLine.SEOUL_2, 80024, "Line 2 Seongsu"),
        MetroStation(MetroLine.SEOUL_2, 80025, "Line 2 Konkuk Univ"),
        MetroStation(MetroLine.SEOUL_2, 80026, "Line 2 Gangbyeon"),
        MetroStation(MetroLine.SEOUL_2, 80027, "Line 2 Jamsil"),
        MetroStation(MetroLine.SEOUL_2, 80028, "Line 2 Sports Complex"),
        MetroStation(MetroLine.SEOUL_2, 80029, "Line 2 Samseong"),
        MetroStation(MetroLine.SEOUL_2, 80030, "Line 2 Yeoksam"),
        MetroStation(MetroLine.SEOUL_2, 80031, "Line 2 Gangnam"),
        MetroStation(MetroLine.SEOUL_2, 80032, "Line 2 Seolleung"),
        MetroStation(MetroLine.SEOUL_2, 80033, "Line 2 Apgujeong"),
        MetroStation(MetroLine.SEOUL_2, 80034, "Line 2 Shinnonhyeon"),
        MetroStation(MetroLine.SEOUL_2, 80035, "Line 2 Hongdae"),
        MetroStation(MetroLine.SEOUL_2, 80036, "Line 2 Hapjeong"),
        MetroStation(MetroLine.SEOUL_2, 80037, "Line 2 Dangsan"),
        MetroStation(MetroLine.SEOUL_2, 80038, "Line 2 Yeongdeungpo-gu Office"),
        MetroStation(MetroLine.SEOUL_2, 80039, "Line 2 Mullae"),
        MetroStation(MetroLine.SEOUL_2, 80040, "Line 2 Sindorim"),
        MetroStation(MetroLine.SEOUL_2, 80041, "Line 2 Kkachisan"),
        MetroStation(MetroLine.SEOUL_2, 80042, "Line 2 Hongje"),
        MetroStation(MetroLine.SEOUL_2, 80043, "Line 2 Sinchon"),
        MetroStation(MetroLine.SEOUL_2, 80044, "Line 2 Ewha Womans Univ"),
        MetroStation(MetroLine.SEOUL_2, 80045, "Line 2 Jamsilsaenae"),
        MetroStation(MetroLine.SEOUL_2, 80046, "Line 2 Olympic Park"),
        MetroStation(MetroLine.SEOUL_2, 80047, "Line 2 Gyodae"),
        MetroStation(MetroLine.SEOUL_2, 80048, "Line 2 Gangnam-gu Office"),
    )

    private val LINE_3 = listOf(
        MetroStation(MetroLine.SEOUL_3, 80049, "Line 3 Gupabal"),
        MetroStation(MetroLine.SEOUL_3, 80050, "Line 3 Yeonsinnae"),
        MetroStation(MetroLine.SEOUL_3, 80051, "Line 3 Bulgwang"),
        MetroStation(MetroLine.SEOUL_3, 80052, "Line 3 Gyeongbokgung"),
        MetroStation(MetroLine.SEOUL_3, 80053, "Line 3 Anguk"),
        MetroStation(MetroLine.SEOUL_3, 80054, "Line 3 Chungmuro"),
        MetroStation(MetroLine.SEOUL_3, 80055, "Line 3 Euljiro 3-ga"),
        MetroStation(MetroLine.SEOUL_3, 80056, "Line 3 Chungjeongno"),
        MetroStation(MetroLine.SEOUL_3, 80057, "Line 3 Ahyeon"),
        MetroStation(MetroLine.SEOUL_3, 80058, "Line 3 Yaksu"),
        MetroStation(MetroLine.SEOUL_3, 80059, "Line 3 Apgujeong"),
        MetroStation(MetroLine.SEOUL_3, 80060, "Line 3 Sinsa"),
        MetroStation(MetroLine.SEOUL_3, 80061, "Line 3 Jamwon"),
        MetroStation(MetroLine.SEOUL_3, 80062, "Line 3 Express Bus Terminal"),
        MetroStation(MetroLine.SEOUL_3, 80063, "Line 3 Nambu Bus Terminal"),
        MetroStation(MetroLine.SEOUL_3, 80064, "Line 3 Yangjae"),
        MetroStation(MetroLine.SEOUL_3, 80065, "Line 3 Maebong"),
        MetroStation(MetroLine.SEOUL_3, 80066, "Line 3 Suseo"),
        MetroStation(MetroLine.SEOUL_3, 80067, "Line 3 Garak Market"),
        MetroStation(MetroLine.SEOUL_3, 80068, "Line 3 Ogeum"),
        MetroStation(MetroLine.SEOUL_3, 80069, "Line 3 Munjeong"),
        MetroStation(MetroLine.SEOUL_3, 80070, "Line 3 Dogok"),
        MetroStation(MetroLine.SEOUL_3, 80071, "Line 3 Daehwa"),
    )

    private val LINE_4 = listOf(
        MetroStation(MetroLine.SEOUL_4, 80072, "Line 4 Myeongdong"),
        MetroStation(MetroLine.SEOUL_4, 80073, "Line 4 Chungmuro"),
        MetroStation(MetroLine.SEOUL_4, 80074, "Line 4 Dongdaemun History & Culture Park"),
        MetroStation(MetroLine.SEOUL_4, 80075, "Line 4 Hyehwa"),
        MetroStation(MetroLine.SEOUL_4, 80076, "Line 4 Hansung Univ"),
        MetroStation(MetroLine.SEOUL_4, 80077, "Line 4 Mia"),
        MetroStation(MetroLine.SEOUL_4, 80078, "Line 4 Suyu"),
        MetroStation(MetroLine.SEOUL_4, 80079, "Line 4 Nowon"),
        MetroStation(MetroLine.SEOUL_4, 80080, "Line 4 Changdong"),
        MetroStation(MetroLine.SEOUL_4, 80081, "Line 4 Sangbong"),
        MetroStation(MetroLine.SEOUL_4, 80082, "Line 4 Geumjeong"),
        MetroStation(MetroLine.SEOUL_4, 80083, "Line 4 Sadang"),
        MetroStation(MetroLine.SEOUL_4, 80084, "Line 4 Isu"),
        MetroStation(MetroLine.SEOUL_4, 80085, "Line 4 Chongshin Univ"),
        MetroStation(MetroLine.SEOUL_4, 80086, "Line 4 Dongjak"),
        MetroStation(MetroLine.SEOUL_4, 80087, "Line 4 Namtaeryeong"),
    )

    private val LINE_5 = listOf(
        MetroStation(MetroLine.SEOUL_5, 80088, "Line 5 Gwanghwamun"),
        MetroStation(MetroLine.SEOUL_5, 80089, "Line 5 Seodaemun"),
        MetroStation(MetroLine.SEOUL_5, 80090, "Line 5 Chungjeongno"),
        MetroStation(MetroLine.SEOUL_5, 80091, "Line 5 Gongdeok"),
        MetroStation(MetroLine.SEOUL_5, 80092, "Line 5 Yeouido"),
        MetroStation(MetroLine.SEOUL_5, 80093, "Line 5 Yeouinaru"),
        MetroStation(MetroLine.SEOUL_5, 80094, "Line 5 Mapo"),
        MetroStation(MetroLine.SEOUL_5, 80095, "Line 5 Banghwa"),
        MetroStation(MetroLine.SEOUL_5, 80096, "Line 5 Gimpo Airport"),
        MetroStation(MetroLine.SEOUL_5, 80097, "Line 5 Magok"),
        MetroStation(MetroLine.SEOUL_5, 80098, "Line 5 Mok-dong"),
        MetroStation(MetroLine.SEOUL_5, 80099, "Line 5 Omokgyo"),
        MetroStation(MetroLine.SEOUL_5, 80100, "Line 5 Cheonggu"),
        MetroStation(MetroLine.SEOUL_5, 80101, "Line 5 Wangsimni"),
        MetroStation(MetroLine.SEOUL_5, 80102, "Line 5 Gunja"),
        MetroStation(MetroLine.SEOUL_5, 80103, "Line 5 Gwangnaru"),
        MetroStation(MetroLine.SEOUL_5, 80104, "Line 5 Achasan"),
    )

    private val LINE_6 = listOf(
        MetroStation(MetroLine.SEOUL_6, 80105, "Line 6 Sangsu"),
        MetroStation(MetroLine.SEOUL_6, 80106, "Line 6 Hapjeong"),
        MetroStation(MetroLine.SEOUL_6, 80107, "Line 6 World Cup Stadium"),
        MetroStation(MetroLine.SEOUL_6, 80108, "Line 6 Digital Media City"),
        MetroStation(MetroLine.SEOUL_6, 80109, "Line 6 Mangwon"),
        MetroStation(MetroLine.SEOUL_6, 80110, "Line 6 Itaewon"),
        MetroStation(MetroLine.SEOUL_6, 80111, "Line 6 Samgakji"),
        MetroStation(MetroLine.SEOUL_6, 80112, "Line 6 Cheonggu"),
        MetroStation(MetroLine.SEOUL_6, 80113, "Line 6 Wangsimni"),
        MetroStation(MetroLine.SEOUL_6, 80114, "Line 6 Ddukseom"),
        MetroStation(MetroLine.SEOUL_6, 80115, "Line 6 Seongsu"),
        MetroStation(MetroLine.SEOUL_6, 80116, "Line 6 Bonghwasan"),
        MetroStation(MetroLine.SEOUL_6, 80117, "Line 6 Taereung"),
        MetroStation(MetroLine.SEOUL_6, 80118, "Line 6 Hwarangdae"),
    )

    private val LINE_7 = listOf(
        MetroStation(MetroLine.SEOUL_7, 80119, "Line 7 Gangnam-gu Office"),
        MetroStation(MetroLine.SEOUL_7, 80120, "Line 7 Hak-dong"),
        MetroStation(MetroLine.SEOUL_7, 80121, "Line 7 Cheonggu"),
        MetroStation(MetroLine.SEOUL_7, 80122, "Line 7 Ttukseom"),
        MetroStation(MetroLine.SEOUL_7, 80123, "Line 7 Junggok"),
        MetroStation(MetroLine.SEOUL_7, 80124, "Line 7 Gunja"),
        MetroStation(MetroLine.SEOUL_7, 80125, "Line 7 Children's Grand Park"),
        MetroStation(MetroLine.SEOUL_7, 80126, "Line 7 Konkuk Univ"),
        MetroStation(MetroLine.SEOUL_7, 80127, "Line 7 Onsu"),
        MetroStation(MetroLine.SEOUL_7, 80128, "Line 7 Bupyeong-gu Office"),
        MetroStation(MetroLine.SEOUL_7, 80129, "Line 7 Sangdong"),
        MetroStation(MetroLine.SEOUL_7, 80130, "Line 7 Yeongdeungpo Market"),
    )

    private val LINE_8 = listOf(
        MetroStation(MetroLine.SEOUL_8, 80131, "Line 8 Amsa"),
        MetroStation(MetroLine.SEOUL_8, 80132, "Line 8 Cheonho"),
        MetroStation(MetroLine.SEOUL_8, 80133, "Line 8 Gangdong-gu Office"),
        MetroStation(MetroLine.SEOUL_8, 80134, "Line 8 Mongchontoseong"),
        MetroStation(MetroLine.SEOUL_8, 80135, "Line 8 Jamsil"),
        MetroStation(MetroLine.SEOUL_8, 80136, "Line 8 Seokchon"),
        MetroStation(MetroLine.SEOUL_8, 80137, "Line 8 Songpa"),
        MetroStation(MetroLine.SEOUL_8, 80138, "Line 8 Garak Market"),
        MetroStation(MetroLine.SEOUL_8, 80139, "Line 8 Munjeong"),
        MetroStation(MetroLine.SEOUL_8, 80140, "Line 8 Bokjeong"),
        MetroStation(MetroLine.SEOUL_8, 80141, "Line 8 Sanseong"),
    )

    private val LINE_9 = listOf(
        MetroStation(MetroLine.SEOUL_9, 80142, "Line 9 Gaehwa"),
        MetroStation(MetroLine.SEOUL_9, 80143, "Line 9 Gimpo Airport"),
        MetroStation(MetroLine.SEOUL_9, 80144, "Line 9 Yangcheon Hyanggyo"),
        MetroStation(MetroLine.SEOUL_9, 80145, "Line 9 Dangsan"),
        MetroStation(MetroLine.SEOUL_9, 80146, "Line 9 Yeouido"),
        MetroStation(MetroLine.SEOUL_9, 80147, "Line 9 National Assembly"),
        MetroStation(MetroLine.SEOUL_9, 80148, "Line 9 Express Bus Terminal"),
        MetroStation(MetroLine.SEOUL_9, 80149, "Line 9 Sinnonhyeon"),
        MetroStation(MetroLine.SEOUL_9, 80150, "Line 9 Sports Complex"),
        MetroStation(MetroLine.SEOUL_9, 80151, "Line 9 VHS Medical Center"),
        MetroStation(MetroLine.SEOUL_9, 80152, "Line 9 Suseo"),
        MetroStation(MetroLine.SEOUL_9, 80153, "Line 9 Seonjeongneung"),
    )

    fun lineForStationName(name: String): MetroLine? {
        val lowered = name.lowercase()
        if (LINE_1.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_1
        if (LINE_2.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_2
        if (LINE_3.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_3
        if (LINE_4.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_4
        if (LINE_5.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_5
        if (LINE_6.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_6
        if (LINE_7.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_7
        if (LINE_8.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_8
        if (LINE_9.any { it.name.lowercase() == lowered }) return MetroLine.SEOUL_9
        val prefix = Regex("^Line (\\d+) ", RegexOption.IGNORE_CASE).find(name)
        return prefix?.groupValues?.get(1)?.toIntOrNull()?.let { num ->
            when (num) {
                1 -> MetroLine.SEOUL_1
                2 -> MetroLine.SEOUL_2
                3 -> MetroLine.SEOUL_3
                4 -> MetroLine.SEOUL_4
                5 -> MetroLine.SEOUL_5
                6 -> MetroLine.SEOUL_6
                7 -> MetroLine.SEOUL_7
                8 -> MetroLine.SEOUL_8
                9 -> MetroLine.SEOUL_9
                else -> null
            }
        }
    }

    fun allStationsRequest(): List<MetroStation> =
        LINE_1 +
                LINE_2 +
                LINE_3 +
                LINE_4 +
                LINE_5 +
                LINE_6 +
                LINE_7 +
                LINE_8 +
                LINE_9
}
