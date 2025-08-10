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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.states.MetroLineState
import io.flatzen.states.Room
import io.flatzen.viewmodel.FilterScreenAction
import io.flatzen.viewmodel.FilterViewModel
import io.flatzen.widgets.FilterSwitch
import io.flatzen.mappers.LocationUiMapper
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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Фильтры") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.onIntent(FilterScreenAction.ClearFilters)
                        navigateBack()
                    }) {
                        Text("Сбросить")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.onIntent(FilterScreenAction.UpdateFilter(currentFilters))
                    navigateBack()
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Применить")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Расположение
            FilterSectionTitle(title = "Расположение")
            ListItem(
                headlineContent = {
                    Text(LocationUiMapper.displayName(state.filters.location?.city?.code.orEmpty()))
                },
                supportingContent = { Text("Метро") },
                trailingContent = { Icon(Icons.Default.Face, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLocation() }
            )

            // Цена
            FilterSectionTitle(title = "Цена")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentFilters.priceFrom?.toString() ?: "",
                    onValueChange = {
                        currentFilters = currentFilters.copy(priceFrom = it.toDoubleOrNull())
                    },
                    label = { Text("От") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = currentFilters.priceTo?.toString() ?: "",
                    onValueChange = {
                        currentFilters = currentFilters.copy(priceTo = it.toDoubleOrNull())
                    },
                    label = { Text("До") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

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

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FilterSectionTitle(
    modifier: Modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier
    )
}
