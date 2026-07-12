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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import repository.domovita.DomovitaRepository
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository
import repository.realt.RealtRepository

class MergedRepositoryImpl(
    private val kufarRepository: KufarRepository,
    private val onlinerRepository: OnlinerRepository,
    private val realtRepository: RealtRepository,
    private val domovitaRepository: DomovitaRepository,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao,
) : MergedRepository {

    override val lastEmittedFlats: MutableSharedFlow<List<AppFlat>> = MutableSharedFlow(replay = 1)

    override fun searchFlats(): Flow<MergedFlatResponse> {
        val filter = filterRepository.lastFilter()
        return searchByFilter(filter, null)
    }

    override fun searchFlats(filter: CommonFilterRequestModel, currentPage: Int): Flow<MergedFlatResponse> {
        return searchByFilter(filter, currentPage)
    }

    private fun searchByFilter(filter: CommonFilterRequestModel, currentPage: Int?): Flow<MergedFlatResponse> {
        val finalFlow = when {
            filter.isRoomForRent -> {
                kufarRepository.searchFlats(filter, currentPage)
                    .zip(onlinerRepository.searchFlats(
                        filter,
                        currentPage
                    )) { kufarList, onlinerList ->
                        listOf(kufarList, onlinerList)
                    }
            }

            filter.isCommercial || filter.adType == AdType.DAILY -> {
                kufarRepository.searchFlats(filter, currentPage)
                    .zip(realtRepository.searchFlats(filter, currentPage)) { k, r -> listOf(k, r) }
            }

            else -> {
                kufarRepository.searchFlats(filter, currentPage)
                    .zip(onlinerRepository.searchFlats(filter, currentPage))
                    { kufarList, onlinerList -> listOf(kufarList, onlinerList) }
                    .zip(domovitaRepository.searchFlats(filter, currentPage)) { kor, d -> kor + d }
                    .zip(realtRepository.searchFlats(filter, currentPage)) { kOn, r -> kOn + r }
            }
        }
        return finalFlow.mapLatest { networkFlats ->
            val appFlats: MutableList<AppFlat> = mutableListOf()
            val errors: MutableList<NetworkErrorInfo> = mutableListOf()

            networkFlats.forEach { nett ->
                when (nett) {
                    is NetworkResponseWrapper.Success<List<AppFlat>> -> {
                        nett.data.forEach { net ->
                            val fromDb = flatsDao.getById(net.adId)

                            val priceUsdSquare =
                                if (net.priceUsd != null && net.totalArea != null && net.totalArea > 0) {
                                    net.priceUsd / net.totalArea
                                } else net.priceUsdSquare

                            val priceBynSquare =
                                if (net.priceByn != null && net.totalArea != null && net.totalArea > 0) {
                                    net.priceByn / net.totalArea
                                } else net.priceBynSquare

                            val updated = net.copy(
                                adType = filter.adType,
                                savedInFavorites = fromDb?.savedInFavorites == true,
                                isViewed = fromDb?.isViewed == true,
                                dislike = fromDb?.dislike == true,
                                priceUsdSquare = priceUsdSquare,
                                priceBynSquare = priceBynSquare
                            )

                            appFlats.add(updated)
                        }
                    }

                    is NetworkResponseWrapper.Error -> {
                        nett.error?.let { err ->
                            errors.add(err)
                        }
                    }
                }
            }

            flatsDao.upsertAll(appFlats)

            val sortedFlatList = applyLocalSortOrFilters(appFlats, filter)
            lastEmittedFlats.emit(sortedFlatList)

            MergedFlatResponse(
                flats = sortedFlatList,
                errors = errors
            )
        }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getFlatByIdWithDetails(
        flatPlatform: FlatPlatform,
        flatId: Long,
        markAsViewed: Boolean,
    ): Flow<AppFlat> {
        val detailFlat = when (flatPlatform) {
            FlatPlatform.KUFAR -> kufarRepository.getFlatByIdWithDetails(flatId)
            FlatPlatform.ONLINER -> onlinerRepository.getFlatByIdWithDetails(flatId)
            FlatPlatform.REALT -> realtRepository.getFlatByIdWithDetails(flatId)
            FlatPlatform.DOMOVITA -> domovitaRepository.getFlatByIdWithDetails(flatId)
        }.flowOn(Dispatchers.IO)
        return detailFlat.mapNotNull { flat ->
            flat?.let {
                if (markAsViewed) it.copy(isViewed = true) else it
            }
        }.onEach { updatedFlat ->
            withContext(Dispatchers.IO) { flatsDao.upsert(updatedFlat) }
        }.catch {
            println("detailFlat LoadError")
        }
    }

    override fun clearCashedFlats() {
        kufarRepository.clearCashedFlats()
        onlinerRepository.clearCashedFlats()
        realtRepository.clearCashedFlats()
        domovitaRepository.clearCashedFlats()
    }

    override fun getAllFlatsFromLocalDb(): Flow<List<AppFlat>> {
        return flatsDao.getAllAsFlow().map { flats ->
            flats.sortedByDescending { it.publishedAt }
        }
    }

    private fun applyLocalSortOrFilters(
        flats: List<AppFlat>,
        currentFilter: CommonFilterRequestModel = filterRepository.lastFilter()
    ): List<AppFlat> {
        var resultList = flats

        // Filter by full price
        val priceIsAdInUsd = currentFilter.adType != AdType.DAILY
        resultList = filterByPrice(
            priceInFilter = currentFilter.priceFull,
            priceInAd = { if(priceIsAdInUsd) it.priceUsd else it.priceByn },
            resultList = resultList
        )

        // Filter by price per square meter
        if(currentFilter.adType != AdType.DAILY) {
            resultList = filterByPrice(
                priceInFilter = currentFilter.pricePerSquare,
                priceInAd = { it.priceUsdSquare },
                resultList = resultList
            )
        }

        //total area
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

        val selectedMetroStation = currentFilter.metroStations.filter { it.selected }
        if (selectedMetroStation.isNotEmpty()) {
            resultList = resultList.filter { flat ->
                selectedMetroStation.any { filterMetroStation ->
                    flat.metroStation == filterMetroStation.name
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

        return resultList.distinctBy { it.adId }
    }

    private fun filterFlatsByPolygons(
        flats: List<AppFlat>,
        activePolygons: List<List<Coordinates>>
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
        resultList: List<AppFlat>
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
        val source: Flow<AppFlat> = when (flatPlatform) {
            FlatPlatform.KUFAR -> kufarRepository.getFlatById(adId)
            FlatPlatform.ONLINER -> onlinerRepository.getFlatById(adId)
            FlatPlatform.REALT -> realtRepository.getFlatById(adId)
            FlatPlatform.DOMOVITA -> domovitaRepository.getFlatById(adId)
        }
        return flow {
            var flatFromDb = source.last()
            if (flatFromDb.savedInFavorites.not()) {
                val flatFromDbWithDetailFlow = when (flatPlatform) {
                    FlatPlatform.KUFAR -> kufarRepository.getFlatByIdWithDetails(adId)
                    FlatPlatform.ONLINER -> onlinerRepository.getFlatByIdWithDetails(adId)
                    FlatPlatform.REALT -> realtRepository.getFlatByIdWithDetails(adId)
                    FlatPlatform.DOMOVITA -> domovitaRepository.getFlatByIdWithDetails(adId)
                }
                val flatFromDbWithDetail = flatFromDbWithDetailFlow.last()
                if (flatFromDbWithDetail != null) {
                    flatFromDb = flatFromDbWithDetail
                }
            }
            val willBeFavorite = flatFromDb.savedInFavorites.not()
            val updated = flatFromDb.copy(
                savedInFavorites = willBeFavorite,
                dislike = if (willBeFavorite) false else flatFromDb.dislike,
            )
            flatsDao.upsert(updated)
            emit(flatsDao.getById(adId))
        }
    }

    override fun setFlatDisliked(
        flatPlatform: FlatPlatform,
        adId: Long,
        disliked: Boolean,
    ): Flow<AppFlat?> {
        val source: Flow<AppFlat> = when (flatPlatform) {
            FlatPlatform.KUFAR -> kufarRepository.getFlatById(adId)
            FlatPlatform.ONLINER -> onlinerRepository.getFlatById(adId)
            FlatPlatform.REALT -> realtRepository.getFlatById(adId)
            FlatPlatform.DOMOVITA -> domovitaRepository.getFlatById(adId)
        }
        return flow {
            val flatFromDb = source.last()
            val updated = flatFromDb.copy(
                dislike = disliked,
                savedInFavorites = if (disliked) false else flatFromDb.savedInFavorites,
            )
            flatsDao.upsert(updated)
            emit(flatsDao.getById(adId))
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
}