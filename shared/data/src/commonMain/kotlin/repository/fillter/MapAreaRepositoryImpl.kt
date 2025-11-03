package repository.fillter

import database.MapAreasDao
import database.SavedFiltersDao
import entities.MapArea
import kotlinx.coroutines.flow.Flow

class MapAreaRepositoryImpl(
    private val mapAreasDao: MapAreasDao,
    private val filtersDao: SavedFiltersDao,
): MapAreaRepository {
    override suspend fun saveArea(area: MapArea): Long {
        return mapAreasDao.saveArea(area)
    }

    override fun getAllSavedAreas(): Flow<List<MapArea>> {
        return mapAreasDao.getAllSavedAreas()
    }

    override suspend fun deleteSavedArea(id: String) {
        val area = mapAreasDao.getSavedAreaById(id)
        area?.let { mapAreasDao.deleteSavedArea(it) }
    }
}