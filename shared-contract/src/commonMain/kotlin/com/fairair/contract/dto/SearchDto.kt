package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared flight search DTOs used by both backend and frontend.
 * These represent the API request/response format for search endpoints.
 */

/**
 * Flight search request DTO.
 */
@Serializable
data class FlightSearchRequestDto(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String? = null,
    val passengers: PassengerCountsDto
)

/**
 * Passenger counts for search request.
 */
@Serializable
data class PassengerCountsDto(
    val adults: Int,
    val children: Int = 0,
    val infants: Int = 0
)

/**
 * Flight search response DTO.
 */
@Serializable
data class FlightSearchResponseDto(
    val flights: List<FlightDto>,
    val searchId: String = ""
)

/**
 * Response DTO for low-fare calendar.
 */
@Serializable
data class LowFaresResponseDto(
    val origin: String,
    val destination: String,
    val dates: List<LowFareDateDto>
)

/**
 * DTO for a single date's lowest fare.
 */
@Serializable
data class LowFareDateDto(
    /** The date in ISO format (YYYY-MM-DD) */
    val date: String,
    /** The lowest fare price in minor units, or null if no flights */
    val priceMinor: Long? = null,
    /** Formatted price display (e.g., "350 SAR"), or null */
    val priceFormatted: String? = null,
    /** Currency code */
    val currency: String? = null,
    /** The fare family code of the lowest fare */
    val fareFamily: String? = null,
    /** Number of flights available on this date */
    val flightsAvailable: Int = 0,
    /** Whether flights are available on this date */
    val available: Boolean = false
)

/**
 * Flight DTO for API responses.
 */
@Serializable
data class FlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int = 0,
    val durationFormatted: String = "",
    val aircraft: String = "",
    val fareFamilies: List<FareFamilyDto> = emptyList(),
    val seatsAvailable: Int = 0,
    val seatsBooked: Int = 0
) {
    /** Convenience property for display */
    val duration: String get() = durationFormatted.ifEmpty { "${durationMinutes}m" }
}

/**
 * Fare family DTO for API responses.
 */
@Serializable
data class FareFamilyDto(
    val code: String,
    val name: String,
    val priceMinor: Long,
    val priceFormatted: String,
    val currency: String,
    val inclusions: FareInclusionsDto
)

/**
 * Fare inclusions DTO.
 */
@Serializable
data class FareInclusionsDto(
    val carryOnBag: String,
    val checkedBag: String? = null,
    val seatSelection: String,
    val changePolicy: String,
    val cancellationPolicy: String,
    val priorityBoarding: Boolean = false,
    val loungeAccess: Boolean = false
)
