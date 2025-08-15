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
import server_request.Currency

data class CommonFilterRequestModel(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
    val currency: Currency = Currency.USD,
    val addressRequestModel: Set<AddressRequestModel> = emptySet(),
    val numberOfRooms: Set<Int>? = emptySet(),
    val metroStations: List<MetroStation> = emptyList(),
    val location: LocationFilter? = null,
    val fromOwnerOnly: Boolean? = false
)

data class AddressRequestModel(
    val address: String
)

enum class Country { BY }
enum class City { MINSK, BREST, VITEBSK, GOMEL, GRODNO, MOGILEV }
data class LocationFilter(val country: Country, val city: City)
enum class MetroLine {
    GREEN, BLUE, RED,
}

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
}