package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flyadeal.app.ui.theme.VelocityColors

/**
 * Glassmorphism effect modifier for creating frosted glass-style cards.
 *
 * Applies:
 * - Semi-transparent white background
 * - Subtle white border
 * - Rounded corners
 *
 * Note: True backdrop blur is not fully supported across all Compose Multiplatform targets.
 * This implementation uses solid semi-transparent backgrounds for cross-platform consistency.
 *
 * @param cornerRadius The corner radius for the glassmorphism effect
 * @param isHovered Whether the element is currently hovered (slightly brighter background)
 */
@Composable
fun Modifier.glassmorphism(
    cornerRadius: Dp = 20.dp,
    isHovered: Boolean = false
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val backgroundColor = if (isHovered) {
        VelocityColors.GlassHover
    } else {
        VelocityColors.GlassBg
    }

    return this
        .clip(shape)
        .background(
            color = backgroundColor,
            shape = shape
        )
        .border(
            width = 1.dp,
            color = VelocityColors.GlassBorder,
            shape = shape
        )
}

/**
 * Glassmorphism modifier with custom background alpha.
 *
 * @param cornerRadius The corner radius for the glassmorphism effect
 * @param alpha The alpha value for the background (0.0 to 1.0)
 */
@Composable
fun Modifier.glassmorphism(
    cornerRadius: Dp = 20.dp,
    alpha: Float
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val backgroundColor = VelocityColors.TextMain.copy(alpha = alpha)

    return this
        .clip(shape)
        .background(
            color = backgroundColor,
            shape = shape
        )
        .border(
            width = 1.dp,
            color = VelocityColors.GlassBorder,
            shape = shape
        )
}

/**
 * Glassmorphism modifier for rounded pill shapes (used for buttons, tags, etc.)
 *
 * @param isHovered Whether the element is currently hovered
 */
@Composable
fun Modifier.glassmorphismPill(
    isHovered: Boolean = false
): Modifier = glassmorphism(
    cornerRadius = 50.dp,
    isHovered = isHovered
)

/**
 * Glassmorphism modifier for small cards with tighter corners.
 *
 * @param isHovered Whether the element is currently hovered
 */
@Composable
fun Modifier.glassmorphismSmall(
    isHovered: Boolean = false
): Modifier = glassmorphism(
    cornerRadius = 12.dp,
    isHovered = isHovered
)
