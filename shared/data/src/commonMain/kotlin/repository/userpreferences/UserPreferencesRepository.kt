package repository.userpreferences

import api.DeviceDocumentResponse
import database.UserPreferencesDao
import entities.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import server_response.flathub.ReferralStatsResponse

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences?>
    suspend fun saveListViewPreferences(isListView: Boolean)
    suspend fun setUser(deviceDocumentResponse: DeviceDocumentResponse)
    suspend fun updateReferralStats(referralStats: ReferralStatsResponse?)
}

class UserPreferencesRepositoryImpl(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences?> {
        return userPreferencesDao.getUserPreferences()
    }

    override suspend fun saveListViewPreferences(isListView: Boolean) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(preferences.copy(isListView = isListView))

    }

    override suspend fun setUser(deviceDocumentResponse: DeviceDocumentResponse) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(
            preferences.copy(deviceDocumentResponse = deviceDocumentResponse)
        )
    }

    override suspend fun updateReferralStats(referralStats: ReferralStatsResponse?) {
        userPreferencesDao.updateReferralStats(referralStats)
    }
}