package listing.gb.openrent

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import listing.gb.isGbSaleDeal
import kotlin.coroutines.cancellation.CancellationException

/**
 * OpenRent UK — private-landlord rent only. SSR HTML + embedded coord arrays.
 * GBP → [AppFlat.mainPrice]. See tmp/gb/api/openrent/NOTES.md.
 */
class OpenRentListingSource(
    private val api: OpenRentApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.OPENRENT
    override val country = CountryCode.GB
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = false,
        supportsDaily = false,
        supportsRoom = true,
        supportsCommercial = false,
        supportsCommercialPropertyTypes = false,
        supportsFromOwnerOnly = true,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        if (filter.adType.isGbSaleDeal()) {
            emit(NetworkResponseWrapper.success(emptyList()))
            return@flow
        }
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            if (page > 1) {
                // MVP: first SSR page only (~20 cards). XHR pagination needs cookie jar.
                NetworkResponseWrapper.success(emptyList())
            } else {
                val slug =
                    Location.mapCityCodeToDomainName(filter.location?.city ?: CityCode.LONDON)
                val html = api.fetchSearchHtml(slug)
                val flats = OpenRentHtmlParser.parseSearch(html)
                NetworkResponseWrapper.success(flats)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: OpenRentBlockedException) {
            println("OpenRentListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: OpenRentHtmlParser.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val html = api.fetchDetailHtml(base.flatDetailUrl)
            val merged = OpenRentHtmlParser.mergeDetail(base, html)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: OpenRentBlockedException) {
            println("OpenRentListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("OpenRentListingSource.detail soft-fail $adId: ${e.message}")
        }
    }
}
