package io.flatzen.widgets

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.insight_price_above_avg
import flatzen.composeapp.generated.resources.insight_price_below_avg
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Shows "X% below/above area average" when [priceVsAreaAvgPercent] is meaningful (|%| ≥ 1).
 * Negative percent = below average (good deal tint).
 *
 * [reserveMinLines] keeps a fixed-height slot (e.g. grid cards for US) so rows stay even
 * when some listings have no insight.
 */
@Composable
fun PriceInsightLabel(
    priceVsAreaAvgPercent: Double?,
    modifier: Modifier = Modifier,
    reserveMinLines: Int = 0,
) {
    val rounded = priceVsAreaAvgPercent?.roundToInt()
    val show = rounded != null && abs(rounded) >= 1
    if (!show && reserveMinLines <= 0) return

    val below = show && (rounded ?: 0) < 0
    val label = when {
        !show -> "\u00A0"
        below -> stringResource(Res.string.insight_price_below_avg, abs(rounded ?: 0))
        else -> stringResource(Res.string.insight_price_above_avg, abs(rounded ?: 0))
    }
    val color = when {
        !show -> Color.Transparent
        below -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        minLines = reserveMinLines.coerceAtLeast(if (show) 1 else 0),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}
