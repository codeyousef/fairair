package com.flyadeal.app.ui.screens.search

import com.flyadeal.app.api.StationDto
import com.flyadeal.app.ui.components.velocity.DestinationTheme
import kotlinx.datetime.LocalDate

/**
 * UI state for the Velocity natural language sentence-builder search interface.
 */
data class VelocitySearchState(
    /**
     * Currently selected origin airport.
     */
    val selectedOrigin: StationDto? = null,

    /**
     * Currently selected destination airport.
     */
    val selectedDestination: StationDto? = null,

    /**
     * Selected departure date.
     */
    val departureDate: LocalDate? = null,

    /**
     * Number of adult passengers (1-9).
     */
    val passengerCount: Int = 1,

    /**
     * List of available origin airports.
     */
    val availableOrigins: List<StationDto> = emptyList(),

    /**
     * List of valid destinations based on selected origin.
     * Empty if no origin is selected.
     */
    val availableDestinations: List<StationDto> = emptyList(),

    /**
     * Route map: origin code -> list of valid destination codes.
     */
    val routeMap: Map<String, List<String>> = emptyMap(),

    /**
     * Currently active/focused input field for showing selection sheet.
     */
    val activeField: SearchField? = null,

    /**
     * Active destination background theme based on selected destination.
     * Null if no destination is selected.
     */
    val destinationBackground: DestinationTheme? = null,

    /**
     * Whether the initial data (stations, routes) is loading.
     */
    val isLoading: Boolean = true,

    /**
     * Whether a search is currently in progress.
     */
    val isSearching: Boolean = false,

    /**
     * Error message if something went wrong.
     */
    val error: String? = null
) {
    /**
     * Whether the search button should be enabled.
     * Requires: origin, destination, and date to be selected.
     */
    val isSearchEnabled: Boolean
        get() = selectedOrigin != null &&
                selectedDestination != null &&
                departureDate != null &&
                !isSearching

    /**
     * Formatted departure date for display (e.g., "Dec 01").
     */
    val formattedDate: String
        get() = departureDate?.let { date ->
            val monthNames = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            val month = monthNames[date.monthNumber - 1]
            val day = date.dayOfMonth.toString().padStart(2, '0')
            "$month $day"
        } ?: ""

    /**
     * Formatted passenger label (e.g., "1 Adult" or "2 Adults").
     */
    val passengerLabel: String
        get() = if (passengerCount == 1) "1 Adult" else "$passengerCount Adults"

    /**
     * Arabic formatted passenger label.
     */
    val passengerLabelArabic: String
        get() = if (passengerCount == 1) "1 بالغ" else "$passengerCount بالغين"

    /**
     * Formatted date for Arabic display.
     */
    val formattedDateArabic: String
        get() = departureDate?.let { date ->
            val monthNames = listOf(
                "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
                "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
            )
            val month = monthNames[date.monthNumber - 1]
            val day = date.dayOfMonth
            "$day $month"
        } ?: ""

    companion object {
        /**
         * Initial state with loading true.
         */
        val Initial = VelocitySearchState()
    }
}
