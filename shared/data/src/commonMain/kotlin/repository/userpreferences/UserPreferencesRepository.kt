package repository.userpreferences

import api.DeviceDocumentResponse
import database.UserPreferencesDao
import entities.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences?>
    suspend fun saveListViewPreferences(isListView: Boolean)
    suspend fun setUser(deviceDocumentResponse: DeviceDocumentResponse)
}

class UserPreferencesRepositoryImpl(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences?> {
        return userPreferencesDao.getUserPreferences()
    }

    override suspend fun saveListViewPreferences(isListView: Boolean) {
        val preferences = getUserPreferences().first()
        preferences?.let {
            userPreferencesDao.saveUserPreferences(it.copy(isListView = isListView))
        }
    }

    override suspend fun setUser(deviceDocumentResponse: DeviceDocumentResponse) {
        val preferences = getUserPreferences().first()
        preferences?.let {
            userPreferencesDao.saveUserPreferences(it.copy(
                deviceDocumentResponse = deviceDocumentResponse
            ))
        }
    }
}