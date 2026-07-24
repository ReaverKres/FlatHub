package listing.ca.housesigma

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
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * HouseSigma — encrypted list cards (`mapsearchv3/list`) with map fallback.
 * See tmp/ca/api/housesigma/NOTES.md.
 */
class HouseSigmaListingSource(
    private val api: HouseSigmaApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.HOUSESIGMA
    override val country = CountryCode.CA
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
        val market = HouseSigmaCities.market(filter.location?.city)
        if (market == null) {
            emit(NetworkResponseWrapper.success(emptyList()))
            return@flow
        }
        val result = try {
            val listType = if (filter.adType.isCaSaleDeal()) LIST_TYPE_SALE else LIST_TYPE_RENT
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val root = api.fetchList(market = market, listType = listType, page = page)
            val flats = HouseSigmaMapper.parseList(root, filter.adType)
                .ifEmpty {
                    val mapRoot =
                        api.fetchMapListing(market = market, listType = listType, page = page)
                    HouseSigmaMapper.parseMapListing(mapRoot, filter.adType)
                }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HouseSigmaApiException) {
            println("HouseSigmaListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: return@flow
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val idListing = HouseSigmaMapper.extractIdListing(base.flatDetailUrl)
        val province = HouseSigmaMapper.extractProvince(base.flatDetailUrl) ?: "ON"
        if (idListing == null) return@flow
        try {
            val root = api.fetchDetail(idListing = idListing, province = province)
            val merged = HouseSigmaMapper.mergeDetail(base, root)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HouseSigmaApiException) {
            println("HouseSigmaListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("HouseSigmaListingSource.detail soft-fail $adId: ${e.message}")
        }
    }

    companion object {
        private const val LIST_TYPE_SALE = 1
        private const val LIST_TYPE_RENT = 2
    }
}
