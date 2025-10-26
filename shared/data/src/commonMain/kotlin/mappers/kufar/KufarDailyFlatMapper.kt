package mappers.kufar

import entities.AppFlat
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import mappers.base.ResponseToEntitiesFlatMapper
import server_response.kufar.KufarDailyListResponse

class KufarDailyFlatMapper: ResponseToEntitiesFlatMapper<KufarDailyListResponse.RentalObject, AppFlat> {

    override fun map(rentalObject: KufarDailyListResponse.RentalObject): AppFlat {
        return AppFlat(
            adId = rentalObject.adId?.toLong() ?: -1L,
            flatDevInfo = FlatDevInfo(
                isDetailData = false,
                isDetailLoaded = false
            ),
            contactInformation = null, // Kufar Daily не предоставляет контактную информацию
            coordinates = rentalObject.coordinates?.let { coords ->
                if (coords.size >= 2) {
                    Coordinates(
                        latitude = coords[1] ?: 0.0,
                        longitude = coords[0] ?: 0.0
                    )
                } else null
            },
            commercialInfo = null, // Для жилой недвижимости
            savedInFavorites = false,
            isViewed = false,
            flatPlatform = FlatPlatform.KUFAR,
            flatDetailUrl = rentalObject.selfUrl ?: "",
            publishedAt = DateConverter.stringToInstant(rentalObject.listTime.orEmpty()),
            publishedAtServer = rentalObject.listTime,
            publishedAtUi = DateConverter.formatInstant(
                DateConverter.stringToInstant(rentalObject.listTime.orEmpty()),
                TimeZone.currentSystemDefault()
            ),
            imageUrls = rentalObject.images?.mapNotNull { image ->
                image?.path?.let { path ->
                    if (image.yamsStorage == true) {
                        // Для YAMS storage
                        "https://yams.kufar.by/api/v1/kufar/$path"
                    } else {
                        // Для RMS storage
                        "https://rms.kufar.by/v1/gallery/$path"
                    }
                }
            },
            priceUsd = null, // Kufar Daily предоставляет только цену в BYN
            priceByn = rentalObject.price?.toDouble(),
            priceUsdSquare = null,
            priceBynSquare = null,
            rooms = rentalObject.rooms,
            district = extractDistrictFromAddress(rentalObject.address),
            address = rentalObject.address,
            metroStation = rentalObject.metroStationNames?.firstOrNull()?.ru,
            description = rentalObject.shortDescription,
            yearBuilt = extractYearFromAdSnapshot(rentalObject.adSnapshot),
            // Основные параметры квартиры
            totalArea = rentalObject.area?.toDouble(),
            livingArea = null, // Kufar Daily не предоставляет
            kitchenArea = null, // Kufar Daily не предоставляет
            floor = rentalObject.floor,
            totalFloors = null,
            sleepingPlaces = null,
            isStudio = rentalObject.adSnapshot?.body?.contains("студия", ignoreCase = true) == true,
            // Параметры ванной и балкона
            bathroomType = null,
            balcony = null,
            // Ремонт и состояние
            repairType = null,
            condition = if (rentalObject.flatNewBuilding == true) "Новостройка" else "Вторичное",
            // Направления окон
            windowDirections = null,
            // Улучшения дома
            buildingImprovements = null,
            // Предоплата
            prepaymentType = null,
            // Удобства и оборудование
            amenities = null,
            kitchenEquipment = null,
            // Дополнительные параметры
            forWhom = null,
            parkingInfo = null,
            owner = rentalObject.isSuperhost // Если не суперхост, вероятно собственник
        )
    }

    // Вспомогательные методы для извлечения данных из описания
    private fun extractDistrictFromAddress(address: String?): String? {
        return address?.substringAfterLast(", ")?.substringBefore(" область")
    }

    private fun extractYearFromAdSnapshot(adSnapshot: KufarDailyListResponse.RentalObject.AdSnapshot?): Int? {
        // Парсим год из описания (пример: "дом 1985 года постройки")
        val yearRegex = """\b(19|20)\d{2}\b""".toRegex()
        return adSnapshot?.body?.let { body ->
            yearRegex.find(body)?.value?.toIntOrNull()
        }
    }
}