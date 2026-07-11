package entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import api.DeviceDocumentResponse
import database.RoomTypeConverter

@Entity(tableName = "user_preferences")
@TypeConverters(RoomTypeConverter::class)
data class UserPreferences(
    @PrimaryKey val id: Int = 0,
    val isListView: Boolean = false, // false = grid, true = list
    val deviceDocumentResponse: DeviceDocumentResponse? = null,
    val themeMode: String? = null, // ThemeMode.name: SYSTEM | LIGHT | DARK
)