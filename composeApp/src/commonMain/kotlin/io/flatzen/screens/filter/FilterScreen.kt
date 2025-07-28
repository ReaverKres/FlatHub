package io.flatzen.screens.filter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.states.Amenity
import io.flatzen.states.RepairType
import io.flatzen.viewmodel.FilterScreenAction
import io.flatzen.viewmodel.FilterViewModel
import org.koin.compose.viewmodel.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    navigateBack: () -> Unit,
    viewModel: FilterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var currentFilters by remember(state.filters) { mutableStateOf(state.filters) }

    Scaffold(
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
            // Цена
            FilterSectionTitle("Цена")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentFilters.priceFrom?.toString() ?: "",
                    onValueChange = { currentFilters = currentFilters.copy(priceFrom = it.toDoubleOrNull()) },
                    label = { Text("От") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = currentFilters.priceTo?.toString() ?: "",
                    onValueChange = { currentFilters = currentFilters.copy(priceTo = it.toDoubleOrNull()) },
                    label = { Text("До") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Ремонт
            FilterSectionTitle("Ремонт")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RepairType.values().forEach { repairType ->
                    FilterChip(
                        selected = currentFilters.repairTypes.contains(repairType),
                        onClick = {
                            val newTypes = currentFilters.repairTypes.toMutableSet()
                            if (newTypes.contains(repairType)) newTypes.remove(repairType) else newTypes.add(repairType)
                            currentFilters = currentFilters.copy(repairTypes = newTypes)
                        },
                        label = { Text(repairType.displayName) }
                    )
                }
            }

            // Удобства
            FilterSectionTitle("Удобства")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Amenity.values().forEach { amenity ->
                    FilterChip(
                        selected = currentFilters.amenities.contains(amenity),
                        onClick = {
                            val newAmenities = currentFilters.amenities.toMutableSet()
                            if (newAmenities.contains(amenity)) newAmenities.remove(amenity) else newAmenities.add(amenity)
                            currentFilters = currentFilters.copy(amenities = newAmenities)
                        },
                        label = { Text(amenity.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}
