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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.commoncomponents.commonentities.FromToRange
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.utils.onlyDoublePredicate
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.filter.Room
import io.flatzen.viewmodel.filter.SavedFilterState
import io.flatzen.widgets.AppTextField
import io.flatzen.widgets.FilterSwitch
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

    LaunchedEffect(Unit) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
            RentSaleSegmentedButtons(state.filters.adType) {
                currentFilters = currentFilters.copy(adType = it)
            }

            // Сортировка
            FilterSectionTitle(title = "Сортировка")
            SortOptionSegmentedButtons(state.filters.sortOption) { sortOption ->
                viewModel.onIntent(FilterScreenAction.UpdateSortOption(sortOption))
            }
            // Расположение
            FilterSectionTitle(title = "Расположение", modifier = Modifier.padding(top = 4.dp))
            ListItem(
                headlineContent = {
                    Text(state.filters.location?.selectedCity?.displayName.orEmpty())
                },
                supportingContent = if (state.filters.metroStationsState.isEmpty()) {
                    null
                } else {
                    { Text("Метро") }
                },
                trailingContent = {
                    if (currentFilters.isLocationFilterActive()) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLocation() }
            )

            FilterSwitch("Только от собственника", currentFilters.fromOwnerOnly) {
                currentFilters = currentFilters.copy(fromOwnerOnly = it)
            }

            FilterSectionTitle(title = "Комнат в квартире")
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

            NumberRange(
                title = "Цена",
                launchedKey = clearAllEffectKey,
                rangeFrom = currentFilters.priceFull?.priceFrom?.toString().orEmpty(),
                fromOnChange = {
                    currentFilters = currentFilters.copy(
                        priceFull = currentFilters.priceFull?.copy(
                            priceFrom = it.toDoubleOrNull()
                        ) ?: Price(
                            priceFrom = it.toDoubleOrNull()
                        )
                    )
                },
                rangeTo = currentFilters.priceFull?.priceTo?.toString().orEmpty(),
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
            NumberRange(
                title = "Цена за м2",
                launchedKey = clearAllEffectKey,
                rangeFrom = currentFilters.pricePerSquare?.priceFrom?.toString().orEmpty(),
                fromOnChange = {
                    currentFilters = currentFilters.copy(
                        pricePerSquare = currentFilters.pricePerSquare?.copy(
                            priceFrom = it.toDoubleOrNull()
                        ) ?: Price(
                            priceFrom = it.toDoubleOrNull()
                        )
                    )
                },
                rangeTo = currentFilters.pricePerSquare?.priceTo?.toString().orEmpty(),
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
fun RentSaleSegmentedButtons(
    selectedAdType: AdType,
    onClick: (AdType) -> Unit
) {

    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = selectedAdType == AdType.RENT,
            onClick = { onClick(AdType.RENT) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors()
        ) {
            Text("Аренда")
        }

        SegmentedButton(
            selected = selectedAdType == AdType.SALE,
            onClick = { onClick(AdType.SALE) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors()
        ) {
            Text("Продажа")
        }
    }
}

@Composable
fun SortOptionSegmentedButtons(
    selectedSortOption: FlatSort,
    onClick: (FlatSort) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selectedSortOption == FlatSort.NEWEST_FIRST,
            onClick = { onClick(FlatSort.NEWEST_FIRST) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
        ) {
            Text(
                "По новизне",
                maxLines = 1
            )
        }

        SegmentedButton(
            selected = selectedSortOption == FlatSort.CHEAPEST_FIRST,
            onClick = { onClick(FlatSort.CHEAPEST_FIRST) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
        ) {
            Text(
                "Сначала дешевле",
                overflow = TextOverflow.StartEllipsis,
                maxLines = 1
            )
        }

        SegmentedButton(
            selected = selectedSortOption == FlatSort.MOST_EXPENSIVE_FIRST,
            onClick = { onClick(FlatSort.MOST_EXPENSIVE_FIRST) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
        ) {
            Text(
                "Сначала дороже",
                overflow = TextOverflow.Visible,
                maxLines = 1
            )
        }
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
private fun NumberRange(
    title: String,
    launchedKey: Any = Unit,
    rangeFrom: String,
    fromOnChange: (String) -> Unit,
    rangeTo: String,
    toOnChange: (String) -> Unit,
) {

    FilterSectionTitle(title = title, modifier = Modifier)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            modifier = Modifier.weight(1f),
            launchedKey = launchedKey,
            text = rangeFrom,
            label = "От",
            onChangePredicate = onlyDoublePredicate,
            onChange = fromOnChange
        )
        AppTextField(
            modifier = Modifier.weight(1f),
            launchedKey = launchedKey,
            text = rangeTo,
            label = "До",
            onChangePredicate = onlyDoublePredicate,
            onChange = toOnChange
        )
    }
}
