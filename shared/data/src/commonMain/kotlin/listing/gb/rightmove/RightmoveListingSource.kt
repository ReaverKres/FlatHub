package listing.gb.rightmove

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
 * Rightmove UK — JSON `/api/_search` + HTML fallback; detail via `PAGE_MODEL`.
 * GBP → [AppFlat.mainPrice]. See tmp/gb/api/rightmove/NOTES.md.
 */
class RightmoveListingSource(
    private val api: RightmoveApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.RIGHTMOVE
    override val country = CountryCode.GB
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
            val isSale = filter.adType is AdType.SALE
            val locationId = RightmoveCities.locationIdentifier(filter.location?.city)
            val pageSize = FeedDelayListBoost.apiPageSize(platform, base = 24)
            val index = (page - 1) * pageSize
            val priceFrom = filter.priceFull?.priceFrom?.toInt()
            val priceTo = filter.priceFull?.priceTo?.toInt()
            val flats = runCatching {
                val json = api.searchJson(
                    locationIdentifier = locationId,
                    isSale = isSale,
                    index = index,
                    pageSize = pageSize,
                    priceFrom = priceFrom,
                    priceTo = priceTo,
                )
                RightmoveMapper.mapSearchJson(json, filter.adType)
            }.getOrElse { primaryError ->
                if (primaryError is CancellationException) throw primaryError
                println("RightmoveListingSource: JSON search failed (${primaryError.message}), trying HTML fallback")
                val html = api.searchHtmlFallback(
                    locationIdentifier = locationId,
                    isSale = isSale,
                    index = index,
                )
                RightmoveMapper.parseSearchHtml(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RightmoveBlockedException) {
            println("RightmoveListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: RightmoveMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(adId)
            val merged = RightmoveMapper.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RightmoveBlockedException) {
            println("RightmoveListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("RightmoveListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
