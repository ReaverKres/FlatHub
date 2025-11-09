package io.flatzen.commoncomponents.commonentities

import kotlinx.serialization.Serializable

@Serializable
sealed class CommercialPropertyType {
    companion object {
        val allInstances: List<CommercialPropertyType> = listOf(
//            All,
            Office,
            Retail,
            Services,
            Industrial,
            Warehouses,
            Other
        )
    }
    @Serializable
    data object All: CommercialPropertyType()
    @Serializable
    data object Office: CommercialPropertyType()
    @Serializable
    data object Retail: CommercialPropertyType()
    @Serializable
    data object Services: CommercialPropertyType()
    @Serializable
    data object Industrial: CommercialPropertyType()
    @Serializable
    data object Warehouses: CommercialPropertyType()
    @Serializable
    data object Other: CommercialPropertyType()
}