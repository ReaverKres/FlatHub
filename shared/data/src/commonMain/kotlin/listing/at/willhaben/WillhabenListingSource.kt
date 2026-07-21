package listing.at.willhaben

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
 * willhaben.at — SSR `__NEXT_DATA__`; soft-fail on IP block.
 * See tmp/at/api/willhaben/NOTES.md.
 */
class WillhabenListingSource(
    private val api: WillhabenApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.WILLHABEN
    override val country = CountryCode.AT
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = true

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val areaId = WillhabenCities.areaId(filter.location?.city)
            val isSale = filter.adType is AdType.SALE
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(
                    areaId = areaId,
                    isSale = isSale,
                    page = p,
                )
                WillhabenMapper.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: WillhabenBlockedException) {
            println("WillhabenListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: WillhabenMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(adId)
            val merged = WillhabenMapper.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: WillhabenBlockedException) {
            println("WillhabenListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("WillhabenListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
