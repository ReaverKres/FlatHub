package io.flatzen.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.monetization.tier.MonetizationSessionState
import io.flatzen.monetization.tier.UserTierProvider
import io.flatzen.themes.FlatHubTheme
import org.koin.compose.koinInject
import io.flatzen.common.localization.stringResource as localizedStringResource

data class PremiumUpsellState(
    val onClick: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
fun rememberPremiumUpsellState(
    navigateToPremium: () -> Unit,
): PremiumUpsellState? {
    val userTierProvider: UserTierProvider = koinInject()
    var dismissed by remember { mutableStateOf(MonetizationSessionState.premiumUpsellDismissed) }
    val show = userTierProvider.shouldShowUpsellBanner(dismissed)
    if (!show) return null
    return PremiumUpsellState(
        onClick = navigateToPremium,
        onDismiss = {
            MonetizationSessionState.premiumUpsellDismissed = true
            dismissed = true
        },
    )
}

/** Embedded premium hint inside swipe card, above the photo step bar. */
@Composable
fun PremiumUpsellCardBanner(
    state: PremiumUpsellState,
    modifier: Modifier = Modifier,
) {
    val semantic = FlatHubTheme.semantic
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(semantic.premiumDelayHint, FlatHubTheme.shapes.extraSmall)
            .padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = localizedStringResource(LocalizationKeys.PREMIUM_UPSELL_BANNER),
            style = MaterialTheme.typography.labelSmall,
            color = semantic.onPremiumDelayHint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = state.onClick),
        )
        IconButton(
            onClick = state.onDismiss,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = localizedStringResource(LocalizationKeys.CLOSE),
                tint = semantic.onPremiumDelayHint,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Inline premium hint for top bars (Home, Map). */
@Composable
fun PremiumUpsellInlineText(
    state: PremiumUpsellState,
    modifier: Modifier = Modifier,
) {
    val semantic = FlatHubTheme.semantic
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = localizedStringResource(LocalizationKeys.PREMIUM_UPSELL_BANNER),
            style = MaterialTheme.typography.labelMedium,
            color = semantic.onPremiumDelayHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .clickable(onClick = state.onClick),
        )
        IconButton(
            onClick = state.onDismiss,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = localizedStringResource(LocalizationKeys.CLOSE),
                tint = semantic.onPremiumDelayHint,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** Compact bar for Swipe and other full-bleed screens. */
@Composable
fun PremiumUpsellCompactBar(
    state: PremiumUpsellState,
    modifier: Modifier = Modifier,
) {
    val semantic = FlatHubTheme.semantic
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = localizedStringResource(LocalizationKeys.PREMIUM_UPSELL_BANNER),
            style = MaterialTheme.typography.labelSmall,
            color = semantic.onPremiumDelayHint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = state.onClick),
        )
        IconButton(
            onClick = state.onDismiss,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = localizedStringResource(LocalizationKeys.CLOSE),
                tint = semantic.onPremiumDelayHint,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
