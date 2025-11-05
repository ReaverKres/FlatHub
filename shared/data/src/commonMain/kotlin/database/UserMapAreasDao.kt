package database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import entities.UserMapArea
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMapAreasDao {
    @Insert
    suspend fun saveArea(area: UserMapArea): Long

    @Query("SELECT * FROM map_areas ORDER BY createdAt DESC")
    fun getAllSavedAreas(): Flow<List<UserMapArea>>

    @Query("SELECT * FROM map_areas WHERE pathId = :pathId")
    suspend fun getSavedAreaById(pathId: String): UserMapArea?

    @Delete
    suspend fun deleteSavedArea(area: UserMapArea)

    @Query("UPDATE map_areas SET isActive = 1 WHERE pathId = :pathId")
    suspend fun selectArea(pathId: String)

    @Query("UPDATE map_areas SET isActive = 0 WHERE pathId = :pathId")
    suspend fun deselectArea(pathId: String)
}