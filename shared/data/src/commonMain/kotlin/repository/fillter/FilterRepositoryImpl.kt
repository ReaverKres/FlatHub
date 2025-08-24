package repository.fillter

import database.SavedFiltersDao
import entities.CommonFilterRequestModel
import entities.SavedFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FilterRepositoryImpl(
    private val savedFiltersDao: SavedFiltersDao
) : FilterRepository {

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
    val commonFilterRequestModel: CommonFilterRequestModel,
    val doNetworkCall: Boolean
)