package database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import entities.UserPreferences
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 0")
    fun getUserPreferences(): Flow<UserPreferences?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPreferences(preferences: UserPreferences)

    @Query("UPDATE user_preferences SET isNotificationAvailable = :isAvailable WHERE id = 0")
    suspend fun setNotificationAvailable(isAvailable: Boolean)

    @Query("UPDATE user_preferences SET isUserRegistered = :isUserRegistered WHERE id = 0")
    suspend fun setRegistrationStatus(isUserRegistered: Boolean)
}