package io.flatzen.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import io.flatzen.commoncomponents.commonentities.more.MoreConfigData.MoreConfigType
import io.flatzen.di.container
import io.flatzen.utils.ToastDurationType
import io.flatzen.utils.ToastLauncher
import io.flatzen.utils.copyLauncher
import io.flatzen.viewmodel.more.FaqContainer
import io.flatzen.viewmodel.more.FaqState
import io.flatzen.viewmodel.more.MoreConfigState
import io.flatzen.viewmodel.more.MoreContainer
import io.flatzen.widgets.AppTextButton
import org.jetbrains.compose.resources.ExperimentalResourceApi
import pro.respawn.flowmvi.compose.dsl.subscribe

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    navigateToFaq: () -> Unit,
    navigateToReferral: () -> Unit
) {

    val moreContainer: MoreContainer = container()
    val moreState by moreContainer.store.subscribe { }

    val faqContainer: FaqContainer = container()
    val faqState by faqContainer.store.subscribe { }

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
                title = { Text("Ещё", style = MaterialTheme.typography.headlineSmall) },
            )
        },
    ) { paddingValues ->

        val uriHandler = LocalUriHandler.current
        val toastLauncher = ToastLauncher()
        val copyLauncher = copyLauncher(
            onCopySuccess = { text: String ->
                toastLauncher.showToast("Скопировано: $text", ToastDurationType.LONG)
            },
            onCopyError = { exception: Exception ->
                // Handle error if needed
            }
        )

        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                // FAQ Button at the top
                if (faqState is FaqState.Success && (faqState as FaqState.Success).faqConfigData.faqItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                        AppTextButton(
                            image = null,
                            text = "Часто задаваемые вопросы (FAQ)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                            onClick = navigateToFaq
                        )
                    }
                }

                if (moreState.isNotificationAvailable.not()) {
                    Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                        AppTextButton(
                            image = null,
                            text = "Пригласительный код",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                            onClick = navigateToReferral
                        )
                    }
                }

                if (moreState.configState is MoreConfigState.Success && (moreState.configState as MoreConfigState.Success).moreConfigData.isVisible == true) {
                    val moreConfigData = (moreState.configState as MoreConfigState.Success).moreConfigData
                    moreConfigData.telegramSupport?.let { telegram ->
                        DescriptionText(
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
                        DescriptionText(
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
                                    copyLauncher.copyText(text = item.value)
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
                                        .padding(bottom = 6.dp)
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
                                    Spacer(Modifier.width(12.dp))
                                    AsyncImage(
                                        model = Res.getUri("drawable/copy.svg"),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                            .clickable(onClick = onClick),
                                        colorFilter = ColorFilter.tint(Color.LightGray)
                                    )
                                }
                            }
                        }
                    }

                } else {
                    DescriptionText(
                        text = telegramSupportDescription,
                    )
                    AppTextButton(
                        image = Res.getUri("drawable/telegram.svg"),
                        text = "Написать разработчикам в Telegram",
                        onClick = {
                            uriHandler.openUri("https://t.me/FlatHub_appbot")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DescriptionText(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    )
}