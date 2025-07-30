package repository.fillter

import AppFlat
import entities.CommonFilterRequestModel
import kotlinx.coroutines.flow.SharedFlow

interface FilterRepository {

    val cashedFilterFlow: SharedFlow<CommonFilterRequestModel>
    var currentAppPage: Int

    suspend fun updateFilter(
        commonFilterRequestModel: CommonFilterRequestModel
    )
}