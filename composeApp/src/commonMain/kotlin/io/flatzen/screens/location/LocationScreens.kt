package io.flatzen.screens.location

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.mappers.LocationUiMapper
import io.flatzen.viewmodel.filter.AddressUiState
import io.flatzen.viewmodel.filter.FilterScreenAction
import io.flatzen.viewmodel.filter.FilterViewModel
import io.flatzen.viewmodel.filter.MetroLineState
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    navigateBack: () -> Unit,
    openCity: () -> Unit,
    openMetro: () -> Unit,
) {
    val viewModel: FilterViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var addressInput by remember { mutableStateOf("") }
    val addresses = state.filters.address?.toMutableSet() ?: mutableSetOf()

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Расположение") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.onIntent(FilterScreenAction.ClearLocationFilters)
                    }) {
                        Text("Сбросить")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ListItem(
                headlineContent = { Text(state.filters.location?.selectedCity?.displayName.orEmpty()) },
                trailingContent = { Icon(Icons.Default.Face, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { openCity() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            // Поле ввода адреса
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Введите адрес/улицу") },
                    maxLines = 1,
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (addressInput.isNotBlank()) {
                            viewModel.onIntent(
                                FilterScreenAction.UpdateAddressFilter(
                                    addresses + AddressUiState(addressInput.trim())
                                )
                            )
                            addressInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить"
                    ) // Можешь заменить на Add
                }
            }

            // Chips список
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                addresses.forEach { addr ->
                    AssistChip(
                        onClick = { /* ничего, просто Chip */ },
                        label = { Text(addr.address) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete, // Можешь заменить на Close
                                contentDescription = "Удалить",
                                modifier = Modifier.clickable {
                                    viewModel.onIntent(FilterScreenAction.UpdateAddressFilter(addresses - addr))
                                }
                            )
                        }
                    )
                }
            }

            if (state.filters.location?.selectedCity?.code == CityCode.MINSK) {
                // Плитки действий (минимум метро)
                ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { openMetro() }) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        BadgedBox(badge = {
                            val count = state.filters.metroStationsState.filter { it.selected }.size
                            if (count > 0) Badge { Text(count.toString()) }
                        }) {
                            Text("Метро")
                        }
                    }
                }
            } else {
                viewModel.onIntent(FilterScreenAction.ClearMetroFilters)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectScreen(
    navigateBack: () -> Unit,
    viewModel: FilterViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cities = state.filters.location?.availableCities.orEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Город") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(cities) { city ->
                ListItem(
                    headlineContent = { Text(city.displayName) },
                    trailingContent = {
                        val checked = state.filters.location?.selectedCity?.code == city.code
                        Checkbox(checked = checked, onCheckedChange = {
                            if (!checked) {
                                viewModel.onIntent(
                                    FilterScreenAction.UpdateFilter(
                                        state.filters.copy(
                                            location = state.filters.location?.copy(
                                                selectedCity = LocationUiMapper.findSelectedCity(city.code)
                                            )
                                        )
                                    )
                                )
                            }
                            navigateBack()
                        })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.onIntent(
                                FilterScreenAction.UpdateFilter(
                                    state.filters.copy(
                                        location = state.filters.location?.copy(
                                            selectedCity = LocationUiMapper.findSelectedCity(city.code)
                                        )
                                    )
                                )
                            )
                            navigateBack()
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                Divider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroSelectScreen(
    navigateBack: () -> Unit,
    viewModel: FilterViewModel = koinViewModel()
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val filteredStation = state.filters.metroStationsState.filter {
        it.name.lowercase().contains(query.text.lowercase())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Метро") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                placeholder = { Text("Поиск станции") }
            )

            LazyColumn {
                items(filteredStation) { station ->
                    val dotColor = when (station.line) {
                        MetroLineState.BLUE -> Color(0xFF1976D2)
                        MetroLineState.RED -> Color(0xFFD32F2F)
                        MetroLineState.GREEN -> Color(0xFF388E3C)
                    }
                    ListItem(
                        headlineContent = {
                            Row {
                                Text(station.name, modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.height(0.dp))
                            }
                        },
                        trailingContent = {
                            Row {
                                Checkbox(
                                    checked = station.selected,
                                    onCheckedChange = {
                                        viewModel.onIntent(
                                            FilterScreenAction.UpdateMetroFilter(
                                                station.copy(selected = it)
                                            )
                                        )
                                    }
                                )
                                Surface(
                                    color = dotColor,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                ) {}
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.onIntent(
                                    FilterScreenAction.UpdateMetroFilter(
                                        station.copy(selected = station.selected.not())
                                    )
                                )
                            },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    Divider()
                }
            }
        }
    }
}


