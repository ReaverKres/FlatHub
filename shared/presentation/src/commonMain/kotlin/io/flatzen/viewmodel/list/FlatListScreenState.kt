package io.flatzen.viewmodel.list

import androidx.compose.runtime.Immutable
import entities.AppFlat
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.utils.asPriceFormat
import io.flatzen.commoncomponents.utils.priceWithCurrency
import io.flatzen.mvi.MviState
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
    val flatPlatform: FlatPlatform,
    val savedInFavorite: Boolean,
    val isViewed: Boolean,
    val imageUrls: List<String>,
    val priceUsd: String,
    val priceByn: String?,
    val numberOfRooms: Int?,
    val publishedAt: String?,
    val metroStation: String?,
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
                    priceByn = it.priceByn?.let { priceByn -> priceWithCurrency(priceByn, "BYN") },
                    priceUsd = priceWithCurrency(it.priceUsd, "$"),
                    numberOfRooms = it.rooms,
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