package entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import database.RoomTypeConverter
import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Entity(tableName = "map_areas")
@Serializable
@TypeConverters(RoomTypeConverter::class)
data class MapArea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pathId: String,
    val coordinates: List<Coordinates>,
    val isActive: Boolean,
    val name: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)
