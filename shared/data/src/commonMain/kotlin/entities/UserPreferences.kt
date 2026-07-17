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
    val appLanguage: String? = null, // AppLanguage.name: SYSTEM | EN | DE | ES | TR | AR | RU | PL | KK | KA
    val swipeOnboardingCompleted: Boolean = false,
    /** Auto-translate listing text when opening detail. */
    val alwaysTranslate: Boolean = false,
    /** AppLanguage.name without SYSTEM; null = use current UI language. */
    val translateTargetLang: String? = null,
)
