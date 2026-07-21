package listing.at.immowelt

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
 * Immowelt.at — SSR HTML list; detail may soft-fail on DataDome.
 * See tmp/at/api/immowelt/NOTES.md.
 */
class ImmoweltAtListingSource(
    private val api: ImmoweltAtApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.IMMOWELT_AT
    override val country = CountryCode.AT
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
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
                    city = ImmoweltAtCities.path(filter.location?.city),
                    isSale = filter.adType is AdType.SALE,
                    page = p,
                )
                ImmoweltAtHtmlParser.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImmoweltAtBlockedException) {
            println("ImmoweltAtListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: ImmoweltAtHtmlParser.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val detailUrl = base.flatDetailUrl ?: return@flow
        try {
            val html = api.fetchDetailHtml(detailUrl)
            val merged = ImmoweltAtHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ImmoweltAtBlockedException) {
            println("ImmoweltAtListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("ImmoweltAtListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
