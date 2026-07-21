package listing.jp.suumo

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
import kotlin.coroutines.cancellation.CancellationException

/**
 * SUUMO — SSR HTML rent cassette + used-mansion sale list. See tmp/jp/api/suumo/NOTES.md
 */
class SuumoListingSource(
    private val api: SuumoApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.SUUMO
    override val country = CountryCode.JP
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsJeonse = false,
        supportsRoom = false,
        supportsCommercial = false,
        supportsFromOwnerOnly = false,
    )

    /** No coords in list; sale detail may expose map pins — no background enrich path yet. */
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(
                    city = filter.location?.city,
                    adType = filter.adType,
                    page = p,
                )
                SuumoHtmlParser.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SuumoBlockedException) {
            println("SuumoListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: SuumoHtmlParser.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val detailUrl =
            base.flatDetailUrl.takeIf { it.startsWith("http") && it.length > 20 } ?: return@flow
        try {
            val html = api.fetchDetailHtml(detailUrl)
            val merged = SuumoHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SuumoBlockedException) {
            println("SuumoListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("SuumoListingSource.detail soft-fail $adId: ${e.message}")
        }
    }
}
