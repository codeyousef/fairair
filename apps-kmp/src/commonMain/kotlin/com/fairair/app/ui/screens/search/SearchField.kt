package com.fairair.app.ui.screens.search

/**
 * Represents the currently active/focused field in the Velocity sentence builder UI.
 */
enum class SearchField {
    /**
     * Origin airport selector - "from [Origin]"
     */
    ORIGIN,

    /**
     * Destination airport selector - "to [Destination]"
     */
    DESTINATION,

    /**
     * Departure date selector - "departing on [Date]"
     */
    DATE,

    /**
     * Return date selector - "returning on [Date]" (for round-trip only)
     */
    RETURN_DATE,

    /**
     * Passenger count selector - "with [Passengers]"
     */
    PASSENGERS,

    /**
     * Trip type selector - "One-way / Round-trip / Multi-city"
     */
    TRIP_TYPE
}
