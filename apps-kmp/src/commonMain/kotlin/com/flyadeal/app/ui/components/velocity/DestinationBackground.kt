package com.flyadeal.app.ui.components.velocity

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource

/**
 * Full-screen destination background image with crossfade animation.
 *
 * Displays a destination-specific background image when a destination is selected.
 * The image is dimmed to 40% brightness to ensure text remains readable.
 * Crossfades smoothly between different destination backgrounds.
 *
 * @param destinationTheme The destination theme containing the background image, or null for no image
 * @param modifier Modifier to apply to the component
 */
@Composable
fun DestinationBackground(
    destinationTheme: DestinationTheme?,
    modifier: Modifier = Modifier
) {
    // Crossfade between destinations based on destination code
    // Using destinationCode as key ensures proper animation when destination changes
    Crossfade(
        targetState = destinationTheme,
        animationSpec = tween(durationMillis = 600),
        label = "destination_background_crossfade",
        modifier = modifier.fillMaxSize()
    ) { theme ->
        if (theme != null) {
            Image(
                painter = painterResource(theme.backgroundImage),
                contentDescription = "Background image for ${theme.cityName}",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Reduce brightness to 40% (60% darkening)
                        // This ensures text and UI elements remain readable
                        alpha = 0.4f
                    },
                contentScale = ContentScale.Crop
            )
        }
        // When theme is null, render nothing (empty box is implicit)
    }
}

/**
 * Layered background combining destination image and gradient overlay.
 *
 * Renders the destination background image (if any) with a semi-transparent
 * gradient overlay on top to maintain the Velocity theme aesthetic while
 * showing destination-specific imagery.
 *
 * @param destinationTheme The destination theme, or null for gradient-only background
 * @param modifier Modifier to apply to the component
 * @param content Content to render on top of the background
 */
@Composable
fun DestinationBackgroundLayer(
    destinationTheme: DestinationTheme?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Destination image (if available)
        DestinationBackground(
            destinationTheme = destinationTheme,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2: Content with gradient overlay applied at the theme level
        content()
    }
}

/**
 * Preview helper for testing destination backgrounds.
 * Lists all available destination themes with their background images.
 */
object DestinationBackgroundPreview {
    /**
     * Get all available destination themes for preview purposes.
     */
    fun allDestinations(): List<DestinationTheme> {
        return DestinationTheme.allDestinations()
    }
}
