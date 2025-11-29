package com.flyadeal.app.ui.screens.results

/**
 * UI state for the Velocity flight results overlay.
 */
data class VelocityResultsState(
    /**
     * Whether the results overlay is visible (slides up from bottom).
     */
    val isVisible: Boolean = false,

    /**
     * List of flight results to display.
     */
    val flights: List<VelocityFlightCard> = emptyList(),

    /**
     * ID of the currently expanded flight card (only one at a time).
     */
    val expandedFlightId: String? = null,

    /**
     * Currently selected flight (for booking flow).
     */
    val selectedFlight: VelocityFlightCard? = null,

    /**
     * Currently selected fare within the selected flight.
     */
    val selectedFare: FareFamily? = null,

    /**
     * Route display string (e.g., "RUH → DXB").
     */
    val routeDisplay: String = "",

    /**
     * Formatted date display (e.g., "MON, 01 DEC").
     */
    val dateDisplay: String = "",

    /**
     * Whether results are currently loading.
     */
    val isLoading: Boolean = false,

    /**
     * Error message if search failed.
     */
    val error: String? = null
) {
    /**
     * Current state type for rendering.
     */
    val stateType: ResultsStateType
        get() = when {
            isLoading -> ResultsStateType.LOADING
            error != null -> ResultsStateType.ERROR
            flights.isEmpty() -> ResultsStateType.EMPTY
            else -> ResultsStateType.CONTENT
        }

    /**
     * Gets flights with correct expansion state applied.
     */
    val flightsWithExpansion: List<VelocityFlightCard>
        get() = flights.map { flight ->
            flight.copy(isExpanded = flight.id == expandedFlightId)
        }

    /**
     * Returns a copy with the specified flight expanded (collapsing others).
     */
    fun withExpandedFlight(flightId: String?): VelocityResultsState {
        // If clicking the same card, collapse it
        val newExpandedId = if (expandedFlightId == flightId) null else flightId
        return copy(expandedFlightId = newExpandedId)
    }

    /**
     * Returns a copy with the specified fare selected on the specified flight.
     */
    fun withSelectedFare(flightId: String, fareId: String): VelocityResultsState {
        val flight = flights.find { it.id == flightId } ?: return this
        val fare = flight.fareFamilies.find { it.id == fareId } ?: return this

        val updatedFlights = flights.map { f ->
            if (f.id == flightId) {
                f.withSelectedFare(fareId)
            } else {
                f.copy(fareFamilies = f.fareFamilies.map { it.copy(isSelected = false) })
            }
        }

        return copy(
            flights = updatedFlights,
            selectedFlight = flight.withSelectedFare(fareId),
            selectedFare = fare.copy(isSelected = true)
        )
    }

    /**
     * Returns a copy with the overlay shown.
     */
    fun show(): VelocityResultsState = copy(isVisible = true)

    /**
     * Returns a copy with the overlay hidden and state reset.
     */
    fun hide(): VelocityResultsState = copy(
        isVisible = false,
        expandedFlightId = null
    )

    /**
     * Returns a copy in loading state.
     */
    fun loading(): VelocityResultsState = copy(
        isLoading = true,
        error = null,
        flights = emptyList(),
        expandedFlightId = null,
        selectedFlight = null,
        selectedFare = null
    )

    /**
     * Returns a copy with error state.
     */
    fun withError(message: String): VelocityResultsState = copy(
        isLoading = false,
        error = message,
        flights = emptyList()
    )

    /**
     * Returns a copy with content loaded.
     */
    fun withContent(
        flights: List<VelocityFlightCard>,
        routeDisplay: String,
        dateDisplay: String
    ): VelocityResultsState = copy(
        isLoading = false,
        error = null,
        flights = flights,
        routeDisplay = routeDisplay,
        dateDisplay = dateDisplay
    )

    companion object {
        /**
         * Initial hidden state.
         */
        val Initial = VelocityResultsState()
    }
}

/**
 * Type of results state for rendering different UI.
 */
enum class ResultsStateType {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT
}
