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
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)
