package mappers.kufar

import io.flatzen.commoncomponents.commonentities.CommercialPropertyType

object KufarPropertyTypes {
    fun asParam(type: CommercialPropertyType): String? = when (type) {
        is CommercialPropertyType.Office -> "1"
        is CommercialPropertyType.Retail -> "2"
        is CommercialPropertyType.Services -> "10"
        is CommercialPropertyType.Industrial -> "3"
        is CommercialPropertyType.Warehouses -> "4"
        CommercialPropertyType.Other -> "6"
        else -> null
    }

    fun numberToPropertyType(number: Int): CommercialPropertyType? = when (number) {
        1 -> CommercialPropertyType.Office
        2 -> CommercialPropertyType.Retail
        10 -> CommercialPropertyType.Services
        3 -> CommercialPropertyType.Industrial
        4 -> CommercialPropertyType.Warehouses
        6 -> CommercialPropertyType.Other
        else -> null
    }
}