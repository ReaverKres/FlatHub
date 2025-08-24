package repository.fillter

import entities.CommonFilterRequestModel
import entities.SavedFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface FilterRepository {

    val cashedFilterFlow: SharedFlow<FilterInfo>
    val lastNetworkFilter: CommonFilterRequestModel?
    var currentAppPage: Int

    suspend fun updateFilter(
        commonFilterRequestModel: CommonFilterRequestModel,
        doNetworkCall: Boolean
    )

    // Saved filters methods
    suspend fun saveFilter(name: String, filter: CommonFilterRequestModel): Long
    fun getAllSavedFilters(): Flow<List<SavedFilter>>
    suspend fun deleteSavedFilter(id: Long)
    suspend fun applySavedFilter(filter: SavedFilter, doNetworkCall: Boolean)
    suspend fun getSavedFilterById(id: Long): SavedFilter?
    suspend fun updateSavedFilterSelection(selectedId: Long)
    suspend fun getSelectedSavedFilter(): SavedFilter?
    suspend fun clearAllSavedFilterSelections()

    // Notification filters methods
    suspend fun saveNotificationFilter(name: String, filter: CommonFilterRequestModel, interval: Int): Long
    suspend fun getNotificationFilter(): SavedFilter?
    suspend fun deleteNotificationFilter()
    suspend fun hasNotificationFilter(): Boolean
}

fun FilterRepository.lastFilter(): CommonFilterRequestModel =
    cashedFilterFlow.replayCache.firstOrNull()?.commonFilterRequestModel ?: CommonFilterRequestModel()