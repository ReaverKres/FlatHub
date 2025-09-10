package entities// entities.AppFlat.kt
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import database.RoomTypeConverter
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.datetime.Instant

@Entity
@TypeConverters(RoomTypeConverter::class)
data class AppFlat(
    @PrimaryKey val adId: Long,
    @Embedded val flatDevInfo: FlatDevInfo,
    @Embedded val contactInformation: ContactInformation?,
    @Embedded val coordinates: Coordinates?,
    val savedInFavorites: Boolean = false,
    val isViewed: Boolean = false,
    val flatPlatform: FlatPlatform,
    val flatDetailUrl: String,
    val publishedAt: Instant?,
    val publishedAtServer: String?,
    val publishedAtUi: String?,
    val imageUrls: List<String>?,
    val priceUsd: Double?,
    val priceByn: Double?,
    val priceUsdSquare: Double? = null,
    val priceBynSquare: Double? = null,
    val rooms: Int?,
    val district: String?,
    val address: String?,
    val metroStation: String?,
    val description: String?,
    val yearBuilt: Int?,

    // Основные параметры квартиры
    val totalArea: Double?,
    val livingArea: Double?,
    val kitchenArea: Double?,
    val floor: Int?,
    val totalFloors: Int?,
    val sleepingPlaces: Int?,
    val isStudio: Boolean?,

    // Параметры ванной и балкона
    val bathroomType: String?, // "Раздельный", "Совмещенный" и т.д.
    val balcony: String?, // "Есть", "Нет", "Лоджия", "2 балкона" и т.д.

    // Ремонт и состояние
    val repairType: String?, // "Косметический", "Евро", "Дизайнерский" и т.д.
    val condition: String?, // "Вторичное", "Новостройка" и т.д.

    // Направления окон
    val windowDirections: List<String>?, // ["Во двор", "На улицу", "Юг"] и т.д.

    // Улучшения дома
    val buildingImprovements: List<String>?, // ["Лифт", "Домофон", "Видеонаблюдение"] и т.д.

    // Предоплата
    val prepaymentType: String?, // "Месяц", "2 месяца", "Залог" и т.д.

    // Удобства и оборудование (заменяем AdditionalParams)
    val amenities: List<String>?, // ["Мебель", "Стиральная машина", "Wi-Fi"] и т.д.
    val kitchenEquipment: List<String>?, // ["Плита", "Холодильник", "Микроволновка"] и т.д.

    // Дополнительные параметры
    val forWhom: List<String>?, // ["Семейным", "Студентам"] и т.д.
    val parkingInfo: String?, // Информация о парковке
    val owner: Boolean?, // Собственник или агент
)

data class FlatDevInfo(
    val isDetailData: Boolean,
    val isDetailLoaded: Boolean
)

data class ContactInformation(
    val phones: List<String>?,
    val ownerName: String?
)