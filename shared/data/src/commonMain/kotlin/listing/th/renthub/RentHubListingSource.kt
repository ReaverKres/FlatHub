package listing.th.renthub

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
import listing.th.ThCities
import listing.th.isThSaleDeal
import kotlin.coroutines.cancellation.CancellationException

/**
 * RentHub Thailand — apartment buildings for rent only. THB → [AppFlat.priceByn].
 * See tmp/th/api/renthub/NOTES.md.
 */
class RentHubListingSource(
    private val api: RentHubApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.RENTHUB
    override val country = CountryCode.TH
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = false,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        if (filter.adType.isThSaleDeal()) {
            emit(NetworkResponseWrapper.success(emptyList()))
            return@flow
        }
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val zone = ThCities.zoneSlug(filter.location?.city)
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val json = api.fetchSearchJson(zoneSlug = zone, page = p)
                RentHubMapper.parseSearch(json)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RentHubBlockedException) {
            println("RentHubListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: RentHubMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val slug = base.flatDetailUrl
                .substringAfterLast('/')
                .substringBefore('?')
                .ifBlank { return@flow }
            val json = api.fetchDetailJson(slug)
            val merged = RentHubMapper.mergeDetail(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RentHubBlockedException) {
            println("RentHubListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("RentHubListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
