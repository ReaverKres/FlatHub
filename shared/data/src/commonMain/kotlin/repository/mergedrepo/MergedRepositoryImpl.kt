package repository.mergedrepo

import core.NetworkErrorInfo
import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.FlatSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.withContext
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
        return kufarRepository.searchFlats()
            .zip(onlinerRepository.searchFlats()) { kufarList, onlinerList ->
                listOf(kufarList, onlinerList)
            }
            .zip(realtRepository.searchFlats()) { kOn, r -> kOn + r }
            .zip(domovitaRepository.searchFlats()) { kor, d -> kor + d }
            .mapLatest { networkFlats ->
                val appFlats: MutableList<AppFlat> = mutableListOf()
                val errors: MutableList<NetworkErrorInfo> = mutableListOf()

                networkFlats.forEach { nett ->
                    when (nett) {
                        is NetworkResponseWrapper.Success<List<AppFlat>> -> {
                            nett.data.forEach { net ->
                                val fromDb = flatsDao.getById(net.adId)

                                val priceUsdSquare = if (net.priceUsd != null && net.totalArea != null && net.totalArea > 0) {
                                    net.priceUsd / net.totalArea
                                } else null

                                val priceBynSquare = if (net.priceByn != null && net.totalArea != null && net.totalArea > 0) {
                                    net.priceByn / net.totalArea
                                } else null

                                val updated = net.copy(
                                    savedInFavorites = fromDb?.savedInFavorites == true,
                                    isViewed = fromDb?.isViewed == true,
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

                val sortedFlatList = applyLocalSortOrFilters(appFlats)
                lastEmittedFlats.emit(sortedFlatList)

                MergedFlatResponse(
                    flats = sortedFlatList,
                    errors = errors
                )
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getFlatById(flatPlatform: FlatPlatform, flatId: Long): Flow<AppFlat> {
        val detailFlat = when (flatPlatform) {
            FlatPlatform.KUFAR -> kufarRepository.getFlatById(flatId)
            FlatPlatform.ONLINER -> onlinerRepository.getFlatById(flatId)
            FlatPlatform.REALT -> realtRepository.getFlatById(flatId)
            FlatPlatform.DOMOVITA -> domovitaRepository.getFlatById(flatId)
        }.flowOn(Dispatchers.IO)
        return detailFlat.map {
            it.copy(isViewed = true)
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

    private fun applyLocalSortOrFilters(flats: List<AppFlat>): List<AppFlat> {
        val currentFilter = filterRepository.lastFilter()
        var resultList = flats

        // Filter by full price
        if (currentFilter.priceFull != null) {
            val priceFrom = currentFilter.priceFull.priceFrom
            val priceTo = currentFilter.priceFull.priceTo

            if (priceFrom != null) {
                resultList = resultList.filter { flat ->
                    flat.priceUsd != null && flat.priceUsd >= priceFrom
                }
            }

            if (priceTo != null) {
                resultList = resultList.filter { flat ->
                    flat.priceUsd != null && flat.priceUsd <= priceTo
                }
            }
        }

        // Filter by price per square meter
        if (currentFilter.pricePerSquare != null) {
            val priceFrom = currentFilter.pricePerSquare.priceFrom
            val priceTo = currentFilter.pricePerSquare.priceTo

            if (priceFrom != null) {
                resultList = resultList.filter { flat ->
                    flat.priceUsdSquare != null && flat.priceUsdSquare >= priceFrom
                }
            }

            if (priceTo != null) {
                resultList = resultList.filter { flat ->
                    flat.priceUsdSquare != null && flat.priceUsdSquare <= priceTo
                }
            }
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

        if (currentFilter.fromOwnerOnly == true) {
            resultList = resultList.filter { it.owner == currentFilter.fromOwnerOnly }
        }

        if (currentFilter.withPhotoOnly) {
            resultList = resultList.filter { it.imageUrls?.isNotEmpty() == true}
        }

        return resultList
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
            val finalFlat = source.last()
            val flatFromDb = flatsDao.getById(adId)
            val updated = finalFlat.copy(
                savedInFavorites = flatFromDb?.savedInFavorites?.not() ?: false
            )
            flatsDao.upsert(updated)
            emit(flatsDao.getById(adId))
        }
    }
}