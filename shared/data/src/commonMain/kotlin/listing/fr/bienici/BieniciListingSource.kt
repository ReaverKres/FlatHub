package listing.fr.bienici

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import listing.fr.isFrSaleDeal
import kotlin.coroutines.cancellation.CancellationException

/**
 * Bien'ici — public JSON list + detail. EUR → [AppFlat.mainPrice].
 * See tmp/fr/api/bienici/NOTES.md.
 */
class BieniciListingSource(
    private val api: BieniciApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.BIENICI
    override val country = CountryCode.FR
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
            val isSale = filter.adType.isFrSaleDeal()
            val location = BieniciCities.locationName(filter.location?.city)
            val rooms = filter.numberOfRooms?.filter { it > 0 }?.toList().orEmpty()
            val roomsMin = rooms.minOrNull()
            val roomsMax = rooms.maxOrNull()
            val size = FeedDelayListBoost.apiPageSize(platform, base = 24)
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val from = (p - 1) * size
                val json = api.fetchSearch(
                    isSale = isSale,
                    locationName = location,
                    from = from,
                    size = size,
                    roomsMin = roomsMin,
                    roomsMax = roomsMax,
                )
                BieniciFlatMapper.mapSearch(json, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BieniciBlockedException) {
            println("BieniciListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId)
        if (base == null) {
            emit(null)
            return@flow
        }
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val externalId = BieniciCities.externalIdFromDetailUrl(base.flatDetailUrl) ?: return@flow
        try {
            val json = api.fetchDetail(externalId)
            val merged = BieniciFlatMapper.mergeDetail(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Soft-fail detail; keep list payload.
        }
    }
}
