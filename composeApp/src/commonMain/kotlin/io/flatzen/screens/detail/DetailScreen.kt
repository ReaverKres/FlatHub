package io.flatzen.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.cancel
import flatzen.composeapp.generated.resources.detail_about_apartment
import flatzen.composeapp.generated.resources.detail_about_commercial
import flatzen.composeapp.generated.resources.detail_address
import flatzen.composeapp.generated.resources.detail_agent
import flatzen.composeapp.generated.resources.detail_amenities
import flatzen.composeapp.generated.resources.detail_balcony
import flatzen.composeapp.generated.resources.detail_bathroom
import flatzen.composeapp.generated.resources.detail_building
import flatzen.composeapp.generated.resources.detail_contact_info
import flatzen.composeapp.generated.resources.detail_contact_on_source
import flatzen.composeapp.generated.resources.detail_contact_person
import flatzen.composeapp.generated.resources.detail_description
import flatzen.composeapp.generated.resources.detail_district
import flatzen.composeapp.generated.resources.detail_floor
import flatzen.composeapp.generated.resources.detail_for_whom
import flatzen.composeapp.generated.resources.detail_in_apartment
import flatzen.composeapp.generated.resources.detail_in_kitchen
import flatzen.composeapp.generated.resources.detail_infrastructure
import flatzen.composeapp.generated.resources.detail_kitchen_area
import flatzen.composeapp.generated.resources.detail_living_area
import flatzen.composeapp.generated.resources.detail_living_conditions
import flatzen.composeapp.generated.resources.detail_location
import flatzen.composeapp.generated.resources.detail_metro
import flatzen.composeapp.generated.resources.detail_open_source
import flatzen.composeapp.generated.resources.detail_owner
import flatzen.composeapp.generated.resources.detail_parking
import flatzen.composeapp.generated.resources.detail_phone
import flatzen.composeapp.generated.resources.detail_prepayment
import flatzen.composeapp.generated.resources.detail_repair
import flatzen.composeapp.generated.resources.detail_rooms_count
import flatzen.composeapp.generated.resources.detail_share
import flatzen.composeapp.generated.resources.detail_share_subject
import flatzen.composeapp.generated.resources.detail_sleeping_places
import flatzen.composeapp.generated.resources.detail_total_area
import flatzen.composeapp.generated.resources.detail_total_floors
import flatzen.composeapp.generated.resources.detail_windows
import flatzen.composeapp.generated.resources.detail_year_built
import flatzen.composeapp.generated.resources.detail_year_suffix
import flatzen.composeapp.generated.resources.filter_property_type
import flatzen.composeapp.generated.resources.filter_rooms_count
import flatzen.composeapp.generated.resources.list_rooms_suffix
import flatzen.composeapp.generated.resources.show_original
import flatzen.composeapp.generated.resources.translate_listing
import flatzen.composeapp.generated.resources.translate_to_title
import flatzen.composeapp.generated.resources.translation_done
import flatzen.composeapp.generated.resources.translation_failed
import flatzen.composeapp.generated.resources.translation_quota_exhausted
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.PriceText
import io.flatzen.commoncomponents.theme.AppLanguage
import io.flatzen.commoncomponents.theme.resolveTranslationTargetTag
import io.flatzen.commoncomponents.theme.shouldAutoTranslateListing
import io.flatzen.di.container
import io.flatzen.kmpapp.screens.EmptyScreenContent
import io.flatzen.localization.LocalAppLanguage
import io.flatzen.localization.LocalAppLocale
import io.flatzen.screens.map.RoomMarker
import io.flatzen.screens.more.labelRes
import io.flatzen.themes.FlatHubTheme
import io.flatzen.utils.ToastDurationType
import io.flatzen.utils.ToastLauncher
import io.flatzen.utils.lonLatToNormalized
import io.flatzen.utils.shareLauncher
import io.flatzen.viewmodel.detailad.ContactInformationUi
import io.flatzen.viewmodel.detailad.FlatDetailAction
import io.flatzen.viewmodel.detailad.FlatDetailContainer
import io.flatzen.viewmodel.detailad.FlatDetailIntent
import io.flatzen.viewmodel.detailad.TranslationToastKey
import io.flatzen.viewmodel.detailad.UiDetailFlat
import io.flatzen.widgets.FlatImagePager
import io.flatzen.widgets.FullscreenPhotoViewer
import io.flatzen.widgets.OpenInMapButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.snapScrollTo
import ovh.plrapps.mapcompose.ui.MapUI
import ovh.plrapps.mapcompose.ui.state.MapState
import pro.respawn.flowmvi.compose.dsl.subscribe
import repository.userpreferences.UserPreferencesRepository
import io.flatzen.common.localization.stringResource as localizedStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    flatPlatform: FlatPlatform,
    objectId: Long,
    markAsViewedOnOpen: Boolean = true,
    modifier: Modifier = Modifier
) {
    val container: FlatDetailContainer = container(
        key = "detail-${flatPlatform.name}-$objectId",
    )
    val toastLauncher = remember { ToastLauncher() }
    val quotaMsg = stringResource(Res.string.translation_quota_exhausted)
    val failedMsg = stringResource(Res.string.translation_failed)
    val doneMsg = stringResource(Res.string.translation_done)

    val state by container.store.subscribe { action ->
        when (action) {
            is FlatDetailAction.ShowToast -> {
                val msg = when (action.messageKey) {
                    TranslationToastKey.QUOTA_EXHAUSTED -> quotaMsg
                    TranslationToastKey.TRANSLATION_FAILED -> failedMsg
                    TranslationToastKey.TRANSLATION_DONE -> doneMsg
                }
                toastLauncher.showToast(msg, ToastDurationType.LONG)
            }
        }
    }

    val userPreferences: UserPreferencesRepository = koinInject()
    val alwaysTranslate by userPreferences.observeAlwaysTranslate()
        .collectAsStateWithLifecycle(initialValue = false)
    val preferredTarget by userPreferences.observeTranslateTargetLang()
        .collectAsStateWithLifecycle(initialValue = null)
    val appLanguage = LocalAppLanguage.current
    val systemLocale = LocalAppLocale.current
    val targetLangTag = remember(appLanguage, preferredTarget, systemLocale) {
        resolveTranslationTargetTag(appLanguage, preferredTarget, systemLocale)
    }

    var showTranslateDialog by rememberSaveable { mutableStateOf(false) }
    var autoTranslateTriggeredFor by rememberSaveable { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(flatPlatform, objectId, markAsViewedOnOpen) {
        autoTranslateTriggeredFor = null
        container.store.intent(
            FlatDetailIntent.LoadFlatDetails(
                flatPlatform = flatPlatform,
                flatId = objectId,
                markAsViewed = markAsViewedOnOpen,
            )
        )
        container.store.intent(
            FlatDetailIntent.TrackScreenView(
                screenName = AppMetrcica.Screens.DETAIL,
                parameters = mapOf(
                    AppMetrcica.Parameters.FLAT_PLATFORM to flatPlatform.name,
                    AppMetrcica.Parameters.OBJECT_ID to objectId
                )
            )
        )
    }

    val flat = state.flat

    LaunchedEffect(
        flat?.adId,
        flat?.isDetailDataLoaded,
        alwaysTranslate,
        targetLangTag,
        flat?.platform
    ) {
        val current = flat ?: return@LaunchedEffect
        if (!alwaysTranslate) return@LaunchedEffect
        if (state.isShowingTranslation || state.isTranslating || state.translationQuotaExhausted) return@LaunchedEffect
        if (current.description.isBlank() && current.address.isBlank()) return@LaunchedEffect
        if (!shouldAutoTranslateListing(current.platform, targetLangTag)) return@LaunchedEffect
        if (autoTranslateTriggeredFor == current.adId) return@LaunchedEffect
        autoTranslateTriggeredFor = current.adId
        container.store.intent(FlatDetailIntent.TranslateListing(targetLangTag))
    }

    if (showTranslateDialog) {
        TranslateLanguageDialog(
            onDismiss = { showTranslateDialog = false },
            onLanguageSelected = { language ->
                showTranslateDialog = false
                language.tag?.let { tag ->
                    scope.launch { userPreferences.setTranslateTargetLang(language) }
                    container.store.intent(FlatDetailIntent.TranslateListing(tag))
                }
            },
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0, 0, 0, 0),
            title = { },
            navigationIcon = {
                IconButton(onClick = { container.store.intent(FlatDetailIntent.NavigateBack) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            },
            actions = {
                if (flat != null) {
                    when {
                        state.isTranslating -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }

                        state.isShowingTranslation -> {
                            TextButton(
                                onClick = {
                                    container.store.intent(FlatDetailIntent.ShowOriginalListing)
                                },
                                enabled = !state.translationQuotaExhausted,
                            ) {
                                Text(stringResource(Res.string.show_original))
                            }
                        }

                        else -> {
                            TextButton(
                                onClick = { showTranslateDialog = true },
                                enabled = !state.translationQuotaExhausted,
                            ) {
                                Text(stringResource(Res.string.translate_listing))
                            }
                        }
                    }
                }
            },
        )

        when {
            flat != null -> {
                FlatDetailContent(
                    flat = flat,
                    mapState = container.mapState,
                    modifier = Modifier.fillMaxSize(),
                    detailError = state.error,
                    isLoadingMore = state.isLoading,
                    isShowingTranslation = state.isShowingTranslation,
                    isTranslating = state.isTranslating,
                    translationActionsEnabled = !state.translationQuotaExhausted,
                    onTranslateClick = { showTranslateDialog = true },
                    onShowOriginalClick = {
                        container.store.intent(FlatDetailIntent.ShowOriginalListing)
                    },
                    clickOnFavorite = {
                        container.store.intent(
                            FlatDetailIntent.ClickOnFavorite(
                                flat.platform,
                                flat.adId,
                            )
                        )
                    },
                    navigateToMap = {
                        container.store.intent(FlatDetailIntent.OpenOnMap(flat.adId))
                    },
                )
            }

            state.isLoading -> {
                Box(Modifier.fillMaxSize()) {
                    LinearProgressIndicator(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(12.dp)
                            .padding(4.dp)
                    )
                }
            }

            else -> {
                EmptyScreenContent(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun TranslateLanguageDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.translate_to_title)) },
        text = {
            Column {
                AppLanguage.entries
                    .filter { it != AppLanguage.SYSTEM && it.tag != null }
                    .forEach { language ->
                        Text(
                            text = stringResource(language.labelRes()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLanguageSelected(language) }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
private fun FlatDetailContent(
    flat: UiDetailFlat,
    mapState: MapState,
    modifier: Modifier = Modifier,
    detailError: String? = null,
    isLoadingMore: Boolean = false,
    isShowingTranslation: Boolean = false,
    isTranslating: Boolean = false,
    translationActionsEnabled: Boolean = true,
    onTranslateClick: () -> Unit = {},
    onShowOriginalClick: () -> Unit = {},
    clickOnFavorite: () -> Unit,
    navigateToMap: () -> Unit
) {
    var photoPageIndex by rememberSaveable { mutableIntStateOf(0) }
    var fullscreenPhotoIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        // Изображения
        FlatImagePager(
            modifier = Modifier.height(300.dp),
            flatPlatform = flat.platform,
            imageUrls = flat.imageUrls,
            contentScale = ContentScale.Fit,
            savedInFavorite = flat.savedInFavorite,
            saveInFavoriteInProgress = flat.saveInFavoriteInProgress,
            clickOnFavorite = clickOnFavorite,
            enablePhotoTapZones = true,
            initialPage = photoPageIndex,
            onPageChange = { photoPageIndex = it },
            onCenterTap = { index -> fullscreenPhotoIndex = index },
        )

        if (isLoadingMore) {
            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )
        }

        if (!detailError.isNullOrBlank()) {
            Text(
                text = detailError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FlatHubTheme.dimens.screenHorizontal, vertical = 8.dp),
            )
        }

        fullscreenPhotoIndex?.let { index ->
            FullscreenPhotoViewer(
                imageUrls = flat.imageUrls,
                initialPage = index,
                onDismiss = { lastPage ->
                    photoPageIndex = lastPage
                    fullscreenPhotoIndex = null
                },
            )
        }

        // Основная информация
        Column(
            modifier = Modifier.padding(FlatHubTheme.dimens.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(FlatHubTheme.dimens.listItemSpacing)
        ) {
            // Информация о публикации
            flat.publishedAt?.let { timeAgo ->
                PublicationInfo(timeAgo = timeAgo)
            }

            SourceLinkSection(flat.platform, flat.flatUrl)

            ListingTranslationButton(
                isShowingTranslation = isShowingTranslation,
                isTranslating = isTranslating,
                enabled = translationActionsEnabled,
                onTranslateClick = onTranslateClick,
                onShowOriginalClick = onShowOriginalClick,
            )

            if (flat.isDetailDataLoaded == true) {
                if (hasContactData(flat.contactInformation) || flat.isOwner != null) {
                    ContactInfoSection(
                        contactInfo = flat.contactInformation,
                        isOwner = flat.isOwner,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.detail_contact_on_source),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Цены
            PriceSection(priceText = flat.priceText)

            val propertyName = flat.commercialUiInfo?.propertyType?.commercialPropertyTypeName
            propertyName?.let { name ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = localizedStringResource(name),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Адрес и метро
            AddressSection(
                address = flat.address,
                metroStation = flat.metroStation
            )

            // Краткая информация
            QuickInfoSection(
                rooms = flat.numberOfRooms,
                year = flat.yearBuilt,
                area = flat.totalArea,
                condition = flat.condition
            )

            if (flat.coordinates != null) {
                val coroutineScope = rememberCoroutineScope()
                mapState.apply {
                    val mercatorCoordinates =
                        flat.coordinates?.let { lonLatToNormalized(it.latitude, it.longitude) }
                            ?: return
                    coroutineScope.launch {
                        snapScrollTo(mercatorCoordinates.first, mercatorCoordinates.second)
                    }
                    addMarker(
                        id = flat.adId.toString(),
                        x = mercatorCoordinates.first,
                        y = mercatorCoordinates.second,
                    ) {
                        RoomMarker(
                            rooms = flat.numberOfRooms.toIntOrNull(),
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier.padding(top = 24.dp, bottom = 12.dp).fillMaxWidth()
                        .height(200.dp).clickable {
                            navigateToMap()
                        }) {
                    MapUI(modifier = Modifier.fillMaxSize(), state = mapState)
                }

                OpenInMapButton(
                    coordinates = flat.coordinates!!,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            // О квартире
            if (hasApartmentData(flat)) {
                HorizontalDivider()
                AboutApartmentSection(flat = flat)
            }

            // Удобства и оборудование
            if (flat.amenities.isNotEmpty() || flat.kitchenEquipment.isNotEmpty()) {
                HorizontalDivider()
                AmenitiesSection(
                    amenities = flat.amenities,
                    kitchenEquipment = flat.kitchenEquipment
                )
            }

            // Условия проживания
            if (!flat.forWhom.isNullOrEmpty() || !flat.prepaymentType.isNullOrBlank()) {
                HorizontalDivider()
                ConditionsSection(
                    forWhom = flat.forWhom,
                    prepaymentType = flat.prepaymentType
                )
            }

            // О доме
            if (hasBuildingData(flat)) {
                HorizontalDivider()
                AboutBuildingSection(flat = flat)
            }

            // Местоположение
            if (!flat.district.isNullOrBlank() || !flat.metroStation.isNullOrBlank() || flat.address.isNotBlank()) {
                HorizontalDivider()
                LocationSection(
                    district = flat.district,
                    address = flat.address,
                    metroStation = flat.metroStation
                )
            }

            // Описание
            if (flat.description.isNotBlank()) {
                HorizontalDivider()
                DescriptionSection(description = flat.description)
            }
        }
    }
}

// Вспомогательные функции для проверки наличия данных
private fun hasApartmentData(flat: UiDetailFlat): Boolean {
    return flat.totalArea != null ||
            flat.livingArea != null ||
            flat.kitchenArea != null ||
            flat.floor != null ||
            !flat.bathroomType.isNullOrBlank() ||
            !flat.balcony.isNullOrBlank() ||
            !flat.repairType.isNullOrBlank() ||
            flat.windowDirection.isNotEmpty() ||
            flat.sleepingPlaces != null ||
            flat.commercialUiInfo != null
}

private fun hasBuildingData(flat: UiDetailFlat): Boolean {
    return flat.totalFloors != null ||
            flat.yearBuilt != null ||
            flat.buildingImprovements.isNotEmpty() ||
            !flat.parkingInfo.isNullOrBlank()
}

@Composable
private fun PublicationInfo(timeAgo: String) {
    Text(
        text = timeAgo,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun ListingTranslationButton(
    isShowingTranslation: Boolean,
    isTranslating: Boolean,
    enabled: Boolean,
    onTranslateClick: () -> Unit,
    onShowOriginalClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isTranslating -> {
            Row(
                modifier = modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(Res.string.translate_listing),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        }

        isShowingTranslation -> {
            TextButton(
                onClick = onShowOriginalClick,
                enabled = enabled,
                modifier = modifier.padding(vertical = 0.dp),
            ) {
                Text(stringResource(Res.string.show_original))
            }
        }

        else -> {
            TextButton(
                onClick = onTranslateClick,
                enabled = enabled,
                modifier = modifier.padding(vertical = 0.dp),
            ) {
                Text(stringResource(Res.string.translate_listing))
            }
        }
    }
}

@Composable
private fun SourceLinkSection(
    platform: FlatPlatform,
    url: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val shareLauncher = shareLauncher()
    val shareSubject = stringResource(Res.string.detail_share_subject)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                Res.string.detail_open_source,
                platform.value.replaceFirstChar { it.uppercase() },
            ),
            modifier = Modifier
                .weight(1f)
                .clickable {
                    uriHandler.openUri(url)
                },
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        )

        IconButton(
            onClick = {
                shareLauncher.shareText(
                    text = url,
                    subject = shareSubject
                )
            }
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(Res.string.detail_share)
            )
        }
    }
}

@Composable
private fun PriceSection(priceText: PriceText) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            priceText.mainPrice?.let { main ->
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = main,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            priceText.mainPerSquarePrice?.let {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "($it)",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                )
            }
        }
        priceText.localPrice?.let { local ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = local,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                priceText.localPerSquarePrice?.let {
                    Text(
                        text = "($it)",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun AddressSection(address: String, metroStation: String?) {
    Column {
        if (address.isNotBlank()) {
            Text(
                text = address,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
        metroStation?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

private fun hasContactData(contactInfo: ContactInformationUi?): Boolean {
    return contactInfo != null && (!contactInfo.ownerName.isNullOrBlank() || !contactInfo.phones.isNullOrEmpty())
}

@Composable
private fun ContactInfoSection(
    contactInfo: ContactInformationUi?,
    isOwner: Boolean?,
) {
    val phones = contactInfo?.phones?.filter { it.isNotBlank() }.orEmpty()
    val hasNameOrPhones = contactInfo != null &&
            (!contactInfo.ownerName.isNullOrBlank() || phones.isNotEmpty())
    if (!hasNameOrPhones && isOwner == null) {
        return
    }

    val uriHandler = LocalUriHandler.current

    SectionCard(title = stringResource(Res.string.detail_contact_info)) {
        isOwner?.let { owner ->
            Text(
                text = if (owner) {
                    stringResource(Res.string.detail_owner)
                } else {
                    stringResource(Res.string.detail_agent)
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }

        contactInfo?.ownerName?.takeIf { it.isNotBlank() }?.let { name ->
            InfoRow(label = stringResource(Res.string.detail_contact_person), value = name)
        }

        if (phones.isEmpty()) {
            Text(
                text = stringResource(Res.string.detail_contact_on_source),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }

        phones.forEach { phone ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.detail_phone),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            uriHandler.openUri("tel:$phone")
                        }
                )
            }
        }
    }
}

@Composable
private fun LocationSection(district: String?, address: String?, metroStation: String?) {
    SectionCard(title = stringResource(Res.string.detail_location)) {
        district?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_district), it)
        }
        address?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_address), it)
        }
        metroStation?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_metro), it)
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    SectionCard(title = stringResource(Res.string.detail_description)) {
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AboutBuildingSection(flat: UiDetailFlat) {
    SectionCard(title = stringResource(Res.string.detail_building)) {
        flat.totalFloors?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_total_floors), it)
        }
        flat.yearBuilt?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_year_built), it)
        }
        flat.parkingInfo?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_parking), it)
        }
        if (flat.buildingImprovements.isNotEmpty()) {
            InfoRow(stringResource(Res.string.detail_infrastructure), flat.buildingImprovements.joinToString(", "))
        }
    }
}

@Composable
private fun AboutApartmentSection(flat: UiDetailFlat) {
    val isCommercialRoom = flat.commercialUiInfo?.isCommercialAd != null
    val propertyTypeName = flat.commercialUiInfo?.propertyType?.commercialPropertyTypeName
    val roomTitle: String
    val roomNumberTitle: String
    val roomNumber: String
    if (isCommercialRoom) {
        roomTitle = stringResource(Res.string.detail_about_commercial)
        roomNumberTitle = stringResource(Res.string.filter_rooms_count)
        roomNumber = flat.commercialUiInfo?.numberOfRooms.orEmpty()
    } else {
        roomTitle = stringResource(Res.string.detail_about_apartment)
        roomNumberTitle = stringResource(Res.string.detail_rooms_count)
        roomNumber = flat.numberOfRooms
    }
    SectionCard(title = roomTitle) {
        propertyTypeName?.let { name ->
            InfoRow(stringResource(Res.string.filter_property_type), localizedStringResource(name))
        }
        if (flat.numberOfRooms.isNotBlank()) {
            InfoRow(roomNumberTitle, roomNumber)
        }
        flat.totalArea?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_total_area), it)
        }
        flat.livingArea?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_living_area), it)
        }
        flat.kitchenArea?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_kitchen_area), it)
        }
        flat.floor?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_floor), it)
        }
        flat.bathroomType?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_bathroom), it)
        }
        flat.balcony?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_balcony), it)
        }
        flat.repairType?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_repair), it)
        }
        if (flat.windowDirection.isNotEmpty()) {
            InfoRow(stringResource(Res.string.detail_windows), flat.windowDirection.joinToString(", "))
        }
        flat.sleepingPlaces?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_sleeping_places), it)
        }
    }
}

@Composable
private fun AmenitiesSection(
    amenities: List<String>,
    kitchenEquipment: List<String>
) {
    SectionCard(title = stringResource(Res.string.detail_amenities)) {
        if (amenities.isNotEmpty()) {
            InfoRow(stringResource(Res.string.detail_in_apartment), amenities.joinToString(", "))
        }
        if (kitchenEquipment.isNotEmpty()) {
            InfoRow(stringResource(Res.string.detail_in_kitchen), kitchenEquipment.joinToString(", "))
        }
    }
}

@Composable
private fun ConditionsSection(
    forWhom: List<String>?,
    prepaymentType: String?
) {
    SectionCard(title = stringResource(Res.string.detail_living_conditions)) {
        forWhom?.takeIf { it.isNotEmpty() }?.let {
            InfoRow(stringResource(Res.string.detail_for_whom), it.joinToString(", "))
        }
        prepaymentType?.takeIf { it.isNotBlank() }?.let {
            InfoRow(stringResource(Res.string.detail_prepayment), it)
        }
    }
}

@Composable
private fun QuickInfoSection(
    rooms: String,
    year: String?,
    area: String?,
    condition: String?
) {
    val roomsSuffix = stringResource(Res.string.list_rooms_suffix)
    val yearSuffix = stringResource(Res.string.detail_year_suffix)
    val info = buildString {
        if (rooms.isNotBlank()) {
            append(rooms)
            append(" $roomsSuffix")
        }
        year?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append("$it $yearSuffix")
        }
        area?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
        condition?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" • ")
            append(it)
        }
    }

    if (info.isNotBlank()) {
        Text(
            text = info,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}