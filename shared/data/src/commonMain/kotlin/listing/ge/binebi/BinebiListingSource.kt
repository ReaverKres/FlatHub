package listing.ge.binebi

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
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * Binebi.ge — CSRF POST search JSON + HTML detail. See tmp/ge/api/binebi/NOTES.md.
 */
class BinebiListingSource(
    private val api: BinebiApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.BINEBI
    override val country = CountryCode.GE
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = true,
        supportsRoom = false,
        supportsCommercial = false,
    )

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val dealType = when (filter.adType) {
                is AdType.SALE -> BinebiApiClient.DEAL_SALE
                is AdType.DAILY -> BinebiApiClient.DEAL_DAILY
                else -> BinebiApiClient.DEAL_RENT
            }
            val json = api.fetchSearch(
                page = page,
                cityId = BinebiCities.cityId(filter.location?.city),
                dealType = dealType,
            )
            NetworkResponseWrapper.success(BinebiFlatMapper.mapSearch(json, filter.adType))
        } catch (e: CancellationException) {
            throw e
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
        val base = flatsDao.getById(platform, adId)
        if (base == null) {
            emit(null)
            return@flow
        }
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(base.flatDetailUrl)
            val merged = BinebiHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Soft-fail detail.
        }
    }
}
