package io.flatzen.screens.location

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Face
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
import io.flatzen.states.MetroLineState
import io.flatzen.viewmodel.FilterScreenAction
import io.flatzen.viewmodel.FilterViewModel
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Расположение") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
                headlineContent = { Text(state.filters.location?.selectedCity?.name.orEmpty()) },
                trailingContent = { Icon(Icons.Default.Face, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().clickable { openCity() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

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
                                                selectedCity = io.flatzen.states.UiCity(city.code)
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
                                            selectedCity = io.flatzen.states.UiCity(city.code)
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

    val filteredStation = remember(query.text) {
        val lower = query.text.lowercase()
        state.filters.metroStationsState.filter { it.name.lowercase().contains(lower) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
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


