package repository

import AppFlat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import server_request.KufarSearchParams

interface KufarRepository {

    val cashedFlatsFlow: SharedFlow<List<AppFlat>>

    fun searchFlats(
        kufarSearchParams: KufarSearchParams
    ): Flow<List<AppFlat>>

    fun getFlatById(
        flatId: Long
    ): Flow<AppFlat>
}