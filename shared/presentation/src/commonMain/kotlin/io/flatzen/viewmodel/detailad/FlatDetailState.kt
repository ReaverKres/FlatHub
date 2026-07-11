package io.flatzen.viewmodel.detailad

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.PriceText
import io.flatzen.commoncomponents.localization.LocalizationKeys
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
@Immutable
data class FlatDetailState(
    val isLoading: Boolean,
    val flat: UiDetailFlat?,
    val error: String?
) : MVIState {
    companion object {
        val Initial = FlatDetailState(isLoading = false, flat = null, error = null)
    }
}

@Immutable
data class UiDetailFlat(
    val adId: Long,
    val platform: FlatPlatform,
    val isDetailDataLoaded: Boolean?,
    val savedInFavorite: Boolean,
    val saveInFavoriteInProgress: Boolean,
    val isViewed: Boolean,
    val commercialUiInfo: CommercialUiInfo?,
    val flatUrl: String,
    val description: String,
    val imageUrls: List<String>,
    val priceUsd: Double?,
    val priceByn: Double?,
    val priceText: PriceText,
    val priceUsdSquare: String?,
    val priceBynSquare: String?,
    val address: String,
    val district: String?,
    val metroStation: String?,
    val numberOfRooms: String,
    val totalArea: String?,
    val livingArea: String?,
    val kitchenArea: String?,
    val floor: String?,
    val totalFloors: String?,
    val sleepingPlaces: String?,
    val isStudio: Boolean,
    val bathroomType: String?,
    val balcony: String?,
    val repairType: String?,
    val condition: String?,
    val windowDirection: List<String>,
    val buildingImprovements: List<String>,
    val amenities: List<String>,
    val kitchenEquipment: List<String>,
    val prepaymentType: String?,
    val yearBuilt: String?,
    val forWhom: List<String>?,
    val parkingInfo: String?,
    val isOwner: Boolean?,
    val publishedAt: String?,
    val contactInformation: ContactInformationUi,
    val coordinates: Coordinates?
)

@Immutable
data class ContactInformationUi(
    val phones: List<String>?,
    val ownerName: String?
)

@Immutable
data class CommercialUiInfo(
    val isCommercialAd: Boolean,
    val numberOfRooms: String? = null,
    val propertyType: PropertyTypeUi
)

@Immutable
data class PropertyTypeUi(
    val commercialPropertyType: CommercialPropertyType? = null,
    val commercialPropertyTypeName: LocalizationKeys? = null
)


// Intent
sealed interface FlatDetailIntent : MVIIntent {
    data class LoadFlatDetails(val flatPlatform: FlatPlatform, val flatId: Long) : FlatDetailIntent
    data class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FlatDetailIntent
    data class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FlatDetailIntent
}

// Action — no side effects for FlatDetail screen
sealed interface FlatDetailAction : MVIAction
