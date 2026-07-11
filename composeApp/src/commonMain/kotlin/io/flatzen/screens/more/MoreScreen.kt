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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.copy_success
import flatzen.composeapp.generated.resources.faq_title
import flatzen.composeapp.generated.resources.more_title
import flatzen.composeapp.generated.resources.premium_menu
import flatzen.composeapp.generated.resources.referral_code
import flatzen.composeapp.generated.resources.telegram_support
import flatzen.composeapp.generated.resources.telegram_support_description
import flatzen.composeapp.generated.resources.theme_dark
import flatzen.composeapp.generated.resources.theme_light
import flatzen.composeapp.generated.resources.theme_system
import flatzen.composeapp.generated.resources.theme_title
import io.flatzen.commoncomponents.commonentities.more.MoreConfigData.MoreConfigType
import io.flatzen.commoncomponents.theme.ThemeMode
import io.flatzen.di.container
import io.flatzen.themes.LocalThemeRevealController
import io.flatzen.utils.ToastDurationType
import io.flatzen.utils.ToastLauncher
import io.flatzen.utils.copyLauncher
import io.flatzen.viewmodel.more.FaqContainer
import io.flatzen.viewmodel.more.FaqState
import io.flatzen.viewmodel.more.MoreConfigState
import io.flatzen.viewmodel.more.MoreContainer
import io.flatzen.widgets.AppTextButton
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pro.respawn.flowmvi.compose.dsl.subscribe
import repository.userpreferences.UserPreferencesRepository

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MoreScreen(
    modifier: Modifier = Modifier,
    navigateToFaq: () -> Unit,
    navigateToReferral: () -> Unit,
    navigateToPremium: () -> Unit = {},
) {
    val moreContainer: MoreContainer = container()
    val moreState by moreContainer.store.subscribe { }

    val faqContainer: FaqContainer = container()
    val faqState by faqContainer.store.subscribe { }

    val userPreferences: UserPreferencesRepository = koinInject()
    val themeMode by userPreferences.observeThemeMode()
        .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val revealController = LocalThemeRevealController.current
    val displayMode = revealController.pendingMode ?: themeMode

    val telegramSupportDescription = stringResource(Res.string.telegram_support_description)
    val uriHandler = LocalUriHandler.current
    val toastLauncher = remember { ToastLauncher() }
    val copySuccessTemplate = stringResource(Res.string.copy_success)
    val copyLauncher = copyLauncher(
        onCopySuccess = { text: String ->
            toastLauncher.showToast(
                copySuccessTemplate.replace("%1\$s", text),
                ToastDurationType.LONG
            )
        },
        onCopyError = { },
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(Res.string.more_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
            )
        },
    ) { paddingValues ->
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                ThemeModeSelector(
                    currentMode = displayMode,
                    onModeSelected = { mode, originInRoot ->
                        if (mode != themeMode && !revealController.isAnimating) {
                            revealController.start(originInRoot = originInRoot, targetMode = mode)
                        }
                    },
                )

                if (faqState is FaqState.Success &&
                    (faqState as FaqState.Success).faqConfigData.faqItems.isNotEmpty()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                        AppTextButton(
                            image = null,
                            text = stringResource(Res.string.faq_title),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                            onClick = navigateToFaq
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                    AppTextButton(
                        image = null,
                        text = stringResource(Res.string.premium_menu),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                        onClick = navigateToPremium
                    )
                }

                if (moreState.isNotificationAvailable.not()) {
                    Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
                        AppTextButton(
                            image = null,
                            text = stringResource(Res.string.referral_code),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                            onClick = navigateToReferral
                        )
                    }
                }

                val configState = moreState.configState
                if (configState is MoreConfigState.Success &&
                    configState.moreConfigData.isVisible == true
                ) {
                    val moreConfigData = configState.moreConfigData
                    moreConfigData.telegramSupport?.let { telegram ->
                        DescriptionText(text = telegramSupportDescription)
                        AppTextButton(
                            image = telegram.imageUrl,
                            text = telegram.text,
                            onClick = { uriHandler.openUri(telegram.value) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (moreConfigData.donateDescription != null) {
                        DescriptionText(text = moreConfigData.donateDescription.orEmpty())
                    }
                    moreConfigData.donateItems.forEach { item ->
                        when {
                            item.type == MoreConfigType.LINK -> {
                                AppTextButton(
                                    image = item.imageUrl,
                                    text = item.text,
                                    onClick = { uriHandler.openUri(item.value) }
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
                                            horizontal = ButtonDefaults.TextButtonContentPadding
                                                .calculateLeftPadding(LayoutDirection.Ltr)
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
                                        modifier = Modifier.size(16.dp).clickable(onClick = onClick),
                                        colorFilter = ColorFilter.tint(Color.LightGray)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    DescriptionText(text = telegramSupportDescription)
                    AppTextButton(
                        image = Res.getUri("drawable/telegram.svg"),
                        text = stringResource(Res.string.telegram_support),
                        onClick = { uriHandler.openUri("https://t.me/FlatHub_appbot") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode, Offset) -> Unit,
) {
    val buttonCenters = remember {
        mutableStateOf(List(ThemeMode.entries.size) { Offset.Zero })
    }

    Card(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.theme_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            val centerLocal = Offset(
                                x = coords.size.width / 2f,
                                y = coords.size.height / 2f,
                            )
                            val rootPos = coords.positionInRoot()
                            val root = Offset(rootPos.x + centerLocal.x, rootPos.y + centerLocal.y)
                            val updated = buttonCenters.value.toMutableList()
                            if (updated[index] != root) {
                                updated[index] = root
                                buttonCenters.value = updated
                            }
                        },
                        selected = currentMode == mode,
                        onClick = {
                            onModeSelected(mode, buttonCenters.value[index])
                        },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> Res.string.theme_system
                                    ThemeMode.LIGHT -> Res.string.theme_light
                                    ThemeMode.DARK -> Res.string.theme_dark
                                }
                            )
                        )
                    }
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
