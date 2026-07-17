package repository.fillter

import entities.CommonFilterRequestModel
import entities.SavedFilter
import entities.UserMapArea
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

interface FilterRepository {

    val cashedFilterFlow: SharedFlow<FilterInfo>
    val forceReloadFlow: SharedFlow<Unit>
    val lastNetworkFilter: CommonFilterRequestModel?
    var currentHomePage: Int

    /**
     * When true, high-volume sources fetch ~2 pages / larger API page size
     * so non-premium feed-delay filter still leaves enough ads on first load.
     */
    var listFetchBoostActive: Boolean

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
}

fun FilterRepository.lastFilter(): CommonFilterRequestModel =
    cashedFilterFlow.replayCache.firstOrNull()?.commonFilterRequestModel
        ?: CommonFilterRequestModel(location = entities.LocationFilter.networkDefault())


suspend fun FilterRepository.areasInFilter(userMapAreaRepository: UserMapAreaRepository): List<UserMapArea> {
    val filter = this.lastFilter()
    val mapAreas = userMapAreaRepository.getAllSavedAreas().first()

    val mapAreasInFilter = mapAreas.map { mapArea ->
        val filterArea = filter.userMapAreas.find { it.pathId == mapArea.pathId }
        if (filterArea != null) {
            mapArea.copy(isActive = filterArea.isActive)
        } else {
            mapArea.copy(isActive = false)
        }
    }

    return mapAreasInFilter
}
