package io.flatzen.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/**
 * Диалог выбора диапазона дат с настраиваемыми параметрами.
 *
 * @param onDismissRequest Обработчик события закрытия диалога (например, при нажатии вне диалога или кнопки "Отмена").
 * @param onDateRangeSelected Колбэк, вызываемый при подтверждении выбора. Передает выбранный диапазон (timestamp начала, timestamp конца).
 * @param selectableDates Объект, определяющий, какие даты могут быть выбраны. По умолчанию - все даты.
 * @param title Заголовок, отображаемый в диалоге.
 * @param confirmButton Композабл для кнопки подтверждения выбора.
 * @param dismissButton Композабл для кнопки отмены.
 * @param initialSelectedStartDateMillis Начальная выбранная дата начала в UTC миллисекундах. null, если нет.
 * @param initialSelectedEndDateMillis Начальная выбранная дата конца в UTC миллисекундах. null, если нет.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismissRequest: () -> Unit,
    onDateRangeSelected: (Pair<Long?, Long?>) -> Unit,
    modifier: Modifier = Modifier,
    selectableDates: SelectableDates = FutureOrPresentSelectableDates,
    title: @Composable (() -> Unit)? = {
        Text("Выберите диапазон дат", style = MaterialTheme.typography.headlineSmall)
    },
    confirmButton: @Composable ((DateRangePickerState) -> Unit) = { state ->
        TextButton(
            onClick = {
                onDateRangeSelected(Pair(state.selectedStartDateMillis, state.selectedEndDateMillis))
                onDismissRequest()
            },
            enabled = state.selectedEndDateMillis != null
        ) {
            Text("ОК")
        }
    },
    dismissButton: @Composable (() -> Unit)? = {
        TextButton(onClick = onDismissRequest) {
            Text("Отмена")
        }
    },
    initialSelectedStartDateMillis: Long? = null,
    initialSelectedEndDateMillis: Long? = null,
) {
    val dateRangePickerState = rememberDateRangePickerState(
        selectableDates = selectableDates,
        initialSelectedStartDateMillis = initialSelectedStartDateMillis,
        initialSelectedEndDateMillis = initialSelectedEndDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { confirmButton(dateRangePickerState) },
        dismissButton = dismissButton,
        modifier = modifier.sizeIn(maxWidth = 450.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (title != null) {
                title()
                Spacer(modifier = Modifier.height(16.dp))
            }

            DateRangePicker(
                state = dateRangePickerState,
                title = null,
                headline = null,
                modifier = Modifier.sizeIn(maxWidth = 450.dp).height(450.dp)
            )
        }
    }
}

// Вспомогательный объект для ограничения выбора только будущими датами (для примера)
@OptIn(ExperimentalMaterial3Api::class)
private val FutureOrPresentSelectableDates = object : SelectableDates {
    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
        return date >= today
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year >= today.year
    }
}