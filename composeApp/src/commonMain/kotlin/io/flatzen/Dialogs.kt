package io.flatzen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.flatzen.viewmodel.filter.FilterDialogState
import io.flatzen.viewmodel.list.InfoDialogState


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