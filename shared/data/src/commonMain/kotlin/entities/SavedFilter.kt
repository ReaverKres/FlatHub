package entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import database.RoomTypeConverter
import kotlinx.datetime.Clock

@Entity(tableName = "saved_filters")
@TypeConverters(RoomTypeConverter::class)
data class SavedFilter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val filterData: CommonFilterRequestModel,
    val selected: Boolean = false,
    val isNotification: Boolean = false,
    val notificationInterval: Int? = null, // Minutes: 15, 30, 60
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)
