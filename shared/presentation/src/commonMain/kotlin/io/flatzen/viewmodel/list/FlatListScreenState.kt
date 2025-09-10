package io.flatzen.viewmodel.list

import androidx.compose.runtime.Immutable
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.mvi.MviState

@Immutable
data class FlatListScreenState(
    val isLoading: Boolean,
    val isRefreshing: Boolean,
    val isLoadingMore: Boolean,
    val noFlatsToLoadMore: Boolean,
    val isAnyFilterApplied: Boolean,
    val flatList: List<UiFlat>
) : MviState

@Immutable
data class UiFlat(
    val adId: Long,
    val flatPlatform: FlatPlatform,
    val savedInFavorite: Boolean,
    val isViewed: Boolean,
    val imageUrls: List<String>,
    val priceUsd: UiPrice,
    val priceByn: UiPrice,
    val numberOfRooms: Int?,
    val publishedAt: String?,
    val metroStation: String,
    val address: String,
    val coordinates: Coordinates?
) {
    companion object {
        fun appFlatListToUiFlatList(appFlatList: List<AppFlat>): List<UiFlat> {
            return appFlatList.map {
                UiFlat(
                    adId = it.adId,
                    flatPlatform = it.flatPlatform,
                    imageUrls = it.imageUrls ?: listOf(),
                    savedInFavorite = it.savedInFavorites,
                    isViewed = it.isViewed,
                    priceByn = UiPrice(
                        price = it.priceByn,
                        currency = "BYN"
                    ),
                    priceUsd = UiPrice(
                        price = it.priceUsd,
                        currency = "USD"
                    ),
                    numberOfRooms = it.rooms,
                    publishedAt = it.publishedAtUi,
                    address = it.address.orEmpty(),
                    metroStation = it.metroStation.orEmpty(),
                    coordinates = it.coordinates
                )
            }
        }
    }
}

@Immutable
data class UiPrice(
    val price: Double?,
    val currency: String
)