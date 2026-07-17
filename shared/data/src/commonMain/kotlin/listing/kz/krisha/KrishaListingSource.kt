package listing.kz.krisha

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
 * Krisha.kz — SSR HTML search + detail. See tmp/kz/api/krisha/NOTES.md.
 */
class KrishaListingSource(
    private val api: KrishaApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.KRISHA
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
            val rooms = filter.numberOfRooms
                ?.filter { it > 0 }
                ?.takeIf { it.size == 1 }
                ?.firstOrNull()
            val html = api.fetchSearchHtml(
                cityAlias = KrishaCities.cityAlias(filter.location?.city),
                isSale = filter.adType is AdType.SALE,
                page = page,
                rooms = rooms,
                priceFrom = filter.priceFull?.priceFrom?.toInt(),
                priceTo = filter.priceFull?.priceTo?.toInt(),
            )
            NetworkResponseWrapper.success(KrishaHtmlParser.parseSearch(html, filter.adType))
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
            val html = api.fetchDetailHtml(adId)
            val merged = KrishaHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Soft-fail detail.
        }
    }
}
