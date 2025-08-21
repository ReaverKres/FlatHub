package repository.fillter

import entities.CommonFilterRequestModel
import kotlinx.coroutines.flow.SharedFlow

interface FilterRepository {

    val cashedFilterFlow: SharedFlow<FilterInfo>
    val lastNetworkFilter: CommonFilterRequestModel?
    var currentAppPage: Int

    suspend fun updateFilter(
        commonFilterRequestModel: CommonFilterRequestModel,
        doNetworkCall: Boolean
    )
}

fun FilterRepository.lastFilter(): CommonFilterRequestModel =
    cashedFilterFlow.replayCache.firstOrNull()?.commonFilterRequestModel ?: CommonFilterRequestModel()