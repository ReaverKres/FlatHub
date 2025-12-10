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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import io.flatzen.utils.ToastDurationType
import io.flatzen.utils.ToastLauncher
import io.flatzen.utils.copyLauncher
import io.flatzen.viewmodel.more.ReferralAction
import io.flatzen.viewmodel.more.ReferralEffect
import io.flatzen.viewmodel.more.ReferralViewModel
import io.flatzen.widgets.AppTextField
import io.flatzen.widgets.dialogs.SimpleAlertDialog
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    navigateBack: () -> Unit
) {
    val viewModel: ReferralViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect {
            when (it) {
                is ReferralEffect.NotificationAvailable -> {
                    navigateBack()
                }

                else -> {}
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        "Пригласительный код",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { paddingValues ->
        val toastLauncher = remember { ToastLauncher() }
        val copier = copyLauncher(
            onCopySuccess = { text ->
                toastLauncher.showToast(
                    "Скопировано: $text",
                    ToastDurationType.LONG
                )
            },
            onCopyError = { _ -> }
        )

        if (state.statsErrorMessage != null) {
            SimpleAlertDialog(
                title = "Ошибка",
                message = state.statsErrorMessage ?: "",
                onDismiss = { viewModel.onIntent(ReferralAction.HideStatsErrorDialog) }
            )
        }

        PullToRefreshBox(
            onRefresh = {
                if (state.isLoading.not()) {
                    viewModel.onIntent(
                        ReferralAction.Load
                    )
                }
            },
            isRefreshing = false,
            modifier = Modifier.padding(paddingValues)
        ) {

            if (state.isLoading) {
                LinearProgressIndicator(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(12.dp)
                        .padding(4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(Modifier.height(16.dp))

                // Ваш приглашительный код с копированием
                val onCopy: () -> Unit = { copier.copyText(text = state.myCode) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(
                            horizontal = ButtonDefaults.TextButtonContentPadding.calculateLeftPadding(
                                LayoutDirection.Ltr
                            )
                        )
                        .padding(bottom = 6.dp)
                        .clickable(onClick = onCopy),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Ваш код: ${state.myCode}")
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = Res.getUri("drawable/copy.svg"),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clickable(onClick = onCopy),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outline)
                    )
                }

                if(state.remainingInvitesIsVisible) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = "Осталось пригласить: ${state.remainingInvites}"
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (!state.usedReferralCode) {
                    AppTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        text = state.inputCode,
                        label = "Введите пригласительный код",
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        onChange = { viewModel.onIntent(ReferralAction.UpdateInput(it)) }
                    )
                    state.submitErrorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Button(
                            modifier = Modifier.align(Alignment.Center),
                            onClick = { viewModel.onIntent(ReferralAction.SubmitCode) },
                            enabled = state.inputCode.isNotBlank() && state.isLoading.not() && state.codeIsLoading.not()
                        ) {
                            Text("Активировать")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}