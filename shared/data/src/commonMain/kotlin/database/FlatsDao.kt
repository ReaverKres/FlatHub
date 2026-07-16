package database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow

@Dao
interface FlatsDao {
    @Upsert
    suspend fun upsert(item: AppFlat)

    @Upsert
    suspend fun upsertAll(items: List<AppFlat>)

    @Query("SELECT * FROM AppFlat")
    fun getAllAsFlow(): Flow<List<AppFlat>>

    @Query("SELECT * FROM AppFlat WHERE savedInFavorites = 1")
    fun getAllFavoritesAsFlow(): Flow<List<AppFlat>>

    @Query("SELECT * FROM AppFlat WHERE flatPlatform = :platform AND adId = :adId")
    suspend fun getById(platform: FlatPlatform, adId: Long): AppFlat?

    @Query("DELETE FROM AppFlat WHERE flatPlatform = :platform AND adId = :adId")
    suspend fun deleteById(platform: FlatPlatform, adId: Long)

    @Query("DELETE FROM AppFlat")
    suspend fun clearAll()

    /** Drops non-favorite flats whose publication time is older than [thresholdEpochMs]. */
    @Query(
        """
        DELETE FROM AppFlat
        WHERE savedInFavorites = 0
          AND publishedAt IS NOT NULL
          AND publishedAt < :thresholdEpochMs
        """
    )
    suspend fun deleteNonFavoritesOlderThan(thresholdEpochMs: Long)
}
