package io.flatzen.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.flatzen.viewmodel.MapAction
import io.flatzen.viewmodel.MapViewModel
import org.koin.compose.viewmodel.koinViewModel
import ovh.plrapps.mapcompose.ui.MapUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = koinViewModel()
) {
    LaunchedEffect(Unit) { viewModel.onIntent(MapAction.Initialize) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Карта", style = MaterialTheme.typography.titleLarge) }) }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
            MapUI(modifier = Modifier.fillMaxSize(), state = viewModel.mapState)

            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = { viewModel.onIntent(MapAction.ZoomIn) }
            ) { Icon(Icons.Default.Add, contentDescription = "Zoom in") }

            FloatingActionButton(
                modifier = Modifier.align(Alignment.BottomStart),
                onClick = { viewModel.onIntent(MapAction.ZoomOut) }
            ) { Icon(Icons.Default.Delete, contentDescription = "Zoom out") }
        }
    }
}


