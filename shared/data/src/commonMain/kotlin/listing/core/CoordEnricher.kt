package listing.core

import database.FlatsDao
import entities.AppFlat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * After list search, fills missing coordinates via [ListingSource.detail] in the background
 * for platforms with [ListingSource.needsBackgroundCoordEnrich].
 * Does not block list emit; updates Room so map/list Flows refresh pins.
 */
class CoordEnricher(
    private val flatsDao: FlatsDao,
    private val registry: ListingSourceRegistry,
    private val state: CoordEnrichState,
    private val scope: CoroutineScope,
) {
    private val semaphore = Semaphore(PERMITS)
    private var job: Job? = null

    fun enqueue(flats: List<AppFlat>) {
        val targets = flats.filter { flat ->
            flat.coordinates == null &&
                    !flat.flatDevInfo.coordsEnriched &&
                    registry.byPlatform(flat.flatPlatform)?.needsBackgroundCoordEnrich == true
        }
        if (targets.isEmpty()) return

        job?.cancel()
        job = scope.launch {
            state.setLoading(true)
            try {
                supervisorScope {
                    targets.forEach { flat ->
                        launch {
                            semaphore.withPermit { enrichOne(flat) }
                        }
                    }
                }
            } finally {
                state.setLoading(false)
            }
        }
    }

    private suspend fun enrichOne(flat: AppFlat) {
        val source = registry.byPlatform(flat.flatPlatform) ?: return
        val enriched = runCatching {
            source.detail(flat.adId).lastOrNull()
        }.getOrNull() ?: return

        val coords = enriched.coordinates ?: flat.coordinates
        val detailDone = enriched.flatDevInfo.isDetailLoaded
        // Only mark done when we got coords or a completed detail (no coords on site).
        // Soft-fail without progress → leave unset so a later search can retry.
        if (coords == null && !detailDone) return

        flatsDao.upsert(
            enriched.copy(
                coordinates = coords,
                flatDevInfo = enriched.flatDevInfo.copy(coordsEnriched = true),
            ),
        )
    }

    companion object {
        private const val PERMITS = 3
    }
}
