package io.flatzen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import io.flatzen.commoncomponents.commonentities.FlatSort
import io.flatzen.entities.SingleChoiceEntity
import io.flatzen.viewmodel.filter.FilterDialogState
import io.flatzen.viewmodel.sharedstates.InfoDialogState
import io.flatzen.viewmodel.sharedstates.SearchErrorDialogState

@Composable
fun SaveFilterDialog(
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
                    text = "Название фильтра не должно превышать 25 символов",
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

@Composable
fun ForceUpdateDialog(infoDialogState: InfoDialogState) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = infoDialogState.title) },
        text = { Text(text = infoDialogState.description) },
        confirmButton = {},
        dismissButton = null
    )
}

@Composable
fun SearchErrorDialog(
    dialogState: SearchErrorDialogState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxHeight(0.4f)
                    .padding(16.dp)
            ) {
                // Title
                Text(
                    text = dialogState.title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Content with scroll
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Scrollable Row with platforms
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        items(FlatPlatform.entries) { platform ->
                            val hasError = dialogState.errorInfo.any { it.platform == platform }
                            val icon =
                                if (hasError) Icons.Default.Lock else Icons.Default.CheckCircle
                            val iconColor = if (hasError) Color.Red else Color.Green

                            Row(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = platform.name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Error details
                    dialogState.errorInfo.forEach { errorInfo ->
                        Text(
                            text = errorInfo.platform.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        errorInfo.errorMessages.forEach { errorMessage ->
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SingleChoiceDialog(
    title: String,
    items: List<SingleChoiceEntity<T>>,
    selectedItem: T? = null,
    onDismissRequest: () -> Unit,
    onSelected: (T) -> Unit
) {
    var selectedOption by remember { mutableStateOf(selectedItem) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Заголовок
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Список опций в LazyColumn
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        SingleChoiceItem(
                            item = item,
                            isSelected = selectedOption == item.type,
                            onSelected = {
                                onSelected(it)
                                onDismissRequest()
                            }
                        )
                    }
                }

                // Кнопки действий
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Отмена")
                    }
                }
            }
        }
    }
}

@Composable
fun <T> SingleChoiceItem(
    item: SingleChoiceEntity<T>,
    isSelected: Boolean,
    onSelected: (T) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onSelected(item.type) }
    ) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { onSelected(item.type) }
            )
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(vertical = 4.dp)
                    .padding(horizontal = 10.dp),
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}