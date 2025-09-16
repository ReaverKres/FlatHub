package repository.userpreferences

import database.UserPreferencesDao
import entities.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences?>
    suspend fun saveUserPreferences(isListView: Boolean)
}

class UserPreferencesRepositoryImpl(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {
    
    override fun getUserPreferences(): Flow<UserPreferences?> {
        return userPreferencesDao.getUserPreferences()
    }
    
    override suspend fun saveUserPreferences(isListView: Boolean) {
        userPreferencesDao.saveUserPreferences(UserPreferences(isListView = isListView))
    }
}