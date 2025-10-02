package repository

import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface FlatsRepository {

    fun searchFlats(): Flow<NetworkResponseWrapper<List<AppFlat>>>

    fun getFlatById(
        flatId: Long
    ): Flow<AppFlat>

    fun getFlatByIdWithDetails(
        flatId: Long
    ): Flow<AppFlat?>

    fun clearCashedFlats()
}

internal suspend fun FlowCollector<AppFlat>.getFlatByIdFromDb(flatId: Long, flatsDao: FlatsDao): AppFlat {
    val flatFromList = flatsDao.getAllAsFlow()
        .map { flats ->
            flats.find { it.adId == flatId }
                ?: throw NoSuchElementException("Flat with id $flatId not found")
        }.first()
    emit(flatFromList)
    return flatFromList
}