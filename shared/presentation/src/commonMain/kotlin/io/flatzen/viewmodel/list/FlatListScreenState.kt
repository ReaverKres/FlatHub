package io.flatzen.viewmodel.list

import androidx.compose.runtime.Immutable
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.CommercialUiInfo
import io.flatzen.viewmodel.PropertyTypeUi
import io.flatzen.viewmodel.filter.CommercialPropertyTypeInfo
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState

@Immutable
data class FlatListScreenState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
    val noFlatsToLoadMore: Boolean,
    val isAnyFilterApplied: Boolean,
    val flatList: List<UiFlat>,
    val currentSearchPage: Int,
    val isListView: Boolean = false,
    val infoDialogState: InfoDialogState? = null,
    val errorDialogState: SearchErrorDialogState? = null
) : MviState

@Immutable
data class UiFlat(
    val adId: Long,
    val adType: AdType,
    val flatPlatform: FlatPlatform,
    val isDetailDataLoaded: Boolean,
    val commercialUiInfo: CommercialUiInfo?,
    val savedInFavorite: Boolean,
    val saveInFavoriteInProgress: Boolean,
    val isViewed: Boolean,
    val imageUrls: List<String>,
    val priceUsd: Double?,
    val priceByn: Double?,
    val numberOfRooms: String?,
    val publishedAt: String?,
    val metroStation: String?,
    val address: String,
    val coordinates: Coordinates?,
) {
    companion object {
        fun appFlatListToUiFlatList(appFlatList: List<AppFlat>): List<UiFlat> {
            return appFlatList.map {
                UiFlat(
                    adId = it.adId,
                    adType = it.getAdTypeNonNull(),
                    flatPlatform = it.flatPlatform,
                    isDetailDataLoaded = it.flatDevInfo.isDetailLoaded,
                    commercialUiInfo = if (it.commercialInfo?.propertyType != null) {
                        CommercialUiInfo(
                            isCommercialAd = true,
                            numberOfRooms = it.commercialInfo?.numberOfRooms.toString(),
                            propertyType = PropertyTypeUi(
                                commercialPropertyType = it.commercialInfo?.propertyType,
                                commercialPropertyTypeName = CommercialPropertyTypeInfo.commercialPropertyTypeName(
                                    it.commercialInfo?.propertyType
                                )
                            )
                        )
                    } else {
                        null
                    },
                    imageUrls = it.imageUrls ?: listOf(),
                    savedInFavorite = it.savedInFavorites,
                    saveInFavoriteInProgress = false,
                    isViewed = it.isViewed,
                    priceByn = it.priceByn,
                    priceUsd = it.priceUsd,
                    numberOfRooms = if (it.rooms != null) {
                        if (it.isStudio == true) "Студия" else "${it.rooms}"
                    } else if (it.commercialInfo?.numberOfRooms != null) {
                        "${it.commercialInfo?.numberOfRooms}"
                    } else {
                        "Не указано"
                    },
                    publishedAt = it.publishedAtUi,
                    address = it.address.orEmpty(),
                    metroStation = if (it.metroStation.isNullOrBlank()) {
                        null
                    } else {
                        "🚇 ${it.metroStation}"
                    },
                    coordinates = it.coordinates
                )
            }
        }
    }
}

@Immutable
data class UiPrice(
    val price: String?,
    val currency: String
)