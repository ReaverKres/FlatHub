package io.flatzen.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MessageSnackbar(
    message: String,
    color: Color = Color.Red,
    modifier: Modifier = Modifier,
    closeBtnText: String? = null,
    actionBtnText: String? = null,
    onCloseBtnClick: () -> Unit = {},
    onActionBtnClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 4.dp)
            .background(color),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // Текст сообщения - убрали weight(1f)
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.fillMaxWidth() // Занимает всю ширину
            )

            // Кнопки справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.End
            ) {
                // Первая кнопка
                if (!closeBtnText.isNullOrEmpty()) {
                    TextButton(
                        onClick = onCloseBtnClick,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = closeBtnText,
                            color = Color.White
                        )
                    }
                }

                // Вторая кнопка
                if (!actionBtnText.isNullOrEmpty()) {
                    TextButton(
                        onClick = onActionBtnClick,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = actionBtnText,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}