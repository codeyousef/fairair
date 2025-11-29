package com.flyadeal.app.ui.components.velocity

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A glassmorphism-styled card component.
 *
 * Provides a semi-transparent frosted glass effect with subtle borders,
 * hover states, and optional click handling.
 *
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler. If null, the card is not clickable
 * @param cornerRadius Corner radius for the card
 * @param contentPadding Padding inside the card
 * @param animateSize Whether to animate content size changes
 * @param content The content to display inside the card
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 24.dp,
    animateSize: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val effectiveHovered = isHovered || isPressed

    val cardModifier = modifier
        .then(
            if (animateSize) {
                Modifier.animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            } else {
                Modifier
            }
        )
        .glassmorphism(
            cornerRadius = cornerRadius,
            isHovered = effectiveHovered
        )
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
        .padding(contentPadding)

    Box(
        modifier = cardModifier,
        content = content
    )
}

/**
 * A compact glassmorphism-styled card with smaller corners and padding.
 *
 * @param modifier Modifier to be applied to the card
 * @param onClick Optional click handler. If null, the card is not clickable
 * @param animateSize Whether to animate content size changes
 * @param content The content to display inside the card
 */
@Composable
fun GlassCardSmall(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    animateSize: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        cornerRadius = 12.dp,
        contentPadding = 16.dp,
        animateSize = animateSize,
        content = content
    )
}

/**
 * A pill-shaped glassmorphism container (rounded ends).
 *
 * @param modifier Modifier to be applied to the container
 * @param onClick Optional click handler. If null, the container is not clickable
 * @param contentPadding Padding inside the container
 * @param content The content to display inside the container
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val effectiveHovered = isHovered || isPressed

    val pillModifier = modifier
        .glassmorphismPill(isHovered = effectiveHovered)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
        .padding(contentPadding)

    Box(
        modifier = pillModifier,
        content = content
    )
}
