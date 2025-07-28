package repository.fillter

import entities.CommonFilterRequestModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class FilterRepositoryImpl: FilterRepository {

    private val _cashedFilterFlow = MutableSharedFlow<CommonFilterRequestModel>(
        replay = 1
    )

    override val cashedFilterFlow: MutableSharedFlow<CommonFilterRequestModel> = _cashedFilterFlow

    override suspend fun updateFilter(commonFilterRequestModel: CommonFilterRequestModel) {
        _cashedFilterFlow.emit(commonFilterRequestModel)
    }
}