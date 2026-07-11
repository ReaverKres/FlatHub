package io.flatzen.viewmodel.list

import androidx.compose.runtime.Immutable
import entities.AppFlat
import entities.getPricesText
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.PriceText
import io.flatzen.commoncomponents.extensions.toNullableString
import io.flatzen.mvi.MviState
import io.flatzen.viewmodel.detailad.CommercialUiInfo
import io.flatzen.viewmodel.detailad.PropertyTypeUi
import io.flatzen.viewmodel.filter.CommercialPropertyTypeInfo
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import pro.respawn.flowmvi.api.MVIState

@Immutable
data class FlatListScreenState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
    val noFlatsToLoadMore: Boolean,
    val isAnyFilterApplied: Boolean,
    val flatList: ImmutableList<UiFlat>,
    val currentSearchPage: Int,
    val isListView: Boolean = false,
    val infoDialogState: InfoDialogState? = null,
    val errorDialogState: SearchErrorDialogState? = null
) : MviState, MVIState {
    companion object {
        val Initial = FlatListScreenState(
            isLoading = true,
            isRefreshing = false,
            isLoadingMore = false,
            flatList = persistentListOf(),
            noFlatsToLoadMore = false,
            isAnyFilterApplied = false,
            currentSearchPage = 1
        )
    }
}

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
    val imageUrls: ImmutableList<String>,
    val mainPrice: Double?,
    val localPrice: Double?,
    val priceText: PriceText,
    val numberOfRooms: String?,
    val totalArea: String?,
    val publishedAt: String?,
    val metroStation: String?,
    val address: String,
    val coordinates: Coordinates?,
) {
    companion object {
        fun appFlatListToUiFlatList(appFlatList: List<AppFlat>): ImmutableList<UiFlat> {
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
                    imageUrls = (it.imageUrls ?: emptyList()).toImmutableList(),
                    savedInFavorite = it.savedInFavorites,
                    saveInFavoriteInProgress = false,
                    isViewed = it.isViewed,
                    localPrice = it.priceByn,
                    mainPrice = it.priceUsd,
                    priceText = it.getPricesText(),
                    numberOfRooms = if (it.rooms != null) {
                        if (it.isStudio == true) "Студия" else "${it.rooms}"
                    } else if (it.commercialInfo?.numberOfRooms != null) {
                        "${it.commercialInfo?.numberOfRooms}"
                    } else {
                        "Не указано"
                    },
                    totalArea = it.totalArea?.toInt().toNullableString(),
                    publishedAt = it.publishedAtUi,
                    address = it.address.orEmpty(),
                    metroStation = if (it.metroStation.isNullOrBlank()) {
                        null
                    } else {
                        "🚇 ${it.metroStation}"
                    },
                    coordinates = it.coordinates
                )
            }.toImmutableList()
        }
    }
}

@Immutable
data class UiPrice(
    val price: String?,
    val currency: String
)