package repository

import entities.AppFlat
import kotlinx.coroutines.flow.Flow
import server_request.KufarSearchParams

interface KufarRepository {
    fun searchFlats(
        kufarSearchParams: KufarSearchParams
    ): Flow<List<AppFlat>>
}