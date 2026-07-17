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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.delete
import flatzen.composeapp.generated.resources.filter_active_areas_prefix
import flatzen.composeapp.generated.resources.filter_add_to_my_filters
import flatzen.composeapp.generated.resources.filter_address_prefix
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
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.BookingDatesFilter
import io.flatzen.commoncomponents.commonentities.CommercialAdType
import io.flatzen.commoncomponents.commonentities.CommercialPropertyType
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.commonentities.isCommercial
import io.flatzen.commoncomponents.commonentities.supportsCommercialPropertyTypeFilter
import io.flatzen.commoncomponents.date.DateConverter
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.commoncomponents.utils.asIntPrice
import io.flatzen.commoncomponents.utils.onlyIntPredicate
import io.flatzen.di.container
import io.flatzen.entities.SingleChoiceEntity
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.utils.ToastDurationType
import io.flatzen.utils.ToastLauncher
import io.flatzen.viewmodel.UiDistrict
import io.flatzen.viewmodel.filter.FilterContainer
import io.flatzen.viewmodel.filter.FilterEffect
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterState
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
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.subscribe
import pro.respawn.flowmvi.dsl.intent
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import io.flatzen.common.localization.stringResource as localizedStringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun FilterScreen() {
    val filterContainer: FilterContainer = container()
    val toastLauncher = remember { ToastLauncher() }
    var pendingToastKey by remember { mutableStateOf<LocalizationKeys?>(null) }
    val state by filterContainer.store.subscribe { action ->
        when (action) {
            is FilterEffect.ShowToastEffect -> pendingToastKey = action.messageKey
        }
    }
    val resolvedToast = pendingToastKey?.let { localizedStringResource(it) }
    LaunchedEffect(pendingToastKey) {
        resolvedToast?.let { text ->
            toastLauncher.showToast(text, ToastDurationType.LONG)
            pendingToastKey = null
        }
    }
    val filters = state.filters
    val updateFilter: (FilterState) -> Unit = { newFilters ->
        filterContainer.intent(FilterScreenAction.UpdateFilter(newFilters, doNetworkCall = false))
    }

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
        filters.commercial.commercialPropertyType?.mapNotNull {
            it.commercialPropertyType?.let { propertyType ->
                SingleChoiceEntity(
                    title = it.commercialPropertyTypeName?.let { key -> localizedStringResource(key) }.orEmpty(),
                    type = propertyType
                )
            }
        } ?: listOf()
    val selectedCommercialPropertyType =
        filters.commercial.commercialPropertyType?.find { it.selected }

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
                    IconButton(onClick = { filterContainer.intent(FilterScreenAction.NavigateBack) }) {
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
                selectedItem = filters.lastCommercialAdType,
                onDismissRequest = {
                    showCommercialAdTypeDialog = false
                },
                onSelected = { adType ->
                    updateFilter(
                        filters.copy(
                            adType = adType,
                            lastCommercialAdType = adType
                        )
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
                selectedAdType = filters.adType,
                lastCommercialAdType = filters.lastCommercialAdType,
                onClick = {
                    updateFilter(filters.copy(adType = it))
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
            SortOptionRadioButtons(filters.sortOption) { sortOption ->
                filterContainer.intent(FilterScreenAction.UpdateSortOption(sortOption))
            }
            Spacer(Modifier.height(8.dp))
            // Расположение
            FilterSectionTitle(title = stringResource(Res.string.filter_location))
            Spacer(Modifier.height(4.dp))
            Card {
                val selectedMetro = if (filters.withAnyMetro) {
                    localizedStringResource(LocalizationKeys.FILTER_METRO_ANY)
                } else {
                    filters.getSelectedMetroStation()
                }
                val countryLabel = filters.location?.selectedCountry?.name
                    ?: filters.location?.selectedCountry?.code?.name
                val cityLabel = filters.location?.selectedCity?.displayName
                LocationItem(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    selectedCity = listOfNotNull(countryLabel, cityLabel)
                        .joinToString(" · ")
                        .ifBlank { cityLabel },
                    selectedMetro = selectedMetro,
                    selectedAddress = filters.getSelectedAddress(),
                    selectedUserAreas = filters.userMapAreas?.filter { it.isActive },
                    selectedDistricts = filters.districtsArea?.filter { it.isChecked },
                    onOpenLocation = {
                        filterContainer.intent(FilterScreenAction.OpenLocation)
                    }
                )
            }

            if (filters.adType != AdType.DAILY) {
                AppSwitch(
                    label = stringResource(Res.string.filter_owner_only),
                    state = filters.fromOwnerOnly
                ) {
                    updateFilter(filters.copy(fromOwnerOnly = it))
                }
            }
            AppSwitch(
                label = stringResource(Res.string.filter_photo_only),
                state = filters.withPhotoOnly
            ) {
                updateFilter(filters.copy(withPhotoOnly = it))
            }
            if (filters.adType == AdType.RENT) {
                AppSwitch(
                    label = stringResource(Res.string.filter_room_only),
                    state = filters.roomOnly
                ) {
                    updateFilter(filters.copy(roomOnly = it))
                }
            }

            if (filters.adType.isCommercial.not() && filters.roomOnly.not()) {
                Spacer(Modifier.height(6.dp))
                FilterSectionTitle(title = stringResource(Res.string.filter_rooms_in_apartment))
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Room.entries.forEach {
                        val room = it.displayName.toInt()
                        val isSelected = filters.rooms.contains(room)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val rooms = filters.rooms.toMutableSet()
                                if (rooms.contains(room)) rooms.remove(room) else rooms.add(
                                    room
                                )
                                updateFilter(filters.copy(rooms = rooms))
                            },
                            label = {
                                Text(
                                    text = it.displayName,
                                    fontWeight = if (isSelected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Normal
                                    },
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            } else if (filters.roomOnly.not()) {
                Spacer(Modifier.height(6.dp))
                NumberRange(
                    title = stringResource(Res.string.filter_rooms_count),
                    rangeFrom = filters.commercial.roomRange?.fromRange?.toInt()?.toString()
                        .orEmpty(),
                    fromOnChange = {
                        updateFilter(
                            filters.copy(
                                commercial = filters.commercial.copy(
                                    roomRange = filters.commercial.roomRange?.copy(
                                        fromRange = it.toDoubleOrNull()
                                    ) ?: FromToRange(fromRange = it.toDoubleOrNull())
                                )
                            )
                        )
                    },
                    rangeTo = filters.commercial.roomRange?.toRange?.toInt()?.toString()
                        .orEmpty(),
                    toOnChange = {
                        updateFilter(
                            filters.copy(
                                commercial = filters.commercial.copy(
                                    roomRange = filters.commercial.roomRange?.copy(
                                        toRange = it.toDoubleOrNull()
                                    ) ?: FromToRange(toRange = it.toDoubleOrNull())
                                )
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (filters.adType.isCommercial &&
                filters.location?.selectedCountry?.code?.supportsCommercialPropertyTypeFilter() == true
            ) {
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

            if (filters.adType == AdType.DAILY) {
                var showDatePicker by rememberSaveable { mutableStateOf(false) }

                val startDateMillis = filters.bookingDatesFilter?.dateFrom?.toEpochMilliseconds()
                val endDateMillis = filters.bookingDatesFilter?.dateTo?.toEpochMilliseconds()
                
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
                        updateFilter(filters.copy(bookingDatesFilter = null))
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
                                val bookingDatesFilter = BookingDatesFilter(
                                    dateFrom = Instant.fromEpochMilliseconds(startDate),
                                    dateTo = Instant.fromEpochMilliseconds(endDate),
                                    timeZone = TimeZone.currentSystemDefault()
                                )
                                updateFilter(filters.copy(bookingDatesFilter = bookingDatesFilter))
                            }
                            showDatePicker = false
                        },
                        initialSelectedStartDateMillis = startDateMillis,
                        initialSelectedEndDateMillis = endDateMillis
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            val currencyText = "(${filters.currency.filterLabel()})"
            val priceTitle = if (filters.adType == AdType.DAILY) {
                stringResource(Res.string.filter_price_daily, currencyText)
            } else {
                stringResource(Res.string.filter_price, currencyText)
            }
            NumberRange(
                title = priceTitle,
                rangeFrom = filters.priceFull?.priceFrom?.asIntPrice().orEmpty(),
                fromOnChange = {
                    updateFilter(
                        filters.copy(
                            priceFull = filters.priceFull?.copy(
                                priceFrom = it.toDoubleOrNull()
                            ) ?: Price(
                                priceFrom = it.toDoubleOrNull()
                            )
                        )
                    )
                },
                rangeTo = filters.priceFull?.priceTo?.asIntPrice().orEmpty(),
                toOnChange = {
                    updateFilter(
                        filters.copy(
                            priceFull = filters.priceFull?.copy(
                                priceTo = it.toDoubleOrNull()
                            ) ?: Price(
                                priceTo = it.toDoubleOrNull()
                            )
                        )
                    )
                }
            )
            if (filters.adType != AdType.DAILY) {
                Spacer(Modifier.height(10.dp))
                NumberRange(
                    title = stringResource(Res.string.filter_price_per_square, currencyText),
                    rangeFrom = filters.pricePerSquare?.priceFrom?.asIntPrice().orEmpty(),
                    fromOnChange = {
                        updateFilter(
                            filters.copy(
                                pricePerSquare = filters.pricePerSquare?.copy(
                                    priceFrom = it.toDoubleOrNull()
                                ) ?: Price(
                                    priceFrom = it.toDoubleOrNull()
                                )
                            )
                        )
                    },
                    rangeTo = filters.pricePerSquare?.priceTo?.asIntPrice().orEmpty(),
                    toOnChange = {
                        updateFilter(
                            filters.copy(
                                pricePerSquare = filters.pricePerSquare?.copy(
                                    priceTo = it.toDoubleOrNull()
                                ) ?: Price(
                                    priceTo = it.toDoubleOrNull()
                                )
                            )
                        )
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            NumberRange(
                title = stringResource(Res.string.filter_area),
                rangeFrom = filters.totalArea?.fromRange?.toInt()?.toString().orEmpty(),
                fromOnChange = {
                    updateFilter(
                        filters.copy(
                            totalArea = filters.totalArea?.copy(
                                fromRange = it.toDoubleOrNull()
                            ) ?: FromToRange(
                                fromRange = it.toDoubleOrNull()
                            )
                        )
                    )
                },
                rangeTo = filters.totalArea?.toRange?.toInt()?.toString().orEmpty(),
                toOnChange = {
                    updateFilter(
                        filters.copy(
                            totalArea = filters.totalArea?.copy(
                                toRange = it.toDoubleOrNull()
                            ) ?: FromToRange(
                                toRange = it.toDoubleOrNull()
                            )
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
                label = {
                    Text(
                        text = filter.name,
                        fontWeight = if (filter.selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = filter.selected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = MaterialTheme.colorScheme.secondary,
                ),
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
                    ?: LocationUiFilter.networkDefault().selectedCity.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            LocationFilterHintRow(
                label = stringResource(Res.string.filter_metro_prefix),
                value = selectedMetro.takeIf { it.isNotEmpty() },
            )
            LocationFilterHintRow(
                label = stringResource(Res.string.filter_address_prefix),
                value = selectedAddress?.takeIf { it.isNotEmpty() },
            )
            LocationFilterHintRow(
                label = stringResource(Res.string.filter_districts_prefix),
                value = selectedDistricts
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ", ") { it.nameLocal },
            )
            LocationFilterHintRow(
                label = stringResource(Res.string.filter_active_areas_prefix),
                value = selectedUserAreas
                    ?.takeIf { it.isNotEmpty() }
                    ?.joinToString(separator = ", ") { it.name },
            )
        }
    }
}

@Composable
private fun LocationFilterHintRow(
    label: String,
    value: String?,
) {
    if (value != null) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
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
