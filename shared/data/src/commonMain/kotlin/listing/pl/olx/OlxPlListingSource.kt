package listing.pl.olx

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
 * OLX.pl — REST `/api/v1/offers/` (see tmp/pl/api/olx/NOTES.md).
 */
class OlxPlListingSource(
    private val api: OlxPlApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.OLX_PL
    override val country = CountryCode.PL
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = true,
        supportsRoom = true,
        supportsCommercial = true,
    )

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val limit = listing.core.FeedDelayListBoost.apiPageSize(platform, PAGE_SIZE)
            val offset = (page - 1) * limit
            val ids = OlxPlCities.idsFor(filter.location?.city)
            val categoryId = OlxPlCities.categoryId(
                isSale = filter.adType is AdType.SALE,
                isRoom = filter.isRoomForRent || filter.roomOnly,
                isCommercial = filter.isCommercial,
            )
            val json = api.fetchOffers(
                categoryId = categoryId,
                regionId = ids.regionId,
                cityId = ids.cityId,
                offset = offset,
                limit = limit,
                priceFrom = filter.priceFull?.priceFrom?.toInt(),
                priceTo = filter.priceFull?.priceTo?.toInt(),
            )
            NetworkResponseWrapper.success(OlxPlFlatMapper.mapOffers(json, filter.adType))
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
            val json = api.fetchOffer(adId)
            val merged = OlxPlDetailMapper.mergeInto(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep list payload; soft-fail detail.
        }
    }

    companion object {
        private const val PAGE_SIZE = 40
    }
}
