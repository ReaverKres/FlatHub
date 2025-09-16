package io.flatzen.uiExtensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

inline fun Modifier.thenIf(
    condition: Boolean,
    factory: Modifier.() -> Modifier
): Modifier {
    return if (condition) factory.invoke(this) else this
}

fun Modifier.removeParentHorizontalPadding(
    horizontalPadding: Dp = 0.dp,
) = removeParentHorizontalPadding(
    start = horizontalPadding,
    end = horizontalPadding,
)

fun Modifier.removeParentPadding(padding: Dp) = removeParentPadding(
    start = padding, end = padding, top = padding, bottom = padding
)

fun Modifier.removeParentPadding(
    start: Dp = 0.dp,
    end: Dp = 0.dp,
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
): Modifier = layout { measurable, constraints ->
    val horizontalPadding = (start + end).roundToPx()
    val verticalPadding = (top + bottom).roundToPx()
    val noPaddingConstraints = constraints.copy(
        maxWidth = constraints.maxWidth + horizontalPadding,
        maxHeight = constraints.maxHeight + verticalPadding
    )
    val placeable = measurable.measure(noPaddingConstraints)
    layout(placeable.width, placeable.height) {
        placeable.place(x = 0, y = 0)
    }
}

fun Modifier.removeParentHorizontalPadding(
    start: Dp = 0.dp,
    end: Dp = 0.dp
): Modifier = layout { measurable, constraints ->
    val horizontalPadding = (start + end).roundToPx()
    val noPaddingConstraints = constraints.copy(
        maxWidth = constraints.maxWidth + horizontalPadding
    )
    val placeable = measurable.measure(noPaddingConstraints)
    layout(placeable.width, placeable.height) {
        placeable.place(x = 0, y = 0)
    }
}