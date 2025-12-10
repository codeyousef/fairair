package com.fairair.contract.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared DTOs for AI Chat functionality.
 * Used by both backend (Spring) and frontend (KMP) for the Pilot AI assistant.
 */

// ============================================================================
// Request DTOs
// ============================================================================

/**
 * Request to send a message to the AI chat.
 */
@Serializable
data class ChatMessageRequestDto(
    /** Unique session identifier for conversation continuity */
    val sessionId: String,
    /** The user's message (text or transcribed voice) */
    val message: String,
    /** Optional locale hint (e.g., "en-US", "ar-SA") */
    val locale: String? = null,
    /** Optional context about the current screen/state */
    val context: ChatContextDto? = null
)

/**
 * Optional context about the user's current state in the app.
 */
@Serializable
data class ChatContextDto(
    /** Current PNR if user is viewing a booking */
    val currentPnr: String? = null,
    /** Current screen name */
    val currentScreen: String? = null,
    /** User ID if logged in */
    val userId: String? = null,
    /** User's email if logged in */
    val userEmail: String? = null,
    /** Last search ID for booking context */
    val lastSearchId: String? = null,
    /** Last selected flight number */
    val lastFlightNumber: String? = null,
    /** User's origin airport code based on geolocation (e.g., "JED", "RUH") */
    val userOriginAirport: String? = null,
    /** User's location latitude (if geolocation enabled) */
    val userLatitude: Double? = null,
    /** User's location longitude (if geolocation enabled) */
    val userLongitude: Double? = null,
    /** Whether geolocation permission was granted */
    val hasLocationPermission: Boolean = false,
    /** Any additional context key-value pairs */
    val metadata: Map<String, String> = emptyMap()
)

// ============================================================================
// Response DTOs
// ============================================================================

/**
 * Response from the AI chat service.
 */
@Serializable
data class ChatResponseDto(
    /** The AI's text response */
    val text: String,
    /** Type of UI component to render (if any) */
    val uiType: ChatUiType? = null,
    /** JSON payload for the UI component */
    val uiData: String? = null,
    /** List of suggested quick replies */
    val suggestions: List<String> = emptyList(),
    /** Whether the AI is still processing (for streaming) */
    val isPartial: Boolean = false,
    /** Detected language of the conversation */
    val detectedLanguage: String? = null
)

/**
 * Types of UI components the AI can request to render.
 */
@Serializable
enum class ChatUiType {
    /** Display a list/carousel of flight options */
    @SerialName("FLIGHT_LIST")
    FLIGHT_LIST,

    /** Display an interactive seat map */
    @SerialName("SEAT_MAP")
    SEAT_MAP,

    /** Display a boarding pass with QR code */
    @SerialName("BOARDING_PASS")
    BOARDING_PASS,

    /** Display a comparison between old and new flight */
    @SerialName("FLIGHT_COMPARISON")
    FLIGHT_COMPARISON,

    /** Display booking summary/confirmation */
    @SerialName("BOOKING_SUMMARY")
    BOOKING_SUMMARY,

    /** Display passenger selection */
    @SerialName("PASSENGER_SELECT")
    PASSENGER_SELECT,

    /** Display payment confirmation */
    @SerialName("PAYMENT_CONFIRM")
    PAYMENT_CONFIRM,

    /** Display ancillary options (bags, meals) */
    @SerialName("ANCILLARY_OPTIONS")
    ANCILLARY_OPTIONS,

    /** Confirmation that a flight was selected */
    @SerialName("FLIGHT_SELECTED")
    FLIGHT_SELECTED,

    /** Confirmation that a booking was created */
    @SerialName("BOOKING_CONFIRMED")
    BOOKING_CONFIRMED,

    /** Display passenger entry form */
    @SerialName("PASSENGER_FORM")
    PASSENGER_FORM,

    /** Display destination suggestions (weather-based, cheapest, etc.) */
    @SerialName("DESTINATION_SUGGESTIONS")
    DESTINATION_SUGGESTIONS,

    /** Clarification needed - ask user for more info */
    @SerialName("CLARIFICATION")
    CLARIFICATION,

    /** Display check-in flow */
    @SerialName("CHECK_IN_FLOW")
    CHECK_IN_FLOW,

    /** Display payment form */
    @SerialName("PAYMENT_FORM")
    PAYMENT_FORM
}

// ============================================================================
// UI Payload DTOs (for uiData field)
// ============================================================================

/**
 * Payload for FLIGHT_LIST UI type.
 */
@Serializable
data class FlightListPayloadDto(
    val flights: List<FlightOptionDto>,
    val origin: String,
    val destination: String,
    val date: String
)

/**
 * A single flight option in the AI response.
 */
@Serializable
data class FlightOptionDto(
    val flightNumber: String,
    val departureTime: String,
    val arrivalTime: String,
    val priceFormatted: String,
    val priceMinor: Long,
    val currency: String = "SAR",
    val seatsAvailable: Int? = null,
    val duration: String? = null
)

/**
 * Payload for SEAT_MAP UI type.
 */
@Serializable
data class SeatMapPayloadDto(
    val pnr: String,
    val passengerName: String,
    val currentSeat: String?,
    val highlightedRow: Int?,
    val availableSeats: List<String>,
    val flightNumber: String
)

/**
 * Payload for FLIGHT_COMPARISON UI type.
 */
@Serializable
data class FlightComparisonPayloadDto(
    val pnr: String,
    val oldFlight: FlightOptionDto,
    val newFlight: FlightOptionDto,
    val priceDifferenceFormatted: String,
    val priceDifferenceMinor: Long
)

/**
 * Payload for BOOKING_SUMMARY UI type.
 */
@Serializable
data class BookingSummaryPayloadDto(
    val pnr: String,
    val status: String,
    val passengers: List<PassengerSummaryPayloadDto>,
    val flight: FlightOptionDto,
    val totalPaidFormatted: String
)

/**
 * Passenger info in booking summary.
 */
@Serializable
data class PassengerSummaryPayloadDto(
    val name: String,
    val seat: String?,
    val type: String
)

/**
 * Payload for PASSENGER_SELECT UI type.
 */
@Serializable
data class PassengerSelectPayloadDto(
    val pnr: String,
    val passengers: List<PassengerSummaryPayloadDto>,
    val action: String // "cancel", "change_seat", "add_baggage"
)

/**
 * Payload for BOARDING_PASS UI type.
 */
@Serializable
data class BoardingPassPayloadDto(
    val pnr: String,
    val passengerName: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val gate: String?,
    val seat: String,
    val boardingGroup: String?,
    val qrCodeData: String
)

/**
 * Payload for DESTINATION_SUGGESTIONS UI type.
 */
@Serializable
data class DestinationSuggestionsPayloadDto(
    val suggestions: List<DestinationSuggestionDto>,
    val suggestionType: String, // "weather", "cheapest", "popular", "quick_getaway"
    val originCode: String? = null
)

/**
 * A single destination suggestion.
 */
@Serializable
data class DestinationSuggestionDto(
    val destinationCode: String,
    val destinationName: String,
    val country: String,
    val lowestPrice: Double? = null,
    val currency: String = "SAR",
    val weather: WeatherInfoDto? = null,
    val flightDuration: String? = null,
    val imageUrl: String? = null,
    val reason: String? = null // e.g., "Sunny beaches", "35% off this week"
)

/**
 * Weather information for a destination.
 */
@Serializable
data class WeatherInfoDto(
    val temperature: Int,
    val condition: String, // "sunny", "cloudy", "rainy", etc.
    val description: String // "Perfect beach weather"
)

/**
 * Payload for PASSENGER_FORM UI type.
 */
@Serializable
data class PassengerFormPayloadDto(
    val flightNumber: String,
    val flightDetails: FlightOptionDto,
    val passengerCount: Int,
    val savedTravelers: List<SavedTravelerDto> = emptyList(),
    val fareFamily: String = "FLY"
)

/**
 * Payload for CLARIFICATION UI type.
 * Note: SavedTravelerDto is defined in ProfileDto.kt (same package)
 */
@Serializable
data class ClarificationPayloadDto(
    val question: String,
    val options: List<ClarificationOptionDto> = emptyList(),
    val inputType: String = "text", // "text", "date", "select", "number"
    val context: String? = null // What info we already have
)

/**
 * An option for clarification.
 */
@Serializable
data class ClarificationOptionDto(
    val label: String,
    val value: String
)

// ============================================================================
// Chat History DTOs
// ============================================================================

/**
 * A single message in the chat history.
 */
@Serializable
data class ChatMessageDto(
    /** Message role: "user" or "assistant" */
    val role: String,
    /** Message content */
    val content: String,
    /** Timestamp (ISO-8601) */
    val timestamp: String? = null,
    /** UI type if this was an assistant message with UI */
    val uiType: ChatUiType? = null,
    /** UI data if this was an assistant message with UI */
    val uiData: String? = null
)

/**
 * Full chat session with history.
 */
@Serializable
data class ChatSessionDto(
    val sessionId: String,
    val messages: List<ChatMessageDto>,
    val createdAt: String,
    val lastActivityAt: String
)
