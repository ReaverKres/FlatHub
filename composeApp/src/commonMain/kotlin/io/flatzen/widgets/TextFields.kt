package io.flatzen.widgets

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun AppTextField(
    modifier: Modifier = Modifier,
    text: String,
    label: String,
    launchedKey: Any = Unit,
    onChangePredicate: (String) -> Boolean = { true },
    onChange: (String) -> Unit,
) {
    var textValue by remember(launchedKey) {
        mutableStateOf(TextFieldValue(text = text))
    }

    val focusFromRequester = remember { FocusRequester() }
    val focusFromManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (onChangePredicate(newValue.text)) {
                textValue = newValue
                onChange(newValue.text)
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier.focusRequester(focusFromRequester),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusFromManager.clearFocus()
            }
        )
    )
}