package com.flyadeal.app.ui.components.velocity

import com.flyadeal.apps_kmp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource

/**
 * Configuration for destination-specific background imagery.
 */
data class DestinationTheme(
    /**
     * 3-letter airport code (e.g., "JED", "DXB").
     */
    val destinationCode: String,

    /**
     * Background image resource.
     */
    val backgroundImage: DrawableResource,

    /**
     * Display name for the destination city.
     */
    val cityName: String,

    /**
     * Arabic display name for the destination city.
     */
    val cityNameArabic: String,

    /**
     * Whether this background is currently active/shown.
     */
    val isActive: Boolean = false
) {
    companion object {
        /**
         * Predefined destination themes.
         */
        private val destinations = mapOf(
            "JED" to DestinationTheme(
                destinationCode = "JED",
                backgroundImage = Res.drawable.bg_jed,
                cityName = "Jeddah",
                cityNameArabic = "جدة"
            ),
            "DXB" to DestinationTheme(
                destinationCode = "DXB",
                backgroundImage = Res.drawable.bg_dxb,
                cityName = "Dubai",
                cityNameArabic = "دبي"
            ),
            "CAI" to DestinationTheme(
                destinationCode = "CAI",
                backgroundImage = Res.drawable.bg_cai,
                cityName = "Cairo",
                cityNameArabic = "القاهرة"
            ),
            "RUH" to DestinationTheme(
                destinationCode = "RUH",
                backgroundImage = Res.drawable.bg_ruh,
                cityName = "Riyadh",
                cityNameArabic = "الرياض"
            ),
            "DMM" to DestinationTheme(
                destinationCode = "DMM",
                backgroundImage = Res.drawable.bg_dmm,
                cityName = "Dammam",
                cityNameArabic = "الدمام"
            )
        )

        /**
         * Gets the destination theme for a given airport code.
         * Returns null if the destination doesn't have a custom background.
         */
        fun forDestination(code: String): DestinationTheme? {
            return destinations[code.uppercase()]?.copy(isActive = true)
        }

        /**
         * Gets all available destination themes.
         */
        fun allDestinations(): List<DestinationTheme> {
            return destinations.values.toList()
        }

        /**
         * List of destination codes that have custom backgrounds.
         */
        val supportedCodes: Set<String> = destinations.keys
    }
}
