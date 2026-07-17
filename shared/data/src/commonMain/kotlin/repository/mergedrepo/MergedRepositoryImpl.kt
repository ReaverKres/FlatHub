package repository.mergedrepo

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.commoncomponents.network.ConnectionMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import listing.core.CoordEnricher
import listing.core.FeedDelayListBoost
import listing.core.ListingSource
import listing.core.ListingSourceRegistry
import metro.MetroProximityEnricher
import metro.MetroStationsGeoCatalog
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

class MergedRepositoryImpl(
    private val listingSourceRegistry: ListingSourceRegistry,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao,
    private val connectionMonitor: ConnectionMonitor,
    private val coordEnricher: CoordEnricher,
) : MergedRepository {

    override val lastEmittedFlats: MutableSharedFlow<List<AppFlat>> = MutableSharedFlow(replay = 1)

    override fun searchFlats(): Flow<MergedFlatResponse> {
        val filter = filterRepository.lastFilter()
        // Must pass page — ListingSources use (currentPage ?: 1), they do not read
        // FilterRepository themselves (unlike legacy BY repos).
        return searchByFilter(filter, filterRepository.currentHomePage)
    }

    override fun searchFlats(filter: CommonFilterRequestModel, currentPage: Int): Flow<MergedFlatResponse> {
        return searchByFilter(filter, currentPage)
    }

    private fun searchByFilter(
        filter: CommonFilterRequestModel,
        currentPage: Int?,
    ): Flow<MergedFlatResponse> = flow {
        FeedDelayListBoost.active = filterRepository.listFetchBoostActive
        val sources = listingSourceRegistry.forFilter(filter)
        val searchedPlatforms = sources.map { it.platform }
        val networkFlats = supervisorScope {
            sources.map { source ->
                async {
                    awaitPlatformSearch(source.platform) {
                        source.search(filter, currentPage).first()
                    }
                }
            }.awaitAll()
        }

        emit(
            mergeNetworkFlats(
                networkFlats = networkFlats,
                filter = filter,
                searchedPlatforms = searchedPlatforms,
            ),
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun awaitPlatformSearch(
        platform: FlatPlatform,
        search: suspend () -> NetworkResponseWrapper<List<AppFlat>>,
    ): NetworkResponseWrapper<List<AppFlat>> {
        return try {
            search()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            println("MergedRepository $platform searchFlats ex ${e.message}")
            NetworkResponseWrapper.error(
                e,
                NetworkErrorInfo(platform, listOf(e.message.orEmpty())),
            )
        }
    }

    private suspend fun mergeNetworkFlats(
        networkFlats: List<NetworkResponseWrapper<List<AppFlat>>>,
        filter: CommonFilterRequestModel,
        searchedPlatforms: List<FlatPlatform>,
    ): MergedFlatResponse {
        MetroStationsGeoCatalog.loadIfNeeded()
        val appFlats: MutableList<AppFlat> = mutableListOf()
        val platformErrors: MutableList<NetworkErrorInfo> = mutableListOf()

        networkFlats.forEach { nett ->
            when (nett) {
                is NetworkResponseWrapper.Success<List<AppFlat>> -> {
                    nett.data.forEach { net ->
                        val fromDb = flatsDao.getById(net.flatPlatform, net.adId)

                        val priceUsdSquare =
                            if (net.priceUsd != null && net.totalArea != null && net.totalArea > 0) {
                                net.priceUsd / net.totalArea
                            } else net.priceUsdSquare

                        val priceBynSquare =
                            if (net.priceByn != null && net.totalArea != null && net.totalArea > 0) {
                                net.priceByn / net.totalArea
                            } else net.priceBynSquare

                        val coords = net.coordinates ?: fromDb?.coordinates
                        val updated = MetroProximityEnricher.enrich(
                            net.copy(
                                adType = filter.adType,
                                savedInFavorites = fromDb?.savedInFavorites == true,
                                isViewed = fromDb?.isViewed == true,
                                dislike = fromDb?.dislike == true,
                                priceUsdSquare = priceUsdSquare,
                                priceBynSquare = priceBynSquare,
                                coordinates = coords,
                                flatDevInfo = net.flatDevInfo.copy(
                                    coordsEnriched = coords != null ||
                                            fromDb?.flatDevInfo?.coordsEnriched == true,
                                ),
                            ),
                        )
                        appFlats.add(updated)
                    }
                }

                is NetworkResponseWrapper.Error -> {
                    nett.error?.let { err ->
                        platformErrors.add(err)
                    }
                }
            }
        }

        flatsDao.upsertAll(appFlats)
        coordEnricher.enqueue(appFlats)

        val sortedFlatList = applyLocalSortOrFilters(appFlats, filter)
        lastEmittedFlats.emit(sortedFlatList)

        val generalError = LocalizationKeys.SEARCH_ERROR_VPN_HINT
            .takeIf { platformErrors.isNotEmpty() && connectionMonitor.isVpnConnected() }

        return MergedFlatResponse(
            flats = sortedFlatList,
            errors = MergedNetworkErrors(
                platformErrors = platformErrors,
                generalError = generalError,
            ),
            searchedPlatforms = searchedPlatforms,
        )
    }

    override suspend fun getFlatByIdWithDetails(
        flatPlatform: FlatPlatform,
        flatId: Long,
        markAsViewed: Boolean,
    ): Flow<AppFlat> {
        MetroStationsGeoCatalog.loadIfNeeded()
        return flow {
            var emitted = false
            sourceFor(flatPlatform).detail(flatId).collect { flat ->
                if (flat == null) return@collect
                val viewed = if (markAsViewed) flat.copy(isViewed = true) else flat
                val enriched = MetroProximityEnricher.enrich(viewed)
                withContext(Dispatchers.IO) { flatsDao.upsert(enriched) }
                emitted = true
                emit(enriched)
            }
            if (!emitted) {
                error("Flat not found: $flatPlatform/$flatId")
            }
        }.flowOn(Dispatchers.IO).catch { cause ->
            if (cause is CancellationException) throw cause
            println("detailFlat LoadError $cause")
            throw cause
        }
    }

    override fun clearCashedFlats() {
        listingSourceRegistry.all().forEach { it.clearCache() }
    }

    override fun getAllFlatsFromLocalDb(): Flow<List<AppFlat>> {
        return flatsDao.getAllAsFlow().map { flats ->
            flats.sortedByDescending { it.publishedAt }
        }
    }

    private fun applyLocalSortOrFilters(
        flats: List<AppFlat>,
        currentFilter: CommonFilterRequestModel = filterRepository.lastFilter(),
    ): List<AppFlat> {
        var resultList = flats

        val priceIsAdInUsd = currentFilter.adType != AdType.DAILY
        resultList = filterByPrice(
            priceInFilter = currentFilter.priceFull,
            priceInAd = { if (priceIsAdInUsd) it.priceUsd else it.priceByn },
            resultList = resultList,
        )

        if (currentFilter.adType != AdType.DAILY) {
            resultList = filterByPrice(
                priceInFilter = currentFilter.pricePerSquare,
                priceInAd = { it.priceUsdSquare },
                resultList = resultList,
            )
        }

        if (currentFilter.totalArea != null) {
            val from = currentFilter.totalArea.fromRange
            val to = currentFilter.totalArea.toRange

            if (from != null) {
                resultList = resultList.filter { flat ->
                    flat.totalArea != null && flat.totalArea >= from
                }
            }

            if (to != null) {
                resultList = resultList.filter { flat ->
                    flat.totalArea != null && flat.totalArea <= to
                }
            }
        }

        if (currentFilter.addressRequestModel.isNotEmpty()) {
            resultList = resultList.filter { flat ->
                currentFilter.addressRequestModel.any { filterAddress ->
                    flat.address?.contains(filterAddress.address, ignoreCase = true) == true
                }
            }
        }

        if (currentFilter.withAnyMetro) {
            resultList = resultList.filter { flat ->
                !flat.metroStation.isNullOrBlank()
            }
        } else {
            val selectedMetroStation = currentFilter.metroStations.filter { it.selected }
            if (selectedMetroStation.isNotEmpty()) {
                resultList = resultList.filter { flat ->
                    selectedMetroStation.any { filterMetroStation ->
                        flat.metroStation == filterMetroStation.name
                    }
                }
            }
        }

        resultList = when (currentFilter.sortOption) {
            FlatSort.NEWEST_FIRST -> resultList.sortedByDescending { it.publishedAt }
            FlatSort.CHEAPEST_FIRST -> {
                resultList.sortedBy { it.priceUsd ?: Double.MAX_VALUE }
            }

            FlatSort.MOST_EXPENSIVE_FIRST -> {
                resultList.sortedBy { it.priceUsd ?: Double.MIN_VALUE }.reversed()
            }
        }

        if (currentFilter.fromOwnerOnly == true && currentFilter.adType != AdType.DAILY) {
            resultList = resultList.filter { it.owner == currentFilter.fromOwnerOnly }
        }

        if (currentFilter.withPhotoOnly) {
            resultList = resultList.filter { it.imageUrls?.isNotEmpty() == true }
        }

        val userPolygons = currentFilter.userMapAreas
            .filter { it.isActive }
            .map { it.coordinates }
        val districtPolygons = currentFilter.districtsArea
            .filter { it.isChecked }
            .map { it.coordinates }
        val allActivePolygons = userPolygons + districtPolygons

        if (allActivePolygons.isNotEmpty()) {
            resultList = filterFlatsByPolygons(resultList, allActivePolygons)
        }

        return resultList.distinctBy { it.flatPlatform to it.adId }
    }

    private fun filterFlatsByPolygons(
        flats: List<AppFlat>,
        activePolygons: List<List<Coordinates>>,
    ): List<AppFlat> {
        if (activePolygons.isEmpty()) return flats

        val result = mutableSetOf<AppFlat>()

        for (polygon in activePolygons) {
            if (polygon.size < 3) continue

            val flatsInThisArea = flats.filter { flat ->
                flat.coordinates?.let { coord ->
                    isPointInPolygon(coord, polygon)
                } ?: false
            }

            result.addAll(flatsInThisArea)
        }

        return result.toList()
    }

    private fun isPointInPolygon(point: Coordinates, polygon: List<Coordinates>): Boolean {
        var inside = false
        var j = polygon.size - 1
        val px = point.longitude
        val py = point.latitude

        for (i in polygon.indices) {
            val xi = polygon[i].longitude
            val yi = polygon[i].latitude
            val xj = polygon[j].longitude
            val yj = polygon[j].latitude

            if ((yi > py) != (yj > py)) {
                if (px < (xj - xi) * (py - yi) / (yj - yi) + xi) {
                    inside = !inside
                }
            }
            j = i
        }

        return inside
    }

    private fun filterByPrice(
        priceInFilter: Price?,
        priceInAd: (AppFlat) -> Double?,
        resultList: List<AppFlat>,
    ): List<AppFlat> {
        var resultList1 = resultList
        if (priceInFilter != null) {
            val priceFrom = priceInFilter.priceFrom
            val priceTo = priceInFilter.priceTo

            if (priceFrom != null) {
                resultList1 = resultList1.filter { flat ->
                    val pricePerSquareInAdValue = priceInAd(flat)
                    pricePerSquareInAdValue != null && pricePerSquareInAdValue >= priceFrom
                }
            }

            if (priceTo != null) {
                resultList1 = resultList1.filter { flat ->
                    val pricePerSquareInAdValue = priceInAd(flat)
                    pricePerSquareInAdValue != null && pricePerSquareInAdValue <= priceTo
                }
            }
        }
        return resultList1
    }

    override fun getFavoritesFromLocalDb(): Flow<List<AppFlat>> {
        return flatsDao.getAllFavoritesAsFlow()
    }

    override fun saveFlatToFavorite(flatPlatform: FlatPlatform, adId: Long): Flow<AppFlat?> {
        val source = flatByIdFlow(flatPlatform, adId)
        return flow {
            var flatFromDb = source.last()
                ?: throw NoSuchElementException("Flat $adId not found")
            if (flatFromDb.savedInFavorites.not()) {
                val enriched = sourceFor(flatPlatform).detail(adId).last()
                if (enriched != null) {
                    flatFromDb = enriched
                }
            }
            val willBeFavorite = flatFromDb.savedInFavorites.not()
            val updated = flatFromDb.copy(
                savedInFavorites = willBeFavorite,
                dislike = if (willBeFavorite) false else flatFromDb.dislike,
            )
            flatsDao.upsert(updated)
            emit(flatsDao.getById(flatPlatform, adId))
        }
    }

    override fun setFlatDisliked(
        flatPlatform: FlatPlatform,
        adId: Long,
        disliked: Boolean,
    ): Flow<AppFlat?> {
        val source = flatByIdFlow(flatPlatform, adId)
        return flow {
            val flatFromDb = source.last()
                ?: throw NoSuchElementException("Flat $adId not found")
            val updated = flatFromDb.copy(
                dislike = disliked,
                savedInFavorites = if (disliked) false else flatFromDb.savedInFavorites,
            )
            flatsDao.upsert(updated)
            emit(flatsDao.getById(flatPlatform, adId))
        }
    }

    override suspend fun cleanupOldFlats() {
        val threshold = Clock.System.now()
            .minus(7, DateTimeUnit.DAY, TimeZone.UTC)
            .toEpochMilliseconds()
        withContext(Dispatchers.IO) {
            flatsDao.deleteNonFavoritesOlderThan(threshold)
        }
    }

    private fun sourceFor(platform: FlatPlatform): ListingSource =
        listingSourceRegistry.byPlatform(platform)
            ?: error("No ListingSource registered for $platform")

    private fun flatByIdFlow(platform: FlatPlatform, adId: Long): Flow<AppFlat?> {
        val fromSource = sourceFor(platform).getById(adId)
        return flow {
            val sourceFlat = fromSource.last()
            if (sourceFlat != null) {
                emit(sourceFlat)
            } else {
                emit(flatsDao.getById(platform, adId))
            }
        }
    }
}
