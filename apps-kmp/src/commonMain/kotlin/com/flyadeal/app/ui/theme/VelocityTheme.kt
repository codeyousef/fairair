package com.flyadeal.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.flyadeal.app.ui.components.velocity.DestinationBackground
import com.flyadeal.app.ui.components.velocity.DestinationTheme

/**
 * Velocity design system theme wrapper.
 *
 * Provides VelocityColors, VelocityTypography, and layout direction (LTR/RTL)
 * to all child composables via CompositionLocal.
 *
 * @param isRtl Whether to use right-to-left layout direction (for Arabic)
 * @param content The content to wrap with the Velocity theme
 */
@Composable
fun VelocityTheme(
    isRtl: Boolean = false,
    content: @Composable () -> Unit
) {
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val fontFamily = if (isRtl) {
        NotoKufiArabicFontFamily()
    } else {
        SpaceGroteskFontFamily()
    }

    val typography = if (isRtl) {
        createArabicTypography(fontFamily)
    } else {
        createVelocityTypography(fontFamily)
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalVelocityColors provides VelocityColors,
        LocalVelocityTypography provides typography
    ) {
        content()
    }
}

/**
 * Velocity theme wrapper with the deep purple gradient background.
 *
 * Use this for screens that need the full Velocity background.
 *
 * @param isRtl Whether to use right-to-left layout direction (for Arabic)
 * @param destinationTheme Optional destination theme for background image crossfade
 * @param content The content to wrap with the Velocity theme and background
 */
@Composable
fun VelocityThemeWithBackground(
    isRtl: Boolean = false,
    destinationTheme: DestinationTheme? = null,
    content: @Composable () -> Unit
) {
    VelocityTheme(isRtl = isRtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Layer 1: Destination background image (if selected)
            // Crossfades between different destinations
            DestinationBackground(
                destinationTheme = destinationTheme,
                modifier = Modifier.fillMaxSize()
            )

            // Layer 2: Semi-transparent gradient overlay
            // When destination image is shown, gradient becomes more translucent
            // to let the destination image show through while maintaining readability
            val gradientAlpha = if (destinationTheme != null) 0.75f else 1f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                VelocityColors.GradientStart.copy(alpha = gradientAlpha),
                                VelocityColors.GradientEnd.copy(alpha = gradientAlpha)
                            )
                        )
                    )
            )

            // Layer 3: Actual content
            content()
        }
    }
}

/**
 * Extension property to access VelocityColors from any composable within VelocityTheme.
 */
object VelocityTheme {
    val colors: VelocityColors
        @Composable
        get() = LocalVelocityColors.current

    val typography: VelocityTypography
        @Composable
        get() = LocalVelocityTypography.current
}
