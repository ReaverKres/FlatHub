package listing.ae.dubizzle

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * Dubizzle UAE — Algolia search (site HTML Incapsula-blocked). AED → [AppFlat.priceByn].
 * See tmp/ae/api/dubizzle/NOTES.md.
 */
class DubizzleListingSource(
    private val api: DubizzleApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.DUBIZZLE
    override val country = CountryCode.AE
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
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val cityId = DubizzleCities.cityId(filter.location?.city)
            val isSale = filter.adType is AdType.SALE
            val pageSize = FeedDelayListBoost.apiPageSize(platform, base = 20)
            val json = api.search(
                cityId = cityId,
                isSale = isSale,
                page = page,
                hitsPerPage = pageSize,
            )
            NetworkResponseWrapper.success(DubizzleMapper.mapSearch(json, filter.adType))
        } catch (e: CancellationException) {
            throw e
        } catch (e: DubizzleBlockedException) {
            println("DubizzleListingSource: ${e.message}")
            NetworkResponseWrapper.success(emptyList())
        } catch (e: Exception) {
            NetworkResponseWrapper.error(
                e,
                NetworkErrorInfo(platform, listOf(e.message.orEmpty())),
            )
        }
        emit(result)
    }

    override fun getById(adId: Long): Flow<AppFlat?> = flatsDao.flowById(platform, adId)

    override fun detail(adId: Long): Flow<AppFlat?> = flow {
        // Detail HTML/API blocked anonymously; list payload is already marked loaded.
        emit(flatsDao.getById(platform, adId) ?: DubizzleMapper.listStub(adId))
    }
}
