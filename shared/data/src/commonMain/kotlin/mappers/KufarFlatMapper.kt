package mappers

import AdditionalParams
import AppFlat
import entities.*
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import server_response.KufarListResponse
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KufarFlatMapper : ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat> {

    init {
        println("KufarFlatMapper initialized")
    }

    override fun map(data: KufarListResponse.Ad): AppFlat {
        val priceUsd = data.priceUsd?.toIntOrNull() ?: 0
        val priceByn = data.priceByn?.toIntOrNull() ?: 0

        val adParams = data.adParameters?.filterNotNull() ?: emptyList()

        val rooms = adParams.findParamValue("rooms")?.safeInt() ?: 0
        val district = adParams.findParamStringValue("re_district").safeString()
        val address = data.accountParameters
            ?.filterNotNull()
            ?.findParamStringValue("address").safeString()

        val coordinates = adParams.findParamValue("coordinates")
            ?.safeDoubleList()
            ?.takeIf { it.size >= 2 }
            ?.let { it[1] to it[0] }

        val metroStation = adParams.findParamStringValue("metro")
            ?.safeStringList()
            ?.firstOrNull()

        val yearBuilt = adParams.findParamValue("year_built")?.safeInt()

        // Новые поля
        val totalArea = adParams.findParamValue("size")?.safeDouble()
        val floor = adParams.findParamValue("floor")?.safeIntList()?.firstOrNull()
        val totalFloors = adParams.findParamValue("re_number_floors")?.safeInt()
        val sleepingPlaces = adParams.findParamValue("flat_rent_couchettes")?.safeInt()
        val isStudio = adParams.findParamValue("studio")?.safeBool() ?: false

        val bathroomType = adParams.findParamValue("bathroom")?.safeString()
            ?.let { BathroomType.values().find { type -> type.value == it } }

        val balconyType = adParams.findParamValue("balcony")?.safeString()
            ?.let { BalconyType.values().find { type -> type.value == it } }

        val repairType = adParams.findParamValue("flat_repair")?.safeString()
            ?.let { RepairType.values().find { type -> type.value == it } }

        val windowDirections = adParams.findParamValue("flat_windows_side")
            ?.safeIntList()
            ?.mapNotNull { windowId ->
                WindowDirection.values().find { it.value == windowId.toString() }
            }

        val buildingImprovements = adParams.findParamValue("flat_building_improvements")
            ?.safeIntList()
            ?.mapNotNull { improvementId ->
                BuildingImprovement.values().find { it.value == improvementId.toString() }
            }

        val prepaymentType = adParams.findParamValue("flat_rent_prepayment")?.safeString()
            ?.let { PrepaymentType.values().find { type -> type.value == it } }

        // Существующая логика для кухни и улучшений
        val kitchenIds = adParams.findParamValue("flat_kitchen")?.safeIntList() ?: emptyList()
        val improvementsIds = adParams.findParamValue("flat_improvement")?.safeIntList() ?: emptyList()

        val additionalParams = AdditionalParams(
            forWhom = adParams.findParamStringValue("flat_rent_for_whom")?.safeStringList(),
            hasWashingMachine = improvementsIds.contains(3) || kitchenIds.contains(3),
            hasStove = kitchenIds.contains(7),
            hasMicrowave = kitchenIds.contains(2),
            hasWifi = improvementsIds.contains(1),
            hasFurniture = improvementsIds.contains(6),
            hasConditioner = improvementsIds.contains(4)
        )

        val images = data.images?.map { "https://rms.kufar.by/v1/gallery/${it?.path}" }

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
            description = data.bodyShort ?: "",
            yearBuilt = yearBuilt,
            additionalParams = additionalParams,
            // Новые поля
            totalArea = totalArea,
            floor = floor,
            totalFloors = totalFloors,
            sleepingPlaces = sleepingPlaces,
            isStudio = isStudio,
            bathroomType = bathroomType,
            balconyType = balconyType,
            repairType = repairType,
            windowDirections = windowDirections,
            buildingImprovements = buildingImprovements,
            prepaymentType = prepaymentType
        )
    }

    // === Вспомогательные функции ===

    private fun List<KufarListResponse.Ad.AdParameter>.findParamValue(paramName: String): JsonElement? =
        firstOrNull { it.p == paramName }?.v

    private fun List<KufarListResponse.Ad.AdParameter>.findParamStringValue(paramName: String): JsonElement? =
        firstOrNull { it.p == paramName }?.vl

    // === Безопасные расширения ===

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