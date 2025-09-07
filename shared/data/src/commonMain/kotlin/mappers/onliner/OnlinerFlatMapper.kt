package mappers.onliner

import entities.AppFlat
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import io.flatzen.commoncomponents.date.DateConverter.formatInstant
import kotlinx.datetime.TimeZone
import mappers.base.ResponseToEntitiesFlatMapper
import server_response.OnlinerListResponse
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class OnlinerFlatMapper : ResponseToEntitiesFlatMapper<OnlinerListResponse.Apartment, AppFlat> {

    override fun map(data: OnlinerListResponse.Apartment): AppFlat {
        val priceUsd = data.price?.converted?.uSD?.amount?.toDoubleOrNull()
        val priceByn = data.price?.converted?.bYN?.amount?.toDoubleOrNull()

        val rooms = parseRoomsFromRentType(data.rentType)

        val coordinates = data.location?.let { location ->
            location.latitude?.let { lat ->
                location.longitude?.let { lng ->
                    Coordinates(lat, lng)
                }
            }
        }


        val images = data.photo?.let { listOf(it) }
        val flatDateInstant = DateConverter.stringToInstant(data.lastTimeUp.orEmpty())
        val publishedAtUi = formatInstant(flatDateInstant, TimeZone.currentSystemDefault())

        return AppFlat(
            flatPlatform = FlatPlatform.ONLINER,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = false
            ),
            flatDetailUrl = data.url.orEmpty(),
            adId = data.id?.toLong() ?: -1L,
            publishedAt = flatDateInstant,
            publishedAtServer = data.lastTimeUp,
            publishedAtUi = publishedAtUi,
            priceUsd = priceUsd ?: Double.NaN,
            priceByn = priceByn ?: Double.NaN,
            imageUrls = images,
            rooms = data.numberOfRooms ?: rooms,
            district = null, // Отсутствует в Onliner
            address = data.location?.address,
            coordinates = coordinates,
            metroStation = null, // Отсутствует в Onliner
            description = null, // Отсутствует в Onliner
            yearBuilt = null, // Отсутствует в Onliner
            // Новые поля - отсутствуют в Onliner
            totalArea = data.area?.total,
            livingArea = data.area?.living,
            kitchenArea = data.area?.kitchen,
            floor = null,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = false, // Можно определить по rent_type, но лучше false по умолчанию
            bathroomType = null,
            repairType = null,
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            balcony = null,
            condition = null,
            amenities = null,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = data.contact?.owner,
            contactInformation = null
        )
    }

    // === Вспомогательные функции ===

    private fun parseRoomsFromRentType(rentType: String?): Int {
        return when (rentType) {
            "room" -> 0 // комната в квартире
            "1_room" -> 1
            "2_rooms" -> 2
            "3_rooms" -> 3
            "4_rooms" -> 4
            "5_rooms" -> 5
            "6_rooms" -> 6
            else -> {
                // Попытка извлечь цифру из строки
                rentType?.let {
                    Regex("(\\d+)_room").find(it)?.groupValues?.get(1)?.toIntOrNull()
                } ?: 0
            }
        }
    }

//    private fun parseIsoDateTime(dateTimeString: String): kotlinx.datetime.Instant? {
//        return try {
//            kotlinx.datetime.Instant.parse(dateTimeString)
//        } catch (e: Exception) {
//            null
//        }
//    }
//
//    private fun calculateTimeAgo(publishedAt: kotlinx.datetime.Instant): String? {
//        return try {
//            val now = kotlinx.datetime.Clock.System.now()
//            val diff = now - publishedAt
//
//            when {
//                diff.inWholeDays > 0 -> "${diff.inWholeDays} дн. назад"
//                diff.inWholeHours > 0 -> "${diff.inWholeHours} ч. назад"
//                diff.inWholeMinutes > 0 -> "${diff.inWholeMinutes} мин. назад"
//                else -> "только что"
//            }
//        } catch (e: Exception) {
//            null
//        }
//    }
}