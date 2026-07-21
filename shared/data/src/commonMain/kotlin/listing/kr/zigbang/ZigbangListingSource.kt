package listing.kr.zigbang

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.SourceCapabilities
import listing.core.flowById
import kotlin.coroutines.cancellation.CancellationException

/**
 * Zigbang (직방) — geohash map search + v3 per-item detail. KRW (만원 × 10_000).
 * See tmp/kr/api/zigbang/NOTES.md.
 */
class ZigbangListingSource(
    private val api: ZigbangApiClient,
    private val flatsDao: FlatsDao,
) : ListingSource {
    override val platform = FlatPlatform.ZIGBANG
    override val country = CountryCode.KR
    override val capabilities = SourceCapabilities(
        supportsRent = true,
        supportsSale = true,
        supportsJeonse = true,
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
            val geohash = ZigbangCities.geohashForPage(filter.location?.city, page)
            if (geohash == null) {
                NetworkResponseWrapper.success(emptyList())
            } else {
                val adType = filter.adType
                val salesType = adType.toZigbangSalesType()
                val segments = segmentsFor(adType)
                val pins = supervisorScope {
                    segments.map { segment ->
                        async {
                            try {
                                val json = api.fetchMapSearchJson(
                                    segment = segment,
                                    geohash = geohash,
                                    salesType = salesType,
                                )
                                ZigbangFlatMapper.parseMapPins(json)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                emptyList()
                            }
                        }
                    }.flatMap { it.await() }
                }.distinctBy { it.itemId }

                val pageSize = FeedDelayListBoost.apiPageSize(platform, base = 30)
                val batch = pins.take(pageSize)
                val flats = enrichDetails(batch, adType)
                NetworkResponseWrapper.success(flats)
            }
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
        val base = flatsDao.getById(platform, adId) ?: ZigbangFlatMapper.listStub(adId)
        emit(base)
        if (base.flatDevInfo.isDetailLoaded) return@flow
        try {
            val json = api.fetchDetailJson(adId)
            val merged = ZigbangDetailMapper.mergeInto(base, json)
            flatsDao.upsert(merged)
            emit(merged)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("ZigbangListingSource.detail soft-fail $adId: ${e.message}")
        }
    }

    private suspend fun enrichDetails(
        pins: List<ZigbangFlatMapper.MapPin>,
        adType: AdType,
    ): List<AppFlat> = supervisorScope {
        pins.map { pin ->
            async {
                val stub = ZigbangFlatMapper.listStub(pin, adType)
                try {
                    val json = api.fetchDetailJson(pin.itemId)
                    ZigbangDetailMapper.mergeInto(stub, json)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    stub
                }
            }
        }.map { it.await() }
    }

    private fun segmentsFor(adType: AdType): List<String> = when (adType) {
        is AdType.SALE -> listOf(ZigbangApiClient.SEGMENT_VILLA)
        else -> listOf(
            ZigbangApiClient.SEGMENT_ONEROOM,
            ZigbangApiClient.SEGMENT_OFFICETEL,
            ZigbangApiClient.SEGMENT_VILLA,
        )
    }

    private fun AdType.toZigbangSalesType(): String = when (this) {
        is AdType.SALE -> ZigbangApiClient.SALES_SALE
        is AdType.JEONSE -> ZigbangApiClient.SALES_JEONSE
        else -> ZigbangApiClient.SALES_RENT
    }
}
