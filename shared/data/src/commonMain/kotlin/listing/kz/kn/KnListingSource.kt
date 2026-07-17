package listing.kz.kn

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
 * kn.kz — HTML search + detail/phone/map enrich. See tmp/kz/api/kn/NOTES.md.
 */
class KnListingSource(
    private val api: KnApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.KN
    override val country = CountryCode.KZ
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
            val html = api.fetchSearchHtml(
                cityAlias = KnCities.cityAlias(filter.location?.city),
                isSale = filter.adType is AdType.SALE,
                page = page,
            )
            NetworkResponseWrapper.success(KnHtmlParser.parseSearch(html, filter.adType))
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
            val detailHtml = api.fetchDetailHtml(adId)
            val mapHtml = runCatching { api.fetchMapHtml(adId) }.getOrNull()
            val phones = runCatching { api.fetchPhones(adId) }.getOrDefault(emptyList())
            val merged = KnHtmlParser.mergeDetail(base, detailHtml, mapHtml, phones)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Soft-fail detail.
        }
    }
}
