package io.flatzen.viewmodel.detailad

import entities.AppFlat
import entities.getPricesText
import io.flatzen.commoncomponents.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AnalyticsManager
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.utils.formatPricePerSquare
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import io.flatzen.utils.mapSizeAtLevel
import io.flatzen.viewmodel.filter.CommercialPropertyTypeInfo
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlin.time.Clock
import ovh.plrapps.mapcompose.api.addLayer
import ovh.plrapps.mapcompose.api.disableRotation
import ovh.plrapps.mapcompose.api.disableScrolling
import ovh.plrapps.mapcompose.api.disableZooming
import ovh.plrapps.mapcompose.core.TileStreamProvider
import ovh.plrapps.mapcompose.ui.layout.Forced
import ovh.plrapps.mapcompose.ui.state.MapState
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import repository.mergedrepo.MergedRepository
import kotlin.math.pow
import kotlin.math.roundToInt

private typealias FlatDetailCtx = PipelineContext<FlatDetailState, FlatDetailIntent, FlatDetailAction>

class FlatDetailContainer(
    private val mergedRepository: MergedRepository,
    private val tileStreamProvider: TileStreamProvider,
    private val analyticsManager: AnalyticsManager,
    private val navigator: FlatHubNavigator,
) : Container<FlatDetailState, FlatDetailIntent, FlatDetailAction> {

    private val maxLevel = 18
    private val minLevel = 16
    private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)

    val mapState = MapState(levelCount = maxLevel + 1, mapSize, mapSize) {
        minimumScaleMode(Forced(1 / 2.0.pow(maxLevel - minLevel)))
        scale(0.0)
    }.apply {
        addLayer(tileStreamProvider)
        disableRotation()
        disableZooming()
        disableScrolling()
    }

    override val store = store<FlatDetailState, FlatDetailIntent, FlatDetailAction>(
        initial = FlatDetailState.Initial
    ) {
        reduce { intent ->
            when (intent) {
                is FlatDetailIntent.LoadFlatDetails -> handleLoadFlatDetails(intent)
                is FlatDetailIntent.ClickOnFavorite -> handleClickOnFavorite(intent)
                is FlatDetailIntent.ClearDislike -> handleClearDislike(intent)
                is FlatDetailIntent.TrackScreenView -> {
                    launch {
                        analyticsManager.registerEvent(
                            AnalyticsEvent(
                                eventName = AppMetrcica.Events.SCREEN_VIEW,
                                parameters = mapOf(
                                    AppMetrcica.Parameters.SCREEN_NAME to intent.screenName,
                                    AppMetrcica.Parameters.TIMESTAMP to Clock.System.now()
                                ) + intent.parameters
                            )
                        )
                    }
                }

                FlatDetailIntent.NavigateBack -> navigator.navigate(FlatHubCommand.NavigateBack)
                is FlatDetailIntent.OpenOnMap -> withState {
                    val flat = flat
                    navigator.navigate(
                        FlatHubCommand.OpenMap(
                            selectedMarker = intent.flatId,
                            latitude = flat?.coordinates?.latitude,
                            longitude = flat?.coordinates?.longitude,
                            rooms = flat?.numberOfRooms?.toIntOrNull(),
                        )
                    )
                }
            }
        }
    }

    private suspend fun FlatDetailCtx.handleLoadFlatDetails(intent: FlatDetailIntent.LoadFlatDetails) {
        mergedRepository.getFlatByIdWithDetails(
            flatPlatform = intent.flatPlatform,
            flatId = intent.flatId,
            markAsViewed = intent.markAsViewed,
        ).asLCE()
            .collect { lce ->
                when (lce) {
                    is LCE.Loading -> updateState { copy(isLoading = true, error = null) }
                    is LCE.Error -> updateState {
                        copy(
                            isLoading = false,
                            error = lce.message,
                            flat = null
                        )
                    }

                    is LCE.Content -> {
                        val uiFlat = appFlatToUiFlat(lce.value)
                        updateState { copy(isLoading = false, flat = uiFlat, error = null) }
                    }
                }
            }
    }

    private suspend fun FlatDetailCtx.handleClickOnFavorite(intent: FlatDetailIntent.ClickOnFavorite) {
        mergedRepository.saveFlatToFavorite(intent.flatPlatform, intent.adId).asLCE()
            .collect { lce ->
                when (lce) {
                    is LCE.Loading -> updateState {
                        if (flat?.isDetailDataLoaded == true) this
                        else copy(flat = flat?.copy(saveInFavoriteInProgress = true))
                    }

                    is LCE.Error -> { /* keep state */
                    }

                    is LCE.Content -> lce.value?.let {
                        updateState {
                            copy(
                                flat = flat?.copy(
                                    saveInFavoriteInProgress = false,
                                    savedInFavorite = lce.value.savedInFavorites,
                                    disliked = lce.value.dislike,
                                )
                            )
                        }
                    }
                }
            }
    }

    private suspend fun FlatDetailCtx.handleClearDislike(intent: FlatDetailIntent.ClearDislike) {
        mergedRepository.setFlatDisliked(intent.flatPlatform, intent.adId, disliked = false)
            .asLCE()
            .collect { lce ->
                if (lce is LCE.Content) {
                    lce.value?.let { updated ->
                        updateState {
                            copy(flat = flat?.copy(disliked = updated.dislike))
                        }
                    }
                }
            }
    }

    private fun appFlatToUiFlat(appFlat: AppFlat): UiDetailFlat {
        return UiDetailFlat(
            adId = appFlat.adId,
            isDetailDataLoaded = appFlat.flatDevInfo.isDetailLoaded,
            isViewed = appFlat.isViewed,
            savedInFavorite = appFlat.savedInFavorites,
            saveInFavoriteInProgress = false,
            disliked = appFlat.dislike,
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
            } else null,
            flatUrl = appFlat.flatDetailUrl,
            description = appFlat.description.orEmpty(),
            imageUrls = appFlat.imageUrls.orEmpty().toImmutableList(),
            priceUsd = appFlat.priceUsd,
            priceByn = appFlat.priceByn,
            priceText = appFlat.getPricesText(),
            priceUsdSquare = appFlat.priceUsdSquare?.let { formatPricePerSquare(it, "USD") },
            priceBynSquare = appFlat.priceBynSquare?.let { formatPricePerSquare(it, "BYN") },
            address = appFlat.address.orEmpty(),
            district = appFlat.district,
            metroStation = if (appFlat.metroStation.isNullOrBlank()) null else "🚇 ${appFlat.metroStation}",
            numberOfRooms = when {
                appFlat.rooms != null -> if (appFlat.isStudio == true) "Студия" else "${appFlat.rooms}"
                appFlat.commercialInfo?.numberOfRooms != null -> "${appFlat.commercialInfo?.numberOfRooms}"
                else -> "Не указано"
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
            windowDirection = appFlat.windowDirections.orEmpty().toImmutableList(),
            buildingImprovements = appFlat.buildingImprovements.orEmpty().toImmutableList(),
            amenities = appFlat.amenities.orEmpty().toImmutableList(),
            kitchenEquipment = appFlat.kitchenEquipment.orEmpty().toImmutableList(),
            prepaymentType = appFlat.prepaymentType,
            yearBuilt = appFlat.yearBuilt?.toString(),
            forWhom = appFlat.forWhom?.toImmutableList(),
            parkingInfo = appFlat.parkingInfo,
            isOwner = appFlat.owner,
            publishedAt = appFlat.publishedAtUi,
            contactInformation = ContactInformationUi(
                phones = appFlat.contactInformation?.phones?.toImmutableList(),
                ownerName = appFlat.contactInformation?.ownerName
            ),
            coordinates = appFlat.coordinates
        )
    }

    private fun formatArea(area: Double): String = "${area.roundToInt()} м²"
}
