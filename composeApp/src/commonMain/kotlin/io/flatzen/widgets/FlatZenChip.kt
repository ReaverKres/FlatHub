package io.flatzen.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.flatzen.themes.FlatHubTheme

enum class FlatZenChipStyle {
    /** Surface chip for list cards (owner, photo, etc.). */
    Surface,

    /** Secondary container — owner / platform. */
    Secondary,

    /** Primary container — "new" badge. */
    Primary,

    /** Photo overlay meta chip (swipe). */
    Overlay,
}

@Composable
fun FlatZenChip(
    text: String,
    modifier: Modifier = Modifier,
    style: FlatZenChipStyle = FlatZenChipStyle.Surface,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    val shapes = FlatHubTheme.shapes
    val (containerColor, contentColor) = when (style) {
        FlatZenChipStyle.Surface -> MaterialTheme.colorScheme.surfaceVariant to
                MaterialTheme.colorScheme.onSurfaceVariant

        FlatZenChipStyle.Secondary -> MaterialTheme.colorScheme.secondaryContainer to
                MaterialTheme.colorScheme.onSecondaryContainer

        FlatZenChipStyle.Primary -> MaterialTheme.colorScheme.primaryContainer to
                MaterialTheme.colorScheme.onPrimaryContainer

        FlatZenChipStyle.Overlay -> FlatHubTheme.semantic.photoOverlayScrim to Color.White
    }

    Text(
        text = text,
        style = textStyle,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(containerColor, shapes.extraSmall)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun FlatZenOverlayChip(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium,
) {
    FlatZenChip(
        text = text,
        modifier = modifier,
        style = FlatZenChipStyle.Overlay,
        textStyle = textStyle,
    )
}

@Composable
fun FlatZenBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Surface(
        modifier = modifier,
        shape = FlatHubTheme.shapes.full,
        color = containerColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
