package entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 0,
    val isListView: Boolean = false, // false = grid, true = list
    val isNotificationAvailable: Boolean? = null,
    val isUserRegistered: Boolean? = null
)