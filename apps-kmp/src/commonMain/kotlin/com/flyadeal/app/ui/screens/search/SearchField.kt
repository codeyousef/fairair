package com.flyadeal.app.ui.screens.search

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
     * Passenger count selector - "with [Passengers]"
     */
    PASSENGERS
}
