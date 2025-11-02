package database

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import entities.MapArea
import kotlinx.coroutines.flow.Flow

interface MapAreasDao {
    @Insert
    suspend fun saveArea(area: MapArea): Long

    @Query("SELECT * FROM map_areas ORDER BY createdAt DESC")
    fun getAllSavedAreas(): Flow<List<MapArea>>

    @Query("SELECT * FROM map_areas WHERE pathId = :pathId")
    suspend fun getSavedAreaById(pathId: String): MapArea?

    @Delete
    suspend fun deleteSavedArea(area: MapArea)

    @Query("UPDATE map_areas SET isActive = 1 WHERE pathId = :pathId")
    suspend fun selectArea(pathId: String)

    @Query("UPDATE map_areas SET isActive = 0 WHERE pathId = :pathId")
    suspend fun deselectArea(pathId: String)
}