package com.fairair.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * FairAir UI design system colors.
 * Modern violet/cyan palette on deep midnight background.
 * Inspired by contemporary fintech and travel apps.
 */
@Immutable
object VelocityColors {
    /**
     * Primary brand color - electric violet (#8B5CF6)
     * Used for primary actions and brand elements
     */
    val Primary = Color(0xFF8B5CF6)

    /**
     * Primary app background - deep midnight (#0F172A)
     */
    val BackgroundDeep = Color(0xFF0F172A)

    /**
     * Mid-tone background - dark slate (#1E293B)
     * Used for cards and elevated surfaces
     */
    val BackgroundMid = Color(0xFF1E293B)

    /**
     * Accent color for CTAs, highlights, and interactive elements - bright cyan (#06B6D4)
     */
    val Accent = Color(0xFF06B6D4)

    /**
     * Glassmorphism card background - semi-transparent midnight
     */
    val GlassBg = Color(0xFF1E293B).copy(alpha = 0.6f)

    /**
     * Glassmorphism card hover/active state
     */
    val GlassHover = Color(0xFF334155).copy(alpha = 0.7f)

    /**
     * Glassmorphism card border - subtle violet tint
     */
    val GlassBorder = Color(0xFF8B5CF6).copy(alpha = 0.15f)

    /**
     * Primary text color - clean white
     */
    val TextMain = Color(0xFFF8FAFC)

    /**
     * Secondary/muted text color - cool gray (#94A3B8)
     */
    val TextMuted = Color(0xFF94A3B8)

    /**
     * Glow effect for launch button - cyan accent glow
     */
    val NeonGlow = Color(0xFF06B6D4).copy(alpha = 0.4f)

    /**
     * Disabled state color
     */
    val Disabled = Color(0xFF475569)

    /**
     * Error state color - rose red (#F43F5E)
     */
    val Error = Color(0xFFF43F5E)

    /**
     * Success state color - emerald (#10B981)
     */
    val Success = Color(0xFF10B981)

    /**
     * Warning state color - amber (#F59E0B)
     */
    val Warning = Color(0xFFF59E0B)

    /**
     * Background gradient start (top) - deep violet (#1E1B4B)
     */
    val GradientStart = Color(0xFF1E1B4B)

    /**
     * Background gradient end (bottom) - midnight (#0F172A)
     */
    val GradientEnd = Color(0xFF0F172A)
    
    // Price indicator colors for low-fare calendar
    
    /**
     * Low/cheap price indicator - green (#10B981)
     */
    val PriceLow = Color(0xFF10B981)
    
    /**
     * Medium price indicator - amber (#F59E0B)
     */
    val PriceMedium = Color(0xFFF59E0B)
    
    /**
     * High/expensive price indicator - rose (#F43F5E)
     */
    val PriceHigh = Color(0xFFF43F5E)
}

/**
 * CompositionLocal for accessing VelocityColors in the composition tree.
 */
val LocalVelocityColors = staticCompositionLocalOf { VelocityColors }
