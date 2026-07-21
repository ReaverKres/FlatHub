package listing.jp.athome

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
 * at home — rent SSR HTML + sale BFF JSON. Soft-fail on Reese/Geetest bot walls.
 * See tmp/jp/api/athome/NOTES.md
 */
class AthomeListingSource(
    private val api: AthomeApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.ATHOME
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
    override val needsBackgroundCoordEnrich: Boolean = true

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
                    fetchSalePage(filter, p)
                }

                else -> FeedDelayListBoost.fetchPages(
                    startPage = page,
                    platform = platform,
                    key = { it.adId },
                ) { p ->
                    val html = api.fetchRentListHtml(filter.location?.city, p)
                    AthomeRentHtmlParser.parseSearch(html)
                }
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AthomeBlockedException) {
            println("AthomeListingSource: ${e.message}")
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
        val base = flatsDao.getById(platform, adId) ?: listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val detailUrl = base.flatDetailUrl ?: return@flow
        val bukkenNo = bukkenNoFromUrl(detailUrl) ?: return@flow
        try {
            val html = when (base.adType) {
                is AdType.SALE -> {
                    val segment =
                        detailUrl.substringAfter("${AthomeApiClient.BASE}/").substringBefore('/')
                    api.fetchSaleDetailHtml(bukkenNo, segment)
                }

                else -> api.fetchRentDetailHtml(bukkenNo)
            }
            val merged = when (base.adType) {
                is AdType.SALE -> AthomeSaleBffMapper.mergeDetail(base, html)
                else -> AthomeRentHtmlParser.mergeDetail(base, html)
            }
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: AthomeBlockedException) {
            println("AthomeListingSource.detail soft-fail $adId: ${e.message}")
        } catch (e: Exception) {
            println("AthomeListingSource.detail soft-fail $adId: ${e.message}")
        }
    }

    private suspend fun fetchSalePage(filter: CommonFilterRequestModel, page: Int): List<AppFlat> {
        val kodateJson = api.fetchSaleListJson(filter.location?.city, page, seoNm = "kodate")
        val kodate = AthomeSaleBffMapper.mapSearch(kodateJson)
        val mansionJson = runCatching {
            api.fetchSaleListJson(filter.location?.city, page, seoNm = "mansion")
        }.getOrNull()
        val mansion = mansionJson?.let { AthomeSaleBffMapper.mapSearch(it) }.orEmpty()
        return (kodate + mansion).distinctBy { it.adId }
    }

    private fun listStub(adId: Long): AppFlat =
        AthomeRentHtmlParser.listStub(adId)

    private fun bukkenNoFromUrl(url: String): String? =
        Regex("""/(?:chintai|kodate|mansion)/(\d+)""").find(url)?.groupValues?.get(1)
}
