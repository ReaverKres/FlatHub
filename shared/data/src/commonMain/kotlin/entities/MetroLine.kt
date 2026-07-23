package entities

import kotlinx.serialization.Serializable

/**
 * Metro / subway line identity for filter UI + geo enrich.
 * Keep GREEN/BLUE/RED first for Minsk (and legacy saved filters).
 * City-specific values follow — do not reuse colour buckets for multi-line cities.
 */
@Serializable
enum class MetroLine {
    GREEN, BLUE, RED,

    // London TfL
    BAKERLOO, CENTRAL, CIRCLE, DISTRICT, HAMMERSMITH_CITY, JUBILEE,
    METROPOLITAN, NORTHERN, PICCADILLY, VICTORIA, WATERLOO_CITY, ELIZABETH,

    // Wien U-Bahn
    WIEN_U1, WIEN_U2, WIEN_U3, WIEN_U4, WIEN_U6,

    // Seoul Subway
    SEOUL_1, SEOUL_2, SEOUL_3, SEOUL_4, SEOUL_5, SEOUL_6, SEOUL_7, SEOUL_8, SEOUL_9,

    // Berlin U-Bahn + major S
    BERLIN_U1, BERLIN_U2, BERLIN_U3, BERLIN_U4, BERLIN_U5,
    BERLIN_U6, BERLIN_U7, BERLIN_U8, BERLIN_U9,
    BERLIN_S1, BERLIN_S2, BERLIN_S3, BERLIN_S5, BERLIN_S7, BERLIN_S9,

    // Madrid Metro
    MADRID_1, MADRID_2, MADRID_3, MADRID_4, MADRID_5, MADRID_6,
    MADRID_7, MADRID_8, MADRID_9, MADRID_10, MADRID_11, MADRID_12, MADRID_R,

    // Barcelona Metro
    BCN_L1, BCN_L2, BCN_L3, BCN_L4, BCN_L5, BCN_L9N, BCN_L9S, BCN_L10, BCN_L11,

    // Bangkok
    BKK_BTS_SUKHUMVIT, BKK_BTS_SILOM, BKK_MRT_BLUE, BKK_MRT_PURPLE, BKK_ARL,

    // Tokyo Metro + Toei + JR Yamanote
    TOKYO_GINZA, TOKYO_MARUNOUCHI, TOKYO_HIBIYA, TOKYO_TOZAI, TOKYO_CHIYODA,
    TOKYO_YURAKUCHO, TOKYO_HANZOMON, TOKYO_NAMBOKU, TOKYO_FUKUTOSHIN,
    TOKYO_ASAKUSA, TOKYO_MITA, TOKYO_SHINJUKU, TOKYO_OEDO,
    TOKYO_YAMANOTE,
}
