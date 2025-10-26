package io.flatzen.viewmodel

import androidx.compose.runtime.Immutable
import entities.AppFlat
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.utils.asPriceFormat
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.error_handling.process
import io.flatzen.mvi.MviAction
import io.flatzen.mvi.MviEffect
import io.flatzen.mvi.MviEvent
import io.flatzen.mvi.MviState
import io.flatzen.utils.mapSizeAtLevel
import io.flatzen.viewmodel.base.BaseMviViewModel
import io.flatzen.viewmodel.filter.CommercialPropertyTypeInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.disableRotation
import ovh.plrapps.mapcompose.api.disableScrolling
import ovh.plrapps.mapcompose.api.disableZooming
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import repository.fillter.FilterRepository
import repository.mergedrepo.MergedRepository
import kotlin.math.pow
import kotlin.math.roundToInt

@Immutable
data class UiDetailFlat(
    val adId: Long,
    val platform: FlatPlatform,
    val isDetailDataLoaded: Boolean?,
    val savedInFavorite: Boolean,
    val isViewed: Boolean,
    val commercialUiInfo: CommercialUiInfo?,
    val flatUrl: String,
    val description: String,
    val imageUrls: List<String>,
    val priceUsd: Double?,
    val priceByn: Double?,
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
    val commercialPropertyTypeName: String? = null
)

sealed interface FlatDetailScreenAction : MviAction {
    data class LoadFlatDetails(val flatPlatform: FlatPlatform, val flatId: Long) :
        FlatDetailScreenAction

    class ClickOnFavorite(val flatPlatform: FlatPlatform, val adId: Long) : FlatDetailScreenAction
    class TrackScreenView(
        val screenName: String,
        val parameters: Map<String, Any> = emptyMap()
    ) : FlatDetailScreenAction
}

@Immutable
data class FlatDetailScreenState(
    val isLoading: Boolean,
    val flat: UiDetailFlat?,
    val error: String?
) : MviState

sealed interface FlatDetailEvents : MviEvent {
    data class FlatLoaded(val flat: LCE<AppFlat>) : FlatDetailEvents
}

class FlatDetailViewModel(
    private val filterRepository: FilterRepository,
    private val mergedRepository: MergedRepository,
    private val tileStreamProvider: TileStreamProvider,
    private val analyticsManager: AnalyticsManager
) : BaseMviViewModel<FlatDetailScreenAction, FlatDetailScreenState, FlatDetailEvents, MviEffect>() {

    private val maxLevel = 18
    private val minLevel = 16
    private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)
    val mapState = MapState(levelCount = maxLevel + 1, mapSize, mapSize) {
        minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
        scale(0.0) // to zoom out initially
    }.apply {
        addLayer(tileStreamProvider)
        disableRotation()
        disableZooming()
        disableScrolling()
    }

    override fun initialState(): FlatDetailScreenState = FlatDetailScreenState(
        isLoading = false,
        flat = null,
        error = null
    )

    override suspend fun handleIntent(
        action: FlatDetailScreenAction,
        currentState: FlatDetailScreenState
    ): Flow<FlatDetailEvents> {
        return when (action) {
            is FlatDetailScreenAction.LoadFlatDetails -> {
                loadFlatDetails(action.flatPlatform, action.flatId)
            }

            is FlatDetailScreenAction.ClickOnFavorite -> {
                mergedRepository.saveFlatToFavorite(action.flatPlatform, action.adId).map {
                    FlatDetailEvents.FlatLoaded(flowOf(it!!).asLCE().last())
                }
            }

            is FlatDetailScreenAction.TrackScreenView -> {
                // Handle screen view analytics tracking
                viewModelScope.launch {
                    analyticsManager.registerEvent(
                        AnalyticsEvent(
                            eventName = AppMetrcica.Events.SCREEN_VIEW,
                            parameters = mapOf(
                                AppMetrcica.Parameters.SCREEN_NAME to action.screenName,
                                AppMetrcica.Parameters.TIMESTAMP to Clock.System.now()
                            ) + action.parameters
                        )
                    )
                }
                flowOf()
            }
        }
    }

    private suspend fun loadFlatDetails(
        flatPlatform: FlatPlatform,
        flatId: Long
    ): Flow<FlatDetailEvents> {
        return mergedRepository.getFlatByIdWithDetails(flatPlatform, flatId).asLCE().map {
            FlatDetailEvents.FlatLoaded(it)
        }
    }

    override suspend fun reduce(
        event: FlatDetailEvents,
        currentState: FlatDetailScreenState
    ): FlatDetailScreenState {
        return when (event) {
            is FlatDetailEvents.FlatLoaded -> event.flat.process(
                onLoading = {
                    currentState.copy(isLoading = true, error = null)
                },
                onError = { message, _ ->
                    currentState.copy(
                        isLoading = false,
                        error = message,
                        flat = null
                    )
                },
                onSuccess = { appFlat ->
                    val uiFlat = appFlatToUiFlat(appFlat)
                    currentState.copy(
                        isLoading = false,
                        flat = uiFlat,
                        error = null
                    )
                }
            )
        }
    }

    private fun appFlatToUiFlat(appFlat: AppFlat): UiDetailFlat {
        return UiDetailFlat(
            adId = appFlat.adId,
            isDetailDataLoaded = appFlat.flatDevInfo.isDetailLoaded,
            isViewed = true,
            savedInFavorite = appFlat.savedInFavorites,
            platform = appFlat.flatPlatform,
            commercialUiInfo = if (appFlat.commercialInfo?.propertyType != null) {
                CommercialUiInfo(
                    isCommercialAd = true,
                    numberOfRooms = appFlat.commercialInfo?.numberOfRooms.toString(),
                    propertyType = PropertyTypeUi(
                        commercialPropertyType = appFlat.commercialInfo?.propertyType,
                        commercialPropertyTypeName = CommercialPropertyTypeInfo.commercialPropertyTypeName(
                            appFlat.commercialInfo?.propertyType
                        )
                    )
                )
            } else {
                null
            },
            flatUrl = appFlat.flatDetailUrl,
            description = appFlat.description.orEmpty(),
            imageUrls = appFlat.imageUrls.orEmpty(),
            priceUsd = appFlat.priceUsd,
            priceByn = appFlat.priceByn,
            priceUsdSquare = appFlat.priceUsdSquare?.let { formatPricePerSquare(it, "USD") },
            priceBynSquare = appFlat.priceBynSquare?.let { formatPricePerSquare(it, "BYN") },
            address = appFlat.address.orEmpty(),
            district = appFlat.district,
            metroStation = if (appFlat.metroStation.isNullOrBlank()) {
                null
            } else {
                "🚇 ${appFlat.metroStation}"
            },
            numberOfRooms = if (appFlat.rooms != null) {
                if (appFlat.isStudio == true) "Студия" else "${appFlat.rooms}"
            } else if (appFlat.commercialInfo?.numberOfRooms != null) {
                "${appFlat.commercialInfo?.numberOfRooms}"
            } else {
                "Не указано"
            },
            totalArea = appFlat.totalArea?.let { formatArea(it) },
            livingArea = appFlat.livingArea?.let { formatArea(it) },
            kitchenArea = appFlat.kitchenArea?.let { formatArea(it) },
            floor = appFlat.floor?.toString(),
            totalFloors = appFlat.totalFloors?.toString(),
            sleepingPlaces = appFlat.sleepingPlaces?.toString(),
            isStudio = appFlat.isStudio ?: false,
            bathroomType = appFlat.bathroomType,
            balcony = appFlat.balcony,
            repairType = appFlat.repairType,
            condition = appFlat.condition,
            windowDirection = appFlat.windowDirections.orEmpty(),
            buildingImprovements = appFlat.buildingImprovements.orEmpty(),
            amenities = appFlat.amenities.orEmpty(),
            kitchenEquipment = appFlat.kitchenEquipment.orEmpty(),
            prepaymentType = appFlat.prepaymentType,
            yearBuilt = appFlat.yearBuilt?.toString(),
            forWhom = appFlat.forWhom,
            parkingInfo = appFlat.parkingInfo,
            isOwner = appFlat.owner,
            publishedAt = appFlat.publishedAtUi,
            contactInformation = ContactInformationUi(
                phones = appFlat.contactInformation?.phones,
                ownerName = appFlat.contactInformation?.ownerName
            ),
            coordinates = appFlat.coordinates
        )
    }

    private fun formatArea(area: Double): String {
        return "${area.roundToInt()} м²"
    }

    private fun formatPricePerSquare(price: Double, currency: String): String {
        return "${price.asPriceFormat()} $currency/м²"
    }
}