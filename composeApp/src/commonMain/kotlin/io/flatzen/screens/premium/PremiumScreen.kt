package io.flatzen.screens.premium

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.premium_active_subtitle
import flatzen.composeapp.generated.resources.premium_active_title
import flatzen.composeapp.generated.resources.premium_active_unlimited
import flatzen.composeapp.generated.resources.premium_active_until
import flatzen.composeapp.generated.resources.premium_done
import flatzen.composeapp.generated.resources.premium_feature_location
import flatzen.composeapp.generated.resources.premium_feature_no_ads
import flatzen.composeapp.generated.resources.premium_feature_realtime
import flatzen.composeapp.generated.resources.premium_manage
import flatzen.composeapp.generated.resources.premium_plans_title
import flatzen.composeapp.generated.resources.premium_restore
import flatzen.composeapp.generated.resources.premium_source_cache
import flatzen.composeapp.generated.resources.premium_source_fallback
import flatzen.composeapp.generated.resources.premium_source_rewarded
import flatzen.composeapp.generated.resources.premium_source_store
import flatzen.composeapp.generated.resources.premium_source_trial
import flatzen.composeapp.generated.resources.premium_subscribe
import flatzen.composeapp.generated.resources.premium_subtitle
import flatzen.composeapp.generated.resources.premium_title
import flatzen.composeapp.generated.resources.premium_watch_ad
import io.flatzen.common.localization.localizedProductTitle
import io.flatzen.commoncomponents.date.DateConverter
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.di.container
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.billing.SubscriptionProduct
import io.flatzen.monetization.billing.SubscriptionStatus
import io.flatzen.themes.FlatHubTheme
import io.flatzen.utils.LaunchedEffectOnce
import io.flatzen.utils.ToastDurationType
import io.flatzen.utils.ToastLauncher
import io.flatzen.utils.manageSubscriptionsUrl
import io.flatzen.viewmodel.premium.PremiumContainer
import io.flatzen.viewmodel.premium.PremiumIntent
import io.flatzen.viewmodel.premium.PremiumMessage
import io.flatzen.viewmodel.premium.PremiumState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.subscribe
import kotlin.time.ExperimentalTime
import io.flatzen.common.localization.stringResource as localizedStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen() {
    val container: PremiumContainer = container()
    val toastLauncher = androidx.compose.runtime.remember { ToastLauncher() }
    val uriHandler = LocalUriHandler.current
    val state by container.store.subscribe()

    LaunchedEffectOnce(Unit) {
        container.store.intent(PremiumIntent.Load)
    }

    val resolvedMessage = state.message?.let { message ->
        when (message) {
            is PremiumMessage.Localized -> localizedStringResource(message.key)
            is PremiumMessage.Raw -> message.text
        }
    }

    androidx.compose.runtime.LaunchedEffect(resolvedMessage) {
        resolvedMessage?.let { text ->
            toastLauncher.showToast(text, ToastDurationType.LONG)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        stringResource(Res.string.premium_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { container.store.intent(PremiumIntent.NavigateBack) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            if (state.showActivePremium) {
                ActivePremiumContent(
                    state = state,
                    onManage = {
                        runCatching { uriHandler.openUri(manageSubscriptionsUrl()) }
                    },
                    onDone = { container.store.intent(PremiumIntent.NavigateBack) },
                    onRestore = { container.store.intent(PremiumIntent.Restore) },
                    purchasing = state.purchasing,
                )
            } else {
                PurchasePremiumContent(
                    state = state,
                    onSelect = { id ->
                        container.store.intent(PremiumIntent.SelectProduct(id))
                    },
                    onPurchase = { container.store.intent(PremiumIntent.Purchase) },
                    onRestore = { container.store.intent(PremiumIntent.Restore) },
                    onWatchAd = { container.store.intent(PremiumIntent.WatchRewardedAd) },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun ActivePremiumContent(
    state: PremiumState,
    onManage: () -> Unit,
    onDone: () -> Unit,
    onRestore: () -> Unit,
    purchasing: Boolean,
) {
    val sourceLabel = when (state.statusSource) {
        SubscriptionStatus.StatusSource.STORE -> stringResource(Res.string.premium_source_store)
        SubscriptionStatus.StatusSource.TRIAL -> stringResource(Res.string.premium_source_trial)
        SubscriptionStatus.StatusSource.REWARDED -> stringResource(Res.string.premium_source_rewarded)
        SubscriptionStatus.StatusSource.CACHE -> stringResource(Res.string.premium_source_cache)
        SubscriptionStatus.StatusSource.FALLBACK -> stringResource(Res.string.premium_source_fallback)
        null -> null
    }

    val untilText = state.expiresAtEpochMs?.let { epochMs ->
        val formatted = DateConverter.formatInstant(
            Instant.fromEpochMilliseconds(epochMs),
            TimeZone.currentSystemDefault(),
        )
        stringResource(Res.string.premium_active_until, formatted)
    } ?: stringResource(Res.string.premium_active_unlimited)

    Surface(
        shape = FlatHubTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.premium_active_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(Res.string.premium_active_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (sourceLabel != null) {
                    Surface(
                        shape = FlatHubTheme.shapes.full,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = sourceLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = untilText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    FeatureList()

    Button(
        onClick = onDone,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(stringResource(Res.string.premium_done))
    }

    OutlinedButton(
        onClick = onManage,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    ) {
        Text(stringResource(Res.string.premium_manage), color = MaterialTheme.colorScheme.primary)
    }

    TextButton(
        onClick = onRestore,
        enabled = !purchasing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.premium_restore))
    }
}

@Composable
private fun PurchasePremiumContent(
    state: PremiumState,
    onSelect: (String) -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onWatchAd: () -> Unit,
) {
    Surface(
        shape = FlatHubTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            Color.Transparent
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(Res.string.premium_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(Res.string.premium_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                FeatureList()
            }
        }
    }

    Text(
        text = stringResource(Res.string.premium_plans_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )

    if (state.loading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    state.products.forEach { product ->
        PlanCard(
            product = product,
            selected = product.id == state.selectedProductId,
            recommended = product.id == MonetizationDefaults.PRODUCT_QUARTER,
            onClick = { onSelect(product.id) },
        )
    }

    Button(
        onClick = onPurchase,
        enabled = !state.purchasing && state.selectedProductId != null,
        modifier = Modifier
            .fillMaxWidth()
            .height(FlatHubTheme.dimens.ctaHeight),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = FlatHubTheme.shapes.cta,
    ) {
        if (state.purchasing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                stringResource(Res.string.premium_subscribe),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    TextButton(
        onClick = onRestore,
        enabled = !state.purchasing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.premium_restore))
    }

    TextButton(
        onClick = onWatchAd,
        enabled = !state.purchasing,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(Res.string.premium_watch_ad))
    }
}

@Composable
private fun FeatureList() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FeatureRow(stringResource(Res.string.premium_feature_realtime))
        FeatureRow(stringResource(Res.string.premium_feature_location))
        FeatureRow(stringResource(Res.string.premium_feature_no_ads))
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanCard(
    product: SubscriptionProduct,
    selected: Boolean,
    recommended: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = when {
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        onClick = onClick,
        shape = FlatHubTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        localizedProductTitle(product),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (recommended) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = FlatHubTheme.shapes.full,
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                text = localizedStringResource(LocalizationKeys.PREMIUM_RECOMMENDED),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
                product.savingsPercent?.takeIf { it > 0 }?.let { percent ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        localizedStringResource(LocalizationKeys.PREMIUM_SAVINGS, percent),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Text(
                product.priceFormatted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
