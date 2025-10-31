package server_response.kufar


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KufarDailyListResponse(
    @SerialName("paginator")
    val paginator: Paginator?,
    @SerialName("pole_position")
    val polePosition: List<RentalObject?>?,
    @SerialName("rental_objects")
    val rentalObjects: List<RentalObject?>?
) {
    @Serializable
    data class Paginator(
        @SerialName("page")
        val page: Int?,
        @SerialName("pages")
        val pages: Int?,
        @SerialName("size")
        val size: Int?,
        @SerialName("total")
        val total: Int?
    )

    @Serializable
    data class RentalObject(
        val appCity: String?,
        //TODO java.lang.IllegalArgumentException: Key "1030745363" was already used. If you are using LazyColumn/Row please make sure you provide a unique key for each item.
        val currentPage: Int = 1,
        @SerialName("ad_id")
        val adId: Int?,
        @SerialName("ad_snapshot")
        val adSnapshot: AdSnapshot?,
        @SerialName("address")
        val address: String?,
        @SerialName("area")
        val area: Int?,
        @SerialName("category")
        val category: String?,
        @SerialName("content_video")
        val contentVideo: String?,
        @SerialName("coordinates")
        val coordinates: List<Double?>?,
        @SerialName("flat_new_building")
        val flatNewBuilding: Boolean?,
        @SerialName("floor")
        val floor: Int?,
//        @SerialName("house_rent_period")
//        val houseRentPeriod: Any?,
        @SerialName("id")
        val id: String?,
        @SerialName("image")
        val image: Image?,
        @SerialName("images")
        val images: List<Image?>?,
        @SerialName("is_calculated_price")
        val isCalculatedPrice: Boolean?,
        @SerialName("is_instant_approval")
        val isInstantApproval: Boolean?,
        @SerialName("is_mine")
        val isMine: Boolean?,
        @SerialName("is_non_refundable_payment")
        val isNonRefundablePayment: Boolean?,
        @SerialName("is_object_checked")
        val isObjectChecked: Boolean?,
        @SerialName("is_online_payment")
        val isOnlinePayment: Boolean?,
        @SerialName("is_superhost")
        val isSuperhost: Boolean?,
        @SerialName("list_id")
        val listId: Int?,
        @SerialName("list_time")
        val listTime: String?,
        @SerialName("metro")
        val metro: List<Int?>?,
        @SerialName("metro_station_names")
        val metroStationNames: List<MetroStationName?>?,
        @SerialName("paid_services")
        val paidServices: PaidServices?,
        @SerialName("photo_3d_enabled")
        val photo3dEnabled: Boolean?,
        @SerialName("price")
        val price: Int?,
        @SerialName("region")
        val region: String?,
        @SerialName("rent_type")
        val rentType: String?,
        @SerialName("rooms")
        val rooms: Int?,
        @SerialName("self_url")
        val selfUrl: String?,
        @SerialName("short_description")
        val shortDescription: String?,
        @SerialName("size")
        val size: Int?,
        @SerialName("subject")
        val subject: String?,
        @SerialName("title")
        val title: Title?,
        @SerialName("type")
        val type: String?
    ) {
        @Serializable
        data class AdSnapshot(
            @SerialName("accommodation_type")
            val accommodationType: Int?,
            @SerialName("address")
            val address: String?,
            @SerialName("area")
            val area: Int?,
            @SerialName("body")
            val body: String?,
            @SerialName("building_type")
            val buildingType: Int?,
            @SerialName("coordinates")
            val coordinates: List<Double?>?,
            @SerialName("hotel_rating")
            val hotelRating: Int?,
            @SerialName("metro_station_names")
            val metroStationNames: List<MetroStationName?>?,
            @SerialName("re_object_checked")
            val reObjectChecked: Boolean?,
            @SerialName("region")
            val region: Int?,
            @SerialName("report_documents")
            val reportDocuments: Boolean?
        ) {
            @Serializable
            data class MetroStationName(
                @SerialName("by")
                val `by`: String?,
                @SerialName("ru")
                val ru: String?
            )
        }

        @Serializable
        data class Image(
            @SerialName("id")
            val id: String?,
            @SerialName("media_storage")
            val mediaStorage: String?,
            @SerialName("path")
            val path: String?,
            @SerialName("yams_storage")
            val yamsStorage: Boolean?
        )

        @Serializable
        data class MetroStationName(
            @SerialName("by")
            val `by`: String?,
            @SerialName("ru")
            val ru: String?
        )

        @Serializable
        data class PaidServices(
            @SerialName("highlight")
            val highlight: Boolean?,
            @SerialName("polepos")
            val polepos: Boolean?,
            @SerialName("ribbons")
            val ribbons: String?
        )

        @Serializable
        data class Title(
            @SerialName("by")
            val `by`: String?,
            @SerialName("ru")
            val ru: String?
        )
    }
}