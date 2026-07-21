package listing.kr.dabang

import io.flatzen.commoncomponents.commonentities.CityCode

/**
 * MVP city → bbox search areas for Dabang list API.
 * Metro short `code` (e.g. 11) returns HTTP 500 — use bbox instead.
 * See tmp/kr/api/dabang/NOTES.md and city_ids.json.
 */
object DabangCities {
    data class Bbox(
        val swLat: Double,
        val swLng: Double,
        val neLat: Double,
        val neLng: Double,
    ) {
        fun toJson(): String =
            """{"sw":{"lat":$swLat,"lng":$swLng},"ne":{"lat":$neLat,"lng":$neLng}}"""
    }

    /** Region gid from NOTES / city_ids.json (metro or city meta). */
    data class CityMeta(
        val gid: Int,
        val code: String,
        val name: String,
    )

    fun bboxes(city: CityCode?): List<Bbox> = when (city) {
        null, CityCode.SEOUL -> listOf(
            // Tile Seoul — one huge bbox can underfill / soft-fail on some clients
            bbox(37.4979, 127.0276, 0.04, 0.05), // Gangnam
            bbox(37.5665, 126.9780, 0.04, 0.05), // Jung
            bbox(37.5796, 126.9770, 0.04, 0.05), // Jongno
            bbox(37.5445, 127.0560, 0.04, 0.05), // Seongdong/Gwangjin
        )

        CityCode.BUSAN -> listOf(bbox(35.189019, 129.034938, 0.06, 0.08))
        CityCode.DAEGU -> listOf(bbox(35.811350, 128.556873, 0.06, 0.07))
        CityCode.INCHEON -> listOf(bbox(37.473972, 126.697984, 0.07, 0.08))
        CityCode.GWANGJU -> listOf(bbox(35.155469, 126.833019, 0.05, 0.06))
        CityCode.DAEJEON -> listOf(bbox(36.341748, 127.403109, 0.05, 0.06))
        CityCode.ULSAN -> listOf(bbox(35.527437, 129.217536, 0.06, 0.07))
        CityCode.SEJONG -> listOf(bbox(36.570330, 127.269248, 0.05, 0.06))
        CityCode.SUWON -> listOf(bbox(37.2636, 127.0286, 0.05, 0.06))
        CityCode.CHANGWON -> listOf(bbox(35.2284, 128.6811, 0.05, 0.06))
        CityCode.JEONJU -> listOf(bbox(35.8242, 127.1480, 0.05, 0.06))
        CityCode.CHEONGJU -> listOf(bbox(36.6424, 127.4890, 0.05, 0.06))
        CityCode.CHUNCHEON -> listOf(bbox(37.883176, 127.743077, 0.04, 0.05))
        CityCode.JEJU -> listOf(bbox(33.499391, 126.531386, 0.05, 0.06))
        else -> listOf(bbox(37.4979, 127.0276, 0.04, 0.05))
    }

    /**
     * 8-digit 법정동 codes for region list search (5-digit gu → HTTP 500).
     * Used as a fallback when bbox returns empty.
     */
    fun regionCodes(city: CityCode?): List<String> = when (city) {
        null, CityCode.SEOUL -> listOf(
            "11680101", // 역삼동
            "11650108", // 서초동
            "11710101", // 잠실동 area (송파)
            "11440120", // 연남동/홍대 vicinity (마포)
            "11140115", // 명동 vicinity
        )

        CityCode.BUSAN -> listOf("26110101")
        CityCode.DAEGU -> listOf("27110101")
        CityCode.INCHEON -> listOf("28110101")
        CityCode.SUWON -> listOf("41115141") // 인계동
        CityCode.CHUNCHEON -> listOf("51110101")
        CityCode.JEJU -> listOf("50110101")
        else -> emptyList()
    }

    fun meta(city: CityCode?): CityMeta = when (city) {
        null, CityCode.SEOUL -> CityMeta(gid = 5315, code = "11", name = "서울특별시")
        CityCode.BUSAN -> CityMeta(gid = 5300, code = "26", name = "부산광역시")
        CityCode.DAEGU -> CityMeta(gid = 5303, code = "27", name = "대구광역시")
        CityCode.INCHEON -> CityMeta(gid = 5310, code = "28", name = "인천광역시")
        CityCode.GWANGJU -> CityMeta(gid = 5313, code = "12", name = "전남광주통합특별시")
        CityCode.DAEJEON -> CityMeta(gid = 5304, code = "30", name = "대전광역시")
        CityCode.ULSAN -> CityMeta(gid = 5316, code = "31", name = "울산광역시")
        CityCode.SEJONG -> CityMeta(gid = 5314, code = "36", name = "세종특별자치시")
        CityCode.SUWON -> CityMeta(gid = 5399, code = "41111", name = "수원시")
        CityCode.CHANGWON -> CityMeta(gid = 5468, code = "48121", name = "창원시")
        CityCode.JEONJU -> CityMeta(gid = 5508, code = "52111", name = "전주시")
        CityCode.CHEONGJU -> CityMeta(gid = 5341, code = "43111", name = "청주시")
        CityCode.CHUNCHEON -> CityMeta(gid = 5380, code = "51110", name = "춘천시")
        CityCode.JEJU -> CityMeta(gid = 5500, code = "50110", name = "제주시")
        else -> CityMeta(gid = 5315, code = "11", name = "서울특별시")
    }

    private fun bbox(lat: Double, lng: Double, halfLat: Double, halfLng: Double): Bbox =
        Bbox(
            swLat = lat - halfLat,
            swLng = lng - halfLng,
            neLat = lat + halfLat,
            neLng = lng + halfLng,
        )
}
