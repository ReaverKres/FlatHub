package listing.pl.otodom

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
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

class OtodomListingSource(
    private val api: OtodomApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform: FlatPlatform = FlatPlatform.OTODOM
    override val country: CountryCode = CountryCode.PL
    override val capabilities: SourceCapabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsRoom = true,
        supportsCommercial = true,
    )

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val adType = filter.adType
            val transaction = when (adType) {
                is AdType.SALE -> "sprzedaz"
                else -> "wynajem"
            }
            val estate = when {
                filter.isCommercial -> "lokal"
                filter.isRoomForRent || filter.roomOnly -> "pokoj"
                else -> "mieszkanie"
            }
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val json = api.fetchSearchJson(
                transactionPath = transaction,
                estatePath = estate,
                cityPath = OtodomCities.pathFor(filter.location?.city),
                page = page,
                priceMin = filter.priceFull?.priceFrom?.toInt(),
                priceMax = filter.priceFull?.priceTo?.toInt(),
            )
            NetworkResponseWrapper.success(OtodomFlatMapper.mapSearchAds(json, adType))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            api.invalidateBuildId()
            NetworkResponseWrapper.error(
                e,
                NetworkErrorInfo(platform, listOf(e.message.orEmpty())),
            )
        }
        emit(result)
    }

    override fun getById(adId: Long): Flow<AppFlat?> = flatsDao.flowById(adId)

    override fun detail(adId: Long): Flow<AppFlat?> = flow {
        val base = flatsDao.getById(adId)
        if (base == null) {
            emit(null)
            return@flow
        }
        // Show list data immediately; enrich after network.
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow

        val slug = OtodomDetailMapper.slugFromDetailUrl(base.flatDetailUrl) ?: return@flow
        try {
            val json = api.fetchDetailJson(slug)
            val merged = OtodomDetailMapper.mergeInto(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            api.invalidateBuildId()
        }
    }
}
