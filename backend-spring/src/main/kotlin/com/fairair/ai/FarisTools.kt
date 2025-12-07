package com.fairair.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Tool definitions for the Faris AI assistant.
 * These define the capabilities the AI can invoke.
 */
object FarisTools {

    /**
     * Search for available flights.
     */
    val searchFlights = ToolDefinition(
        name = "search_flights",
        description = "Finds available flights between two cities on a specific date. Use this when the user wants to search for or book a new flight.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "origin" to mapOf(
                    "type" to "string",
                    "description" to "IATA airport code for departure city (e.g., RUH for Riyadh, JED for Jeddah). REQUIRED - ask user if not provided."
                ),
                "destination" to mapOf(
                    "type" to "string",
                    "description" to "IATA airport code for arrival city (e.g., JED for Jeddah, DXB for Dubai)"
                ),
                "date" to mapOf(
                    "type" to "string",
                    "description" to "Travel date in YYYY-MM-DD format. Calculate from relative dates like 'tomorrow', 'next Friday'"
                ),
                "passengers" to mapOf(
                    "type" to "integer",
                    "description" to "Number of passengers (default: 1)"
                )
            ),
            "required" to listOf("destination")
        )
    )

    /**
     * Retrieve an existing booking.
     */
    val getBooking = ToolDefinition(
        name = "get_booking",
        description = "Retrieves booking details by PNR (6-character booking reference). Use this to look up existing reservations.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code (PNR)"
                )
            ),
            "required" to listOf("pnr")
        )
    )

    /**
     * Cancel a specific passenger from a booking (Split PNR operation).
     */
    val cancelSpecificPassenger = ToolDefinition(
        name = "cancel_specific_passenger",
        description = "Cancels ONE specific passenger from a group booking, leaving other passengers active. This performs a PNR split operation. Always confirm with the user before cancelling.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "First name of the passenger to remove from the booking"
                )
            ),
            "required" to listOf("pnr", "passenger_name")
        )
    )

    /**
     * Calculate change fees for moving to a different flight.
     */
    val calculateChangeFees = ToolDefinition(
        name = "calculate_change_fees",
        description = "Calculates the cost difference and any fees for changing a booking to a different flight.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "new_flight_number" to mapOf(
                    "type" to "string",
                    "description" to "Flight number of the new flight to change to"
                )
            ),
            "required" to listOf("pnr", "new_flight_number")
        )
    )

    /**
     * Change a booking to a different flight.
     */
    val changeFlight = ToolDefinition(
        name = "change_flight",
        description = "Changes an existing booking to a different flight. Requires prior fee calculation.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "new_flight_number" to mapOf(
                    "type" to "string",
                    "description" to "Flight number of the new flight"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "Optional: specific passenger to change (if not all)"
                )
            ),
            "required" to listOf("pnr", "new_flight_number")
        )
    )

    /**
     * Get seat map for a flight.
     */
    val getSeatMap = ToolDefinition(
        name = "get_seat_map",
        description = "Retrieves the seat map showing available and occupied seats for a booking's flight.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "Optional: specific passenger to show seat options for"
                )
            ),
            "required" to listOf("pnr")
        )
    )

    /**
     * Change a passenger's seat.
     */
    val changeSeat = ToolDefinition(
        name = "change_seat",
        description = "Changes the seat assignment for a passenger. Ask user preference (aisle/window) if not specified.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "First name of the passenger"
                ),
                "new_seat" to mapOf(
                    "type" to "string",
                    "description" to "New seat number (e.g., '12A', '15F')"
                ),
                "preference" to mapOf(
                    "type" to "string",
                    "description" to "Seat preference: 'window', 'aisle', or 'middle'"
                )
            ),
            "required" to listOf("pnr", "passenger_name")
        )
    )

    /**
     * Get available meals for a flight.
     */
    val getAvailableMeals = ToolDefinition(
        name = "get_available_meals",
        description = "Lists available meal options for a booking.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                )
            ),
            "required" to listOf("pnr")
        )
    )

    /**
     * Add a meal to a booking.
     */
    val addMeal = ToolDefinition(
        name = "add_meal",
        description = "Adds a meal selection for a passenger. Suggest the FareAir Bundle if adding items separately.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "First name of the passenger"
                ),
                "meal_code" to mapOf(
                    "type" to "string",
                    "description" to "Code of the meal to add"
                )
            ),
            "required" to listOf("pnr", "passenger_name", "meal_code")
        )
    )

    /**
     * Add baggage to a booking.
     */
    val addBaggage = ToolDefinition(
        name = "add_baggage",
        description = "Adds extra baggage allowance for a passenger. Suggest the FareAir Bundle if adding items separately.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "First name of the passenger"
                ),
                "weight_kg" to mapOf(
                    "type" to "integer",
                    "description" to "Baggage weight in kilograms (typically 20, 25, or 30)"
                )
            ),
            "required" to listOf("pnr", "passenger_name", "weight_kg")
        )
    )

    /**
     * Check in a passenger.
     */
    val checkIn = ToolDefinition(
        name = "check_in",
        description = "Performs online check-in for passengers on a booking.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "Optional: specific passenger to check in (otherwise all eligible)"
                )
            ),
            "required" to listOf("pnr")
        )
    )

    /**
     * Get boarding pass.
     */
    val getBoardingPass = ToolDefinition(
        name = "get_boarding_pass",
        description = "Retrieves the boarding pass for a checked-in passenger.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "pnr" to mapOf(
                    "type" to "string",
                    "description" to "6-character booking reference code"
                ),
                "passenger_name" to mapOf(
                    "type" to "string",
                    "description" to "First name of the passenger"
                )
            ),
            "required" to listOf("pnr", "passenger_name")
        )
    )

    /**
     * All available tools for the AI assistant.
     */
    val allTools: List<ToolDefinition> = listOf(
        searchFlights,
        getBooking,
        cancelSpecificPassenger,
        calculateChangeFees,
        changeFlight,
        getSeatMap,
        changeSeat,
        getAvailableMeals,
        addMeal,
        addBaggage,
        checkIn,
        getBoardingPass
    )
}

/**
 * Definition of a tool that the AI can invoke.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)
