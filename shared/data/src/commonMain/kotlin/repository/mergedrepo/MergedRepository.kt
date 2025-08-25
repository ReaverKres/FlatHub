package repository.mergedrepo

import entities.AppFlat
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow

interface MergedRepository {

    val lastEmittedFlats: Flow<List<AppFlat>>

    fun searchFlats(): Flow<List<AppFlat>>

    fun getFlatById(
        flatPlatform: FlatPlatform,
        flatId: Long
    ): Flow<AppFlat>

    fun clearCashedFlats()

    fun getAllFlatsFromLocalDb(): Flow<List<AppFlat>>

    fun getFavoritesFromLocalDb(): Flow<List<AppFlat>>

    fun saveFlatToFavorite(flatPlatform: FlatPlatform, adId: Long): Flow<AppFlat?>

    suspend fun getFlatsIds(filter: CommonFilterRequestModel): List<Long>
    suspend fun fetchAndSaveFlats(filter: CommonFilterRequestModel)
}