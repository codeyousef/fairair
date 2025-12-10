package com.fairair.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Tool definitions for the Pilot AI assistant.
 * These define the capabilities the AI can invoke.
 */
object PilotTools {

    /**
     * Search for available flights.
     */
    val searchFlights = ToolDefinition(
        name = "search_flights",
        description = """Finds available flights to a SPECIFIC destination. Use this when:
- User wants to fly TO a specific city (e.g., "flight to Riyadh", "cheapest flight to Dubai")
- User mentions a destination city/airport
- User wants to book a flight to somewhere specific
Always use this when a destination is mentioned, even if they say "cheapest".""",
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
     * Select a flight from search results.
     * This is called when a user clicks on a flight card or says "select flight X".
     */
    val selectFlight = ToolDefinition(
        name = "select_flight",
        description = """Selects a specific flight from search results. Use ONLY when user first picks a flight (clicks card or says "I'll take F3100"). 
DO NOT use this when user confirms a booking - use create_booking instead.
After selection, get passenger details with get_saved_travelers before asking for confirmation.""",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "flight_number" to mapOf(
                    "type" to "string",
                    "description" to "The flight number to select (e.g., F3100, F3101)"
                )
            ),
            "required" to listOf("flight_number")
        )
    )

    /**
     * Get the user's saved travelers for booking.
     */
    val getSavedTravelers = ToolDefinition(
        name = "get_saved_travelers",
        description = "Retrieves the user's saved travelers (family members, frequent travelers). Use this to show the user their saved passengers before booking. If user is not logged in, this will return empty.",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf<String, Any>(),
            "required" to listOf<String>()
        )
    )

    /**
     * Create a booking for the selected flight.
     */
    val createBooking = ToolDefinition(
        name = "create_booking",
        description = """Creates an actual booking and returns a PNR confirmation. 
WHEN TO USE: Call this IMMEDIATELY when the user says "yes", "ok", "confirm", "book it", "proceed" after you showed them a booking summary with passenger details.
REQUIRED: You must have firstName, lastName, dateOfBirth, documentNumber for each passenger from get_saved_travelers.
DO NOT call select_flight or search_flights when user confirms - call create_booking instead.""",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "flight_number" to mapOf(
                    "type" to "string",
                    "description" to "The flight number to book (e.g., F3100)"
                ),
                "fare_family" to mapOf(
                    "type" to "string",
                    "description" to "Fare class: 'FLY', 'FLY_PLUS', or 'FLY_MAX'. Default to 'FLY' if not specified."
                ),
                "passengers" to mapOf(
                    "type" to "array",
                    "description" to "List of passengers - MUST include all required fields from get_saved_travelers",
                    "items" to mapOf(
                        "type" to "object",
                        "properties" to mapOf(
                            "firstName" to mapOf("type" to "string", "description" to "First name (REQUIRED)"),
                            "lastName" to mapOf("type" to "string", "description" to "Last name (REQUIRED)"),
                            "dateOfBirth" to mapOf("type" to "string", "description" to "Date of birth in YYYY-MM-DD format (REQUIRED)"),
                            "gender" to mapOf("type" to "string", "description" to "MALE or FEMALE (REQUIRED)"),
                            "documentNumber" to mapOf("type" to "string", "description" to "Passport/ID number (REQUIRED)"),
                            "nationality" to mapOf("type" to "string", "description" to "2-letter country code e.g. SA, AE (REQUIRED)")
                        ),
                        "required" to listOf("firstName", "lastName", "dateOfBirth", "gender", "documentNumber", "nationality")
                    )
                ),
                "contact_email" to mapOf(
                    "type" to "string",
                    "description" to "Email for booking confirmation (optional, uses user's email if not provided)"
                )
            ),
            "required" to listOf("flight_number", "passengers")
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
     * Find destinations with nice weather.
     * Used when user asks for destinations based on weather preferences.
     */
    val findWeatherDestinations = ToolDefinition(
        name = "find_weather_destinations",
        description = """Finds destinations with good weather conditions from the user's origin.
Use this when user asks for:
- "destinations with nice weather"
- "where can I go for sun/beach"
- "sunny destinations"
- "closest place with good weather"
The tool returns destinations with current weather info.""",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "origin" to mapOf(
                    "type" to "string",
                    "description" to "IATA airport code for departure city (default: RUH)"
                ),
                "weather_preference" to mapOf(
                    "type" to "string",
                    "description" to "Weather preference: 'sunny', 'warm', 'cool', 'any'. Default: 'sunny'"
                ),
                "max_results" to mapOf(
                    "type" to "integer",
                    "description" to "Maximum number of destinations to return (default: 5)"
                )
            ),
            "required" to listOf<String>()
        )
    )

    /**
     * Find the cheapest flights from origin.
     * Used when user asks for budget-friendly options WITHOUT specifying a destination.
     */
    val findCheapestFlights = ToolDefinition(
        name = "find_cheapest_flights",
        description = """Finds the cheapest available flights from the user's origin to ANY destination (exploring options).
ONLY use this when user does NOT specify a destination:
- "cheapest flights" (no destination mentioned)
- "where can I fly cheap"
- "best deals from Jeddah"
DO NOT use this if user mentions a destination like "to Riyadh" - use search_flights instead.
Returns a list of destinations sorted by lowest price.""",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "origin" to mapOf(
                    "type" to "string",
                    "description" to "IATA airport code for departure city (default: RUH)"
                ),
                "date_from" to mapOf(
                    "type" to "string",
                    "description" to "Start date range in YYYY-MM-DD format (default: tomorrow)"
                ),
                "date_to" to mapOf(
                    "type" to "string",
                    "description" to "End date range in YYYY-MM-DD format (default: 7 days from now)"
                ),
                "max_results" to mapOf(
                    "type" to "integer",
                    "description" to "Maximum number of destinations to return (default: 5)"
                )
            ),
            "required" to listOf<String>()
        )
    )

    /**
     * Get popular destinations from origin.
     * Used for general destination suggestions.
     */
    val getPopularDestinations = ToolDefinition(
        name = "get_popular_destinations",
        description = """Gets popular destination suggestions from the user's origin.
Use this when user asks:
- "where should I travel"
- "suggest destinations"
- "popular places to visit"
Returns trending destinations with sample prices.""",
        parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "origin" to mapOf(
                    "type" to "string",
                    "description" to "IATA airport code for departure city (default: RUH)"
                ),
                "travel_type" to mapOf(
                    "type" to "string",
                    "description" to "Type of travel: 'leisure', 'business', 'family', 'adventure'. Default: any"
                ),
                "max_results" to mapOf(
                    "type" to "integer",
                    "description" to "Maximum number of destinations to return (default: 6)"
                )
            ),
            "required" to listOf<String>()
        )
    )

    /**
     * All available tools for the AI assistant.
     */
    val allTools: List<ToolDefinition> = listOf(
        searchFlights,
        selectFlight,
        getSavedTravelers,
        createBooking,
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
        getBoardingPass,
        findWeatherDestinations,
        findCheapestFlights,
        getPopularDestinations
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
