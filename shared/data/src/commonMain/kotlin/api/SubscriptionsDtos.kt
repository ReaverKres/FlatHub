package api

import entities.AddressRequestModel
import entities.LocationFilter
import entities.MetroStation
import entities.PriceType
import entities.UserMapArea
import io.flatzen.commoncomponents.commonentities.BookingDatesFilter
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import kotlinx.serialization.Serializable
import repository.osm.OsmDistricts
import server_request.Currency

@Serializable
data class CommonFilterRequestDto(
    val name: String? = null,
    val adType: AdTypeDto? = null,
    val commercial: CommercialDto? = null,
    val lastCommercialAdType: AdTypeDto? = null,
    val priceFull: Price? = null,
    val pricePerSquare: Price? = null,
    val totalArea: FromToRange? = null,
    val priceType: PriceType = PriceType.FULL,
    val currency: Currency = Currency.USD,
    val addressRequestModel: Set<AddressRequestModel> = emptySet(),
    val numberOfRooms: Set<Int>? = emptySet(),
    val metroStations: List<MetroStation> = emptyList(),
    val withAnyMetro: Boolean = false,
    val districtsArea: List<OsmDistricts> = emptyList(),
    val location: LocationFilter? = null,
    val userMapAreas: List<UserMapArea> = emptyList(),
    val roomOnly: Boolean = false,
    val fromOwnerOnly: Boolean? = null,
    val withPhotoOnly: Boolean = false,
    val sortOption: FlatSort = FlatSort.NEWEST_FIRST, // Added sort option
    val bookingDatesFilter: BookingDatesFilter? = null,
    val isNotificationEnabled: Boolean = false
)

@Serializable
data class AdTypeDto(
    val type: String,
    val commercialAdType: String? = null
)

@Serializable
data class CommercialDto(
    val commercialPropertyType: CommercialPropertyTypeDto? = null
)

@Serializable
data class CommercialPropertyTypeDto(
    val type: String
)





