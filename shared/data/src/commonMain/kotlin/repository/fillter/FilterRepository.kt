package repository.fillter

import entities.CommonFilterRequestModel
import entities.MapArea
import entities.SavedFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

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
}

fun FilterRepository.lastFilter(): CommonFilterRequestModel =
    cashedFilterFlow.replayCache.firstOrNull()?.commonFilterRequestModel
        ?: CommonFilterRequestModel()

suspend fun FilterRepository.areasInFilter(mapAreaRepository: MapAreaRepository): List<MapArea> {
    val filter = this.lastFilter()
    val mapAreas = mapAreaRepository.getAllSavedAreas().first()

    val mapAreasInFilter = mapAreas.map { mapArea ->
        val filterArea = filter.mapAreas.find { it.pathId == mapArea.pathId }
        if (filterArea != null) {
            mapArea.copy(isActive = filterArea.isActive)
        } else {
            mapArea.copy(isActive = false)
        }
    }

    return mapAreasInFilter
}
