package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared check-in DTOs used by both backend and frontend.
 * These represent the API request/response format for check-in endpoints.
 */

/**
 * Check-in lookup request DTO.
 */
@Serializable
data class CheckInLookupRequestDto(
    val pnr: String,
    val lastName: String
)

/**
 * Check-in lookup response DTO.
 */
@Serializable
data class CheckInLookupResponseDto(
    val pnr: String,
    val flight: CheckInFlightDto,
    val passengers: List<CheckInPassengerDto>,
    val isEligibleForCheckIn: Boolean,
    val eligibilityMessage: String? = null
)

/**
 * Flight info DTO for check-in.
 */
@Serializable
data class CheckInFlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val departureDate: String,
    val aircraft: String = ""
)

/**
 * Passenger DTO for check-in.
 */
@Serializable
data class CheckInPassengerDto(
    val passengerId: String,
    val firstName: String,
    val lastName: String,
    val type: String,
    val isCheckedIn: Boolean = false,
    val seatAssignment: String? = null,
    val boardingGroup: String? = null
)

/**
 * Check-in process request DTO.
 */
@Serializable
data class CheckInProcessRequestDto(
    val pnr: String,
    val passengerIds: List<String>,
    val seatPreferences: Map<String, SeatPreferenceDto> = emptyMap()
)

/**
 * Seat preference DTO.
 */
@Serializable
data class SeatPreferenceDto(
    val preferWindow: Boolean = false,
    val preferAisle: Boolean = false,
    val preferFront: Boolean = false
)

/**
 * Check-in result DTO.
 */
@Serializable
data class CheckInResultDto(
    val pnr: String,
    val checkedInPassengers: List<CheckedInPassengerDto>,
    val message: String
)

/**
 * Checked-in passenger DTO.
 */
@Serializable
data class CheckedInPassengerDto(
    val passengerId: String,
    val name: String,
    val seatNumber: String,
    val boardingGroup: String,
    val boardingPassUrl: String? = null
)

/**
 * Boarding pass DTO.
 */
@Serializable
data class BoardingPassDto(
    val pnr: String,
    val flightNumber: String,
    val passengerName: String,
    val seatNumber: String,
    val boardingGroup: String,
    val gate: String? = null,
    val boardingTime: String? = null,
    val departureTime: String,
    val origin: String,
    val destination: String,
    val barcodeData: String
)
