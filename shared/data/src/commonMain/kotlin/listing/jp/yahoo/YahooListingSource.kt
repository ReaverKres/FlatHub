package listing.jp.yahoo

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
 * Yahoo!不動産 — rent JSON search (coords in list) + used-mansion sale SSR HTML.
 * See tmp/jp/api/yahoo/NOTES.md
 */
class YahooListingSource(
    private val api: YahooApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.YAHOO_RE
    override val country = CountryCode.JP
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsJeonse = false,
        supportsRoom = false,
        supportsCommercial = false,
        supportsFromOwnerOnly = false,
    )
    override val needsBackgroundCoordEnrich: Boolean = false

    override fun search(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<NetworkResponseWrapper<List<AppFlat>>> = flow {
        val result = try {
            val page = (currentPage ?: 1).coerceAtLeast(1)
            val flats = when (filter.adType) {
                is AdType.SALE -> FeedDelayListBoost.fetchPages(
                    startPage = page,
                    platform = platform,
                    key = { it.adId },
                ) { p ->
                    val html = api.fetchSaleSearchHtml(filter.location?.city, p)
                    YahooSaleHtmlParser.parseSearch(html)
                }

                else -> FeedDelayListBoost.fetchPages(
                    startPage = page,
                    platform = platform,
                    key = { it.adId },
                ) { p ->
                    val json = api.fetchRentSearch(filter.location?.city, p)
                        ?: return@fetchPages emptyList()
                    YahooFlatMapper.mapRentSearch(json)
                }
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: YahooBlockedException) {
            println("YahooListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: YahooFlatMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val detailUrl =
            base.flatDetailUrl.takeIf { it.startsWith("http") && it.length > 30 } ?: return@flow
        try {
            val merged = when (base.adType) {
                is AdType.SALE -> {
                    val html = api.fetchSaleDetailHtml(detailUrl)
                    YahooSaleHtmlParser.mergeDetail(base, html)
                }

                else -> return@flow
            }
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: YahooBlockedException) {
            println("YahooListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("YahooListingSource.detail soft-fail $adId: ${e.message}")
        }
    }
}
