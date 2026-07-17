package listing.kz.kn

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * kn.kz — HTML search + detail/phone/map enrich. See tmp/kz/api/kn/NOTES.md.
 */
class KnListingSource(
    private val api: KnApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.KN
    override val country = CountryCode.KZ
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = true,
        supportsRoom = false,
        supportsCommercial = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = true

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val section = when (filter.adType) {
                is AdType.SALE -> "prodazha-kvartir"
                is AdType.DAILY -> "arenda-kvartir-posutochno"
                else -> "arenda-kvartir"
            }
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                val html = api.fetchSearchHtml(
                    cityAlias = KnCities.cityAlias(filter.location?.city),
                    section = section,
                    page = p,
                )
                KnHtmlParser.parseSearch(html, filter.adType)
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
            // Detail HTML first (required); map/phones in parallel so one slow call
            // does not block the whole enrich for ~20s×N.
            val merged = coroutineScope {
                val detailHtml = api.fetchDetailHtml(adId)
                val mapDeferred = async {
                    runCatching { api.fetchMapHtml(adId) }.getOrNull()
                }
                val phonesDeferred = async {
                    runCatching { api.fetchPhones(adId) }.getOrDefault(emptyList())
                }
                KnHtmlParser.mergeDetail(
                    base,
                    detailHtml,
                    mapDeferred.await(),
                    phonesDeferred.await(),
                )
            }
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Base already on screen — inline error under photos.
            println("KnListingSource.detail soft-fail $adId: ${e.message}")
            throw e
        }
    }
}
