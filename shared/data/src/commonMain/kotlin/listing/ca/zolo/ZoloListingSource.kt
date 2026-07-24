package listing.ca.zolo

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.ca.isCaSaleDeal
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * Zolo.ca — SSR gallery HTML. CAD → [AppFlat.mainPrice]. Coords on list.
 * See tmp/ca/api/zolo/NOTES.md.
 */
class ZoloListingSource(
    private val api: ZoloApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.ZOLO
    override val country = CountryCode.CA
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = true,
        supportsCommercial = false,
        supportsCommercialPropertyTypes = false,
        supportsFromOwnerOnly = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val slug = ZoloCities.slug(filter.location?.city)
        if (slug == null) {
            emit(NetworkResponseWrapper.success(emptyList()))
            return@flow
        }
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val forRent = !filter.adType.isCaSaleDeal()
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchGalleryHtml(slug = slug, forRent = forRent, page = p)
                ZoloHtmlParser.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ZoloBlockedException) {
            println("ZoloListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: ZoloHtmlParser.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(base.flatDetailUrl)
            val merged = ZoloHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ZoloBlockedException) {
            println("ZoloListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("ZoloListingSource.detail soft-fail $adId: ${e.message}")
        }
    }
}
