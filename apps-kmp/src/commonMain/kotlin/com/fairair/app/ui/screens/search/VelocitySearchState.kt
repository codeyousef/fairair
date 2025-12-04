package com.fairair.app.ui.screens.search

import com.fairair.app.api.LowFareDateDto
import com.fairair.app.api.StationDto
import com.fairair.app.ui.components.velocity.DestinationTheme
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
    val adultsCount: Int = 1,

    /**
     * Number of child passengers (0-8). Children: 2-11 years.
     */
    val childrenCount: Int = 0,

    /**
     * Number of infant passengers (0-4). Infants: under 2 years.
     * Cannot exceed number of adults (1 infant per adult lap).
     */
    val infantsCount: Int = 0,

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
     * Whether destinations are being fetched from the backend.
     */
    val loadingDestinations: Boolean = false,

    /**
     * Whether a search is currently in progress.
     */
    val isSearching: Boolean = false,
    
    /**
     * Low fare prices by date for calendar display.
     * Key is LocalDate, value is the low fare data for that date.
     */
    val lowFares: Map<LocalDate, LowFareDateDto> = emptyMap(),
    
    /**
     * Whether low fares are currently being loaded.
     */
    val loadingLowFares: Boolean = false,

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
     * Total passenger count for display purposes.
     */
    val totalPassengerCount: Int
        get() = adultsCount + childrenCount + infantsCount

    /**
     * Formatted passenger label (e.g., "1 Adult" or "2 Adults, 1 Child").
     */
    val passengerLabel: String
        get() {
            val parts = mutableListOf<String>()
            if (adultsCount > 0) {
                parts.add(if (adultsCount == 1) "1 Adult" else "$adultsCount Adults")
            }
            if (childrenCount > 0) {
                parts.add(if (childrenCount == 1) "1 Child" else "$childrenCount Children")
            }
            if (infantsCount > 0) {
                parts.add(if (infantsCount == 1) "1 Infant" else "$infantsCount Infants")
            }
            return parts.joinToString(", ").ifEmpty { "1 Adult" }
        }

    /**
     * Arabic formatted passenger label.
     */
    val passengerLabelArabic: String
        get() {
            val parts = mutableListOf<String>()
            if (adultsCount > 0) {
                parts.add(if (adultsCount == 1) "1 بالغ" else "$adultsCount بالغين")
            }
            if (childrenCount > 0) {
                parts.add(if (childrenCount == 1) "1 طفل" else "$childrenCount أطفال")
            }
            if (infantsCount > 0) {
                parts.add(if (infantsCount == 1) "1 رضيع" else "$infantsCount رضع")
            }
            return parts.joinToString("، ").ifEmpty { "1 بالغ" }
        }

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
