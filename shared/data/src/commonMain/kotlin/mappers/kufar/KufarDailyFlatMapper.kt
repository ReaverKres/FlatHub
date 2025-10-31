package mappers.kufar

import entities.AppFlat
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.TimeZone
import mappers.base.ResponseToEntitiesFlatMapper
import server_response.kufar.KufarDailyListResponse

class KufarDailyFlatMapper :
    ResponseToEntitiesFlatMapper<KufarDailyListResponse.RentalObject, AppFlat> {

    override fun map(rentalObject: KufarDailyListResponse.RentalObject): AppFlat {
        val metroStations = rentalObject.metroStationNames?.map { it?.ru }?.let {
            if (it.isNotEmpty()) {
                it.joinToString(separator = ", ")
            } else {
                null
            }
        }
        val cityName = rentalObject.appCity ?: "minsk"
        val detailUrl = rentalObject.selfUrl.takeIf { it.isNullOrEmpty().not() }
            ?: "https://travel.kufar.by/vi/$cityName/${rentalObject.adId}"

        return AppFlat(
            adId = ((rentalObject.adId ?: -1) + rentalObject.currentPage).toLong(),
            flatDevInfo = FlatDevInfo(
                isDetailData = true,
                isDetailLoaded = true
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
            commercialInfo = null,
            savedInFavorites = false,
            isViewed = false,
            flatPlatform = FlatPlatform.KUFAR,
            flatDetailUrl = detailUrl,
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
            priceByn = rentalObject.price?.toDouble()?.div(100),
            priceUsdSquare = null,
            priceBynSquare = null,
            rooms = rentalObject.rooms,
            district = null, //area?,
            address = rentalObject.address,
            metroStation = metroStations,
            description = rentalObject.adSnapshot?.body,
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
            owner = null
//            owner = rentalObject.isSuperhost // Если не суперхост, вероятно собственник
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