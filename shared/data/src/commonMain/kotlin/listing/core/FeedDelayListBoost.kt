package listing.core

import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import listing.core.FeedDelayListBoost.active
import kotlin.concurrent.Volatile

/**
 * When non-premium feed delay is active, page 1 of high-volume sites is often
 * all "too new" and gets filtered out. Fetch ~2 pages worth, scaled vs Domovita.
 *
 * [active] is set by presentation (via [repository.fillter.FilterRepository]) before search.
 */
object FeedDelayListBoost {
    @Volatile
    var active: Boolean = false

    /** Relative listing volume vs Domovita (= 1.0). */
    private val popularity: Map<FlatPlatform, Double> = mapOf(
        FlatPlatform.DOMOVITA to 1.0,
        FlatPlatform.REALT to 1.5,
        FlatPlatform.KUFAR to 2.5,
        FlatPlatform.ONLINER to 3.0,
        FlatPlatform.OTODOM to 2.5,
        FlatPlatform.OLX_PL to 2.5,
        FlatPlatform.GRATKA to 2.0,
        FlatPlatform.MORIZON to 1.5,
        FlatPlatform.SS_GE to 2.0,
        FlatPlatform.LIVO to 2.0,
        FlatPlatform.BINEBI to 1.5,
        FlatPlatform.KRISHA to 3.0,
        FlatPlatform.OLX_KZ to 2.0,
        FlatPlatform.KN to 1.5,
        FlatPlatform.FOTOCASA to 2.5,
        FlatPlatform.PISOS to 2.0,
        FlatPlatform.IS24 to 3.0,
        FlatPlatform.IMMOWELT to 2.0,
        FlatPlatform.KLEINANZEIGEN to 2.5,
        FlatPlatform.IS24_AT to 3.0,
        FlatPlatform.IMMOWELT_AT to 2.0,
        FlatPlatform.WILLHABEN to 2.5,
        FlatPlatform.EMLAKJET to 2.5,
        FlatPlatform.PROPERTY_FINDER to 3.0,
        FlatPlatform.DUBIZZLE to 3.0,
        FlatPlatform.OPENSOOQ to 1.5,
        FlatPlatform.PROPERTYHUB to 3.0,
        FlatPlatform.LIVINGINSIDER to 2.5,
        FlatPlatform.RENTHUB to 2.0,
        FlatPlatform.ZUMPER to 2.0,
        FlatPlatform.DABANG to 3.0,
        FlatPlatform.ZIGBANG to 3.0,
        FlatPlatform.SUUMO to 3.0,
        FlatPlatform.YAHOO_RE to 2.5,
        FlatPlatform.ATHOME to 2.5,
        FlatPlatform.FLATFOX to 2.0,
        FlatPlatform.RIGHTMOVE to 1.0,
        FlatPlatform.ONTHEMARKET to 2.0,
        FlatPlatform.OPENRENT to 2.5,
        FlatPlatform.BIENICI to 2.0,
    )

    /** Caps relative volume so high-traffic sites are closer to mid-tier; keeps max page size 120. */
    private fun cappedPopularity(platform: FlatPlatform): Double =
        (popularity[platform] ?: 1.5).coerceAtMost(2.0)

    /** API list: inflate page size ≈ 2 × base × popularity. */
    fun apiPageSize(platform: FlatPlatform, base: Int): Int {
        if (!active) return base
        val pop = cappedPopularity(platform)
        val boosted = (base * 2.0 * pop).toInt().coerceIn(base, 120)
        // OLX rejects oversized limits (400 with limit=120).
        return when (platform) {
            FlatPlatform.OLX_PL, FlatPlatform.OLX_KZ -> boosted.coerceAtMost(40)
            else -> boosted
        }
    }

    /**
     * HTML scrape: extra pages to fetch in parallel (0 = only [startPage]).
     * Do not grow a single HTML response — that slows parsing badly.
     */
    fun htmlExtraPages(platform: FlatPlatform): Int {
        if (!active) return 0
        // Immowelt pagination is DataDome-broken / ignored — first page only.
        if (platform == FlatPlatform.IMMOWELT || platform == FlatPlatform.IMMOWELT_AT) return 0
        val pop = cappedPopularity(platform)
        return if (pop >= 1.5) 1 else 0
    }

    /** Parallel page fetches when boost is on; otherwise a single page. */
    suspend fun <T> fetchPages(
        startPage: Int,
        platform: FlatPlatform,
        key: (T) -> Any?,
        fetchPage: suspend (Int) -> List<T>,
    ): List<T> = supervisorScope {
        val extra = htmlExtraPages(platform)
        val pages = (startPage..(startPage + extra)).toList()
        pages.map { p ->
            async {
                try {
                    fetchPage(p)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("FeedDelayListBoost $platform page $p soft-fail: ${e.message}")
                    emptyList()
                }
            }
        }
            .awaitAll()
            .flatten()
            .distinctBy(key)
    }
}
