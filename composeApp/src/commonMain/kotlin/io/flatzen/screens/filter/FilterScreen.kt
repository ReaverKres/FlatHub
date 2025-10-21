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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.SaveFilterDialog
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.commoncomponents.utils.asIntPrice
import io.flatzen.commoncomponents.utils.asPriceFormat
import io.flatzen.commoncomponents.utils.onlyIntPredicate
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.filter.LocationUiFilter
import io.flatzen.viewmodel.filter.Room
import io.flatzen.viewmodel.filter.SavedFilterState
import io.flatzen.widgets.AppTextField
import io.flatzen.widgets.FilterSwitch
import io.flatzen.widgets.RentSaleButtons
import io.flatzen.widgets.SortOptionRadioButtons
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    navigateBack: () -> Unit,
    onOpenLocation: () -> Unit = {},
) {
    val viewModel: FilterViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var currentFilters by remember(state.filters) { mutableStateOf(state.filters) }
    var clearAllEffectKey by remember { mutableStateOf(0) }

    LaunchedEffect(currentFilters) {
        viewModel.onIntent(FilterScreenAction.UpdateFilter(currentFilters, false))
    }

    LaunchedEffectOnce(Unit) {
        // Track screen view through MviAction
        viewModel.onIntent(
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
                title = { Text("Фильтры") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        clearAllEffectKey = clearAllEffectKey + 1
                        viewModel.onIntent(FilterScreenAction.ClearAllFilters)
                    }) {
                        Text("Сбросить")
                    }
                }
            )
        },
    ) { paddingValues ->
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
                    title = "Мои фильтры",
                    style = MaterialTheme.typography.titleMedium
                )
                SavedFiltersChips(
                    savedFilters = state.savedFilters,
                    onFilterClick = { filter ->
                        viewModel.onIntent(FilterScreenAction.ToggleSavedFilterSelection(filter.id))
                    },
                    onDeleteClick = { filterId ->
                        viewModel.onIntent(FilterScreenAction.DeleteSavedFilter(filterId))
                    }
                )
                HorizontalDivider()
            }

            // Продажа или Аренда
            RentSaleButtons(state.filters.adType) {
                currentFilters = currentFilters.copy(adType = it)
            }

            // Сортировка
            FilterSectionTitle(title = "Сортировка")
            SortOptionRadioButtons(state.filters.sortOption) { sortOption ->
                viewModel.onIntent(FilterScreenAction.UpdateSortOption(sortOption))
            }
            // Расположение
            FilterSectionTitle(title = "Расположение", modifier = Modifier.padding(top = 4.dp))
            LocationItem(
                selectedCity = state.filters.location?.selectedCity?.displayName,
                selectedMetro = state.filters.getSelectedMetroStation(),
                selectedAddress = state.filters.getSelectedAddress(),
                isLocationFilterActive = currentFilters.isLocationFilterActive(),
                onOpenLocation = {
                    onOpenLocation()
                }
            )

            FilterSwitch("Только от собственника", currentFilters.fromOwnerOnly) {
                currentFilters = currentFilters.copy(fromOwnerOnly = it)
            }
            Spacer(Modifier.height(6.dp))
            FilterSwitch("Только с фото", currentFilters.withPhotoOnly) {
                currentFilters = currentFilters.copy(withPhotoOnly = it)
            }

            FilterSectionTitle(title = "Комнат в квартире")
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Room.values().forEach {
                    val room = it.displayName.toInt()
                    FilterChip(
                        selected = currentFilters.rooms.contains(room),
                        onClick = {
                            val newTypes = currentFilters.rooms.toMutableSet()
                            if (newTypes.contains(room)) newTypes.remove(room) else newTypes.add(
                                room
                            )
                            currentFilters = currentFilters.copy(rooms = newTypes)
                        },
                        label = { Text(it.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            NumberRange(
                title = "Цена ($)",
                launchedKey = clearAllEffectKey,
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
            Spacer(Modifier.height(10.dp))
            NumberRange(
                title = "Цена за м2 ($)",
                launchedKey = clearAllEffectKey,
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

            Spacer(Modifier.height(10.dp))

            NumberRange(
                title = "Площадь",
                launchedKey = clearAllEffectKey,
                rangeFrom = currentFilters.totalArea?.fromRange?.toString().orEmpty(),
                fromOnChange = {
                    currentFilters = currentFilters.copy(
                        totalArea = currentFilters.totalArea?.copy(
                            fromRange = it.toDoubleOrNull()
                        ) ?: FromToRange(
                            fromRange = it.toDoubleOrNull()
                        )
                    )
                },
                rangeTo = currentFilters.totalArea?.toRange?.toString().orEmpty(),
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
                    viewModel.onIntent(FilterScreenAction.ShowSaveFilterDialog)
                }) {
                Text("Добавить в Мои фильтры")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Диалог сохранения фильтра
    if (state.dialogState.isVisible) {
        SaveFilterDialog(
            dialogState = state.dialogState,
            onNameChange = { name ->
                viewModel.onIntent(FilterScreenAction.UpdateFilterName(name))
            },
            onSave = {
                viewModel.onIntent(FilterScreenAction.SaveFilter)
            },
            onCancel = {
                viewModel.onIntent(FilterScreenAction.HideSaveFilterDialog)
            }
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
                            contentDescription = "Удалить фильтр",
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
    selectedCity: String?,
    selectedMetro: String,
    selectedAddress: String?,
    isLocationFilterActive: Boolean,
    onOpenLocation: () -> Unit,
    ) {
    Row(
        modifier = Modifier
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
                    text = "Метро: $selectedMetro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!selectedAddress.isNullOrEmpty()) {
                Text(
                    text = "Адрес: $selectedAddress",
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
    launchedKey: Any = Unit,
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
            launchedKey = launchedKey,
            text = rangeFrom,
            label = "От",
            onChangePredicate = onlyIntPredicate,
            onChange = fromOnChange
        )
        AppTextField(
            modifier = Modifier.weight(1f),
            launchedKey = launchedKey,
            text = rangeTo,
            label = "До",
            onChangePredicate = onlyIntPredicate,
            onChange = toOnChange
        )
    }
}
