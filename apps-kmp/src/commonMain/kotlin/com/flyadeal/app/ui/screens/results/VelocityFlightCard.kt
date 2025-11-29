package com.flyadeal.app.ui.screens.results

import com.flyadeal.contract.model.Money

/**
 * Display model for a single flight result card in the Velocity UI.
 */
data class VelocityFlightCard(
    /**
     * Unique flight identifier (flight number).
     */
    val id: String,

    /**
     * Flight number for display (e.g., "F3 1234").
     */
    val flightNumber: String,

    /**
     * Formatted departure time (e.g., "08:00").
     */
    val departureTime: String,

    /**
     * Formatted arrival time (e.g., "09:40").
     */
    val arrivalTime: String,

    /**
     * 3-letter origin airport code.
     */
    val originCode: String,

    /**
     * 3-letter destination airport code.
     */
    val destinationCode: String,

    /**
     * Flight duration in minutes.
     */
    val durationMinutes: Int,

    /**
     * Lowest fare price for display on collapsed card.
     */
    val lowestPrice: Money,

    /**
     * Available fare options (Fly, Fly+, FlyMax).
     */
    val fareFamilies: List<FareFamily>,

    /**
     * Whether this card is currently expanded to show fare grid.
     */
    val isExpanded: Boolean = false,

    /**
     * Whether this flight is the currently selected one.
     */
    val isSelected: Boolean = false
) {
    /**
     * Formatted duration string (e.g., "1h 40m").
     */
    val durationFormatted: String
        get() {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return when {
                hours == 0 -> "${minutes}m"
                minutes == 0 -> "${hours}h"
                else -> "${hours}h ${minutes}m"
            }
        }

    /**
     * Formatted duration string for Arabic.
     */
    val durationFormattedArabic: String
        get() {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return when {
                hours == 0 -> "${minutes}د"
                minutes == 0 -> "${hours}س"
                else -> "${hours}س ${minutes}د"
            }
        }

    /**
     * Route display string (e.g., "RUH → DXB").
     */
    val routeDisplay: String
        get() = "$originCode → $destinationCode"

    /**
     * Route display string for Arabic (reversed arrow direction).
     */
    val routeDisplayArabic: String
        get() = "$destinationCode ← $originCode"

    /**
     * Gets the selected fare family, if any.
     */
    val selectedFare: FareFamily?
        get() = fareFamilies.find { it.isSelected }

    /**
     * Creates a copy with the specified fare selected.
     */
    fun withSelectedFare(fareId: String): VelocityFlightCard {
        return copy(
            fareFamilies = fareFamilies.map { fare ->
                fare.copy(isSelected = fare.id == fareId)
            }
        )
    }

    /**
     * Creates a copy with expansion state toggled.
     */
    fun toggleExpanded(): VelocityFlightCard {
        return copy(isExpanded = !isExpanded)
    }

    /**
     * Creates a copy with expansion state set.
     */
    fun withExpanded(expanded: Boolean): VelocityFlightCard {
        return copy(isExpanded = expanded)
    }
}
