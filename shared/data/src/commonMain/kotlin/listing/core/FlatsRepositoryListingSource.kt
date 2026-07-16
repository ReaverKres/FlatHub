package listing.core

import core.NetworkResponseWrapper
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import repository.FlatsRepository

/**
 * Adapts legacy [FlatsRepository] implementations into [ListingSource].
 */
class FlatsRepositoryListingSource(
    override val platform: FlatPlatform,
    override val country: CountryCode,
    override val capabilities: SourceCapabilities,
    private val repository: FlatsRepository,
) : ListingSource {
    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> =
        repository.searchFlats(filter, currentPage)

    override fun getById(adId: Long): Flow<AppFlat?> =
        repository.getFlatById(adId).map { it }

    override fun detail(adId: Long): Flow<AppFlat?> =
        repository.getFlatByIdWithDetails(adId)

    override fun clearCache() {
        repository.clearCashedFlats()
    }
}
