package listing.at.is24

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
import listing.de.is24.Is24ApiClient
import listing.de.is24.Is24BlockedException
import kotlin.coroutines.cancellation.CancellationException

/**
 * ImmobilienScout24.at — reuses DE mobile JSON API + AT geocodes.
 * See tmp/at/api/is24/NOTES.md.
 */
class Is24AtListingSource(
    private val api: Is24ApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.IS24_AT
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
            val isSale = filter.adType is AdType.SALE
            val geocode = Is24AtCities.geocode(filter.location?.city)
            val pageSize = FeedDelayListBoost.apiPageSize(platform, base = 20)
            val rooms = filter.numberOfRooms.orEmpty().filter { it > 0 }.sorted()
            val json = api.search(
                geocode = geocode,
                isSale = isSale,
                pageNumber = page,
                pageSize = pageSize,
                priceFrom = filter.priceFull?.priceFrom?.toInt(),
                priceTo = filter.priceFull?.priceTo?.toInt(),
                roomsFrom = rooms.firstOrNull(),
                roomsTo = rooms.lastOrNull(),
            )
            NetworkResponseWrapper.success(Is24AtFlatMapper.mapSearch(json, filter.adType))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Is24BlockedException) {
            println("Is24AtListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: Is24AtFlatMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val json = api.detail(adId.toString())
            val merged = Is24AtFlatMapper.mergeDetail(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Is24BlockedException) {
            println("Is24AtListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("Is24AtListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
