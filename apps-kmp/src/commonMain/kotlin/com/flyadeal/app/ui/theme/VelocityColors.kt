package com.flyadeal.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Velocity UI design system colors.
 * Deep purple background with neon lime accent for a modern, immersive experience.
 */
@Immutable
object VelocityColors {
    /**
     * Primary app background - deep purple/black (#120521)
     */
    val BackgroundDeep = Color(0xFF120521)

    /**
     * Accent color for CTAs, highlights, and interactive elements - neon lime (#CCFF00)
     */
    val Accent = Color(0xFFCCFF00)

    /**
     * Glassmorphism card background - semi-transparent white
     */
    val GlassBg = Color.White.copy(alpha = 0.1f)

    /**
     * Glassmorphism card hover/active state
     */
    val GlassHover = Color.White.copy(alpha = 0.15f)

    /**
     * Glassmorphism card border - subtle white
     */
    val GlassBorder = Color.White.copy(alpha = 0.1f)

    /**
     * Primary text color - pure white
     */
    val TextMain = Color.White

    /**
     * Secondary/muted text color - 60% white
     */
    val TextMuted = Color.White.copy(alpha = 0.6f)

    /**
     * Glow effect for launch button - accent with reduced alpha
     */
    val NeonGlow = Color(0xFFCCFF00).copy(alpha = 0.3f)

    /**
     * Disabled state color
     */
    val Disabled = Color.White.copy(alpha = 0.3f)

    /**
     * Error state color
     */
    val Error = Color(0xFFFF6B6B)

    /**
     * Success state color
     */
    val Success = Color(0xFF4ADE80)

    /**
     * Background gradient start (top)
     */
    val GradientStart = Color(0xFF1A0A2E)

    /**
     * Background gradient end (bottom)
     */
    val GradientEnd = Color(0xFF120521)
}

/**
 * CompositionLocal for accessing VelocityColors in the composition tree.
 */
val LocalVelocityColors = staticCompositionLocalOf { VelocityColors }
