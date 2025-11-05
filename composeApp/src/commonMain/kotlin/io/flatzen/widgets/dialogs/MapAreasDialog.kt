package io.flatzen.widgets.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.flatzen.viewmodel.filter.MapAreasUi
import io.flatzen.viewmodel.sharedstates.SavedAreasDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAreasDialog(
    state: SavedAreasDialogState,
    onCheckArea: (areaId: String, isChecked: Boolean) -> Unit,
    onDeleteArea: (areaId: String) -> Unit,
    onDismiss: () -> Unit,
) {

    val title = state.title ?: ""
    val mapAreas = state.savedAreas

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Заголовок
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if(mapAreas.isEmpty()) {
                    Text("Сохранённные области не найдены")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mapAreas) { item ->
                            SavedAreaItem(
                                mapArea = item,
                                onApply = onCheckArea,
                                onDelete = onDeleteArea
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
fun SavedAreaItem(
    mapArea: MapAreasUi,
    onApply: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = mapArea.isActive, onCheckedChange = {
            onApply(mapArea.id, !mapArea.isActive)
        })
        Spacer(Modifier.width(6.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = mapArea.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = {
            onDelete(mapArea.id)
        }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить"
            )
        }
        Spacer(Modifier.width(6.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrictAreasDialog(
    state: SavedAreasDialogState,
    onCheckArea: (areaId: String, isChecked: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {

    val title = state.title ?: ""
    val mapAreas = state.savedAreas

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Заголовок
                Spacer(Modifier.height(16.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if(mapAreas.isEmpty()) {
                    Text("Районы города не найдены")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mapAreas) { item ->
                            DistrictAreaItem(
                                mapArea = item,
                                onApply = onCheckArea,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

@Composable
fun DistrictAreaItem(
    mapArea: MapAreasUi,
    onApply: (String, Boolean) -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = mapArea.isActive, onCheckedChange = {
            onApply(mapArea.id, !mapArea.isActive)
        })
        Spacer(Modifier.width(6.dp))
        Text(
            modifier = Modifier.weight(1f),
            text = mapArea.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(6.dp))
    }
}