package mappers

import entities.AppFlat
import entities.ContactInformation
import entities.Coordinates
import entities.FlatDevInfo
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.date.DateConverter
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import mappers.base.ResponseToEntitiesFlatMapper
import server_response.DomovitaListResponse

class DomovitaFlatMapper :
    ResponseToEntitiesFlatMapper<DomovitaListResponse.DomovitaFlat, AppFlat> {

    override fun map(data: DomovitaListResponse.DomovitaFlat): AppFlat {

        val coordinates = data.address?.coords?.let {
            Coordinates(
                latitude = it.positionX,
                longitude = it.positionY
            )
        }

        val address = "${data.address?.streetName}, ${data.address?.houseNumber}"
        val district = data.address?.district?.name

        // Извлекаем изображения
        val imageUrls = data.photos?.map { it?.url }

        // Обрабатываем описание (объединяем description и ads_text)
        val description = listOfNotNull(data.description, data.adsText)
            .joinToString("\n")
            .takeIf { it.isNotBlank() }

        // Определяем тип собственника
        val isFromOwner = data.agency == null

        // Контактная информация
        val contactInfo = data.contacts?.let {
            ContactInformation(
                phones = it.phones?.takeIf { phones -> phones.isNotEmpty() }?.filterNotNull(),
                ownerName = it.contactName
            )
        }

        // Обрабатываем дату
        val dateReceptionInstant = data.dateReception?.let {
            DateConverter.parseDomovitaDate(it)
        }
        val dateRevisionInstant = data.dateRevision?.let {
            DateConverter.parseDomovitaDate(it)
        } ?: Clock.System.now() // fallback на текущее время если dateRevision null

        // Форматируем дату с учетом условия
        val publishedAtUi = DateConverter.formatInstantWithDomovitaDates(
            dateReceptionInstant,
            dateRevisionInstant,
            TimeZone.currentSystemDefault()
        )
        return AppFlat(
            flatPlatform = FlatPlatform.DOMOVITA,
            flatDevInfo = FlatDevInfo(
                isDetailData = true,
                isDetailLoaded = true
            ),
            flatDetailUrl = data.url.orEmpty(),
            adId = data.id?.toLong() ?: -1,
            publishedAt = dateRevisionInstant,
            publishedAtServer = data.dateRevision,
            publishedAtUi = publishedAtUi,
            priceUsd = data.price?.uSD,
            priceByn = data.price?.bYN,
            imageUrls = imageUrls.takeIf { it?.isNotEmpty() == true }?.filterNotNull(),
            rooms = data.rooms,
            district = district,
            address = address,
            coordinates = coordinates,
            metroStation = data.address?.metro?.name,
            description = description,
            yearBuilt = data.buildingYear,
            totalArea = data.area?.total,
            livingArea = data.area?.living,
            kitchenArea = data.area?.kitchen,
            floor = data.storey,
            totalFloors = data.storeys,
            sleepingPlaces = null, // Domovita не предоставляет
            isStudio = data.rooms == 0, // Студия если 0 комнат
            bathroomType = null,
            balcony = null,
            repairType = null,
            condition = determineCondition(data.buildingYear),
            windowDirections = null,
            buildingImprovements = null,
            prepaymentType = null,
            amenities = null,
            kitchenEquipment = null,
            forWhom = null,
            parkingInfo = null,
            owner = isFromOwner,
            contactInformation = contactInfo
        )
    }

    private fun determineCondition(buildingYear: Int?): String? {
        return when {
            buildingYear == null -> null
            buildingYear >= 2010 -> "Новостройка"
            else -> "Вторичное"
        }
    }
}