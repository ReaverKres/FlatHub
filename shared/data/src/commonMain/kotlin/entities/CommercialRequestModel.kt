package entities

import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FromToRange
import kotlinx.serialization.Serializable

@Serializable
data class CommercialRequestModel(
    val roomRange: FromToRange?,
    val commercialPropertyType: CommercialPropertyType?
)

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