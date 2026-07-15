package mappers.onliner

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import mappers.base.AdditionalParamMapper
import metro.MetroProximityEnricher
import kotlin.time.ExperimentalTime

class OnlinerDetailHtmlMapper : AdditionalParamMapper<String, AppFlat> {

    @OptIn(ExperimentalTime::class)
    override fun map(baseFlat: AppFlat, html: String): AppFlat {
        if (html.isBlank()) return MetroProximityEnricher.enrich(baseFlat)
        val doc = Ksoup.parse(html)

        // Парсим количество комнат
        val roomsText = doc.select(".apartment-bar__value")
            .firstOrNull { it.text().contains("комнатная") }?.text()
        val rooms = extractRoomsFromText(roomsText)

        // Парсим адрес и описание
        val address = doc.select(".apartment-info__sub-line_large").firstOrNull()?.text()
        val description = doc.select(".apartment-info__sub-line_extended-bottom")
            .firstOrNull()?.text()

        // Парсим координаты из JavaScript
        val coordinates = extractCoordinatesFromScript(doc)

        // ИСПРАВЛЕННЫЙ парсинг удобств
        val allAmenities = doc.select(".apartment-options__item").map { element ->
            val text = element.text()
            val isAbsent = element.hasClass("apartment-options__item_lack")
            text to !isAbsent
        }

        // Разделяем на присутствующие удобства и кухонное оборудование
        val presentAmenities = allAmenities.filter { it.second }.map { it.first }
        val (kitchenEquipment, generalAmenities) = presentAmenities.partition {
            it in listOf(
                "Плита", "Холодильник", "Микроволновка", "Посудомоечная машина",
                "Кухонная мебель", "Духовка"
            )
        }

        // Парсим дополнительные параметры из таблицы
        val tableParams = parseParametersTable(doc)

//        // Парсим владельца
//        val owner = doc.select(".apartment-bar__value")
//            .any { it.text() == "Собственник" }

        // Парсим изображения
        val images = doc.select(".apartment-gallery__slide")
            .mapNotNull { element ->
                element.attr("style")
                    .substringAfter("url(")
                    .substringBefore(")")
                    .takeIf { it.isNotBlank() }
            }
        val updatedFlat = baseFlat.copy(
            flatPlatform = FlatPlatform.ONLINER,
            flatDevInfo = FlatDevInfo(
                isDetailData = true,
                isDetailLoaded = true
            ),
            flatDetailUrl = baseFlat.flatDetailUrl,
            adId = baseFlat.adId,
            publishedAt = baseFlat.publishedAt,
            publishedAtServer = baseFlat.publishedAtServer,
            publishedAtUi = baseFlat.publishedAtUi,
            imageUrls = images,
            rooms = rooms,
            district = tableParams["Район"],
            address = address,
            coordinates = coordinates ?: baseFlat.coordinates,
            metroStation = tableParams["Метро"] ?: baseFlat.metroStation,
            description = description,
            yearBuilt = tableParams["Год постройки"]?.toIntOrNull(),
            totalArea = baseFlat.totalArea ?: tableParams["Общая площадь"]?.replace("м²", "")
                ?.trim()?.toDoubleOrNull(),
            livingArea = baseFlat.livingArea ?: tableParams["Жилая площадь"]?.replace("м²", "")
                ?.trim()?.toDoubleOrNull(),
            kitchenArea = baseFlat.kitchenArea ?: tableParams["Площадь кухни"]?.replace("м²", "")
                ?.trim()?.toDoubleOrNull(),
            floor = tableParams["Этаж"]?.substringBefore("/")?.toIntOrNull(),
            totalFloors = tableParams["Этаж"]?.substringAfter("/")?.toIntOrNull(),
            sleepingPlaces = tableParams["Спальных мест"]?.toIntOrNull(),
            isStudio = roomsText?.contains("студия", ignoreCase = true),
            bathroomType = tableParams["Санузел"],
            balcony = if (presentAmenities.any {
                    it.contains("балкон", ignoreCase = true) ||
                            it.contains("лоджия", ignoreCase = true)
                })
                presentAmenities.firstOrNull {
                    it.contains("балкон", ignoreCase = true) ||
                            it.contains("лоджия", ignoreCase = true)
                }
            else "Нет",
            repairType = tableParams["Ремонт"],
            condition = tableParams["Тип дома"],
            windowDirections = null, // Onliner не предоставляет эту информацию в HTML
            buildingImprovements = parseTableListParam(tableParams["Дом"]),
            prepaymentType = tableParams["Предоплата"],
            amenities = generalAmenities,
            kitchenEquipment = kitchenEquipment,
            forWhom = parseTableListParam(tableParams["Кому сдается"]),
            parkingInfo = tableParams["Парковка"],
            owner = baseFlat.owner,
            contactInformation = ContactInformation(
                phones = listOfNotNull(extractRawPhoneNumber(doc)),
                ownerName = extractOwnerName(doc)
            )
        )
        val flatWithMetro = MetroProximityEnricher.enrich(
            updatedFlat
        )

        return flatWithMetro
    }

    private fun extractRoomsFromText(text: String?): Int? {
        return when {
            text == null -> null
            text.contains("студия", ignoreCase = true) -> 0
            else -> Regex("(\\d+)-комнатная").find(text)?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    private fun extractRawPhoneNumber(doc: Document): String? {
        val phoneLink = doc.selectFirst("a[href^='tel:']")
        return phoneLink?.attr("href")?.removePrefix("tel:")
    }

    private fun extractOwnerName(doc: Document): String? {
        return doc.select("a[href^='https://profile.onliner.by/user/']")
            .firstOrNull()
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun extractCoordinatesFromScript(doc: Document): Coordinates? {
        val scripts = doc.select("script").map { it.html() }
        for (script in scripts) {
            val latMatch = Regex("latitude\\s*=\\s*([\\d.]+)").find(script)
            val lonMatch = Regex("longitude\\s*=\\s*([\\d.]+)").find(script)

            if (latMatch != null && lonMatch != null) {
                val lat = latMatch.groupValues[1].toDoubleOrNull()
                val lon = lonMatch.groupValues[1].toDoubleOrNull()
                if (lat != null && lon != null) {
                    return Coordinates(lat, lon)
                }
            }
        }
        return null
    }

    private fun parseParametersTable(doc: Document): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // Ищем таблицу с параметрами квартиры
        doc.select(".apartment-info__item").forEach { item ->
            val label = item.select(".apartment-info__item-label").text()
            val value = item.select(".apartment-info__item-value").text()
            if (label.isNotBlank() && value.isNotBlank()) {
                params[label] = value
            }
        }

        return params
    }

    private fun parseTableListParam(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
    }
}