package repository.mergedrepo

import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.zip
import repository.fillter.FilterRepository
import repository.fillter.lastFilter
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository
import repository.realt.RealtRepository
import repository.domovita.DomovitaRepository

class MergedRepositoryImpl(
    private val kufarRepository: KufarRepository,
    private val onlinerRepository: OnlinerRepository,
    private val realtRepository: RealtRepository,
    private val domovitaRepository: DomovitaRepository,
    private val filterRepository: FilterRepository,
    private val flatsDao: FlatsDao,
) : MergedRepository {

    override val lastEmittedFlats: MutableSharedFlow<List<AppFlat>> = MutableSharedFlow(replay = 1)

    override fun searchFlats(): Flow<List<AppFlat>> {
        val loadedFromNetworkFlats = kufarRepository.searchFlats()
            .zip(onlinerRepository.searchFlats()) { kufarList, onlinerList -> kufarList + onlinerList }
            .zip(realtRepository.searchFlats()) { kOn, r -> kOn + r }
            .zip(domovitaRepository.searchFlats()) { kor, d -> kor + d }
            .mapLatest { networkFlats ->
                val merged = networkFlats.map { net ->
                    val fromDb = flatsDao.getById(net.adId)
                    // Calculate price per square meter
                    val priceUsdSquare = if (net.priceUsd != null && net.totalArea != null && net.totalArea > 0) {
                        net.priceUsd / net.totalArea
                    } else {
                        null
                    }
                    val priceBynSquare = if (net.priceByn != null && net.totalArea != null && net.totalArea > 0) {
                        net.priceByn / net.totalArea
                    } else {
                        null
                    }
                    net.copy(
                        flatSavedInFavorites = fromDb?.flatSavedInFavorites == true,
                        priceUsdSquare = priceUsdSquare,
                        priceBynSquare = priceBynSquare
                    )
                }
                flatsDao.upsertAll(merged)
                val sortedFlatList = applyLocalSortOrFilters(merged)
                lastEmittedFlats.emit(sortedFlatList)
                sortedFlatList
            }
        return loadedFromNetworkFlats
    }

    override fun getFlatById(flatPlatform: FlatPlatform, flatId: Long): Flow<AppFlat> {
        return when (flatPlatform) {
            FlatPlatform.KUFAR -> kufarRepository.getFlatById(flatId)
            FlatPlatform.ONLINER -> onlinerRepository.getFlatById(flatId)
            FlatPlatform.REALT -> realtRepository.getFlatById(flatId)
            FlatPlatform.DOMOVITA -> domovitaRepository.getFlatById(flatId)
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
        var resultList = flats.sortedByDescending { it.publishedAt }

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
                flatSavedInFavorites = flatFromDb?.flatSavedInFavorites?.not() ?: false
            )
            flatsDao.upsert(updated)
            emit(flatsDao.getById(adId))
        }
    }
}