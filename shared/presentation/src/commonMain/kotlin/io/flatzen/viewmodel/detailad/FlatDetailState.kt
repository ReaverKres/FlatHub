package io.flatzen.viewmodel.detailad

import androidx.compose.runtime.Immutable
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.PriceText
import io.flatzen.commoncomponents.localization.LocalizationKeys
import kotlinx.collections.immutable.ImmutableList
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

// State
@Immutable
data class FlatDetailState(
    val isLoading: Boolean,
    val flat: UiDetailFlat?,
    /** Original listing before translation; null when showing original. */
    val originalFlat: UiDetailFlat? = null,
    val isTranslating: Boolean = false,
    val isShowingTranslation: Boolean = false,
    val translationQuotaExhausted: Boolean = false,
    val error: String?,
) : MVIState {
    companion object {
        val Initial = FlatDetailState(isLoading = false, flat = null, error = null)
    }

    val displayFlat: UiDetailFlat? get() = flat
}

@Immutable
data class UiDetailFlat(
    val adId: Long,
    val platform: FlatPlatform,
    val isDetailDataLoaded: Boolean?,
    val savedInFavorite: Boolean,
    val saveInFavoriteInProgress: Boolean,
    val isViewed: Boolean,
    val disliked: Boolean,
    val commercialUiInfo: CommercialUiInfo?,
    val flatUrl: String,
    val description: String,
    val imageUrls: ImmutableList<String>,
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
    val windowDirection: ImmutableList<String>,
    val buildingImprovements: ImmutableList<String>,
    val amenities: ImmutableList<String>,
    val kitchenEquipment: ImmutableList<String>,
    val prepaymentType: String?,
    val yearBuilt: String?,
    val forWhom: ImmutableList<String>?,
    val parkingInfo: String?,
    val isOwner: Boolean?,
    val publishedAt: String?,
    val contactInformation: ContactInformationUi,
    val coordinates: Coordinates?
)

@Immutable
data class ContactInformationUi(
    val phones: ImmutableList<String>?,
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
    data class LoadFlatDetails(
        val flatPlatform: FlatPlatform,
        val flatId: Long,
        val markAsViewed: Boolean = true,
    ) : FlatDetailIntent
    data class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FlatDetailIntent
    data class ClearDislike(val flatPlatform: FlatPlatform, val adId: Long) : FlatDetailIntent
    data class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FlatDetailIntent

    data object NavigateBack : FlatDetailIntent
    data class OpenOnMap(val flatId: Long) : FlatDetailIntent

    /** Translate listing text to [targetLangTag] (e.g. "en", "ru"). */
    data class TranslateListing(val targetLangTag: String) : FlatDetailIntent
    data object ShowOriginalListing : FlatDetailIntent
    data object DismissTranslationQuotaMessage : FlatDetailIntent
}

// Action
sealed interface FlatDetailAction : MVIAction {
    data class ShowToast(val messageKey: TranslationToastKey) : FlatDetailAction
}

enum class TranslationToastKey {
    QUOTA_EXHAUSTED,
    TRANSLATION_FAILED,
    TRANSLATION_DONE,
}