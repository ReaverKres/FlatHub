package listing.pl.gratka

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/** Gratka.pl GraphQL — see tmp/pl/api/gratka/NOTES.md */
class GratkaListingSource(
    private val api: GratkaApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.GRATKA
    override val country = CountryCode.PL
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = true,
        supportsRoom = true,
        supportsCommercial = true,
        supportsFromOwnerOnly = true,
    )

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val flats = listing.core.FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val url = GratkaCities.listingUrl(
                    city = filter.location?.city,
                    adType = filter.adType,
                    isRoom = filter.isRoomForRent || filter.roomOnly,
                    isCommercial = filter.isCommercial,
                    page = p,
                    priceFrom = filter.priceFull?.priceFrom?.toInt(),
                    priceTo = filter.priceFull?.priceTo?.toInt(),
                )
                val json = api.searchProperties(url)
                GratkaFlatMapper.mapSearch(json, filter.adType)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
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
        try {
            val json = api.fetchProperty(base.flatDetailUrl)
            val merged = GratkaDetailMapper.mergeInto(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep list payload; soft-fail detail.
        }
    }
}
