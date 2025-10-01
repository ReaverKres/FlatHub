package io.flatzen.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import io.flatzen.commoncomponents.commonentities.more.MoreConfigData.MoreConfigType
import io.flatzen.commoncomponents.utils.DevicePlatform
import io.flatzen.utils.shareLauncher
import io.flatzen.viewmodel.more.FaqUiState
import io.flatzen.viewmodel.more.FaqViewModel
import io.flatzen.viewmodel.more.MoreScreenViewModel
import io.flatzen.viewmodel.more.MoreUiState
import io.flatzen.widgets.AppTextButton
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    navigateToFaq: () -> Unit = {}
) {

    val viewModel: MoreScreenViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val faqViewModel: FaqViewModel = koinViewModel()
    val faqState by faqViewModel.uiState.collectAsStateWithLifecycle()

    val telegramSupportDescription: String = remember {
        "Здесь вы можете:\n" +
                "• 🐞 Сообщить об ошибке или проблеме \n" +
                "  (Если нашли ошибку прикрепите запись экрана и ссылку на объявление)\n" +
                "• 💡 Предложить идею для улучшения приложения"
    }

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
        val shareLauncher = shareLauncher()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            ) {
            // FAQ Button at the top
            if(faqState is FaqUiState.Success && (faqState as FaqUiState.Success).faqConfigData.faqItems.isNotEmpty()){
                Spacer(modifier = Modifier.height(24.dp))
                AppTextButton(
                    image = null,
                    text = "Часто задаваемые вопросы (FAQ)",
                    onClick = navigateToFaq
                )
            }
            
            if (state is MoreUiState.Success && (state as MoreUiState.Success).moreConfigData.isVisible == true) {
                val moreConfigData = (state as MoreUiState.Success).moreConfigData
                moreConfigData.telegramSupport?.let { telegram ->
                    MoreDescription(
                        text = telegramSupportDescription
                    )
                    AppTextButton(
                        image = telegram.imageUrl,
                        text = telegram.text,
                        onClick = {
                            uriHandler.openUri(telegram.value)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (moreConfigData.donateDescription != null) {
                    MoreDescription(
                        text = moreConfigData.donateDescription.orEmpty()
                    )
                }
                moreConfigData.donateItems.forEach { item ->
                    when {
                        item.type == MoreConfigType.LINK -> {
                            AppTextButton(
                                image = item.imageUrl,
                                text = item.text,
                                onClick = {
                                    uriHandler.openUri(item.value)
                                }
                            )
                        }

                        item.type == MoreConfigType.CRYPTO -> {
                            val onClick: () -> Unit = {
                                shareLauncher.shareText(text = item.value)
                            }
                            Row(
                                modifier = modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(
                                        horizontal = ButtonDefaults.TextButtonContentPadding.calculateLeftPadding(
                                            LayoutDirection.Ltr
                                        )
                                    )
                                    .clickable(onClick = onClick),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(text = item.text)
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = onClick
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Поделиться"
                                    )
                                }
                            }
                        }
                    }
                }

            } else {
                MoreDescription(
                    text = telegramSupportDescription,
                )
                AppTextButton(
                    image = Res.getUri("drawable/telegram.svg"),
                    text = "Написать разработчикам в Telegram",
                    onClick = {
                        uriHandler.openUri("https://t.me/theLocus_bot")
                    }
                )
            }
        }
    }
}

@Composable
private fun MoreDescription(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    )

}