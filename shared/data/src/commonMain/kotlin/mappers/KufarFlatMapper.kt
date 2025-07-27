// KufarFlatMapper.kt
package mappers

import AppFlat
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.*
import server_response.KufarListResponse
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KufarFlatMapper : ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat> {

    // Маппинг ID к названиям удобств/оборудования
    private val kitchenMapping = mapOf(
        2 to "Микроволновка",
        3 to "Стиральная машина",
        7 to "Плита"
    )

    private val improvementMapping = mapOf(
        1 to "Wi-Fi",
        3 to "Стиральная машина",
        4 to "Кондиционер",
        6 to "Мебель"
    )

    private val windowDirectionMapping = mapOf(
        "1" to "Во двор",
        "2" to "На речку",
        "3" to "В парк",
        "4" to "На улицу",
        "5" to "Юг",
        "8" to "Запад"
    )

    private val buildingImprovementMapping = mapOf(
        "1" to "Лифт",
        "2" to "Пандус",
        "3" to "Мусоропровод",
        "6" to "Стояночное место",
        "7" to "Домофон",
        "8" to "Видеонаблюдение"
    )

    private val bathroomMapping = mapOf(
        "1" to "Раздельный",
        "2" to "Совмещенный"
    )

    private val balconyMapping = mapOf(
        "1" to "Есть",
        "2" to "Нет",
        "3" to "Лоджия"
    )

    private val repairMapping = mapOf(
        "1" to "Косметический",
        "5" to "Евро"
    )

    private val prepaymentMapping = mapOf(
        "5" to "Месяц",
        "10" to "2 месяца",
        "25" to "Залог"
    )

    override fun map(data: KufarListResponse.Ad): AppFlat {
        val priceUsd = data.priceUsd?.let { convertKufarPriceToDouble(it) }
        val priceByn = data.priceByn?.let { convertKufarPriceToDouble(it) }

        val adParams = data.adParameters?.filterNotNull() ?: emptyList()

        // Основные параметры
        val rooms = adParams.findParamValue("rooms")?.safeInt()
        val district = adParams.findParamStringValue("re_district")?.safeString()
        val address = data.accountParameters
            ?.filterNotNull()
            ?.findParamStringValue("address")?.safeString()

        val coordinates = adParams.findParamValue("coordinates")
            ?.safeDoubleList()
            ?.takeIf { it.size >= 2 }
            ?.let { it[1] to it[0] }

        val metroStation = adParams.findParamStringValue("metro")
            ?.safeStringList()
            ?.firstOrNull()

        val yearBuilt = adParams.findParamValue("year_built")?.safeInt()
        val totalArea = adParams.findParamValue("size")?.safeDouble()
        val floor = adParams.findParamValue("floor")?.safeIntList()?.firstOrNull()
        val totalFloors = adParams.findParamValue("re_number_floors")?.safeInt()
        val sleepingPlaces = adParams.findParamValue("flat_rent_couchettes")?.safeInt()
        val isStudio = adParams.findParamValue("studio")?.safeBool()

        // Параметры с маппингом
        val bathroomType = adParams.findParamValue("bathroom")?.safeString()
            ?.let { bathroomMapping[it] }

        val balcony = adParams.findParamValue("balcony")?.safeString()
            ?.let { balconyMapping[it] }

        val repairType = adParams.findParamValue("flat_repair")?.safeString()
            ?.let { repairMapping[it] }

        val condition = adParams.findParamStringValue("condition")?.safeString()
            ?.let { if (it == "1") "Вторичное" else "Новостройка" }

        val windowDirections = adParams.findParamValue("flat_windows_side")
            ?.safeIntList()
            ?.mapNotNull { windowDirectionMapping[it.toString()] }

        val buildingImprovements = adParams.findParamValue("flat_building_improvements")
            ?.safeIntList()
            ?.mapNotNull { buildingImprovementMapping[it.toString()] }

        val prepaymentType = adParams.findParamValue("flat_rent_prepayment")?.safeString()
            ?.let { prepaymentMapping[it] }

        // Удобства и оборудование
        val kitchenIds = adParams.findParamValue("flat_kitchen")?.safeIntList() ?: emptyList()
        val improvementsIds = adParams.findParamValue("flat_improvement")?.safeIntList() ?: emptyList()

        val kitchenEquipment = mutableListOf<String>()
        val amenities = mutableListOf<String>()

        // Добавляем кухонное оборудование
        kitchenIds.forEach { id ->
            kitchenMapping[id]?.let { kitchenEquipment.add(it) }
        }

        // Добавляем общие удобства
        improvementsIds.forEach { id ->
            when (id) {
                3 -> if (!kitchenEquipment.contains("Стиральная машина")) {
                    amenities.add("Стиральная машина")
                }
                else -> improvementMapping[id]?.let { amenities.add(it) }
            }
        }

        // Для кого сдается
        val forWhom = adParams.findParamStringValue("flat_rent_for_whom")?.safeStringList()

        // Изображения
        val images = data.images?.mapNotNull { image ->
            image?.path?.let { "https://rms.kufar.by/v1/gallery/$it" }
        }

        return AppFlat(
            flatPlatform = FlatPlatform.KUFAR,
            flatDetailUrl = data.adLink.orEmpty(),
            adId = data.adId ?: -1,
            publishedAt = null,
            timeAgo = null,
            priceUsd = priceUsd,
            priceByn = priceByn,
            imageUrls = images,
            rooms = rooms,
            district = district,
            address = address,
            coordinates = coordinates,
            metroStation = metroStation,
            description = data.bodyShort,
            yearBuilt = yearBuilt,
            totalArea = totalArea,
            livingArea = null, // Kufar не предоставляет
            kitchenArea = null, // Kufar не предоставляет
            floor = floor,
            totalFloors = totalFloors,
            sleepingPlaces = sleepingPlaces,
            isStudio = isStudio,
            bathroomType = bathroomType,
            balcony = balcony,
            repairType = repairType,
            condition = condition,
            windowDirections = windowDirections,
            buildingImprovements = buildingImprovements,
            prepaymentType = prepaymentType,
            amenities = amenities.takeIf { it.isNotEmpty() },
            kitchenEquipment = kitchenEquipment.takeIf { it.isNotEmpty() },
            forWhom = forWhom,
            parkingInfo = if (buildingImprovements?.contains("Стояночное место") == true)
                "Есть парковочное место" else null,
            owner = data.companyAd?.let { !it } // Если не компания, то собственник
        )
    }

    private fun convertKufarPriceToDouble(kufarPrice: String): Double? {
        return try {
            val priceIntegerStr = kufarPrice.substring(0, kufarPrice.length - 2)
            val priceFractionalStr = kufarPrice.substring(kufarPrice.length - 2)
            "$priceIntegerStr.$priceFractionalStr".toDouble()
        } catch (e: Exception) {
            null
        }
    }

    // Вспомогательные функции
    private fun List<KufarListResponse.Ad.AdParameter>.findParamValue(paramName: String): JsonElement? =
        firstOrNull { it.p == paramName }?.v

    private fun List<KufarListResponse.Ad.AdParameter>.findParamStringValue(paramName: String): JsonElement? =
        firstOrNull { it.p == paramName }?.vl

    // Безопасные расширения
    private fun JsonElement?.safeString(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement?.safeInt(): Int? =
        (this as? JsonPrimitive)?.intOrNull

    private fun JsonElement?.safeDouble(): Double? =
        (this as? JsonPrimitive)?.doubleOrNull

    private fun JsonElement?.safeBool(): Boolean? =
        (this as? JsonPrimitive)?.booleanOrNull

    private fun JsonElement?.safeDoubleList(): List<Double>? =
        (this as? JsonArray)?.mapNotNull { it.safeDouble() }

    private fun JsonElement?.safeStringList(): List<String>? =
        (this as? JsonArray)?.mapNotNull { it.safeString() }
            ?: this.safeString()?.let { listOf(it) }

    private fun JsonElement?.safeIntList(): List<Int> =
        (this as? JsonArray)?.mapNotNull { it.safeInt() } ?: emptyList()
}