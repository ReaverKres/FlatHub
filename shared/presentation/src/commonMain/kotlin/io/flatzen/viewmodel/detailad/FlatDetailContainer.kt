package io.flatzen.viewmodel.detailad

import entities.AppFlat
import entities.getPricesText
import io.flatzen.analytics.Analytics
import io.flatzen.analytics.AnalyticsEvent
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.commonentities.usesSquareFeet
import io.flatzen.error_handling.LCE
import io.flatzen.error_handling.asLCE
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.commoncomponents.utils.PlatformType
import io.flatzen.navigation.FlatHubCommand
import io.flatzen.navigation.FlatHubNavigator
import io.flatzen.translation.QuotaExceededException
import io.flatzen.translation.TranslateRequest
import io.flatzen.translation.TranslationService
import io.flatzen.translation.TranslationUnavailableException
import io.flatzen.utils.mapSizeAtLevel
import io.flatzen.viewmodel.filter.CommercialPropertyTypeInfo
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import kotlin.time.Clock

private typealias FlatDetailCtx = PipelineContext<FlatDetailState, FlatDetailIntent, FlatDetailAction>

class FlatDetailContainer(
    private val mergedRepository: MergedRepository,
    private val tileStreamProvider: TileStreamProvider,
    private val analytics: Analytics,
    private val navigator: FlatHubNavigator,
    private val translationService: TranslationService,
    private val devicePlatform: DevicePlatform,
) : Container<FlatDetailState, FlatDetailIntent, FlatDetailAction> {

    private val maxLevel = 18
    private val minLevel = 16
    private val mapSize = mapSizeAtLevel(maxLevel, tileSize = 256)
    private var loadJob: Job? = null
    private var translateJob: Job? = null

    val mapState = MapState(
        levelCount = maxLevel + 1,
        fullWidth = mapSize,
        fullHeight = mapSize,
        workerCount = 8,
    ) {
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
                is FlatDetailIntent.TrackScreenView -> onTrackScreenView(intent)
                FlatDetailIntent.NavigateBack -> navigator.navigate(FlatHubCommand.NavigateBack)
                is FlatDetailIntent.OpenOnMap -> onOpenOnMap(intent)
                is FlatDetailIntent.TranslateListing -> handleTranslate(intent.targetLangTag)
                FlatDetailIntent.ShowOriginalListing -> handleShowOriginal()
                FlatDetailIntent.DismissTranslationQuotaMessage -> onDismissTranslationQuotaMessage()
            }
        }
    }

    private fun FlatDetailCtx.onTrackScreenView(intent: FlatDetailIntent.TrackScreenView) {
        launch {
            analytics.track(
                AnalyticsEvent(
                    eventName = AppMetrcica.Events.SCREEN_VIEW,
                    parameters = mapOf(
                        AppMetrcica.Parameters.SCREEN_NAME to intent.screenName,
                        AppMetrcica.Parameters.TIMESTAMP to Clock.System.now(),
                    ) + intent.parameters,
                ),
            )
        }
    }

    private suspend fun FlatDetailCtx.onOpenOnMap(intent: FlatDetailIntent.OpenOnMap) {
        withState {
            val flat = flat
            navigator.navigate(
                FlatHubCommand.OpenMap(
                    selectedMarker = intent.flatId,
                    latitude = flat?.coordinates?.latitude,
                    longitude = flat?.coordinates?.longitude,
                    rooms = flat?.numberOfRooms?.toIntOrNull(),
                ),
            )
        }
    }

    private suspend fun FlatDetailCtx.onDismissTranslationQuotaMessage() {
        updateState { copy(translationQuotaExhausted = false) }
    }

    private fun FlatDetailCtx.handleLoadFlatDetails(intent: FlatDetailIntent.LoadFlatDetails) {
        loadJob?.cancel()
        translateJob?.cancel()
        loadJob = launch {
            updateState {
                val sameAd = flat?.adId == intent.flatId && flat?.platform == intent.flatPlatform
                copy(
                    isLoading = !sameAd,
                    flat = if (sameAd) flat else null,
                    originalFlat = if (sameAd) originalFlat else null,
                    isShowingTranslation = if (sameAd) isShowingTranslation else false,
                    isTranslating = false,
                    translationQuotaExhausted = false,
                    error = null,
                )
            }
            mergedRepository.getFlatByIdWithDetails(
                flatPlatform = intent.flatPlatform,
                flatId = intent.flatId,
                markAsViewed = intent.markAsViewed,
            ).asLCE()
                .collect { lce ->
                    when (lce) {
                        is LCE.Loading -> updateState {
                            copy(isLoading = flat == null, error = null)
                        }

                        is LCE.Error -> updateState {
                            copy(
                                isLoading = false,
                                error = lce.message
                                    ?: lce.throwable.message
                                    ?: lce.throwable.toString(),
                            )
                        }

                        is LCE.Content -> {
                            val uiFlat = appFlatToUiFlat(lce.value)
                            updateState {
                                // Keep translation if same ad already translated and content refresh
                                if (isShowingTranslation && originalFlat != null &&
                                    originalFlat.adId == uiFlat.adId
                                ) {
                                    copy(isLoading = false, error = null)
                                } else {
                                    copy(
                                        isLoading = false,
                                        flat = uiFlat,
                                        originalFlat = null,
                                        isShowingTranslation = false,
                                        error = null,
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun FlatDetailCtx.handleTranslate(targetLangTag: String) {
        if (devicePlatform.platformType != PlatformType.ANDROID) return
        translateJob?.cancel()
        translateJob = launch {
            try {
                var sourceFlat: UiDetailFlat? = null
                withState { sourceFlat = originalFlat ?: flat }
                val flatToTranslate = sourceFlat ?: return@launch

                updateState { copy(isTranslating = true, translationQuotaExhausted = false) }

                val texts = ListingTextTranslator.collect(flatToTranslate)
                val result = translationService.translate(
                    TranslateRequest(
                        texts = texts,
                        targetLang = targetLangTag,
                    )
                )
                val translatedFlat = ListingTextTranslator.apply(flatToTranslate, result.texts)
                updateState {
                    copy(
                        originalFlat = flatToTranslate,
                        flat = translatedFlat,
                        isShowingTranslation = true,
                        isTranslating = false,
                        translationQuotaExhausted = false,
                    )
                }
                action(FlatDetailAction.ShowToast(TranslationToastKey.TRANSLATION_DONE))
            } catch (_: QuotaExceededException) {
                updateState {
                    copy(
                        isTranslating = false,
                        translationQuotaExhausted = true,
                        flat = originalFlat ?: flat,
                        isShowingTranslation = false,
                    )
                }
                action(FlatDetailAction.ShowToast(TranslationToastKey.QUOTA_EXHAUSTED))
            } catch (_: TranslationUnavailableException) {
                updateState {
                    copy(
                        isTranslating = false,
                        translationQuotaExhausted = true,
                        flat = originalFlat ?: flat,
                        isShowingTranslation = false,
                    )
                }
                action(FlatDetailAction.ShowToast(TranslationToastKey.QUOTA_EXHAUSTED))
            } catch (_: Throwable) {
                updateState { copy(isTranslating = false) }
                action(FlatDetailAction.ShowToast(TranslationToastKey.TRANSLATION_FAILED))
            }
        }
    }

    private suspend fun FlatDetailCtx.handleShowOriginal() {
        updateState {
            val original = originalFlat
            if (original != null) {
                copy(
                    flat = original,
                    isShowingTranslation = false,
                )
            } else {
                this
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
                                ),
                                originalFlat = originalFlat?.copy(
                                    savedInFavorite = lce.value.savedInFavorites,
                                    disliked = lce.value.dislike,
                                ),
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
                            copy(
                                flat = flat?.copy(disliked = updated.dislike),
                                originalFlat = originalFlat?.copy(disliked = updated.dislike),
                            )
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
                    numberOfRooms = appFlat.commercialInfo?.numberOfRooms?.toString(),
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
            mainPrice = appFlat.mainPrice,
            secondPrice = appFlat.secondPrice,
            priceText = appFlat.getPricesText(),
            mainPriceSquare = appFlat.getPricesText().mainPerSquarePrice,
            secondPriceSquare = appFlat.getPricesText().localPerSquarePrice,
            address = appFlat.address.orEmpty(),
            district = appFlat.district,
            metroStation = if (appFlat.metroStation.isNullOrBlank()) null else "🚇 ${appFlat.metroStation}",
            numberOfRooms = when {
                appFlat.rooms != null -> {
                    val n = if (appFlat.isStudio == true || appFlat.rooms == 0) 1 else appFlat.rooms
                    "$n"
                }
                appFlat.commercialInfo?.numberOfRooms != null -> "${appFlat.commercialInfo?.numberOfRooms}"
                appFlat.adType?.isCommercial == true || appFlat.commercialInfo != null -> ""
                else -> "Не указано"
            },
            totalArea = appFlat.totalArea?.let {
                formatArea(
                    it,
                    appFlat.flatPlatform.usesSquareFeet()
                )
            },
            livingArea = appFlat.livingArea?.let {
                formatArea(
                    it,
                    appFlat.flatPlatform.usesSquareFeet()
                )
            },
            kitchenArea = appFlat.kitchenArea?.let {
                formatArea(
                    it,
                    appFlat.flatPlatform.usesSquareFeet()
                )
            },
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
            coordinates = appFlat.coordinates,
            priceVsAreaAvgPercent = appFlat.listingInsights?.priceVsAreaAvgPercent,
        )
    }

    private fun formatArea(area: Double, usesSquareFeet: Boolean): String {
        val unit = if (usesSquareFeet) "sqft" else "м²"
        return "${area.roundToInt()} $unit"
    }
}
