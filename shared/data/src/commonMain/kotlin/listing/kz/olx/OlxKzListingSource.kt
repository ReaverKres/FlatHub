package listing.kz.olx

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
 * OLX.kz — REST `/api/v1/offers/` (see tmp/kz/api/olx/NOTES.md).
 */
class OlxKzListingSource(
    private val api: OlxKzApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.OLX_KZ
    override val country = CountryCode.KZ
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
    )

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val limit = listing.core.FeedDelayListBoost.apiPageSize(platform, PAGE_SIZE)
            val offset = (page - 1) * limit
            val ids = OlxKzCities.idsFor(filter.location?.city)
            val categoryId = OlxKzCities.categoryId(isSale = filter.adType is AdType.SALE)
            val json = api.fetchOffers(
                categoryId = categoryId,
                regionId = ids.regionId,
                cityId = ids.cityId,
                offset = offset,
                limit = limit,
            )
            NetworkResponseWrapper.success(OlxKzFlatMapper.mapOffers(json, filter.adType))
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
            val phones = runCatching { api.fetchPhones(adId) }.getOrDefault(emptyList())
            val merged = OlxKzDetailMapper.mergeInto(base, json, phones)
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
