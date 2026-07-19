package listing.th.livinginsider

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import listing.th.isThSaleDeal
import kotlin.coroutines.cancellation.CancellationException

/**
 * Livinginsider — SSR searchword Condo Rent/Buysell. THB → [AppFlat.mainPrice].
 * See tmp/th/api/livinginsider/NOTES.md.
 */
class LivinginsiderListingSource(
    private val api: LivinginsiderApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.LIVINGINSIDER
    override val country = CountryCode.TH
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
        supportsFromOwnerOnly = true,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val isSale = filter.adType.isThSaleDeal()
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(isSale = isSale, page = p)
                LivinginsiderMapper.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LivinginsiderBlockedException) {
            println("LivinginsiderListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: LivinginsiderMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(base.flatDetailUrl)
            val merged = LivinginsiderMapper.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LivinginsiderBlockedException) {
            println("LivinginsiderListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("LivinginsiderListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
