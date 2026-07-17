package listing.ge.ss

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
 * SS.ge (home.ss.ge) — LegendSearch API. See tmp/ge/api/ss/NOTES.md.
 */
class SsListingSource(
    private val api: SsApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.SS_GE
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
                is AdType.SALE -> SsApiClient.DEAL_SALE
                is AdType.DAILY -> SsApiClient.DEAL_DAILY
                else -> SsApiClient.DEAL_RENT
            }
            val json = api.legendSearch(
                page = page,
                cityId = SsCities.cityId(filter.location?.city),
                dealType = dealType,
                pageSize = listing.core.FeedDelayListBoost.apiPageSize(platform, base = 40),
                priceFrom = filter.priceFull?.priceFrom?.toInt(),
                priceTo = filter.priceFull?.priceTo?.toInt(),
            )
            NetworkResponseWrapper.success(SsFlatMapper.mapSearch(json, filter.adType))
        } catch (e: CancellationException) {
            throw e
        } catch (e: SsCloudflareBlockedException) {
            // Silent skip while CF blocks Android clients — do not surface in error dialog.
            // Livo remains the working GE source. See docs/current_stage.md.
            println("SsListingSource: ${e.message}")
            NetworkResponseWrapper.success(emptyList())
        } catch (e: Exception) {
            api.invalidateAuth()
            NetworkResponseWrapper.error(
                e,
                NetworkErrorInfo(platform, listOf(e.message.orEmpty())),
            )
        }
        emit(result)
    }

    override fun getById(adId: Long): Flow<AppFlat?> = flatsDao.flowById(platform, adId)

    override fun detail(adId: Long): Flow<AppFlat?> = flow {
        // List payload already marked detail-loaded; emit base immediately.
        emit(flatsDao.getById(platform, adId))
    }
}
