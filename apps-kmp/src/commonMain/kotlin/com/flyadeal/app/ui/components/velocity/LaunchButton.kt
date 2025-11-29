package com.flyadeal.app.ui.components.velocity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.flyadeal.app.ui.theme.VelocityColors

/**
 * Circular launch button with neon glow effect for initiating flight search.
 *
 * Features:
 * - 80dp circular shape with accent color
 * - Radial gradient glow effect when enabled
 * - Scale animation on press
 * - Disabled state (grayed out) when conditions not met
 * - Loading state with progress indicator
 *
 * @param enabled Whether the button is enabled (destination must be selected)
 * @param loading Whether a search is in progress
 * @param onClick Callback when the button is clicked
 * @param modifier Modifier to apply to the component
 */
@Composable
fun LaunchButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "launch_button_scale"
    )

    val backgroundColor = when {
        loading -> VelocityColors.Accent.copy(alpha = 0.7f)
        enabled -> VelocityColors.Accent
        else -> VelocityColors.Disabled
    }

    val glowAlpha = when {
        !enabled -> 0f
        loading -> 0.15f
        isPressed -> 0.5f
        else -> 0.3f
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                // Glow effect
                if (glowAlpha > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                VelocityColors.Accent.copy(alpha = glowAlpha),
                                VelocityColors.Accent.copy(alpha = glowAlpha * 0.5f),
                                Color.Transparent
                            ),
                            radius = size.minDimension * 0.9f
                        ),
                        radius = size.minDimension * 0.9f
                    )
                }
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = VelocityColors.BackgroundDeep,
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Search",
                tint = VelocityColors.BackgroundDeep,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Smaller variant of the launch button for compact layouts.
 */
@Composable
fun LaunchButtonSmall(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !loading) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "launch_button_small_scale"
    )

    val backgroundColor = when {
        loading -> VelocityColors.Accent.copy(alpha = 0.7f)
        enabled -> VelocityColors.Accent
        else -> VelocityColors.Disabled
    }

    Box(
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = VelocityColors.BackgroundDeep,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Search",
                tint = VelocityColors.BackgroundDeep,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
