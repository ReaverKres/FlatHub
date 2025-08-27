package server_response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DomovitaListResponse(
    @SerialName("items")
    val items: List<DomovitaFlat?>,
    @SerialName("totalCount")
    val totalCount: Int?
) {
    @Serializable
    data class DomovitaFlat(
        @SerialName("address")
        val address: Address?,
        @SerialName("ads_text")
        val adsText: String?,
        @SerialName("agency")
        val agency: Agency?,
        @SerialName("area")
        val area: Area?,
        @SerialName("building_year")
        val buildingYear: Int?,
        @SerialName("contacts")
        val contacts: Contacts?,
        @SerialName("date_reception")
        val dateReception: String?,
        @SerialName("date_revision")
        val dateRevision: String?,
        @SerialName("description")
        val description: String?,
        @SerialName("house_type")
        val houseType: String?,
        @SerialName("id")
        val id: Int?,
        @SerialName("is_favorite")
        val isFavorite: Boolean?,
        @SerialName("is_vip")
        val isVip: Int?,
        @SerialName("isVipColor")
        val isVipColor: Boolean?,
        @SerialName("owner_type")
        val ownerType: String?,
        @SerialName("photos")
        val photos: List<Photo?>?,
        @SerialName("price")
        val price: Price?,
        @SerialName("price_history")
        val priceHistory: List<PriceHistory?>?,
        @SerialName("repair_year")
        val repairYear: Int?,
        @SerialName("rooms")
        val rooms: Int?,
        @SerialName("separate_rooms")
        val separateRooms: Int?,
        @SerialName("status")
        val status: String?,
        @SerialName("storey")
        val storey: Int?,
        @SerialName("storeys")
        val storeys: Int?,
        @SerialName("title")
        val title: String?,
        @SerialName("type")
        val type: String?,
        @SerialName("url")
        val url: String?,
        @SerialName("user_id")
        val userId: Int?,
    ) {
        @Serializable
        data class Address(
            @SerialName("coords")
            val coords: Coords?,
            @SerialName("district")
            val district: District?,
            @SerialName("house_number")
            val houseNumber: String?,
            @SerialName("metro")
            val metro: Metro?,
            @SerialName("street_name")
            val streetName: String?,
            @SerialName("town")
            val town: Town?
        ) {
            @Serializable
            data class Coords(
                @SerialName("position_x")
                val positionX: Double,
                @SerialName("position_y")
                val positionY: Double
            )

            @Serializable
            data class District(
                @SerialName("id")
                val id: Int?,
                @SerialName("name")
                val name: String?,
                @SerialName("subDistrict")
                val subDistrict: SubDistrict?
            ) {
                @Serializable
                data class SubDistrict(
                    @SerialName("id")
                    val id: Int?,
                    @SerialName("name")
                    val name: String?
                )
            }

            @Serializable
            data class Metro(
                @SerialName("branch")
                val branch: String?,
                @SerialName("id")
                val id: Int?,
                @SerialName("name")
                val name: String?,
                @SerialName("position_x")
                val positionX: Double?,
                @SerialName("position_y")
                val positionY: Double?,
                @SerialName("time_by_transport")
                val timeByTransport: String?,
                @SerialName("time_by_walking")
                val timeByWalking: String?
            )

            @Serializable
            data class Town(
                @SerialName("id")
                val id: Int?,
                @SerialName("name")
                val name: String?,
                @SerialName("stateRegion")
                val stateRegion: StateRegion?,
                @SerialName("type")
                val type: String?
            ) {
                @Serializable
                data class StateRegion(
                    @SerialName("name")
                    val name: String?
                )
            }
        }

        @Serializable
        data class Agency(
            @SerialName("id")
            val id: Int?,
            @SerialName("license")
            val license: String?,
            @SerialName("license_number")
            val licenseNumber: String?,
            @SerialName("link")
            val link: String?,
            @SerialName("logo_url")
            val logoUrl: String?,
            @SerialName("name")
            val name: String?,
            @SerialName("rating")
            val rating: Double?,
            @SerialName("reviews_number")
            val reviewsNumber: Int?,
            @SerialName("unp")
            val unp: String?
        )

        @Serializable
        data class Area(
            @SerialName("kitchen")
            val kitchen: Double?,
            @SerialName("living")
            val living: Double?,
            @SerialName("total")
            val total: Double?
        )

        @Serializable
        data class Contacts(
            @SerialName("contact_name")
            val contactName: String?,
            @SerialName("email")
            val email: String?,
            @SerialName("period_on_site")
            val periodOnSite: String?,
            @SerialName("phones")
            val phones: List<String?>?,
            @SerialName("role")
            val role: String?,
            @SerialName("viber_phone")
            val viberPhone: String?
        )

        @Serializable
        data class Photo(
            @SerialName("url")
            val url: String?,
            @SerialName("url_mini")
            val urlMini: String?,
            @SerialName("url_mob")
            val urlMob: String?
        )

        @Serializable
        data class Price(
            @SerialName("BYN")
            val bYN: Double?,
            @SerialName("EUR")
            val eUR: Double?,
            @SerialName("RUB")
            val rUB: Double?,
            @SerialName("USD")
            val uSD: Double?
        )

        @Serializable
        data class PriceHistory(
            @SerialName("changePrice")
            val changePrice: Price?,
            @SerialName("date")
            val date: Int?,
            @SerialName("isUp")
            val isUp: Boolean?,
            @SerialName("price")
            val price: Price?
        )
    }

    @Serializable
    data class Price(
        @SerialName("BYN")
        val bYN: Double?,
        @SerialName("EUR")
        val eUR: Double?,
        @SerialName("RUB")
        val rUB: Double?,
        @SerialName("USD")
        val uSD: Int?
    )
}