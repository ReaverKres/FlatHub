package listing.core

import core.NetworkResponseWrapper
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * One listing platform (site) for a given market country.
 * Implementations live under `listing/{by|pl|ge|kz}/...`.
 */
interface ListingSource {
    val platform: FlatPlatform
    val country: CountryCode
    val capabilities: SourceCapabilities

    fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>>

    /**
     * Local/list payload for the ad. Default: none (caller may fall back to DB).
     */
    fun getById(adId: Long): Flow<AppFlat?> = flowOf(null)

    /**
     * Detail enrichment (network). Default: same as [getById] / no-op.
     */
    fun detail(adId: Long): Flow<AppFlat?> = getById(adId)

    /** Clears any in-memory / page caches for this source. */
    fun clearCache() {}
}
