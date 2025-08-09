package database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import entities.AppFlat
import kotlinx.coroutines.flow.Flow

@Dao
interface FlatsDao {
    @Upsert
    suspend fun upsert(item: AppFlat)

    @Upsert
    suspend fun upsertAll(items: List<AppFlat>)

    @Query("SELECT * FROM AppFlat")
    fun getAllAsFlow(): Flow<List<AppFlat>>

    @Query("SELECT * FROM AppFlat WHERE adId = :id")
    suspend fun getById(id: Long): AppFlat?

    @Query("DELETE FROM AppFlat WHERE adId = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM AppFlat")
    suspend fun clearAll()
}