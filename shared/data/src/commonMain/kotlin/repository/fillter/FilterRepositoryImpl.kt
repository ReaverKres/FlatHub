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

    override suspend fun saveNotificationFilter(
        name: String,
        filter: CommonFilterRequestModel,
        interval: Int
    ): Long {
        // Clear any existing notification filter first
        deleteNotificationFilter()
        
        val notificationFilter = SavedFilter(
            name = name,
            filterData = filter,
            isNotification = true,
            notificationInterval = interval,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        return savedFiltersDao.saveFilter(notificationFilter)
    }

    override suspend fun getNotificationFilter(): SavedFilter? {
        return savedFiltersDao.getNotificationFilter()
    }

    override suspend fun deleteNotificationFilter() {
        val notificationFilter = savedFiltersDao.getNotificationFilter()
        notificationFilter?.let { filter ->
            savedFiltersDao.deleteSavedFilter(filter)
        }
        savedFiltersDao.clearNotificationFilter()
    }

    override suspend fun hasNotificationFilter(): Boolean {
        return savedFiltersDao.hasNotificationFilter()
    }
}

class FilterInfo(
    val commonFilterRequestModel: CommonFilterRequestModel,
    val doNetworkCall: Boolean
)