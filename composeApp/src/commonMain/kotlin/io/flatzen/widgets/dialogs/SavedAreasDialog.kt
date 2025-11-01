package io.flatzen.widgets.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    onApplyFilter: (savedFilterId: String, isChecked: Boolean) -> Unit,
    onDeleteFilter: (savedFilterId: String) -> Unit,
    onDismiss: () -> Unit,
) {

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Сохраненные области",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (state.savedFilters.isEmpty()) {
                    Text(
                        text = "Нет сохраненных областей",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(state.savedFilters) { savedFilter ->
                            SavedAreaItem(
                                mapArea = savedFilter,
                                onApply = { id, isChecked ->
                                    onApplyFilter(savedFilter.id, isChecked)
                                    onDismiss()
                                },
                                onDelete = {
                                    onDeleteFilter(savedFilter.id)
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Checkbox(checked = mapArea.isActive, onCheckedChange = {
                    onApply(mapArea.id, !mapArea.isActive)
                })
                Spacer(Modifier.width(6.dp))
                Text(
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
            }
        }
    }
}