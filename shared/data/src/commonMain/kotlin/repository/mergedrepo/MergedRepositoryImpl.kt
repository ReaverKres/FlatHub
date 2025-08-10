package repository.mergedrepo

import database.FlatsDao
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.zip
import repository.kufar.KufarRepository
import repository.onliner.OnlinerRepository
import repository.realt.RealtRepository

class MergedRepositoryImpl(
    private val kufarRepository: KufarRepository,
    private val onlinerRepository: OnlinerRepository,
    private val realtRepository: RealtRepository,
    private val flatsDao: FlatsDao,
) : MergedRepository {

    override fun searchFlats(): Flow<List<AppFlat>> {
        val loadedFromNetworkFlats = kufarRepository.searchFlats()
            .zip(onlinerRepository.searchFlats()) { kufarList, onlinerList -> kufarList + onlinerList }
            .zip(realtRepository.searchFlats()) { kOn, r -> kOn + r }
            .mapLatest { networkFlats ->
                val merged = networkFlats.map { net ->
                    val fromDb = flatsDao.getById(net.adId)
                    net.copy(flatSavedInFavorites = fromDb?.flatSavedInFavorites == true)
                }
                flatsDao.upsertAll(merged)
                merged
            }

        val favoritesFlow = flatsDao.getAllFavoritesAsFlow()

        return combine(loadedFromNetworkFlats, favoritesFlow) { list, favs ->
            val favIds = favs.map { it.adId }.toHashSet()
            val merged = list.map { it.copy(flatSavedInFavorites = it.adId in favIds) }
            merged
        }
    }

    override fun getFlatById(flatPlatform: FlatPlatform, flatId: Long): Flow<AppFlat> {
        return when (flatPlatform) {
            FlatPlatform.KUFAR -> kufarRepository.getFlatById(flatId)
            FlatPlatform.ONLINER -> onlinerRepository.getFlatById(flatId)
            FlatPlatform.REALT -> realtRepository.getFlatById(flatId)
        }
    }

    override fun clearCashedFlats() {
        kufarRepository.clearCashedFlats()
        onlinerRepository.clearCashedFlats()
        realtRepository.clearCashedFlats()
    }

    override fun getAllFlatsFromLocalDb(): Flow<List<AppFlat>> {
        return flatsDao.getAllAsFlow()
    }

    override fun getFavoritesFromLocalDb(): Flow<List<AppFlat>> {
        return flatsDao.getAllFavoritesAsFlow()
    }

    override fun saveFlatToFavorite(flatPlatform: FlatPlatform, adId: Long): Flow<AppFlat?> {
        val source: Flow<AppFlat> = when (flatPlatform) {
            FlatPlatform.KUFAR -> kufarRepository.getFlatById(adId)
            FlatPlatform.ONLINER -> onlinerRepository.getFlatById(adId)
            FlatPlatform.REALT -> realtRepository.getFlatById(adId)
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