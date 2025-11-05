package repository.fillter

import database.UserMapAreasDao
import entities.UserMapArea
import kotlinx.coroutines.flow.Flow

class UserMapAreaRepositoryImpl(
    private val userMapAreasDao: UserMapAreasDao,
): UserMapAreaRepository {
    override suspend fun saveArea(area: UserMapArea): Long {
        return userMapAreasDao.saveArea(area)
    }

    override fun getAllSavedAreas(): Flow<List<UserMapArea>> {
        return userMapAreasDao.getAllSavedAreas()
    }

    override suspend fun deleteSavedArea(id: String) {
        val area = userMapAreasDao.getSavedAreaById(id)
        area?.let { userMapAreasDao.deleteSavedArea(it) }
    }
}