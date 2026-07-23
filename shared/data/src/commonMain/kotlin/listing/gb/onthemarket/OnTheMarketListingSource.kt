package listing.gb.onthemarket

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
import listing.gb.GbCities
import kotlin.coroutines.cancellation.CancellationException

/**
 * OnTheMarket UK — SSR `__NEXT_DATA__` (`initialReduxState.results.list`).
 * GBP → [AppFlat.mainPrice]. See tmp/gb/api/onthemarket/NOTES.md.
 */
class OnTheMarketListingSource(
    private val api: OnTheMarketApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.ONTHEMARKET
    override val country = CountryCode.GB
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
            val slug = GbCities.onTheMarketSlug(filter.location?.city)
            val isSale = filter.adType is AdType.SALE
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(
                    locationSlug = slug,
                    isSale = isSale,
                    page = p,
                )
                OnTheMarketMapper.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OnTheMarketBlockedException) {
            println("OnTheMarketListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId)
            ?: OnTheMarketMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(adId)
            val merged = OnTheMarketMapper.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OnTheMarketBlockedException) {
            println("OnTheMarketListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("OnTheMarketListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
