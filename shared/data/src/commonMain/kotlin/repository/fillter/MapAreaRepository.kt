package repository.fillter

import entities.MapArea
import kotlinx.coroutines.flow.Flow

interface MapAreaRepository {

    suspend fun saveArea(area: MapArea): Long
    fun getAllSavedAreas(): Flow<List<MapArea>>
    suspend fun deleteSavedArea(id: String)
    suspend fun activateMapArea(id: String)
    suspend fun deactivateMapArea(id: String)
}