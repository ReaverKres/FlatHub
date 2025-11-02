package database

import androidx.room.TypeConverter
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

@Suppress("Unused")
class RoomTypeConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAdType(adType: AdType): String {
        return adType.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toAdType(value: String): AdType {
        return Json.decodeFromString<AdType>(value)
    }

    @TypeConverter
    fun fromCommercialPropertyType(commercialPropertyType: CommercialPropertyType?): String? {
        return commercialPropertyType?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toCommercialPropertyType(value: String?): CommercialPropertyType? {
        return value?.let { Json.decodeFromString<CommercialPropertyType>(it) }
    }

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

    @TypeConverter
    fun fromCoordinates(coordinates: Coordinates?): String? {
        return coordinates?.let {
            Json.encodeToString(Coordinates.serializer(), it)
        }
    }

    @TypeConverter
    fun toCoordinates(coordinatesString: String?): Coordinates? {
        return coordinatesString?.let {
            Json.decodeFromString(Coordinates.serializer(), it)
        }
    }
}