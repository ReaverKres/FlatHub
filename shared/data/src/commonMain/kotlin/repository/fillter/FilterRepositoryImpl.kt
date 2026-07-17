package repository.fillter

import database.SavedFiltersDao
import entities.CommonFilterRequestModel
import entities.SavedFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.Clock

class FilterRepositoryImpl(
    private val savedFiltersDao: SavedFiltersDao
) : FilterRepository {

    private val _cashedFilterFlow = MutableSharedFlow<FilterInfo>(
        replay = 1
    )
    override val cashedFilterFlow: SharedFlow<FilterInfo> = _cashedFilterFlow
    private val _forceReloadFlow = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1
    )
    override val forceReloadFlow: SharedFlow<Unit> = _forceReloadFlow
    override var lastNetworkFilter: CommonFilterRequestModel? = null

    override var currentHomePage: Int = 1
    override var listFetchBoostActive: Boolean = false

    override suspend fun updateFilter(
        commonFilterRequestModel: CommonFilterRequestModel,
        doNetworkCall: Boolean
    ) {
        if (doNetworkCall) {
            lastNetworkFilter = commonFilterRequestModel
            _forceReloadFlow.tryEmit(Unit)
        }
        _cashedFilterFlow.emit(FilterInfo(commonFilterRequestModel))
    }

    override suspend fun saveFilter(name: String, filter: CommonFilterRequestModel): Long {
        val savedFilter = SavedFilter(
            name = name,
            filterData = filter,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        return savedFiltersDao.saveFilter(savedFilter)
    }

    override fun getAllSavedFilters(): Flow<List<SavedFilter>> {
        return savedFiltersDao.getAllSavedFilters()
    }

    override suspend fun deleteSavedFilter(id: Long) {
        val filter = savedFiltersDao.getSavedFilterById(id)
        filter?.let { savedFiltersDao.deleteSavedFilter(it) }
    }

    override suspend fun applySavedFilter(filter: SavedFilter, doNetworkCall: Boolean) {
        updateSavedFilterSelection(filter.id)
        updateFilter(filter.filterData, doNetworkCall)
    }

    override suspend fun getSavedFilterById(id: Long): SavedFilter? {
        return savedFiltersDao.getSavedFilterById(id)
    }

    override suspend fun updateSavedFilterSelection(selectedId: Long) {
        savedFiltersDao.deselectAllFilters()
        savedFiltersDao.selectFilter(selectedId)
    }

    override suspend fun getSelectedSavedFilter(): SavedFilter? {
        return savedFiltersDao.getSelectedFilter()
    }

    override suspend fun clearAllSavedFilterSelections() {
        savedFiltersDao.deselectAllFilters()
    }
}

class FilterInfo(
    val commonFilterRequestModel: CommonFilterRequestModel
)