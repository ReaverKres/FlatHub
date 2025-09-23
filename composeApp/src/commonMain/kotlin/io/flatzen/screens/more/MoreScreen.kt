package io.flatzen.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(modifier: Modifier = Modifier) {

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Ещё", style = MaterialTheme.typography.headlineMedium) },
            )
        },
    ) { paddingValues ->

        val uriHandler = LocalUriHandler.current

        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
                text = "Здесь вы можете:\n" +
                        "• 🐞 Сообщить об ошибке или проблеме \n" +
                        "  (Если нашли ошибку прикрепите запись экрана и ссылку на объявление)\n" +
                        "• 💡 Предложить идею для улучшения приложения",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            // Кнопка для связи в Telegram
            TextButton(
                colors = ButtonDefaults.textButtonColors()
                    .copy(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                onClick = {
                    uriHandler.openUri("https://t.me/theLocus_bot")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = Res.getUri("drawable/telegram.svg"),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Написать разработчикам в Telegram",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}