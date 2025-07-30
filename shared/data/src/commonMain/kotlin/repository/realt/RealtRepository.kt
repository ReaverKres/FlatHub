package repository.realt

import AppFlat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import server_request.KufarSearchParams

interface RealtRepository {

    val cashedFlatsFlow: SharedFlow<List<AppFlat>>

    fun searchFlats(): Flow<List<AppFlat>>

    fun getFlatById(
        flatId: Long
    ): Flow<AppFlat>

    fun clearCashedFlats()
}