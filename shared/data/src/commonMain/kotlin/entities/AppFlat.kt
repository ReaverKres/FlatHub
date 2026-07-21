package entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.TypeConverters
import database.RoomTypeConverter
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.PriceText
import io.flatzen.commoncomponents.commonentities.usesSquareFeet
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
    @Embedded val listingInsights: ListingInsights? = null,
    val savedInFavorites: Boolean = false,
    val isViewed: Boolean = false,
    val dislike: Boolean = false,
    val flatPlatform: FlatPlatform,
    val flatDetailUrl: String,
    val publishedAt: Instant?,
    val publishedAtServer: String?,
    val publishedAtUi: String?,
    val imageUrls: List<String>?,
    val mainPrice: Double?,
    val secondPrice: Double?,
    val mainPriceSquare: Double? = null,
    val secondPriceSquare: Double? = null,
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
    val hasSecond = secondPrice != null

    val mainCurrencyLabel = when {
        mainPrice == null -> null
        hasSecond && flatPlatform.isBelarusPlatform() -> "$"
        hasSecond && flatPlatform.isGeorgiaPlatform() -> localCurrency
        hasSecond -> localCurrency
        adType == AdType.DAILY -> localCurrency
        flatPlatform.isBelarusPlatform() -> "$"
        else -> localCurrency
    }

    val mainPriceText = mainCurrencyLabel?.let { currency ->
        formatMainPrice(mainPrice, currency)
    }

    val secondCurrencyLabel = when {
        flatPlatform.isGeorgiaPlatform() -> "$"
        else -> localCurrency
    }
    val localPriceText = if (adType != AdType.DAILY && hasSecond) {
        formatSecondPrice(secondPrice, mainPriceText != null, secondCurrencyLabel)
    } else null

    val mainSquareCurrency = when {
        mainPriceSquare == null -> null
        hasSecond && flatPlatform.isBelarusPlatform() -> "$"
        hasSecond && flatPlatform.isGeorgiaPlatform() -> localCurrency
        adType == AdType.DAILY -> localCurrency
        flatPlatform.isBelarusPlatform() -> "$"
        else -> localCurrency
    }
    val priceMainPerSquare = mainPriceSquare?.let { price ->
        mainSquareCurrency?.let { currency ->
            formatPricePerSquare(price, currency, flatPlatform.usesSquareFeet())
        }
    }

    val secondSquareCurrency = when {
        secondPriceSquare == null -> null
        flatPlatform.isGeorgiaPlatform() -> "$"
        else -> localCurrency
    }
    val priceLocalPerSquare = secondPriceSquare?.let { price ->
        secondSquareCurrency?.let { currency ->
            formatPricePerSquare(price, currency, flatPlatform.usesSquareFeet())
        }
    }

    return PriceText(
        mainPrice = mainPriceText,
        mainPerSquarePrice = priceMainPerSquare,
        localPrice = localPriceText,
        localPerSquarePrice = priceLocalPerSquare
    )
}

private fun FlatPlatform.isBelarusPlatform(): Boolean = when (this) {
    FlatPlatform.KUFAR,
    FlatPlatform.ONLINER,
    FlatPlatform.DOMOVITA,
    FlatPlatform.REALT,
        -> true

    else -> false
}

private fun FlatPlatform.isGeorgiaPlatform(): Boolean = when (this) {
    FlatPlatform.SS_GE,
    FlatPlatform.LIVO,
    FlatPlatform.BINEBI,
        -> true

    else -> false
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

    FlatPlatform.FOTOCASA,
    FlatPlatform.PISOS,
        -> "€"

    FlatPlatform.IS24,
    FlatPlatform.IMMOWELT,
    FlatPlatform.KLEINANZEIGEN,
    FlatPlatform.IS24_AT,
    FlatPlatform.IMMOWELT_AT,
    FlatPlatform.WILLHABEN,
        -> "€"

    FlatPlatform.EMLAKJET,
        -> "₺"

    FlatPlatform.PROPERTY_FINDER,
    FlatPlatform.DUBIZZLE,
    FlatPlatform.OPENSOOQ,
        -> "AED"

    FlatPlatform.PROPERTYHUB,
    FlatPlatform.LIVINGINSIDER,
    FlatPlatform.RENTHUB,
        -> "฿"

    FlatPlatform.DABANG,
    FlatPlatform.ZIGBANG,
        -> "₩"

    FlatPlatform.SUUMO,
    FlatPlatform.YAHOO_RE,
    FlatPlatform.ATHOME,
        -> "¥"

    FlatPlatform.FLATFOX,
        -> "CHF"

    FlatPlatform.ZUMPER,
        -> "$"

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

data class ListingInsights(
    /** Negative = below area average, e.g. -5.0 means 5% below. */
    val priceVsAreaAvgPercent: Double? = null,
    val daysOnMarket: Int? = null,
    val hoaMonthly: Double? = null,
    /** e.g. single_family, condo, townhouse, apartment */
    val propertySubtype: String? = null,
)
