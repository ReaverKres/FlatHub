package listing.es.fotocasa

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
 * Fotocasa.es — gateway REST JSON. See tmp/es/api/fotocasa/NOTES.md.
 * EUR → [AppFlat.priceByn], priceUsd = null.
 */
class FotocasaListingSource(
    private val api: FotocasaApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.FOTOCASA
    override val country = CountryCode.ES
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
    )

    /** List already includes coords; enrich only for phones/description. */
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val tx = if (filter.adType is AdType.SALE) {
                FotocasaApiClient.TX_SALE
            } else {
                FotocasaApiClient.TX_RENT
            }
            val location = FotocasaCities.location(filter.location?.city)
            val pageSize = FeedDelayListBoost.apiPageSize(platform, base = 30)
            val json = api.search(
                location = location,
                transactionTypeId = tx,
                pageNumber = page,
                resultsPerPage = pageSize,
                priceFrom = filter.priceFull?.priceFrom?.toInt(),
                priceTo = filter.priceFull?.priceTo?.toInt(),
            )
            NetworkResponseWrapper.success(FotocasaFlatMapper.mapSearch(json, filter.adType))
        } catch (e: CancellationException) {
            throw e
        } catch (e: FotocasaBlockedException) {
            println("FotocasaListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: FotocasaFlatMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val tx = if (base.adType is AdType.SALE) {
            FotocasaApiClient.TX_SALE
        } else {
            FotocasaApiClient.TX_RENT
        }
        try {
            val json = api.detail(tx, adId)
            val merged = FotocasaFlatMapper.mergeDetail(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FotocasaBlockedException) {
            println("FotocasaListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("FotocasaListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
