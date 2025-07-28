package entities

import server_request.Currency

data class CommonFilterRequestModel(
    val priceFrom: Double? = null,
    val priceTo: Double? = null,
    val currency: Currency = Currency.USD,
    val metroLine: List<MetroLine> = emptyList()
)
enum class MetroLine {
    GREEN, BLUE, RED,
}

internal class KufarMetroStation(
    val line: MetroLine,
    val metroId: Int
)

//TODO Обновить id
object KufarMetroStations {
    // Красная ветка (Московская линия)
    internal val RED_LINE = listOf(
        KufarMetroStation(MetroLine.RED, 2),   // Площадь Ленина
        KufarMetroStation(MetroLine.RED, 8),   // Площадь Якуба Коласа
        KufarMetroStation(MetroLine.RED, 9),   // Академия наук
        KufarMetroStation(MetroLine.RED, 12),  // Молодёжная
        KufarMetroStation(MetroLine.RED, 13),  // Фрунзенская
        KufarMetroStation(MetroLine.RED, 19),  // Парк Челюскинцев
        KufarMetroStation(MetroLine.RED, 27)   // Михалово
    )

    // Синяя ветка (Автозаводская линия)
    internal val BLUE_LINE = listOf(
        KufarMetroStation(MetroLine.BLUE, 15), // Немига
        KufarMetroStation(MetroLine.BLUE, 18), // Купаловская
        KufarMetroStation(MetroLine.BLUE, 24), // Борисовский тракт
        KufarMetroStation(MetroLine.BLUE, 25), // Уручье
        KufarMetroStation(MetroLine.BLUE, 29)  // Грушевка
    )

    // Зелёная ветка (Зеленолужская линия)
    internal val GREEN_LINE = listOf(
        KufarMetroStation(MetroLine.GREEN, 29), // Грушевка (пересечение с синей)
        KufarMetroStation(MetroLine.GREEN, 32), // Каменная Горка
        KufarMetroStation(MetroLine.GREEN, 33), // Кунцевщина
        KufarMetroStation(MetroLine.GREEN, 34), // Спортивная
        KufarMetroStation(MetroLine.GREEN, 35), // Пушкинская
        KufarMetroStation(MetroLine.GREEN, 36)  // Малиновка
    )

    // Все станции
    internal val ALL_STATIONS = RED_LINE + BLUE_LINE + GREEN_LINE

    // Получить ID станций по ветке
    fun getStationIdsByLine(line: MetroLine): List<Int> {
        return when (line) {
            MetroLine.RED -> RED_LINE.map { it.metroId }
            MetroLine.BLUE -> BLUE_LINE.map { it.metroId }
            MetroLine.GREEN -> GREEN_LINE.map { it.metroId }
        }
    }
}