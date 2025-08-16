package repository.fillter

import entities.CommonFilterRequestModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class FilterRepositoryImpl: FilterRepository {

    private val _cashedFilterFlow = MutableSharedFlow<FilterInfo>(
        replay = 1
    )
    override val cashedFilterFlow: SharedFlow<FilterInfo> = _cashedFilterFlow
    override var lastNetworkFilter: CommonFilterRequestModel? = null


    override var currentAppPage: Int = 1

    override suspend fun updateFilter(
        commonFilterRequestModel: CommonFilterRequestModel,
        doNetworkCall: Boolean
    ) {
        if (doNetworkCall) {
            lastNetworkFilter = commonFilterRequestModel
        }
        _cashedFilterFlow.emit(FilterInfo(commonFilterRequestModel, doNetworkCall))
    }
}

class FilterInfo(
    val commonFilterRequestModel: CommonFilterRequestModel,
    val doNetworkCall: Boolean
)