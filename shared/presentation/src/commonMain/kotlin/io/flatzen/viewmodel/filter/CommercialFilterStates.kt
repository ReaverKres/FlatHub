package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FromToRange

@Immutable
data class CommercialFilters(
    val roomRange: FromToRange? = null,
    val commercialPropertyType: List<CommercialPropertyTypeInfo>? = null,
)

@Immutable
data class CommercialPropertyTypeInfo(
    val selected: Boolean = false,
    val commercialPropertyType: CommercialPropertyType? = null,
    val commercialPropertyTypeName: String? = null
) {
    //1 - Office
//2 - Retail premises
//10 - Services
//6 - Other commercial
//3 - Industrial premises
//4 - Warehouses

//1 - офис
//2 - торговые помещения
//10 - сфера услуг
//6 - Прочая коммерческая
//3 промышленные помещения
//4 склады

    companion object {
        fun commercialPropertyTypeName(commercialPropertyType: CommercialPropertyType?): String? {
            return when (commercialPropertyType) {
                CommercialPropertyType.All -> "Все"
                CommercialPropertyType.Industrial -> "Промышленные помещения"
                CommercialPropertyType.Office -> "Офис"
                CommercialPropertyType.Retail -> "Торговые помещения"
                CommercialPropertyType.Services -> "Сфера услуг"
                CommercialPropertyType.Warehouses -> "Склады"
                CommercialPropertyType.Other -> "Прочая коммерческая"
                null -> null
            }
        }
    }
}