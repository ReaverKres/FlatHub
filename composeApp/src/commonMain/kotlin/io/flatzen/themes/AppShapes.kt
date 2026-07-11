package io.flatzen.themes

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

class AppShapes(
    val none: CornerBasedShape = RoundedCornerShape(0.dp),
    val extraSmall: CornerBasedShape = RoundedCornerShape(8.dp),
    val small: CornerBasedShape = RoundedCornerShape(12.dp),
    val medium: CornerBasedShape = RoundedCornerShape(16.dp),
    val large: CornerBasedShape = RoundedCornerShape(20.dp),
    val extraLarge: CornerBasedShape = RoundedCornerShape(24.dp),
    val full: CornerBasedShape = RoundedCornerShape(50),
    val cta: CornerBasedShape = RoundedCornerShape(14.dp),
    /** @deprecated Prefer [medium]. Kept for gradual migration. */
    val cardItemDefault: CornerBasedShape = medium,
)

fun AppShapes.toMaterialShapes(): Shapes = Shapes(
    extraSmall = extraSmall,
    small = small,
    medium = medium,
    large = large,
    extraLarge = extraLarge,
)
