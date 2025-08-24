package database

import androidx.room.TypeConverter
import entities.CommonFilterRequestModel
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

@Suppress("Unused")
class RoomTypeConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun fromListString(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    fun toListString(value: String?): List<String>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromCommonFilterRequestModel(value: CommonFilterRequestModel?): String? = 
        value?.let { json.encodeToString(CommonFilterRequestModel.serializer(), it) }

    @TypeConverter
    fun toCommonFilterRequestModel(value: String?): CommonFilterRequestModel? = 
        value?.let { json.decodeFromString(CommonFilterRequestModel.serializer(), it) }
}