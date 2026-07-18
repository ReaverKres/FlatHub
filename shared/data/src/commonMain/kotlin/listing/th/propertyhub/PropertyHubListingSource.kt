package listing.th.propertyhub

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import entities.FlatDevInfo
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
 * PropertyHub Thailand — `_next/data` condo rent/sale. THB → [AppFlat.priceByn].
 * See tmp/th/api/propertyhub/NOTES.md.
 */
class PropertyHubListingSource(
    private val api: PropertyHubApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.PROPERTYHUB
    override val country = CountryCode.TH
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = false,
        supportsCommercial = false,
        supportsFromOwnerOnly = true,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val zone = ThCities.zoneSlug(filter.location?.city)
            val isSale = filter.adType.isThSaleDeal()
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val json = api.fetchSearchJson(zoneSlug = zone, isSale = isSale, page = p)
                PropertyHubMapper.parseSearch(json, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: PropertyHubBlockedException) {
            println("PropertyHubListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: PropertyHubMapper.listStub(adId)
        emit(base)
        // List payload already has phones/coords; no anonymous detail when rate-limited.
        if (!base.flatDevInfo.isDetailLoaded) {
            val marked = base.copy(
                flatDevInfo = FlatDevInfo(isDetailData = true, isDetailLoaded = true),
            )
            flatsDao.upsert(marked)
            emit(marked)
        }
    }
}
