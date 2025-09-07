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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.AdType
import io.flatzen.commoncomponents.commonentities.Price
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.commoncomponents.analytics.AppMetrcica
import io.flatzen.viewmodel.filter.FilterDialogState
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.filter.Room
import io.flatzen.viewmodel.filter.SavedFilterState
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
                title = { Text("Фильтры") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = {
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
                FilterSectionTitle(title = "Мои фильтры")
                SavedFiltersChips(
                    savedFilters = state.savedFilters,
                    onFilterClick = { filter ->
                        viewModel.onIntent(FilterScreenAction.ToggleSavedFilterSelection(filter.id))
                    },
                    onDeleteClick = { filterId ->
                        viewModel.onIntent(FilterScreenAction.DeleteSavedFilter(filterId))
                    }
                )
            }

            // Продажа или Аренда
            RentSaleSegmentedButtons(state.filters.adType) {
                currentFilters = currentFilters.copy(adType = it)
            }

            // Расположение
            FilterSectionTitle(title = "Расположение")
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

            PriceSection(
                title = "Цена",
                priceFrom = currentFilters.priceFull?.priceFrom?.toString().orEmpty(),
                priceFromOnChange = {
                    currentFilters = currentFilters.copy(
                        priceFull = currentFilters.priceFull?.copy(
                            priceFrom = it.toDoubleOrNull()
                        ) ?: Price(
                            priceFrom = it.toDoubleOrNull()
                        )
                    )
                },
                priceTo = currentFilters.priceFull?.priceTo?.toString().orEmpty(),
                priceToOnChange = {
                    currentFilters = currentFilters.copy(
                        priceFull = currentFilters.priceFull?.copy(
                            priceTo = it.toDoubleOrNull()
                        ) ?: Price(
                            priceTo = it.toDoubleOrNull()
                        )
                    )
                }
            )
            PriceSection(
                title = "Цена за м2",
                priceFrom = currentFilters.pricePerSquare?.priceFrom?.toString().orEmpty(),
                priceFromOnChange = {
                    currentFilters = currentFilters.copy(
                        pricePerSquare = currentFilters.pricePerSquare?.copy(
                            priceFrom = it.toDoubleOrNull()
                        ) ?: Price(
                            priceFrom = it.toDoubleOrNull()
                        )
                    )
                },
                priceTo = currentFilters.pricePerSquare?.priceTo?.toString().orEmpty(),
                priceToOnChange = {
                    currentFilters = currentFilters.copy(
                        pricePerSquare = currentFilters.pricePerSquare?.copy(
                            priceTo = it.toDoubleOrNull()
                        ) ?: Price(
                            priceTo = it.toDoubleOrNull()
                        )
                    )
                }
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
private fun FilterSectionTitle(
    modifier: Modifier = Modifier.padding(vertical = 4.dp),
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
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
private fun PriceSection(
    title: String,
    priceFrom: String,
    priceFromOnChange: (String) -> Unit,
    priceTo: String,
    priceToOnChange: (String) -> Unit,
) {
    // Цена
    FilterSectionTitle(title = title)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = priceFrom,
            onValueChange = { newValue -> 
                priceFromOnChange(newValue)
            },
            label = { Text("От") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = priceTo,
            onValueChange = { newValue -> 
                priceToOnChange(newValue)
            },
            label = { Text("До") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun SaveFilterDialog(
    dialogState: FilterDialogState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Сохранить фильтр") },
        text = {
            Column {
                OutlinedTextField(
                    value = dialogState.filterName,
                    onValueChange = onNameChange,
                    label = { Text("Название фильтра") },
                    isError = !dialogState.isNameValid,
                    modifier = Modifier.fillMaxWidth()
                )
                if (dialogState.errorMessage != null) {
                    Text(
                        text = dialogState.errorMessage.orEmpty(),
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = "Название фильтра не должно превышать 15 символов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = dialogState.isNameValid && dialogState.filterName.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Отменить")
            }
        }
    )
}
