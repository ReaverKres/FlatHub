package database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import entities.SavedFilter
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedFiltersDao {
    @Insert
    suspend fun saveFilter(filter: SavedFilter): Long

    @Query("SELECT * FROM saved_filters ORDER BY createdAt DESC")
    fun getAllSavedFilters(): Flow<List<SavedFilter>>

    @Delete
    suspend fun deleteSavedFilter(filter: SavedFilter)

    @Query("SELECT * FROM saved_filters WHERE id = :id")
    suspend fun getSavedFilterById(id: Long): SavedFilter?

    @Query("UPDATE saved_filters SET selected = 0")
    suspend fun deselectAllFilters()

    @Query("UPDATE saved_filters SET selected = 1 WHERE id = :id")
    suspend fun selectFilter(id: Long)

    @Query("SELECT * FROM saved_filters WHERE selected = 1 LIMIT 1")
    suspend fun getSelectedFilter(): SavedFilter?
}
