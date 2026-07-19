package listing.de.immowelt

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
 * Immowelt.de — SSR HTML list; detail may soft-fail on DataDome.
 * EUR → [AppFlat.mainPrice], secondPrice = null.
 */
class ImmoweltListingSource(
    private val api: ImmoweltApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.IMMOWELT
    override val country = CountryCode.DE
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
    )

    /** Detail is DataDome-blocked; list uses Bezirk centroids when possible. */
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            // Recon: Immowelt ignores/blocks pagination — only contribute page 1.
            if (page > 1) {
                emit(NetworkResponseWrapper.success(emptyList()))
                return@flow
            }
            val flats = FeedDelayListBoost.fetchPages(
                startPage = 1,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(
                    city = ImmoweltCities.path(filter.location?.city),
                    isSale = filter.adType is AdType.SALE,
                    page = p,
                )
                ImmoweltHtmlParser.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImmoweltBlockedException) {
            println("ImmoweltListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: ImmoweltHtmlParser.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val detailUrl = base.flatDetailUrl ?: return@flow
        try {
            val html = api.fetchDetailHtml(detailUrl)
            val merged = ImmoweltHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImmoweltBlockedException) {
            println("ImmoweltListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("ImmoweltListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
