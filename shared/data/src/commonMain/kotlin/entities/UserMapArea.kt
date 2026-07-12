package entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import database.RoomTypeConverter
import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlin.time.Clock
import kotlinx.serialization.Serializable

@Entity(tableName = "map_areas")
@Serializable
@TypeConverters(RoomTypeConverter::class)
data class UserMapArea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pathId: String,
    val coordinates: List<Coordinates>,
    val isActive: Boolean,
    val name: String,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as UserMapArea

        if (id != other.id) return false
        if (isActive != other.isActive) return false
        if (pathId != other.pathId) return false
        if (coordinates != other.coordinates) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + pathId.hashCode()
        result = 31 * result + coordinates.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
