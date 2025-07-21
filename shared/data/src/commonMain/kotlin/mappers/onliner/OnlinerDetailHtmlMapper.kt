package mappers.onliner

import AdditionalParams
import AppFlat
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import mappers.AdditionalParamMapper
import kotlin.time.ExperimentalTime

class OnlinerDetailHtmlMapper : AdditionalParamMapper<String, AppFlat> {

    @OptIn(ExperimentalTime::class)
    override fun map(baseFlat: AppFlat, html: String): AppFlat {
        if (html.isBlank()) return baseFlat

        val doc = Ksoup.parse(html)

        // --- Новые, более надежные селекторы ---
        val description = doc.selectFirst(".apartment-info__sub-line_extended-bottom")?.text()
        val amenities = doc.select(".apartment-options__item").map { it.text() } // Берем все, а не только _yes

        // Извлекаем параметры из таблицы
        val parameters = doc.select(".apartment-parameters__item")
        val yearBuilt = extractParameterValue(parameters, "Год постройки")?.toIntOrNull()
        val (floor, totalFloors) = extractFloors(parameters)
        val totalArea = extractParameterValue(parameters, "Общая")?.replace(",", ".")?.toDoubleOrNull()

        // Новый метод для извлечения фото
        val imageUrls = extractImageUrlsFromStyles(doc)

        val additionalParams = AdditionalParams(
            forWhom = null,
            hasWashingMachine = amenities.any { it.contains("Стиральная машина", ignoreCase = true) },
            hasStove = amenities.any { it.contains("Плита", ignoreCase = true) },
            hasMicrowave = amenities.any { it.contains("Микроволновая печь", ignoreCase = true) }, // Добавил микроволновку
            hasWifi = amenities.any { it.contains("Интернет", ignoreCase = true) },
            hasFurniture = amenities.any { it.contains("Мебель", ignoreCase = true) },
            hasConditioner = amenities.any { it.contains("Кондиционер", ignoreCase = true) }
        )

        return baseFlat.copy(
            description = description ?: baseFlat.description,
            imageUrls = if (imageUrls.isNotEmpty()) imageUrls else baseFlat.imageUrls,
            yearBuilt = yearBuilt, // Просто присваиваем, может быть null
            floor = floor,
            totalFloors = totalFloors,
            totalArea = totalArea,
            additionalParams = additionalParams
        )
    }

    // === Обновленные и новые вспомогательные функции ===

    /**
     * Универсальная функция для извлечения значения параметра из списка.
     * Ищет элемент, у которого label соответствует искомому тексту, и возвращает значение.
     */
    private fun extractParameterValue(elements: List<Element>, labelText: String): String? {
        return elements
            .firstOrNull { it.select(".apartment-parameters__label").text().equals(labelText, ignoreCase = true) }
            ?.select(".apartment-parameters__value")?.text()
    }

    /**
     * Извлекает этаж и этажность из списка параметров.
     */
    private fun extractFloors(elements: List<Element>): Pair<Int?, Int?> {
        val floorString = extractParameterValue(elements, "Этаж") ?: return null to null
        val parts = floorString.split("/").map { it.trim().toIntOrNull() }
        return (parts.getOrNull(0)) to (parts.getOrNull(1))
    }

    /**
     * Извлекает год постройки из списка удобств (менее надежный метод, как запасной).
     */
    private fun extractYear(amenities: List<String>): Int? {
        return amenities.firstOrNull { it.contains("года", ignoreCase = true) }
            ?.filter { it.isDigit() }
            ?.toIntOrNull()
    }

    /**
     * НОВЫЙ МЕТОД: Извлекает URL изображений из style-атрибутов превью.
     * Это более надежно, чем поиск JS-переменной.
     */
    private fun extractImageUrlsFromStyles(doc: Document): List<String> {
        val urlRegex = """url\((.*?)\)""".toRegex()
        return doc.select(".apartment-cover__thumbnail")
            .mapNotNull { element ->
                val style = element.attr("style")
                urlRegex.find(style)?.groups?.get(1)?.value
            }
    }
}