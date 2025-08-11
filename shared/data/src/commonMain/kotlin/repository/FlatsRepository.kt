package repository

import entities.AppFlat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface FlatsRepository {

    fun searchFlats(): Flow<List<AppFlat>>

    fun getFlatById(
        flatId: Long
    ): Flow<AppFlat>

    fun clearCashedFlats()
}