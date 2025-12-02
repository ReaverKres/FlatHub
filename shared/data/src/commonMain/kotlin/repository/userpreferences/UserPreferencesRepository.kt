package repository.userpreferences

import database.UserPreferencesDao
import entities.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences?>
    suspend fun saveListViewPreferences(isListView: Boolean)
    suspend fun setNotificationAvailable(isAvailable: Boolean)
    suspend fun setRegistrationStatus(isRegistered: Boolean)
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

    override suspend fun setNotificationAvailable(isAvailable: Boolean) {
        // ensure a row exists and then update the flag
        userPreferencesDao.setNotificationAvailable(isAvailable)
    }

    override suspend fun setRegistrationStatus(isRegistered: Boolean) {
        userPreferencesDao.setRegistrationStatus(isRegistered)

    }
}