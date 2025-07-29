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
    //2,3,36,4,     5,6,7,8,        32,9,10,11,     12,13,14,15,
    //35,16,17,18,  19,20,21,22,    33,23,24,25,    26,34,27,28,29
    internal val RED_LINE = listOf(
        KufarMetroStation(MetroLine.RED, 2),   // Автозаводская
        KufarMetroStation(MetroLine.RED, 8),   // Каменная горка
        KufarMetroStation(MetroLine.RED, 9),  // Кунцевщина
        KufarMetroStation(MetroLine.RED, 12),  // Могилёвская
        KufarMetroStation(MetroLine.RED, 13),  // Молодёжная
        KufarMetroStation(MetroLine.RED, 15),  // Немига
        KufarMetroStation(MetroLine.RED, 16),   // Октябрьская
        KufarMetroStation(MetroLine.RED, 18),   // Партизанская
        KufarMetroStation(MetroLine.RED, 19),   // Первомайская
        KufarMetroStation(MetroLine.RED, 24),   // пролетарская
        KufarMetroStation(MetroLine.RED, 25),   // пушкинская
        KufarMetroStation(MetroLine.RED, 26),   // спортивная
        KufarMetroStation(MetroLine.RED, 27),   // Тракторный завод
        KufarMetroStation(MetroLine.RED, 29), // Фрунзенская юбилейная
    )

    // Синяя ветка (Автозаводская линия)
    internal val BLUE_LINE = listOf(
        KufarMetroStation(MetroLine.BLUE, 3), // Академия наук
        KufarMetroStation(MetroLine.BLUE, 4), // Борисовский тракт
        KufarMetroStation(MetroLine.BLUE, 5), // Восток
        KufarMetroStation(MetroLine.BLUE, 6), // Грушевка
        KufarMetroStation(MetroLine.BLUE, 7),  // Институт культуры
        KufarMetroStation(MetroLine.BLUE, 10),  // Малиновка
        KufarMetroStation(MetroLine.BLUE, 11),  // Михалово
        KufarMetroStation(MetroLine.BLUE, 14),  // Московская
        KufarMetroStation(MetroLine.BLUE, 16),  // Купаловская
        KufarMetroStation(MetroLine.BLUE, 17),  // Парк челюскинцев
        KufarMetroStation(MetroLine.BLUE, 20),  // Петровщина
        KufarMetroStation(MetroLine.BLUE, 21),  // Площадь ленина
        KufarMetroStation(MetroLine.BLUE, 22),  // Площадь победы
        KufarMetroStation(MetroLine.BLUE, 23),  // Площадь якуба коласа
        KufarMetroStation(MetroLine.BLUE, 28),  // Уручье
    )

    // Зелёная ветка (Зеленолужская линия)
    internal val GREEN_LINE = listOf(
        KufarMetroStation(MetroLine.GREEN, 36), // Аэродромная
        KufarMetroStation(MetroLine.GREEN, 32), // Ковальская слобода
        KufarMetroStation(MetroLine.GREEN, 35), // Наморшанский сад
        KufarMetroStation(MetroLine.GREEN, 21), // Вокзальная
        KufarMetroStation(MetroLine.GREEN, 33), // площадь ф богушевича
        KufarMetroStation(MetroLine.GREEN, 34), // Случкий гостинец
        KufarMetroStation(MetroLine.GREEN, 29), // Фрунзенская юбилейная
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