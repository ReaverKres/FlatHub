package listing.kr.dabang

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * Dabang (다방) KR — v5 room-list API. 만원 → KRW on ingest.
 * Rent/jeonse via `one-two`; sale via `officetel` only. Detail 403 without login.
 * See tmp/kr/api/dabang/NOTES.md.
 */
class DabangListingSource(
    private val api: DabangApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.DABANG
    override val country = CountryCode.KR
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsDaily = false,
        supportsJeonse = true,
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
            val flats = FeedDelayListBoost.fetchPages(
                startPage = page,
                platform = platform,
                key = { it.adId },
            ) { p ->
                searchPage(filter.adType, filter.location?.city, p)
            }
            NetworkResponseWrapper.success(flats)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DabangBlockedException) {
            println("DabangListingSource: ${e.message}")
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
            ?: DabangFlatMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        val roomId = DabangFlatMapper.roomIdFromDetailUrl(base.flatDetailUrl) ?: return@flow
        try {
            val detailJson = api.fetchRoomDetail(roomId)
            val merged = DabangDetailMapper.mergeDetail(base, detailJson)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: DabangBlockedException) {
            println("DabangListingSource.detail soft-fail $adId: ${e.message}")
            val marked = DabangDetailMapper.mergeDetail(base, null)
            flatsDao.upsert(marked)
            emit(marked)
        } catch (e: Exception) {
            println("DabangListingSource.detail soft-fail $adId: ${e.message}")
        }
    }

    private suspend fun searchPage(
        adType: AdType,
        city: CityCode?,
        page: Int,
    ): List<AppFlat> = coroutineScope {
        val (category, filters) = when (adType) {
            is AdType.SALE -> "officetel" to api.officetelSaleFilters()
            is AdType.JEONSE -> "one-two" to api.oneTwoRentFilters("LEASE")
            else -> "one-two" to api.oneTwoRentFilters("MONTHLY_RENT")
        }
        val bboxes = DabangCities.bboxes(city)
        val fromBbox = bboxes.map { bbox ->
            async {
                try {
                    val json = api.fetchRoomList(
                        category = category,
                        areaType = "bbox",
                        page = page,
                        filtersJson = filters,
                        bboxJson = bbox.toJson(),
                    ) ?: return@async emptyList()
                    DabangFlatMapper.mapSearch(json, adType)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("DabangListingSource bbox soft-fail: ${e.message}")
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        if (fromBbox.isNotEmpty()) {
            return@coroutineScope fromBbox.distinctBy { it.adId }
        }

        // Fallback: region/dong codes (NOTES — more reliable on some networks)
        val codes = DabangCities.regionCodes(city)
        codes.map { code ->
            async {
                try {
                    val json = api.fetchRoomList(
                        category = category,
                        areaType = "region",
                        page = page,
                        filtersJson = filters,
                        regionCode = code,
                    ) ?: return@async emptyList()
                    DabangFlatMapper.mapSearch(json, adType)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    println("DabangListingSource region soft-fail $code: ${e.message}")
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.adId }
    }
}
