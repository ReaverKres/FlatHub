package listing.es.idealista

import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById

/**
 * Idealista.com — deferred: anonymous traffic blocked by DataDome (403).
 * See tmp/es/api/idealista/NOTES.md. Silent empty like SS.ge CF skip.
 */
class IdealistaListingSource(
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.IDEALISTA
    override val country = CountryCode.ES
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        println("IdealistaListingSource: skipped (DataDome) — see tmp/es/api/idealista/NOTES.md")
        emit(NetworkResponseWrapper.success(emptyList()))
    }

    override fun getById(adId: Long): Flow<AppFlat?> = flatsDao.flowById(platform, adId)

    override fun detail(adId: Long): Flow<AppFlat?> = flowOf(null)
}
