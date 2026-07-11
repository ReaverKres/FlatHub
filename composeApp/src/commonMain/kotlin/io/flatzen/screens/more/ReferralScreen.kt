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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.cancel
import flatzen.composeapp.generated.resources.close
import flatzen.composeapp.generated.resources.copy_success
import flatzen.composeapp.generated.resources.error
import flatzen.composeapp.generated.resources.referral_activate
import flatzen.composeapp.generated.resources.referral_code
import flatzen.composeapp.generated.resources.referral_description
import flatzen.composeapp.generated.resources.referral_input_hint
import flatzen.composeapp.generated.resources.referral_my_code
import flatzen.composeapp.generated.resources.referral_notifications_available
import flatzen.composeapp.generated.resources.referral_remaining_invites
import io.flatzen.di.container
import io.flatzen.utils.ToastDurationType
import io.flatzen.utils.ToastLauncher
import io.flatzen.utils.copyLauncher
import io.flatzen.viewmodel.more.ReferralAction
import io.flatzen.viewmodel.more.ReferralContainer
import io.flatzen.viewmodel.more.ReferralIntent
import io.flatzen.widgets.AppTextField
import io.flatzen.widgets.dialogs.SimpleAlertDialog
import io.github.vinceglb.confettikit.compose.ConfettiKit
import io.github.vinceglb.confettikit.core.Angle
import io.github.vinceglb.confettikit.core.Party
import io.github.vinceglb.confettikit.core.Position
import io.github.vinceglb.confettikit.core.Spread
import io.github.vinceglb.confettikit.core.emitter.Emitter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.subscribe
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    navigateBack: () -> Unit
) {
    val toastLauncher = remember { ToastLauncher() }
    val copySuccessTemplate = stringResource(Res.string.copy_success, "%s")
    val copier = copyLauncher(
        onCopySuccess = { text ->
            toastLauncher.showToast(
                copySuccessTemplate.replace("%s", text),
                ToastDurationType.LONG
            )
        },
        onCopyError = { _ -> }
    )
    val container: ReferralContainer = container()
    val state by container.store.subscribe { action ->
        when (action) {
            is ReferralAction.Copy -> copier.copyText(text = action.text)
            is ReferralAction.ShowMessage -> toastLauncher.showToast(
                action.text,
                ToastDurationType.LONG
            )

            is ReferralAction.NotificationAvailable -> { /* handled by state.isNotificationAvailable */
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
                        stringResource(Res.string.referral_code),
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
        if (state.statsErrorMessage != null) {
            SimpleAlertDialog(
                title = stringResource(Res.string.error),
                message = state.statsErrorMessage ?: "",
                onDismiss = { container.store.intent(ReferralIntent.HideStatsErrorDialog) }
            )
        }

        PullToRefreshBox(
            onRefresh = {
                if (state.isLoading.not()) {
                    container.store.intent(ReferralIntent.Load)
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
                val onCopy: () -> Unit = { container.store.intent(ReferralIntent.CopyMyCode) }
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
                    Text(
                        text = stringResource(Res.string.referral_my_code, state.myCode),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    AsyncImage(
                        model = Res.getUri("drawable/copy.svg"),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).clickable(onClick = onCopy),
                        colorFilter = ColorFilter.tint(Color.LightGray)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                if (state.remainingInvitesIsVisible) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(Res.string.referral_remaining_invites, state.remainingInvites.toString())
                    )

                    Spacer(Modifier.height(2.dp))
                    DescriptionText(
                        text = stringResource(Res.string.referral_description)
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (!state.usedReferralCode) {
                    AppTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        text = state.inputCode,
                        label = stringResource(Res.string.referral_input_hint),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        onChange = { container.store.intent(ReferralIntent.UpdateInput(it)) }
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
                            onClick = { container.store.intent(ReferralIntent.SubmitCode) },
                            enabled = state.inputCode.isNotBlank() && state.isLoading.not() && state.codeIsLoading.not()
                        ) {
                            Text(stringResource(Res.string.referral_activate))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(24.dp))
            }
            if (state.isNotificationAvailable) {
                OnNotificationAvailable(
                    navigateBack = navigateBack,
                    toastLauncher = toastLauncher,
                    message = stringResource(Res.string.referral_notifications_available)
                )
            }
        }
    }
}

@Composable
private fun OnNotificationAvailable(
    navigateBack: () -> Unit,
    toastLauncher: ToastLauncher,
    message: String
) {

    rememberCoroutineScope().launch {
        delay(2000)
        navigateBack()
    }
    toastLauncher.showToast(
        message,
        ToastDurationType.LONG
    )

    val parties = remember {
        listOf(
            Party(
                speed = 0f,
                maxSpeed = 15f,
                damping = 0.9f,
                angle = Angle.BOTTOM,
                spread = Spread.ROUND,
                colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
                emitter = Emitter(duration = 2.seconds).perSecond(100),
                position = Position.Relative(0.0, 0.0)
                    .between(Position.Relative(1.0, 0.0))
            )
        )
    }
    ConfettiKit(
        modifier = Modifier.fillMaxSize(),
        parties = parties
    )
}
