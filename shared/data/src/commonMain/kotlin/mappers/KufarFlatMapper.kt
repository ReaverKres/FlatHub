package mappers

import entities.AdditionalParams
import entities.AppFlat
import server_response.KufarListResponse
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class KufarFlatMapper : ResponseToEntitiesFlatMapper<KufarListResponse.Ad, AppFlat> {

    override fun map(data: KufarListResponse.Ad): AppFlat {
//        val publishedAt = Instant.parse(response.listTime).toKotlinInstant()
//
//        val relativeTime = calculateRelativeTime(publishedAt)

        val priceUsd = data.priceUsd.toIntOrNull() ?: 0
        val priceByn = data.priceByn.toIntOrNull() ?: 0

        val rooms = data.adParameters
            .firstOrNull { it.p == "rooms" }
            ?.v
            ?.toInt() ?: 0

        val district = data.adParameters
            .firstOrNull { it.p == "district" }
            ?.vl
            .orEmpty()

        val address = data.adParameters
            .firstOrNull { it.p == "address" }
            ?.vl
            .orEmpty()

        val coordinates = data.adParameters
            .firstOrNull { it.p == "coordinates" }
            ?.vl
            ?.split(",")
            ?.let { (lat, lon) -> lat.trim().toDouble() to lon.trim().toDouble() }

        val metroStation = data.adParameters
            .firstOrNull { it.p == "metro" }
            ?.vl

        val yearBuilt = data.adParameters
            .firstOrNull { it.p == "year_built" }
            ?.v
            ?.toInt()

        val additionalParams = AdditionalParams(
            forWhom = data.adParameters
                .firstOrNull { it.p == "for_whom" }
                ?.vl
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() },
            hasWashingMachine = data.adParameters.any { it.p == "washing_machine" && it.vl == "1" },
            hasStove = data.adParameters.any { it.p == "stove" && it.vl == "1" },
            hasMicrowave = data.adParameters.any { it.p == "microwave" && it.vl == "1" },
            hasWifi = data.adParameters.any { it.p == "wifi" && it.vl == "1" },
            hasFurniture = data.adParameters.any { it.p == "furniture" && it.vl == "1" },
            hasConditioner = data.adParameters.any { it.p == "conditioner" && it.vl == "1" }
        )

        return AppFlat(
            publishedAt = null,
            timeAgo = null,
            priceUsd = priceUsd,
            priceByn = priceByn,
            rooms = rooms,
            district = district,
            address = address,
            coordinates = coordinates,
            metroStation = metroStation,
            description = data.body,
            yearBuilt = yearBuilt,
            additionalParams = additionalParams
        )
    }

//    private fun calculateRelativeTime(instant: kotlin.time.Instant): RelativeTime {
//        val now = Clock.System.now()
//        val days = now.daysUntil(instant, TimeZone.currentSystemDefault())
//
//        return when {
//            days == 0 -> RelativeTime.TODAY
//            days == -1 -> RelativeTime.YESTERDAY
//            days > -7 -> RelativeTime.DAYS_AGO
//            days > -30 -> RelativeTime.WEEKS_AGO
//            else -> RelativeTime.MONTHS_AGO
//        }
//    }
}