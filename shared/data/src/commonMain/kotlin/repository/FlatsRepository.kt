package repository

import core.NetworkResponseWrapper
import database.FlatsDao
import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

interface FlatsRepository {

    fun searchFlats(filter: CommonFilterRequestModel, currentPage: Int?): Flow<NetworkResponseWrapper<List<AppFlat>>>

    fun getFlatById(
        flatId: Long
    ): Flow<AppFlat>

    fun getFlatByIdWithDetails(
        flatId: Long
    ): Flow<AppFlat?>

    fun clearCashedFlats()
}

internal suspend fun FlowCollector<AppFlat>.getFlatByIdFromDb(
    flatId: Long,
    flatsDao: FlatsDao,
    platform: FlatPlatform,
): AppFlat {
    val flatFromList = flatsDao.getById(platform, flatId)
        ?: throw NoSuchElementException("Flat $platform/$flatId not found")
    emit(flatFromList)
    return flatFromList
}