package io.flatzen.viewmodel.filter

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.localization.LocalizationKeys

@Immutable
data class CommercialFilters(
    val roomRange: FromToRange? = null,
    val commercialPropertyType: List<CommercialPropertyTypeInfo>? = null,
)

@Immutable
data class CommercialPropertyTypeInfo(
    val selected: Boolean = false,
    val commercialPropertyType: CommercialPropertyType? = null,
    val commercialPropertyTypeName: LocalizationKeys? = null
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
        fun commercialPropertyTypeName(commercialPropertyType: CommercialPropertyType?): LocalizationKeys? {
            return when (commercialPropertyType) {
                CommercialPropertyType.All -> LocalizationKeys.COMMERCIAL_PROPERTY_ALL
                CommercialPropertyType.Industrial -> LocalizationKeys.COMMERCIAL_PROPERTY_INDUSTRIAL
                CommercialPropertyType.Office -> LocalizationKeys.COMMERCIAL_PROPERTY_OFFICE
                CommercialPropertyType.Retail -> LocalizationKeys.COMMERCIAL_PROPERTY_RETAIL
                CommercialPropertyType.Services -> LocalizationKeys.COMMERCIAL_PROPERTY_SERVICES
                CommercialPropertyType.Warehouses -> LocalizationKeys.COMMERCIAL_PROPERTY_WAREHOUSES
                CommercialPropertyType.Other -> LocalizationKeys.COMMERCIAL_PROPERTY_OTHER
                CommercialPropertyType.Land -> LocalizationKeys.COMMERCIAL_PROPERTY_LAND
                CommercialPropertyType.Showroom -> LocalizationKeys.COMMERCIAL_PROPERTY_SHOWROOM
                null -> null
            }
        }
    }
}