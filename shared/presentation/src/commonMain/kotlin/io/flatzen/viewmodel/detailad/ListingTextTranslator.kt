package io.flatzen.viewmodel.detailad

import kotlinx.collections.immutable.toImmutableList

/**
 * Packs/unpacks listing free-text fields for a single batch translate call.
 */
internal object ListingTextTranslator {

    fun collect(flat: UiDetailFlat): List<String> {
        val texts = mutableListOf<String>()
        texts += flat.description
        texts += flat.address
        texts += flat.district.orEmpty()
        texts += flat.metroStation.orEmpty()
        texts += flat.bathroomType.orEmpty()
        texts += flat.balcony.orEmpty()
        texts += flat.repairType.orEmpty()
        texts += flat.condition.orEmpty()
        texts += flat.prepaymentType.orEmpty()
        texts += flat.parkingInfo.orEmpty()
        texts += flat.contactInformation.ownerName.orEmpty()
        texts += flat.windowDirection
        texts += flat.buildingImprovements
        texts += flat.amenities
        texts += flat.kitchenEquipment
        texts += flat.forWhom.orEmpty()
        return texts
    }

    fun apply(flat: UiDetailFlat, translated: List<String>): UiDetailFlat {
        require(translated.size == collect(flat).size) {
            "Translated size mismatch: ${translated.size} vs ${collect(flat).size}"
        }
        var i = 0
        fun next(): String = translated[i++]

        val description = next()
        val address = next()
        val district = next().ifBlank { null }
        val metroStation = next().ifBlank { null }
        val bathroomType = next().ifBlank { null }
        val balcony = next().ifBlank { null }
        val repairType = next().ifBlank { null }
        val condition = next().ifBlank { null }
        val prepaymentType = next().ifBlank { null }
        val parkingInfo = next().ifBlank { null }
        val ownerName = next().ifBlank { null }

        val windowDirection = flat.windowDirection.map { next() }.toImmutableList()
        val buildingImprovements = flat.buildingImprovements.map { next() }.toImmutableList()
        val amenities = flat.amenities.map { next() }.toImmutableList()
        val kitchenEquipment = flat.kitchenEquipment.map { next() }.toImmutableList()
        val forWhom = flat.forWhom?.map { next() }?.toImmutableList()

        return flat.copy(
            description = description,
            address = address,
            district = district,
            metroStation = metroStation,
            bathroomType = bathroomType,
            balcony = balcony,
            repairType = repairType,
            condition = condition,
            prepaymentType = prepaymentType,
            parkingInfo = parkingInfo,
            contactInformation = flat.contactInformation.copy(ownerName = ownerName),
            windowDirection = windowDirection,
            buildingImprovements = buildingImprovements,
            amenities = amenities,
            kitchenEquipment = kitchenEquipment,
            forWhom = forWhom,
        )
    }
}
