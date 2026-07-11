package repository.mergedrepo

import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow

interface MergedRepository {

    val lastEmittedFlats: Flow<List<AppFlat>>

    fun searchFlats(): Flow<MergedFlatResponse>

    fun searchFlats(filter: CommonFilterRequestModel, currentPage: Int): Flow<MergedFlatResponse>

    suspend fun getFlatByIdWithDetails(
        flatPlatform: FlatPlatform,
        flatId: Long,
        markAsViewed: Boolean = true,
    ): Flow<AppFlat>

    fun clearCashedFlats()

    fun getAllFlatsFromLocalDb(): Flow<List<AppFlat>>

    fun getFavoritesFromLocalDb(): Flow<List<AppFlat>>

    fun saveFlatToFavorite(flatPlatform: FlatPlatform, adId: Long): Flow<AppFlat?>

    fun setFlatDisliked(
        flatPlatform: FlatPlatform,
        adId: Long,
        disliked: Boolean,
    ): Flow<AppFlat?>

    /** Deletes non-favorite flats published more than ~30 days ago. */
    suspend fun cleanupOldFlats()
}