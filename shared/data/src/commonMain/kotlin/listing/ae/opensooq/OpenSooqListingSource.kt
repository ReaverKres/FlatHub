package listing.ae.opensooq

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.ae.AeCommercialTypes
import listing.ae.isAeSaleDeal
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * OpenSooq UAE — SSR `__NEXT_DATA__`. AED → [AppFlat.mainPrice].
 * Residential apartments + commercial SERP. See tmp/ae/api/opensooq/NOTES.md.
 */
class OpenSooqListingSource(
    private val api: OpenSooqApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.OPENSOOQ
    override val country = CountryCode.AE
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = true,
        supportsCommercialPropertyTypes = true,
        supportsFromOwnerOnly = true,
    )
    override val needsBackgroundCoordEnrich: Boolean = true

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val citySlug = OpenSooqCities.slug(filter.location?.city)
            val isSale = filter.adType.isAeSaleDeal()
            val commercialKind = if (filter.isCommercial) {
                AeCommercialTypes.openSooqKind(
                    type = filter.commercial?.commercialPropertyType,
                    isSale = isSale,
                )
            } else {
                null
            }
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(
                    citySlug = citySlug,
                    isSale = isSale,
                    isCommercial = filter.isCommercial,
                    commercialKind = commercialKind,
                    page = p,
                )
                OpenSooqMapper.parseSearch(html, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OpenSooqBlockedException) {
            println("OpenSooqListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: OpenSooqMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(base.flatDetailUrl)
            val merged = OpenSooqMapper.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OpenSooqBlockedException) {
            println("OpenSooqListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("OpenSooqListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
