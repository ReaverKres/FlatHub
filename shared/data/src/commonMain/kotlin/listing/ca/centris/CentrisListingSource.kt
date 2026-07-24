package listing.ca.centris

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
 * Centris.ca — Quebec MLS via SSR HTML. CAD → [AppFlat.mainPrice].
 * See tmp/ca/api/centris/NOTES.md.
 */
class CentrisListingSource(
    private val api: CentrisApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.CENTRIS
    override val country = CountryCode.CA
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
        supportsCommercialPropertyTypes = false,
        supportsFromOwnerOnly = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val regionSlug = CentrisCities.regionSlug(filter.location?.city)
        if (regionSlug == null) {
            emit(NetworkResponseWrapper.success(emptyList()))
            return@flow
        }
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val forRent = !filter.adType.isCaSaleDeal()
            val firstHtml = api.fetchListHtml(
                regionSlug = regionSlug,
                forRent = forRent,
                page = page,
                sortSeed = null,
            )
            val sortSeed = CentrisHtmlParser.extractSortSeed(firstHtml)
            val flats = mutableListOf<AppFlat>()
            flats += CentrisHtmlParser.parseSearch(firstHtml, filter.adType)
            val extra = FeedDelayListBoost.htmlExtraPages(platform)
            if (extra > 0 && !sortSeed.isNullOrBlank()) {
                val nextHtml = api.fetchListHtml(
                    regionSlug = regionSlug,
                    forRent = forRent,
                    page = page + 1,
                    sortSeed = sortSeed,
                )
                flats += CentrisHtmlParser.parseSearch(nextHtml, filter.adType)
            }
            NetworkResponseWrapper.success(flats.distinctBy { it.adId })
        } catch (e: CancellationException) {
            throw e
        } catch (e: CentrisBlockedException) {
            println("CentrisListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: CentrisHtmlParser.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(base.flatDetailUrl)
            val merged = CentrisHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: CentrisBlockedException) {
            println("CentrisListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("CentrisListingSource.detail soft-fail $adId: ${e.message}")
        }
    }
}
