package entities// entities.AppFlat.kt
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.TypeConverters
import database.RoomTypeConverter
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.PriceText
import io.flatzen.commoncomponents.utils.formatMainPrice
import io.flatzen.commoncomponents.utils.formatPricePerSquare
import io.flatzen.commoncomponents.utils.formatSecondPrice
import kotlin.time.Instant

@Entity(primaryKeys = ["flatPlatform", "adId"])
@TypeConverters(RoomTypeConverter::class)
data class AppFlat(
    val adId: Long,
    @Embedded val adType: AdType? = AdType.RENT,
    @Embedded val flatDevInfo: FlatDevInfo,
    @Embedded val contactInformation: ContactInformation?,
    @Embedded val coordinates: Coordinates?,
    @Embedded val commercialInfo: CommercialInfo?,
    val savedInFavorites: Boolean = false,
    val isViewed: Boolean = false,
    val dislike: Boolean = false,
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
) {
    fun getAdTypeNonNull(): AdType = adType ?: AdType.RENT
}

fun AppFlat.getPricesText(): PriceText {
    val localCurrency = localCurrencyLabel(flatPlatform)
    val localIsMain = this.priceUsd == null && this.priceByn != null
    val mainPriceText = if (localIsMain) {
        formatMainPrice(this.priceByn, localCurrency)
    } else if (this.priceUsd != null) {
        formatMainPrice(this.priceUsd)
    } else null

    val localPriceText = if (this.adType != AdType.DAILY && !localIsMain) {
        formatSecondPrice(this.priceByn, mainPriceText != null, localCurrency)
    } else null
    val priceLocalPerSquare = if (priceBynSquare != null) {
        formatPricePerSquare(this.priceBynSquare, localCurrency)
    } else null

    val priceMainPerSquare = if (priceUsdSquare != null) {
        formatPricePerSquare(this.priceUsdSquare, "$")
    } else null
    return PriceText(
        mainPrice = mainPriceText,
        mainPerSquarePrice = priceMainPerSquare,
        localPrice = localPriceText,
        localPerSquarePrice = priceLocalPerSquare
    )
}

private fun localCurrencyLabel(platform: FlatPlatform): String = when (platform) {
    FlatPlatform.OTODOM,
    FlatPlatform.OLX_PL,
    FlatPlatform.GRATKA,
    FlatPlatform.MORIZON,
        -> "PLN"

    FlatPlatform.SS_GE,
    FlatPlatform.LIVO,
    FlatPlatform.BINEBI,
        -> "GEL"

    FlatPlatform.KRISHA,
    FlatPlatform.OLX_KZ,
    FlatPlatform.KN,
        -> "KZT"

    FlatPlatform.IDEALISTA,
    FlatPlatform.FOTOCASA,
    FlatPlatform.PISOS,
        -> "€"

    else -> "BYN"
}

data class FlatDevInfo(
    val isDetailData: Boolean,
    val isDetailLoaded: Boolean,
    /** True after background coord enrich finished for this ad (success or no coords). */
    val coordsEnriched: Boolean = false,
)

data class ContactInformation(
    val phones: List<String>?,
    val ownerName: String?
)

data class CommercialInfo(
    val numberOfRooms: Int?,
    val propertyType: CommercialPropertyType?
)