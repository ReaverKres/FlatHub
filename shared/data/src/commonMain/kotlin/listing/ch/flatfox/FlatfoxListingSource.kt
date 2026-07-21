package listing.ch.flatfox

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import listing.ch.isChSaleDeal
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * Flatfox Switzerland — pin bbox → batched public-listing (`pk=` + `expand=images`).
 * CHF → [AppFlat.mainPrice]. Rent-only. See tmp/ch/api/flatfox/NOTES.md.
 */
class FlatfoxListingSource(
    private val api: FlatfoxApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.FLATFOX
    override val country = CountryCode.CH
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = false,
        supportsDaily = false,
        supportsRoom = true,
        supportsCommercial = false,
        supportsCommercialPropertyTypes = false,
        supportsFromOwnerOnly = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        if (filter.adType.isChSaleDeal()) {
            emit(NetworkResponseWrapper.success(emptyList()))
            return@flow
        }
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val bbox = FlatfoxCities.bbox(filter.location?.city)
            val pinsArray = api.fetchPins(bbox) ?: emptyList()
            val pins = FlatfoxMapper.parsePins(pinsArray)
            val pageSize = FeedDelayListBoost.apiPageSize(platform, base = 30)
                .coerceAtMost(FlatfoxApiClient.MAX_LIST_PAGE)
            val start = (page - 1) * pageSize
            if (start >= pins.size) {
                NetworkResponseWrapper.success(emptyList())
            } else {
                val batch = pins.drop(start).take(pageSize)
                val flats = hydrateBatch(batch)
                NetworkResponseWrapper.success(flats)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FlatfoxBlockedException) {
            println("FlatfoxListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: FlatfoxMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val listing = api.fetchListing(adId)
            val merged = FlatfoxMapper.mergeDetail(base, listing)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FlatfoxBlockedException) {
            println("FlatfoxListingSource.detail soft-fail $adId: ${e.message}")
            val marked = FlatfoxMapper.mergeDetail(base, null)
            flatsDao.upsert(marked)
            emit(marked)
        } catch (e: Exception) {
            println("FlatfoxListingSource.detail soft-fail $adId: ${e.message}")
        }
    }

    /** One (or few) list requests instead of N× detail. Preserves pin order. */
    private suspend fun hydrateBatch(pins: List<FlatfoxMapper.Pin>): List<AppFlat> {
        if (pins.isEmpty()) return emptyList()
        return supervisorScope {
            pins.chunked(FlatfoxApiClient.MAX_LIST_PAGE).map { chunk ->
                async {
                    try {
                        val listings = api.fetchListingsByPks(chunk.map { it.pk })
                        FlatfoxMapper.mapBatch(listings, chunk)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        chunk.map { FlatfoxMapper.mapPinStub(it) }
                    }
                }
            }.flatMap { it.await() }
        }
    }
}
