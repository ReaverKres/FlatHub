package mappers

import entities.AdditionalParams
import entities.AppFlat
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import server_response.KufarListResponse
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class KufarFlatMapper : ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat> {

    override fun map(data: KufarListResponse.Ad): AppFlat {
        val priceUsd = data.priceUsd?.toIntOrNull() ?: 0
        val priceByn = data.priceByn?.toIntOrNull() ?: 0

        val adParams = data.adParameters?.filterNotNull() ?: emptyList()

        val rooms = adParams
            .firstOrNull { it.p == "rooms" }
            ?.v
            ?.safeInt() ?: 0

        val district = adParams
            .firstOrNull { it.p == "re_district" }
            ?.vl
            ?.safeString() ?: ""

        val address = data.accountParameters
            ?.filterNotNull()
            ?.firstOrNull { it.p == "address" }
            ?.vl
            ?.safeString() ?: ""

        val coordinates = adParams
            .firstOrNull { it.p == "coordinates" }
            ?.v
            ?.safeDoubleList()
            ?.takeIf { it.size >= 2 }
            ?.let { it[1] to it[0] } // lat, lon

        val metroStation = adParams
            .firstOrNull { it.p == "metro" }
            ?.vl
            ?.safeStringList()
            ?.firstOrNull()

        val yearBuilt = adParams
            .firstOrNull { it.p == "year_built" }
            ?.v
            ?.safeInt()

        val kitchenIds = adParams
            .firstOrNull { it.p == "flat_kitchen" }
            ?.v
            ?.safeIntList()
            ?: emptyList()

        val improvementsIds = adParams
            .firstOrNull { it.p == "flat_improvement" }
            ?.v
            ?.safeIntList()
            ?: emptyList()

        val additionalParams = AdditionalParams(
            forWhom = adParams
                .firstOrNull { it.p == "for_whom" }
                ?.vl
                ?.safeStringList(),

            hasWashingMachine = improvementsIds.contains(3) || kitchenIds.contains(3),
            hasStove = kitchenIds.contains(7),
            hasMicrowave = kitchenIds.contains(2),
            hasWifi = improvementsIds.contains(1),
            hasFurniture = improvementsIds.contains(6),
            hasConditioner = false // пока нет в API
        )
        val images = data.images?.map { "https://rms.kufar.by/v1/gallery/${it?.path }" }

        return AppFlat(
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
            additionalParams = additionalParams
        )
    }

    // === Безопасные расширения ===

    private fun JsonElement?.safeString(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement?.safeInt(): Int? =
        (this as? JsonPrimitive)?.intOrNull

    private fun JsonElement?.safeDoubleList(): List<Double>? =
        (this as? JsonArray)?.mapNotNull { it.safeDouble() }

    private fun JsonElement?.safeStringList(): List<String>? =
        (this as? JsonArray)?.mapNotNull { it.safeString() }
            ?: this.safeString()?.let { listOf(it) }

    private fun JsonElement?.safeDouble(): Double? =
        (this as? JsonPrimitive)?.doubleOrNull

    private fun JsonElement?.safeIntList(): List<Int> =
        (this as? JsonArray)?.mapNotNull { it.safeInt() } ?: emptyList()
}