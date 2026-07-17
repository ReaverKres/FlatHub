package entities

import entities.MetroStationNames.AERODROMNAYA
import entities.MetroStationNames.AKADEMIYA_NAUK
import entities.MetroStationNames.AVTOZAVODSKAYA
import entities.MetroStationNames.BORISOVSKIY_TRACT
import entities.MetroStationNames.FRUNZENSKAYA
import entities.MetroStationNames.GRUSHEVKA
import entities.MetroStationNames.INSTITUT_KULTURY
import entities.MetroStationNames.KAMENNAYA_GORKA
import entities.MetroStationNames.KOVALSKAYA_SLOBODA
import entities.MetroStationNames.KUNCEVSHINA
import entities.MetroStationNames.KUPALOVSKAYA
import entities.MetroStationNames.MALINOVKA
import entities.MetroStationNames.MIHALOVO
import entities.MetroStationNames.MOGILEVSKAYA
import entities.MetroStationNames.MOLODYOZHNAYA
import entities.MetroStationNames.MOSKOVSKAYA
import entities.MetroStationNames.NEMIGA
import entities.MetroStationNames.NEMORSHANSKIY_SAD
import entities.MetroStationNames.OKTYABRSKAYA
import entities.MetroStationNames.PARK_CHELYUSKINCEV
import entities.MetroStationNames.PARTIZANSKAYA
import entities.MetroStationNames.PERVOMAYSKAYA
import entities.MetroStationNames.PETROVSHINA
import entities.MetroStationNames.PLOCHAD_FRANTISHKA_BOGUSHEVICHA
import entities.MetroStationNames.PLOCHAD_LENINA
import entities.MetroStationNames.PLOCHAD_POBEDY
import entities.MetroStationNames.PLOCHAD_YAKUBA_KOLASA
import entities.MetroStationNames.PROLETARSKAYA
import entities.MetroStationNames.PUSHKINSKAYA
import entities.MetroStationNames.SLUTSKIY_GOSTINEC
import entities.MetroStationNames.SPORTIVNAYA
import entities.MetroStationNames.TRAKTORNY_ZAVOD
import entities.MetroStationNames.URUCHIE
import entities.MetroStationNames.VOKZALNAYA
import entities.MetroStationNames.VOSTOK
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.BookingDatesFilter
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.CountryCode
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.commonentities.isCommercial
import kotlinx.serialization.Serializable
import repository.osm.OsmDistricts
import server_request.Currency

@Serializable
data class CommonFilterRequestModel(
    val name: String? = null,
    val adType: AdType = AdType.RENT,
    val lastCommercialAdType: AdType = AdType.COMMERCIAL(),
    val priceFull: Price? = null,
    val pricePerSquare: Price? = null,
    val totalArea: FromToRange? = null,
    val priceType: PriceType = PriceType.FULL,
    val currency: Currency = Currency.USD,
    val addressRequestModel: Set<AddressRequestModel> = emptySet(),
    val numberOfRooms: Set<Int>? = emptySet(),
    val metroStations: List<MetroStation> = emptyList(),
    val withAnyMetro: Boolean = false,
    val districtsArea: List<OsmDistricts> = emptyList(),
    val location: LocationFilter? = null,
    val userMapAreas: List<UserMapArea> = emptyList(),
    val roomOnly: Boolean = false,
    val fromOwnerOnly: Boolean? = null,
    val withPhotoOnly: Boolean = false,
    val sortOption: FlatSort = FlatSort.NEWEST_FIRST, // Added sort option
    val commercial: CommercialRequestModel? = null,
    val bookingDatesFilter: BookingDatesFilter? = null,
    val isNotificationEnabled: Boolean = false
) {

    val isRentType: Boolean
        get() = adType == AdType.RENT

    val isRoomForRent: Boolean
        get() = adType == AdType.RENT && roomOnly

    val isCommercial: Boolean
        get() = adType.isCommercial

    val isPricePerSquareNeeded: Boolean
        get() = adType == AdType.SALE || isCommercial

    private fun compareCommercial(
        commercial1: CommercialRequestModel?,
        commercial2: CommercialRequestModel?
    ): Boolean {
        val isCommercialPropertyTypeEqual = compareCommercialPropertyType(
            commercial1?.commercialPropertyType,
            commercial2?.commercialPropertyType
        )

        return when {
            commercial1 == null && commercial2 == null -> true
            commercial1 == null && commercial2 != null ->
                isCommercialPropertyTypeEqual && commercial2.roomRange == null

            commercial1 != null && commercial2 == null ->
                isCommercialPropertyTypeEqual && commercial1.roomRange == null

            else -> isCommercialPropertyTypeEqual && commercial1?.roomRange == commercial2?.roomRange
        }
    }

    private fun compareCommercialPropertyType(
        propertyType1: CommercialPropertyType?,
        propertyType2: CommercialPropertyType?
    ): Boolean {
        return when {
            propertyType1 == null && propertyType2 == null -> true
            propertyType1 == null -> propertyType2 == CommercialPropertyType.Office
            propertyType2 == null -> propertyType1 == CommercialPropertyType.Office
            else -> propertyType1 == propertyType2 ||
                    (propertyType1 == CommercialPropertyType.Office && propertyType2 == null) ||
                    (propertyType1 == null && propertyType2 == CommercialPropertyType.Office)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CommonFilterRequestModel

        // Всегда сравниваем только выбранные станции
        val thisSelectedMetro = this.metroStations.filter { it.selected }
        val otherSelectedMetro = other.metroStations.filter { it.selected }

        // Всегда сравниваем только выбранные области
        val thisSelectedAreas = this.userMapAreas.filter { it.isActive }
        val otherSelectedAreas = other.userMapAreas.filter { it.isActive }

        // Всегда сравниваем только выбранные области
        val thisDistrictsAreas = this.districtsArea.filter { it.isChecked }
        val otherDistrictsAreas = other.districtsArea.filter { it.isChecked }

        //TODO
        // Специальная логика сравнения location: null эквивалентен LocationFilter(BY, MINSK)
        val isLocationEqual = when {
            this.location == null && other.location == null -> true
            this.location == null && other.location != null ->
                other.location.country == CountryCode.BY && other.location.city == CityCode.MINSK

            this.location != null && other.location == null ->
                this.location.country == CountryCode.BY && this.location.city == CityCode.MINSK

            else -> this.location == other.location
        }
        //TODO
        // Специальная логика сравнения fromOwnerOnly: null эквивалентен false

        val isFromOwnerOnlyEqual = when {
            this.fromOwnerOnly == null && other.fromOwnerOnly == null -> true
            this.fromOwnerOnly == null -> other.fromOwnerOnly == false
            other.fromOwnerOnly == null -> this.fromOwnerOnly == false
            else -> this.fromOwnerOnly == other.fromOwnerOnly
        }
        val isCommercialEqual = compareCommercial(this.commercial, other.commercial)

        if (roomOnly != other.roomOnly) return false
        if (withPhotoOnly != other.withPhotoOnly) return false
        if (withAnyMetro != other.withAnyMetro) return false
        if (adType != other.adType) return false
        if (!isCommercialEqual) return false
        if (bookingDatesFilter != other.bookingDatesFilter) return false
        if (totalArea != other.totalArea) return false
        if (priceFull != other.priceFull) return false
        if (pricePerSquare != other.pricePerSquare) return false
        if (!isFromOwnerOnlyEqual) return false
        if (priceType != other.priceType) return false
        //TODO
//        if (currency != other.currency) return false
        if (addressRequestModel != other.addressRequestModel) return false
        if (numberOfRooms != other.numberOfRooms) return false
        if (thisSelectedMetro != otherSelectedMetro) return false
        if (thisSelectedAreas != otherSelectedAreas) return false
        if (thisDistrictsAreas != otherDistrictsAreas) return false
        if (!isLocationEqual) return false // Используем кастомную проверку location
        if (sortOption != other.sortOption) return false // Added sort option comparison

        return true
    }

    override fun hashCode(): Int {
        var result = priceFull?.hashCode() ?: 0
        val commercialForHashCode = normalizeCommercialForHashCode(commercial)
        result = 31 * result + (commercialForHashCode?.hashCode() ?: 0)
        result = 31 * result + (bookingDatesFilter?.hashCode() ?: 0)
        result = 31 * result + (totalArea?.hashCode() ?: 0)
        result = 31 * result + (pricePerSquare?.hashCode() ?: 0)
        result = 31 * result + (fromOwnerOnly ?: false).hashCode()
        result = 31 * result + roomOnly.hashCode()
        result = 31 * result + withPhotoOnly.hashCode()
        result = 31 * result + withAnyMetro.hashCode()
        result = 31 * result + adType.hashCode()
        //TODO
//        result = 31 * result + currency.hashCode()
        result = 31 * result + priceType.hashCode()
        result = 31 * result + addressRequestModel.hashCode()
        result = 31 * result + (numberOfRooms?.hashCode() ?: 0)
        result = 31 * result + metroStations.filter { it.selected }.hashCode()
        result = 31 * result + userMapAreas.filter { it.isActive }.hashCode()
        result = 31 * result + districtsArea.filter { it.isChecked }.hashCode()
        result = 31 * result + sortOption.hashCode() // Added sort option to hash code

        //TODO
        // Для hashCode тоже учитываем специальную логику
        val locationForHashCode = location ?: LocationFilter(
            country = CountryCode.BY, city = CityCode.MINSK
        )
        result = 31 * result + (locationForHashCode.hashCode())

        return result
    }
}

private fun normalizeCommercialForHashCode(commercial: CommercialRequestModel?): CommercialRequestModel? {
    return when {
        commercial == null -> null
        commercial.commercialPropertyType == null || commercial.commercialPropertyType == CommercialPropertyType.Office ->
            commercial.copy(commercialPropertyType = null)

        else -> commercial
    }
}

enum class PriceType {
    PER_SQUARE, FULL
}

@Serializable
data class AddressRequestModel(
    val address: String
)

@Serializable
data class LocationFilter(val country: CountryCode, val city: CityCode)

@Serializable
enum class MetroLine {
    GREEN, BLUE, RED,
}

@Serializable
data class MetroStation(
    val line: MetroLine,
    val metroId: Int,
    val name: String,
    val selected: Boolean = false
)

object MetroStations {
    // Красная ветка (Московская линия)
    //2,3,36,4,     5,6,7,8,        32,9,10,11,     12,13,14,15,
    //35,16,17,18,  19,20,21,22,    33,23,24,25,    26,34,27,28,29
    // Красная ветка (Московская линия)
    internal val RED_LINE = listOf(
        MetroStation(MetroLine.RED, 2, AVTOZAVODSKAYA),
        MetroStation(MetroLine.RED, 8, KAMENNAYA_GORKA),
        MetroStation(MetroLine.RED, 9, KUNCEVSHINA),
        MetroStation(MetroLine.RED, 12, MOGILEVSKAYA),
        MetroStation(MetroLine.RED, 13, MOLODYOZHNAYA),
        MetroStation(MetroLine.RED, 15, NEMIGA),
        MetroStation(MetroLine.RED, 16, OKTYABRSKAYA),
        MetroStation(MetroLine.RED, 18, PARTIZANSKAYA),
        MetroStation(MetroLine.RED, 19, PERVOMAYSKAYA),
        MetroStation(MetroLine.RED, 24, PROLETARSKAYA),
        MetroStation(MetroLine.RED, 25, PUSHKINSKAYA),
        MetroStation(MetroLine.RED, 26, SPORTIVNAYA),
        MetroStation(MetroLine.RED, 27, TRAKTORNY_ZAVOD),
        MetroStation(MetroLine.RED, 29, FRUNZENSKAYA),
    )

    // Синяя ветка (Автозаводская линия)
    internal val BLUE_LINE = listOf(
        MetroStation(MetroLine.BLUE, 3, AKADEMIYA_NAUK),
        MetroStation(MetroLine.BLUE, 4, BORISOVSKIY_TRACT),
        MetroStation(MetroLine.BLUE, 5, VOSTOK),
        MetroStation(MetroLine.BLUE, 6, GRUSHEVKA),
        MetroStation(MetroLine.BLUE, 7, INSTITUT_KULTURY),
        MetroStation(MetroLine.BLUE, 10, MALINOVKA),
        MetroStation(MetroLine.BLUE, 11, MIHALOVO),
        MetroStation(MetroLine.BLUE, 14, MOSKOVSKAYA),
        MetroStation(MetroLine.BLUE, 16, KUPALOVSKAYA),
        MetroStation(MetroLine.BLUE, 17, PARK_CHELYUSKINCEV),
        MetroStation(MetroLine.BLUE, 20, PETROVSHINA),
        MetroStation(MetroLine.BLUE, 21, PLOCHAD_LENINA),
        MetroStation(MetroLine.BLUE, 22, PLOCHAD_POBEDY),
        MetroStation(MetroLine.BLUE, 23, PLOCHAD_YAKUBA_KOLASA),
        MetroStation(MetroLine.BLUE, 28, URUCHIE),
    )

    // Зелёная ветка (Зеленолужская линия)
    internal val GREEN_LINE = listOf(
        MetroStation(MetroLine.GREEN, 36, AERODROMNAYA),
        MetroStation(MetroLine.GREEN, 32, KOVALSKAYA_SLOBODA),
        MetroStation(MetroLine.GREEN, 35, NEMORSHANSKIY_SAD),
        MetroStation(MetroLine.GREEN, 21, VOKZALNAYA),
        MetroStation(MetroLine.GREEN, 33, PLOCHAD_FRANTISHKA_BOGUSHEVICHA),
        MetroStation(MetroLine.GREEN, 34, SLUTSKIY_GOSTINEC),
        MetroStation(MetroLine.GREEN, 29, FRUNZENSKAYA),
    )

    // Получить ID станций по ветке
    fun getStationIdsByLine(line: MetroLine): List<Int> {
        return when (line) {
            MetroLine.RED -> RED_LINE.map { it.metroId }
            MetroLine.BLUE -> BLUE_LINE.map { it.metroId }
            MetroLine.GREEN -> GREEN_LINE.map { it.metroId }
        }
    }

    fun allStationsRequest(): List<MetroStation> {
        // Порядок: Московская (BLUE), Автозаводская (RED), Зеленолужская (GREEN)
        val orderedLines = listOf(MetroLine.BLUE, MetroLine.RED, MetroLine.GREEN)
        return orderedLines.flatMap { line ->
            when (line) {
                MetroLine.BLUE -> BLUE_LINE
                MetroLine.RED -> RED_LINE
                MetroLine.GREEN -> GREEN_LINE
            }.map { MetroStation(it.line, it.metroId, it.name) }
        }
    }

    fun stationsForCity(city: CityCode?): List<MetroStation> {
        return when (city) {
            CityCode.WARSZAWA -> WarsawMetroStations.allStationsRequest()
            CityCode.TBILISI -> TbilisiMetroStations.allStationsRequest()
            CityCode.ALMATY -> AlmatyMetroStations.allStationsRequest()
            CityCode.MINSK, null -> allStationsRequest()
            else -> emptyList()
        }
    }
}