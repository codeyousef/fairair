package com.fairair.app.ui.screens.search

/**
 * Represents the type of trip for flight search.
 */
enum class TripType {
    /**
     * One-way trip - outbound flight only
     */
    ONE_WAY,

    /**
     * Round trip - outbound and return flights
     */
    ROUND_TRIP,

    /**
     * Multi-city trip - multiple segments with different origins/destinations
     */
    MULTI_CITY
}
