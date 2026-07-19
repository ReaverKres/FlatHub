package listing.de.kleinanzeigen

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
 * Kleinanzeigen.de — SSR HTML. EUR → [AppFlat.mainPrice], secondPrice = null.
 */
class KleinanzeigenListingSource(
    private val api: KleinanzeigenApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.KLEINANZEIGEN
    override val country = CountryCode.DE
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
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(
                    city = KleinanzeigenCities.location(filter.location?.city),
                    isSale = filter.adType is AdType.SALE,
                    page = p,
                )
                KleinanzeigenHtmlParser.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: KleinanzeigenBlockedException) {
            println("KleinanzeigenListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: KleinanzeigenHtmlParser.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val detailUrl = base.flatDetailUrl ?: return@flow
        try {
            val html = api.fetchDetailHtml(detailUrl)
            val merged = KleinanzeigenHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: KleinanzeigenBlockedException) {
            println("KleinanzeigenListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("KleinanzeigenListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
