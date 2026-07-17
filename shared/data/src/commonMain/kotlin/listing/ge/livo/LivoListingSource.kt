package listing.ge.livo

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

/**
 * Livo.ge — GET /v1/statements. See tmp/ge/api/livo/NOTES.md.
 */
class LivoListingSource(
    private val api: LivoApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.LIVO
    override val country = CountryCode.GE
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = true,
        supportsRoom = false,
        supportsCommercial = false,
    )

    /** List often has empty lat/lng; detail usually fills them. */
    override val needsBackgroundCoordEnrich: Boolean = true

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val dealType = when (filter.adType) {
                is AdType.SALE -> LivoApiClient.DEAL_SALE
                is AdType.DAILY -> LivoApiClient.DEAL_DAILY
                else -> LivoApiClient.DEAL_RENT
            }
            val json = api.fetchStatements(
                page = page,
                cityId = LivoCities.cityId(filter.location?.city),
                dealType = dealType,
                perPage = listing.core.FeedDelayListBoost.apiPageSize(platform, base = 40),
                priceFrom = filter.priceFull?.priceFrom?.toInt(),
                priceTo = filter.priceFull?.priceTo?.toInt(),
                currencyId = LivoApiClient.CURRENCY_GEL,
            )
            NetworkResponseWrapper.success(LivoFlatMapper.mapSearch(json, filter.adType))
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
            val json = api.fetchStatement(adId)
            val merged = LivoFlatMapper.mapDetail(json, base)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Soft-fail detail; keep list payload.
        }
    }
}
