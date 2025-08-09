package repository.mergedrepo

import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.coroutines.flow.Flow

interface MergedRepository {

    fun searchFlats(): Flow<List<AppFlat>>

    fun getFlatById(
        flatPlatform: FlatPlatform,
        flatId: Long
    ): Flow<AppFlat>

    fun clearCashedFlats()

    fun getAllFlatsFromLocalDb(): Flow<List<AppFlat>>
}