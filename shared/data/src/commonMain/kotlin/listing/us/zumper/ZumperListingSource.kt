package listing.us.zumper

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
import listing.us.isUsSaleDeal
import kotlin.coroutines.cancellation.CancellationException

/**
 * Zumper US — SSR `window.__PRELOADED_STATE__`. USD → [AppFlat.mainPrice].
 * Rent-only: sale SERP (`houses-for-sale/{slug}`) redirects to sitemap (2026-07).
 * See tmp/us/api/zumper/NOTES.md.
 */
class ZumperListingSource(
    private val api: ZumperApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.ZUMPER
    override val country = CountryCode.US
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = false,
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
        if (filter.adType.isUsSaleDeal()) {
            emit(NetworkResponseWrapper.success(emptyList()))
            return@flow
        }
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val slug = UsCities.slug(filter.location?.city)
            val adType = filter.adType
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(slug = slug, page = p)
                ZumperMapper.parseSearch(html, adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ZumperBlockedException) {
            println("ZumperListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: ZumperMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(base.flatDetailUrl)
            val merged = ZumperMapper.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ZumperBlockedException) {
            println("ZumperListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("ZumperListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
