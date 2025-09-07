package server_response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnlinerListResponse(
    @SerialName("apartments")
    val apartments: List<Apartment?>?,
    @SerialName("page")
    val page: Page?,
    @SerialName("total")
    val total: Int?
) {
    @Serializable
    data class Apartment(
        @SerialName("contact")
        val contact: Contact?,
        @SerialName("created_at")
        val createdAt: String?,
        @SerialName("id")
        val id: Int?,
        @SerialName("last_time_up")
        val lastTimeUp: String?,
        @SerialName("location")
        val location: Location?,
        @SerialName("photo")
        val photo: String?,
        @SerialName("price")
        val price: Price?,
        @SerialName("rent_type")
        val rentType: String?,
        @SerialName("up_available_in")
        val upAvailableIn: Int?,
        @SerialName("url")
        val url: String?,

        //Sale
        @SerialName("number_of_rooms")
        val numberOfRooms: Int?,
        @SerialName("area")
        val area: OnlinerArea?
    ) {
        @Serializable
        data class Contact(
            @SerialName("owner")
            val owner: Boolean?
        )

        @Serializable
        data class OnlinerArea(
            @SerialName("total")
            val total: Double?,
            @SerialName("living")
            val living: Double?,
            @SerialName("kitchen")
            val kitchen: Double?
        )

        @Serializable
        data class Location(
            @SerialName("address")
            val address: String?,
            @SerialName("latitude")
            val latitude: Double?,
            @SerialName("longitude")
            val longitude: Double?,
            @SerialName("user_address")
            val userAddress: String?
        )

        @Serializable
        data class Price(
            @SerialName("amount")
            val amount: String?,
            @SerialName("converted")
            val converted: Converted?,
            @SerialName("currency")
            val currency: String?
        ) {
            @Serializable
            data class Converted(
                @SerialName("BYN")
                val bYN: BYN?,
                @SerialName("USD")
                val uSD: USD?
            ) {
                @Serializable
                data class BYN(
                    @SerialName("amount")
                    val amount: String?,
                    @SerialName("currency")
                    val currency: String?
                )

                @Serializable
                data class USD(
                    @SerialName("amount")
                    val amount: String?,
                    @SerialName("currency")
                    val currency: String?
                )
            }
        }
    }

    @Serializable
    data class Page(
        @SerialName("current")
        val current: Int?,
        @SerialName("items")
        val items: Int?,
        @SerialName("last")
        val last: Int?,
        @SerialName("limit")
        val limit: Int?
    )
}