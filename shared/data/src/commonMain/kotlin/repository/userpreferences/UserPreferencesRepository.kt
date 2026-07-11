package repository.userpreferences

import api.DeviceDocumentResponse
import database.UserPreferencesDao
import entities.UserPreferences
import io.flatzen.commoncomponents.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import server_response.flathub.ReferralStatsResponse

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences?>
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun saveListViewPreferences(isListView: Boolean)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setUser(deviceDocumentResponse: DeviceDocumentResponse)
    suspend fun updateReferralStats(referralStats: ReferralStatsResponse?)
}

class UserPreferencesRepositoryImpl(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {

    override fun getUserPreferences(): Flow<UserPreferences?> {
        return userPreferencesDao.getUserPreferences()
    }

    override fun observeThemeMode(): Flow<ThemeMode> {
        return getUserPreferences().map { prefs ->
            ThemeMode.fromStored(prefs?.themeMode)
        }
    }

    override suspend fun saveListViewPreferences(isListView: Boolean) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(preferences.copy(isListView = isListView))
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(preferences.copy(themeMode = themeMode.name))
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
