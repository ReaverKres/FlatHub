package database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import entities.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import server_response.flathub.ReferralStatsResponse

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 0")
    fun getUserPreferences(): Flow<UserPreferences?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPreferences(preferences: UserPreferences)

    @Transaction
    suspend fun updateReferralStats(referralStats: ReferralStatsResponse?) {
        val currentPrefs = getUserPreferences().firstOrNull() ?: UserPreferences()

        val currentResponse = currentPrefs.deviceDocumentResponse
        val updatedResponse = currentResponse?.copy(referralStats = referralStats)
        val updatedPrefs = currentPrefs.copy(deviceDocumentResponse = updatedResponse)

        updatedPrefs.let { saveUserPreferences(updatedPrefs) }
    }
}