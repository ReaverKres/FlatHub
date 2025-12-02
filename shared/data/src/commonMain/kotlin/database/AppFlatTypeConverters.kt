package database

import androidx.room.TypeConverter
import api.DeviceDocumentResponse
import entities.CommonFilterRequestModel
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
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
    fun fromCoordinatesList(coordinatesList: List<Coordinates>?): String? {
        return coordinatesList?.let {
            Json.encodeToString(ListSerializer(Coordinates.serializer()), it)
        }
    }

    @TypeConverter
    fun toCoordinatesList(coordinatesListString: String?): List<Coordinates>? {
        return coordinatesListString?.let {
            Json.decodeFromString(ListSerializer(Coordinates.serializer()), it)
        }
    }

    @TypeConverter
    fun fromDeviceDocumentResponse(value: DeviceDocumentResponse?): String? =
        value?.let { json.encodeToString(DeviceDocumentResponse.serializer(), it) }

    @TypeConverter
    fun toDeviceDocumentResponse(value: String?): DeviceDocumentResponse? =
        value?.let { json.decodeFromString(DeviceDocumentResponse.serializer(), it) }
}