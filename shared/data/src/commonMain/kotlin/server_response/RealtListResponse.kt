package server_response


import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class RealtListResponse {
    @Serializable
    data class RealtListResponseItem(
        @SerialName("data")
        val `data`: Data?
    ) {
        @Serializable
        data class Data(
            @SerialName("searchObjects")
            val searchObjects: SearchObjects?
        ) {
            @Serializable
            data class SearchObjects(
                @SerialName("body")
                val body: Body?,
//                @SerialName("errors")
//                val errors: List<Any?>?,
                @SerialName("success")
                val success: Boolean?,
                @SerialName("__typename")
                val typename: String?
            ) {
                @Serializable
                data class Body(
                    @SerialName("extraFields")
                    val extraFields: ExtraFields?,
                    @SerialName("pagination")
                    val pagination: Pagination?,
                    @SerialName("rates")
                    val rates: List<Rate?>?,
                    @SerialName("results")
                    val results: List<RealtFlatResponse?>?,
                    @SerialName("__typename")
                    val typename: String?
                ) {
                    @Serializable
                    data class ExtraFields(
                        @SerialName("minPriceAggregation")
                        val minPriceAggregation: Int?,
                        @SerialName("__typename")
                        val typename: String?
                    )
    
                    @Serializable
                    data class Pagination(
                        @SerialName("page")
                        val page: Int?,
                        @SerialName("pageSize")
                        val pageSize: Int?,
                        @SerialName("totalCount")
                        val totalCount: Int?,
                        @SerialName("__typename")
                        val typename: String?
                    )
    
                    @Serializable
                    data class Rate(
                        @SerialName("from")
                        val from: Int?,
                        @SerialName("rate")
                        val rate: Double?,
                        @SerialName("to")
                        val to: Int?,
                        @SerialName("__typename")
                        val typename: String?
                    )
    
                    @Serializable
                    data class RealtFlatResponse(
                        val adType: AdType = AdType.RENT,
                        val commercialPropertyType: CommercialPropertyType?,
                        @SerialName("address")
                        val address: String?,
                        @SerialName("agencyName")
                        val agencyName: String?,
                        @SerialName("agencyUuid")
                        val agencyUuid: String?,
                        @SerialName("appliances")
                        val appliances: List<Int?>?,
                        @SerialName("areaKitchen")
                        val areaKitchen: Double?,
                        @SerialName("areaLand")
                        val areaLand: Double?,
                        @SerialName("areaLiving")
                        val areaLiving: Double?,
                        @SerialName("areaMax")
                        val areaMax: Double?,
                        @SerialName("areaMin")
                        val areaMin: Double?,
                        @SerialName("areaTotal")
                        val areaTotal: Double?,
                        @SerialName("balconyType")
                        val balconyType: Int?,
                        @SerialName("buildingNumber")
                        val buildingNumber: String?,
                        @SerialName("buildingYear")
                        val buildingYear: Int?,
                        @SerialName("category")
                        val category: Int?,
                        @SerialName("code")
                        val code: Int?,
                        @SerialName("comments")
                        val comments: String?,
                        @SerialName("communicationMethod")
                        val communicationMethod: Int?,
                        @SerialName("contactEmail")
                        val contactEmail: String?,
                        @SerialName("contactName")
                        val contactName: String?,
                        @SerialName("contactPhones")
                        val contactPhones: List<String?>?,
                        @SerialName("createdAt")
                        val createdAt: String?,
                        @SerialName("customSorting")
                        val customSorting: Int?,
                        @SerialName("description")
                        val description: String?,
                        @SerialName("furniture")
                        val furniture: Int?,
                        @SerialName("has3dTour")
                        val has3dTour: Boolean?,
                        @SerialName("hasVideo")
                        val hasVideo: Boolean?,
                        @SerialName("headline")
                        val headline: String?,
                        @SerialName("houseNumber")
                        val houseNumber: Int?,
                        @SerialName("houseType")
                        val houseType: Int?,
                        @SerialName("images")
                        val images: List<String?>?,
                        @SerialName("isFavorite")
                        val isFavorite: Boolean?,
                        @SerialName("isObjectInRealtyDeal")
                        val isObjectInRealtyDeal: Boolean?,
                        @SerialName("location")
                        val location: List<Double?>?,
                        @SerialName("metroLineId")
                        val metroLineId: Int?,
                        @SerialName("metroStationName")
                        val metroStationName: String?,
                        @SerialName("metroTime")
                        val metroTime: Int?,
                        @SerialName("metroTimeType")
                        val metroTimeType: Int?,
                        @SerialName("numberOfBeds")
                        val numberOfBeds: Int?,
                        @SerialName("paymentStatus")
                        val paymentStatus: Int?,
                        @SerialName("price")
                        val price: Double?,
                        @SerialName("priceChangeDate")
                        val priceChangeDate: String?,
                        @SerialName("priceChangeDirection")
                        val priceChangeDirection: Int?,
                        @SerialName("priceCurrency")
                        val priceCurrency: Int?,
                        @SerialName("priceMax")
                        val priceMax: Int?,
                        @SerialName("priceMin")
                        val priceMin: Int?,
                        @SerialName("pricePerM2")
                        val pricePerM2: Double?,
                        @SerialName("pricePerM2Max")
                        val pricePerM2Max: Double?,
                        @SerialName("pricePerPerson")
                        val pricePerPerson: Double?,
                        @SerialName("rooms")
                        val rooms: Int?,
                        @SerialName("specialComment")
                        val specialComment: String?,
                        @SerialName("stateDistrictName")
                        val stateDistrictName: String?,
                        @SerialName("stateRegionName")
                        val stateRegionName: String?,
                        @SerialName("stateRegionUuid")
                        val stateRegionUuid: String?,
                        @SerialName("storey")
                        val storey: Int?,
                        @SerialName("storeys")
                        val storeys: Int?,
                        @SerialName("streetName")
                        val streetName: String?,
                        @SerialName("title")
                        val title: String?,
//                        @SerialName("townDistance")
//                        val townDistance: Any?,
                        @SerialName("townName")
                        val townName: String?,
                        @SerialName("townType")
                        val townType: Int?,
                        @SerialName("townUuid")
                        val townUuid: String?,
                        @SerialName("__typename")
                        val typename: String?,
                        @SerialName("updatedAt")
                        val updatedAt: String?,
                        @SerialName("userUuid")
                        val userUuid: String?,
                        @SerialName("uuid")
                        val uuid: String?,
//                        @SerialName("wallMaterial")
//                        val wallMaterial: Any?
                    )
                }
            }
        }
    }
}