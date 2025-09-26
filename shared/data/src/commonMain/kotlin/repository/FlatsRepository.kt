package repository

import core.NetworkResponseWrapper
import entities.AppFlat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface FlatsRepository {

    fun searchFlats(): Flow<NetworkResponseWrapper<List<AppFlat>>>

    fun getFlatById(
        flatId: Long
    ): Flow<AppFlat>

    fun clearCashedFlats()
}