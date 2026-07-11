package io.flatzen.screens.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.add
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.delete
import flatzen.composeapp.generated.resources.filter_active_areas_prefix
import flatzen.composeapp.generated.resources.filter_address_prefix
import flatzen.composeapp.generated.resources.filter_add_to_my_filters
import flatzen.composeapp.generated.resources.filter_area
import flatzen.composeapp.generated.resources.filter_booking_date
import flatzen.composeapp.generated.resources.filter_deal_type
import flatzen.composeapp.generated.resources.filter_districts_prefix
import flatzen.composeapp.generated.resources.filter_location
import flatzen.composeapp.generated.resources.filter_metro_prefix
import flatzen.composeapp.generated.resources.filter_my_filters
import flatzen.composeapp.generated.resources.filter_owner_only
import flatzen.composeapp.generated.resources.filter_photo_only
import flatzen.composeapp.generated.resources.filter_price
import flatzen.composeapp.generated.resources.filter_price_daily
import flatzen.composeapp.generated.resources.filter_price_per_square
import flatzen.composeapp.generated.resources.filter_property_type
import flatzen.composeapp.generated.resources.filter_rent
import flatzen.composeapp.generated.resources.filter_room_only
import flatzen.composeapp.generated.resources.filter_rooms_count
import flatzen.composeapp.generated.resources.filter_rooms_in_apartment
import flatzen.composeapp.generated.resources.filter_sale
import flatzen.composeapp.generated.resources.filter_selected_date_range
import flatzen.composeapp.generated.resources.filter_sorting
import flatzen.composeapp.generated.resources.filters_title
import flatzen.composeapp.generated.resources.from
import flatzen.composeapp.generated.resources.reset
import flatzen.composeapp.generated.resources.to
import io.flatzen.common.localization.stringResource as localizedStringResource
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.BookingDatesFilter
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.date.DateConverter
import io.flatzen.commoncomponents.utils.asIntPrice
import io.flatzen.commoncomponents.utils.onlyIntPredicate
import io.flatzen.di.container
import io.flatzen.entities.SingleChoiceEntity
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.viewmodel.UiDistrict
import io.flatzen.viewmodel.filter.CommercialPropertyTypeInfo
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.filter.FilterEffect
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.LocationUiFilter
import io.flatzen.viewmodel.filter.MapAreasUi
import io.flatzen.viewmodel.filter.Room
import io.flatzen.viewmodel.filter.SavedFilterState
import io.flatzen.viewmodel.notifications.ToggleNotificationsAction
import io.flatzen.viewmodel.notifications.ToggleNotificationsContainer
import io.flatzen.viewmodel.notifications.ToggleNotificationsIntent
import io.flatzen.widgets.AppReadOnlyTextField
import io.flatzen.widgets.AppSwitch
import io.flatzen.widgets.AppTextField
import io.flatzen.widgets.DateRangePickerDialog
import io.flatzen.widgets.RentSaleButtons
import io.flatzen.widgets.SortOptionRadioButtons
import io.flatzen.widgets.dialogs.SaveDialog
import io.flatzen.widgets.dialogs.SingleChoiceDialog
import io.flatzen.widgets.dialogs.SystemSettingsDialog
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.subscribe
import pro.respawn.flowmvi.dsl.intent
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun FilterScreen(
    navigateBack: () -> Unit,
    onOpenLocation: () -> Unit = {},
    onOpenReferralScreen: () -> Unit,
) {
    val filterContainer: FilterContainer = container()
    val state by filterContainer.store.subscribe { action ->
        when (action) {
            is FilterEffect.NavigateToReferralEffect -> onOpenReferralScreen()
        }
    }
    var currentFilters by remember(state.filters) { mutableStateOf(state.filters) }

    val factory = rememberPermissionsControllerFactory()
    val permissionsController: PermissionsController = remember(factory) { factory.createPermissionsController() }
    BindEffect(permissionsController)

    var showCommercialAdTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showCommercialPropertyTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationsSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val toggleNotificationsContainer: ToggleNotificationsContainer = container {
        parametersOf(permissionsController)
    }
    toggleNotificationsContainer.store.subscribe { action ->
        when (action) {
            is ToggleNotificationsAction.ShowSettingsDialog -> showNotificationsSettingsDialog = true
        }
    }
    val propertyTypes: List<SingleChoiceEntity<CommercialPropertyType>> =
        currentFilters.commercial.commercialPropertyType?.mapNotNull {
            it.commercialPropertyType?.let { propertyType ->
                SingleChoiceEntity(
                    title = it.commercialPropertyTypeName?.let { key -> localizedStringResource(key) }.orEmpty(),
                    type = propertyType
                )
            }
        } ?: listOf()
    val selectedCommercialPropertyType: CommercialPropertyTypeInfo? by remember(state.filters) {
        val selectedItem = state.filters.commercial.commercialPropertyType?.find { it.selected }
        mutableStateOf(selectedItem)
    }

    LaunchedEffect(currentFilters) {
        filterContainer.intent(FilterScreenAction.UpdateFilter(currentFilters, false))
    }

    LaunchedEffectOnce(Unit) {
        // Track screen view through MviAction
        filterContainer.intent(
            FilterScreenAction.TrackScreenView(
                screenName = AppMetrcica.Screens.FILTER,
                parameters = mapOf(
                    AppMetrcica.Parameters.SCREEN_TYPE to AppMetrcica.ScreenTypes.MODAL
                )
            )
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(stringResource(Res.string.filters_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = {
                        filterContainer.intent(FilterScreenAction.ClearAllFilters)
                    }) {
                        Text(stringResource(Res.string.reset))
                    }
                }
            )
        },
    ) { paddingValues ->
        if (showCommercialPropertyTypeDialog) {
            SingleChoiceDialog(
                title = stringResource(Res.string.filter_property_type),
                items = propertyTypes,
                selectedItem = selectedCommercialPropertyType?.commercialPropertyType,
                onDismissRequest = {
                    showCommercialPropertyTypeDialog = false
                },
                onSelected = { selectedPropertyType ->
                    filterContainer.intent(
                        FilterScreenAction.UpdateSelectedCommercialPropertyType(
                            selectedPropertyType
                        )
                    )
                    showCommercialPropertyTypeDialog = false
                }
            )
        }

        if (showCommercialAdTypeDialog) {
            SingleChoiceDialog(
                title = stringResource(Res.string.filter_deal_type),
                items = listOf(
                    SingleChoiceEntity(
                        title = stringResource(Res.string.filter_sale), AdType.COMMERCIAL(
                            CommercialAdType.SALE
                        )
                    ),
                    SingleChoiceEntity(
                        title = stringResource(Res.string.filter_rent), AdType.COMMERCIAL(
                            CommercialAdType.RENT
                        )
                    )
                ),
                selectedItem = currentFilters.lastCommercialAdType,
                onDismissRequest = {
                    showCommercialAdTypeDialog = false
                },
                onSelected = { adType ->
                    currentFilters = currentFilters.copy(
                        adType = adType,
                        lastCommercialAdType = adType
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Сохраненные фильтры
            if (state.savedFilters.isNotEmpty()) {
                HorizontalDivider()
                FilterSectionTitle(
                    title = stringResource(Res.string.filter_my_filters),
                    style = MaterialTheme.typography.titleMedium
                )
                SavedFiltersChips(
                    savedFilters = state.savedFilters,
                    onFilterClick = { filter ->
                        filterContainer.intent(FilterScreenAction.ToggleSavedFilterSelection(filter.id))
                    },
                    onDeleteClick = { filterId ->
                        filterContainer.intent(FilterScreenAction.DeleteSavedFilter(filterId))
                    }
                )
                HorizontalDivider()
            }

            // Продажа или Аренда
            RentSaleButtons(
                selectedAdType = state.filters.adType,
                lastCommercialAdType = state.filters.lastCommercialAdType,
                onClick = {
                    currentFilters = currentFilters.copy(adType = it)
                },
                fewTypeInOneClick = {
                    if (it.isCommercial) {
                        showCommercialAdTypeDialog = true
                    }
                },
            )

            Spacer(Modifier.height(8.dp))

            // Сортировка
            FilterSectionTitle(title = stringResource(Res.string.filter_sorting))
            SortOptionRadioButtons(state.filters.sortOption) { sortOption ->
                filterContainer.intent(FilterScreenAction.UpdateSortOption(sortOption))
            }
            Spacer(Modifier.height(8.dp))
            // Расположение
            FilterSectionTitle(title = stringResource(Res.string.filter_location))
            Spacer(Modifier.height(4.dp))
            Card {
                LocationItem(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    selectedCity = state.filters.location?.selectedCity?.displayName,
                    selectedMetro = state.filters.getSelectedMetroStation(),
                    selectedAddress = state.filters.getSelectedAddress(),
                    selectedUserAreas = state.filters.userMapAreas?.filter { it.isActive },
                    selectedDistricts = state.filters.districtsArea?.filter { it.isChecked },
                    isLocationFilterActive = currentFilters.isLocationFilterActive(),
                    onOpenLocation = {
                        onOpenLocation()
                    }
                )
            }

            if(currentFilters.adType != AdType.DAILY) {
                AppSwitch(label = stringResource(Res.string.filter_owner_only), state = currentFilters.fromOwnerOnly) {
                    currentFilters = currentFilters.copy(fromOwnerOnly = it)
                }
            }
            AppSwitch(label = stringResource(Res.string.filter_photo_only), state = currentFilters.withPhotoOnly) {
                currentFilters = currentFilters.copy(withPhotoOnly = it)
            }
            if (currentFilters.adType == AdType.RENT) {
                AppSwitch(label = stringResource(Res.string.filter_room_only), state = currentFilters.roomOnly) {
                    currentFilters = currentFilters.copy(roomOnly = it)
                }
            }

            if (currentFilters.adType.isCommercial.not() && currentFilters.roomOnly.not()) {
                Spacer(Modifier.height(6.dp))
                FilterSectionTitle(title = stringResource(Res.string.filter_rooms_in_apartment))
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Room.entries.forEach {
                        val room = it.displayName.toInt()
                        FilterChip(
                            selected = currentFilters.rooms.contains(room),
                            onClick = {
                                val rooms = currentFilters.rooms.toMutableSet()
                                if (rooms.contains(room)) rooms.remove(room) else rooms.add(
                                    room
                                )
                                currentFilters = currentFilters.copy(rooms = rooms)
                            },
                            label = { Text(it.displayName) }
                        )
                    }
                }
            } else if(currentFilters.roomOnly.not()) {
                Spacer(Modifier.height(6.dp))
                NumberRange(
                    title = stringResource(Res.string.filter_rooms_count),
                    rangeFrom = currentFilters.commercial.roomRange?.fromRange?.toInt()?.toString()
                        .orEmpty(),
                    fromOnChange = {
                        currentFilters = currentFilters.copy(
                            commercial = currentFilters.commercial.copy(
                                roomRange = currentFilters.commercial.roomRange?.copy(
                                    fromRange = it.toDoubleOrNull()
                                ) ?: FromToRange(fromRange = it.toDoubleOrNull())
                            )
                        )
                    },
                    rangeTo = currentFilters.commercial.roomRange?.toRange?.toInt()?.toString()
                        .orEmpty(),
                    toOnChange = {
                        currentFilters = currentFilters.copy(
                            commercial = currentFilters.commercial.copy(
                                roomRange = currentFilters.commercial.roomRange?.copy(
                                    toRange = it.toDoubleOrNull()
                                ) ?: FromToRange(toRange = it.toDoubleOrNull())
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (currentFilters.adType.isCommercial) {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showCommercialPropertyTypeDialog = true }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            modifier = Modifier
                                .wrapContentSize()
                                .padding(vertical = 6.dp),
                            text = stringResource(Res.string.filter_property_type),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        selectedCommercialPropertyType?.commercialPropertyTypeName?.let {
                            Text(
                                text = localizedStringResource(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if(currentFilters.adType == AdType.DAILY) {
                var showDatePicker by rememberSaveable { mutableStateOf(false) }

                // Get the current booking dates from the filter
                val startDateMillis = currentFilters.bookingDatesFilter?.dateFrom?.toEpochMilliseconds()
                val endDateMillis = currentFilters.bookingDatesFilter?.dateTo?.toEpochMilliseconds()
                
                // Format the date range for display
                val formattedDateRange = if (startDateMillis != null && endDateMillis != null) {
                    val dateFrom = Instant.fromEpochMilliseconds(startDateMillis)
                    val dateFromText = DateConverter.formatInstant(
                        instant = dateFrom,
                        timeZone = TimeZone.currentSystemDefault(),
                        onlyDayAndMonth = true
                    )
                    val dateTo = Instant.fromEpochMilliseconds(endDateMillis)
                    val dateToText = DateConverter.formatInstant(
                        instant = dateTo,
                        timeZone = TimeZone.currentSystemDefault(),
                        onlyDayAndMonth = true
                    )
                    stringResource(Res.string.filter_selected_date_range, dateFromText, dateToText)
                } else {
                    ""
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppReadOnlyTextField(
                        modifier = Modifier.weight(1f),
                        text = formattedDateRange,
                        label = stringResource(Res.string.filter_booking_date),
                        onChange = {},
                        onClick = { showDatePicker = true }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = {
                        currentFilters = currentFilters.copy(bookingDatesFilter = null)
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.delete))
                    }
                }

                if (showDatePicker) {
                    DateRangePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        onDateRangeSelected = { dateRange ->
                            val (startDate, endDate) = dateRange
                            if (startDate != null && endDate != null) {
                                // Update the filter with the selected dates
                                val bookingDatesFilter = BookingDatesFilter(
                                    dateFrom = Instant.fromEpochMilliseconds(startDate),
                                    dateTo = Instant.fromEpochMilliseconds(endDate),
                                    timeZone = TimeZone.currentSystemDefault()
                                )
                                currentFilters = currentFilters.copy(bookingDatesFilter = bookingDatesFilter)
                            }
                            showDatePicker = false
                        },
                        initialSelectedStartDateMillis = startDateMillis,
                        initialSelectedEndDateMillis = endDateMillis
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            val currencyText = if(currentFilters.adType == AdType.DAILY) "(BYN)" else "($)"
            val priceTitle = if(currentFilters.adType == AdType.DAILY) {
                stringResource(Res.string.filter_price_daily, currencyText)
            } else {
                stringResource(Res.string.filter_price, currencyText)
            }
            NumberRange(
                title = priceTitle,
                rangeFrom = currentFilters.priceFull?.priceFrom?.asIntPrice().orEmpty(),
                fromOnChange = {
                    currentFilters = currentFilters.copy(
                        priceFull = currentFilters.priceFull?.copy(
                            priceFrom = it.toDoubleOrNull()
                        ) ?: Price(
                            priceFrom = it.toDoubleOrNull()
                        )
                    )
                },
                rangeTo = currentFilters.priceFull?.priceTo?.asIntPrice().orEmpty(),
                toOnChange = {
                    currentFilters = currentFilters.copy(
                        priceFull = currentFilters.priceFull?.copy(
                            priceTo = it.toDoubleOrNull()
                        ) ?: Price(
                            priceTo = it.toDoubleOrNull()
                        )
                    )
                }
            )
            if(currentFilters.adType != AdType.DAILY) {
                Spacer(Modifier.height(10.dp))
                NumberRange(
                    title = stringResource(Res.string.filter_price_per_square, currencyText),
                    rangeFrom = currentFilters.pricePerSquare?.priceFrom?.asIntPrice().orEmpty(),
                    fromOnChange = {
                        currentFilters = currentFilters.copy(
                            pricePerSquare = currentFilters.pricePerSquare?.copy(
                                priceFrom = it.toDoubleOrNull()
                            ) ?: Price(
                                priceFrom = it.toDoubleOrNull()
                            )
                        )
                    },
                    rangeTo = currentFilters.pricePerSquare?.priceTo?.asIntPrice().orEmpty(),
                    toOnChange = {
                        currentFilters = currentFilters.copy(
                            pricePerSquare = currentFilters.pricePerSquare?.copy(
                                priceTo = it.toDoubleOrNull()
                            ) ?: Price(
                                priceTo = it.toDoubleOrNull()
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            NumberRange(
                title = stringResource(Res.string.filter_area),
                rangeFrom = currentFilters.totalArea?.fromRange?.toInt()?.toString().orEmpty(),
                fromOnChange = {
                    currentFilters = currentFilters.copy(
                        totalArea = currentFilters.totalArea?.copy(
                            fromRange = it.toDoubleOrNull()
                        ) ?: FromToRange(
                            fromRange = it.toDoubleOrNull()
                        )
                    )
                },
                rangeTo = currentFilters.totalArea?.toRange?.toInt()?.toString().orEmpty(),
                toOnChange = {
                    currentFilters = currentFilters.copy(
                        totalArea = currentFilters.totalArea?.copy(
                            toRange = it.toDoubleOrNull()
                        ) ?: FromToRange(
                            toRange = it.toDoubleOrNull()
                        )
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = {
                    filterContainer.intent(FilterScreenAction.ShowSaveFilterDialog)
                }) {
                Text(stringResource(Res.string.filter_add_to_my_filters))
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Диалог сохранения фильтра
    if (state.saveDialogState.isVisible) {
        SaveDialog(
            dialogState = state.saveDialogState,
            onNameChange = { name ->
                filterContainer.intent(FilterScreenAction.UpdateFilterName(name))
            },
            onNotificationChange = { enabled ->
                filterContainer.intent(FilterScreenAction.NotificationEnable(enabled))
            },
            onSave = { notificationEnabled ->
                currentFilters = currentFilters.copy(
                    name = state.saveDialogState.filterName,
                    isNotificationEnabled = notificationEnabled
                )
                toggleNotificationsContainer.intent(
                    ToggleNotificationsIntent.ToggleNotifications(
                        filterName = state.saveDialogState.filterName,
                        enabled = notificationEnabled
                    )
                )
                filterContainer.intent(FilterScreenAction.SaveFilter)
            },
            onCancel = {
                filterContainer.intent(FilterScreenAction.HideSaveFilterDialog)
            }
        )
    }

	if (showNotificationsSettingsDialog) {
        SystemSettingsDialog(
            onDismissRequest = { showNotificationsSettingsDialog = false },
            onConfirmClick ={
                showNotificationsSettingsDialog = false
                permissionsController.openAppSettings()
            },
            onCloseClick = { showNotificationsSettingsDialog = false }
        )
	}
}

@Composable
private fun FilterSectionTitle(
    modifier: Modifier = Modifier.padding(vertical = 4.dp),
    title: String,
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    Text(
        text = title,
        style = style,
        modifier = modifier
    )
}

@Composable
private fun SavedFiltersChips(
    savedFilters: List<SavedFilterState>,
    onFilterClick: (SavedFilterState) -> Unit,
    onDeleteClick: (Long) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        savedFilters.forEach { filter ->
            FilterChip(
                selected = filter.selected,
                onClick = { onFilterClick(filter) },
                label = { Text(filter.name) },
                trailingIcon = {
                    IconButton(
                        onClick = { onDeleteClick(filter.id) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.delete),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun LocationItem(
    modifier: Modifier,
    selectedCity: String?,
    selectedMetro: String,
    selectedAddress: String?,
    selectedDistricts: List<UiDistrict>?,
    selectedUserAreas: List<MapAreasUi>?,
    isLocationFilterActive: Boolean,
    onOpenLocation: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onOpenLocation() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = selectedCity
                    ?: LocationUiFilter().selectedCity.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (selectedMetro.isNotEmpty()) {
                Text(
                    text = "${stringResource(Res.string.filter_metro_prefix)}: $selectedMetro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!selectedAddress.isNullOrEmpty()) {
                Text(
                    text = "${stringResource(Res.string.filter_address_prefix)}: $selectedAddress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!selectedDistricts.isNullOrEmpty()) {
                val areasText = selectedDistricts.joinToString(separator = ", ") { it.nameLocal }
                Text(
                    text = "${stringResource(Res.string.filter_districts_prefix)}: $areasText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!selectedUserAreas.isNullOrEmpty()) {
                val areasText = selectedUserAreas.joinToString(separator = ", ") { it.name }
                Text(
                    text = "${stringResource(Res.string.filter_active_areas_prefix)}: $areasText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

//        if (isLocationFilterActive) {
//            Icon(
//                Icons.Default.CheckCircle,
//                contentDescription = "Фильтр активен",
//                tint = MaterialTheme.colorScheme.primary
//            )
//        }
    }
}

@Composable
private fun NumberRange(
    title: String,
    rangeFrom: String,
    fromOnChange: (String) -> Unit,
    rangeTo: String,
    toOnChange: (String) -> Unit,
) {

    FilterSectionTitle(title = title, modifier = Modifier)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            modifier = Modifier.weight(1f),
            text = rangeFrom,
            label = stringResource(Res.string.from),
            onChangePredicate = onlyIntPredicate,
            onChange = fromOnChange
        )
        AppTextField(
            modifier = Modifier.weight(1f),
            text = rangeTo,
            label = stringResource(Res.string.to),
            onChangePredicate = onlyIntPredicate,
            onChange = toOnChange
        )
    }
}
