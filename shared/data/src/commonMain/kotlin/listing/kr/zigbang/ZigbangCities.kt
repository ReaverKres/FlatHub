package listing.kr.zigbang

import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.Coordinates

/**
 * MVP city centers for Zigbang geohash map search (precision 5 ≈ 4.9 km tile).
 * See tmp/kr/api/zigbang/NOTES.md.
 *
 * Pagination: page 1 = center tile; pages 2–9 = eight neighboring tiles (lat/lng offset
 * re-encoded to geohash). Page 10+ returns null — no cursor API on map endpoints.
 */
object ZigbangCities {
    private const val PRECISION = 5

    /** ~half a precision-5 cell at mid-latitude (~4.9 km). */
    private val NEIGHBOR_OFFSETS: List<Pair<Double, Double>> = listOf(
        0.044 to 0.0,       // N
        0.044 to 0.056,     // NE
        0.0 to 0.056,       // E
        -0.044 to 0.056,    // SE
        -0.044 to 0.0,      // S
        -0.044 to -0.056,   // SW
        0.0 to -0.056,      // W
        0.044 to -0.056,    // NW
    )

    fun coordinates(city: CityCode?): Coordinates = when (city) {
        null, CityCode.SEOUL -> Coordinates(37.5663306, 126.9782351)
        CityCode.BUSAN -> Coordinates(35.1797777, 129.0740342)
        CityCode.DAEGU -> Coordinates(35.8713943, 128.601763)
        CityCode.INCHEON -> Coordinates(37.455234, 126.709635)
        CityCode.GWANGJU -> Coordinates(35.159659, 126.852601)
        CityCode.DAEJEON -> Coordinates(36.3504163, 127.384547)
        CityCode.ULSAN -> Coordinates(35.537892, 129.311375)
        CityCode.SEJONG -> Coordinates(36.592897, 127.292375)
        CityCode.SUWON -> Coordinates(37.2576065, 126.976922)
        CityCode.CHANGWON -> Coordinates(35.1983833, 128.702524)
        CityCode.JEONJU -> Coordinates(35.8293991, 127.095734)
        CityCode.CHEONGJU -> Coordinates(36.634603, 127.488728)
        CityCode.CHUNCHEON -> Coordinates(37.8813133, 127.729988)
        CityCode.JEJU -> Coordinates(33.4995689, 126.531188)
        else -> Coordinates(37.5663306, 126.9782351)
    }

    /** Geohash tile for [page] (1-based), or null when out of MVP tile grid. */
    fun geohashForPage(city: CityCode?, page: Int): String? {
        if (page < 1) return null
        val base = coordinates(city)
        val (lat, lng) = when {
            page == 1 -> base.latitude to base.longitude
            else -> {
                val offset = NEIGHBOR_OFFSETS.getOrNull(page - 2) ?: return null
                (base.latitude + offset.first) to (base.longitude + offset.second)
            }
        }
        return encodeGeohash(lat, lng, PRECISION)
    }

    fun encodeGeohash(lat: Double, lng: Double, precision: Int): String {
        var latMin = -90.0
        var latMax = 90.0
        var lngMin = -180.0
        var lngMax = 180.0
        val sb = StringBuilder()
        var bits = 0
        var bitCount = 0
        var even = true
        while (sb.length < precision) {
            if (even) {
                val mid = (lngMin + lngMax) / 2
                if (lng >= mid) {
                    bits = bits * 2 + 1
                    lngMin = mid
                } else {
                    bits = bits * 2
                    lngMax = mid
                }
            } else {
                val mid = (latMin + latMax) / 2
                if (lat >= mid) {
                    bits = bits * 2 + 1
                    latMin = mid
                } else {
                    bits = bits * 2
                    latMax = mid
                }
            }
            even = !even
            bitCount++
            if (bitCount == 5) {
                sb.append(BASE32[bits])
                bits = 0
                bitCount = 0
            }
        }
        return sb.toString()
    }

    private const val BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz"
}
