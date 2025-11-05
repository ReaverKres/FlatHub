package repository.fillter

import entities.UserMapArea
import kotlinx.coroutines.flow.Flow

interface UserMapAreaRepository {

    suspend fun saveArea(area: UserMapArea): Long
    fun getAllSavedAreas(): Flow<List<UserMapArea>>
    suspend fun deleteSavedArea(id: String)
}