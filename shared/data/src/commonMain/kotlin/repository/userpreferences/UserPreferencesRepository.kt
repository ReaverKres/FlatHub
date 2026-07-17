package repository.userpreferences

import api.DeviceDocumentResponse
import database.UserPreferencesDao
import entities.UserPreferences
import io.flatzen.commoncomponents.theme.AppLanguage
import io.flatzen.commoncomponents.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import server_response.flathub.ReferralStatsResponse

interface UserPreferencesRepository {
    fun getUserPreferences(): Flow<UserPreferences?>
    fun observeThemeMode(): Flow<ThemeMode>
    fun observeAppLanguage(): Flow<AppLanguage>
    fun observeAlwaysTranslate(): Flow<Boolean>
    fun observeTranslateTargetLang(): Flow<AppLanguage?>
    suspend fun saveListViewPreferences(isListView: Boolean)
    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setAlwaysTranslate(enabled: Boolean)
    suspend fun setTranslateTargetLang(language: AppLanguage?)
    suspend fun setUser(deviceDocumentResponse: DeviceDocumentResponse)
    suspend fun updateReferralStats(referralStats: ReferralStatsResponse?)
    suspend fun setSwipeOnboardingCompleted(completed: Boolean)
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

    override fun observeAppLanguage(): Flow<AppLanguage> {
        return getUserPreferences().map { prefs ->
            AppLanguage.fromStored(prefs?.appLanguage)
        }
    }

    override fun observeAlwaysTranslate(): Flow<Boolean> {
        return getUserPreferences().map { prefs -> prefs?.alwaysTranslate == true }
    }

    override fun observeTranslateTargetLang(): Flow<AppLanguage?> {
        return getUserPreferences().map { prefs ->
            prefs?.translateTargetLang
                ?.let { AppLanguage.fromStored(it) }
                ?.takeUnless { it == AppLanguage.SYSTEM }
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

    override suspend fun setAppLanguage(language: AppLanguage) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(preferences.copy(appLanguage = language.name))
    }

    override suspend fun setAlwaysTranslate(enabled: Boolean) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(preferences.copy(alwaysTranslate = enabled))
    }

    override suspend fun setTranslateTargetLang(language: AppLanguage?) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(
            preferences.copy(
                translateTargetLang = language
                    ?.takeUnless { it == AppLanguage.SYSTEM }
                    ?.name
            )
        )
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

    override suspend fun setSwipeOnboardingCompleted(completed: Boolean) {
        val preferences = getUserPreferences().firstOrNull() ?: UserPreferences()
        userPreferencesDao.saveUserPreferences(
            preferences.copy(swipeOnboardingCompleted = completed)
        )
    }
}
