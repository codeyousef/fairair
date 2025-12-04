package com.fairair.contract.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Check-in request with PNR and last name verification.
 */
@Serializable
data class CheckInRequest(
    val pnr: String,
    val lastName: String
) {
    init {
        require(pnr.length == 6 && pnr.all { it.isUpperCase() || it.isDigit() }) {
            "PNR must be 6 alphanumeric uppercase characters: '$pnr'"
        }
        require(lastName.length >= 2) {
            "Last name must be at least 2 characters: '$lastName'"
        }
    }
}

/**
 * Check-in eligibility response showing booking details and which passengers can check in.
 */
@Serializable
data class CheckInEligibility(
    val pnr: PnrCode,
    val flight: FlightSummary,
    val passengers: List<PassengerCheckInStatus>,
    val checkInOpenTime: Instant,
    val checkInCloseTime: Instant,
    val isCheckInOpen: Boolean
)

/**
 * Individual passenger check-in status.
 */
@Serializable
data class PassengerCheckInStatus(
    val passengerIndex: Int,
    val fullName: String,
    val type: PassengerType,
    val isEligible: Boolean,
    val isCheckedIn: Boolean,
    val seatNumber: String?,
    val boardingGroup: String?,
    val ineligibilityReason: CheckInIneligibilityReason?
)

/**
 * Reasons why a passenger might not be eligible for check-in.
 */
@Serializable
enum class CheckInIneligibilityReason {
    /**
     * Check-in window not yet open (typically 48 hours before departure).
     */
    TOO_EARLY,
    
    /**
     * Check-in window has closed (typically 1-3 hours before departure).
     */
    TOO_LATE,
    
    /**
     * Passenger has already checked in.
     */
    ALREADY_CHECKED_IN,
    
    /**
     * Booking has been cancelled.
     */
    BOOKING_CANCELLED,
    
    /**
     * Flight has been cancelled.
     */
    FLIGHT_CANCELLED,
    
    /**
     * Passenger requires special assistance verification.
     */
    REQUIRES_ASSISTANCE_VERIFICATION,
    
    /**
     * Infant passenger (must check in at airport).
     */
    INFANT_PASSENGER,
    
    /**
     * Documentation issues require airport verification.
     */
    DOCUMENT_VERIFICATION_REQUIRED
}

/**
 * Request to complete check-in for specific passengers.
 */
@Serializable
data class CompleteCheckInRequest(
    val passengerIndices: List<Int>,
    val seatPreferences: List<SeatPreference>?
)

/**
 * Seat preference for check-in.
 */
@Serializable
data class SeatPreference(
    val passengerIndex: Int,
    val preferredSeat: String?,
    val preferenceType: SeatPreferenceType
)

/**
 * Types of seat preferences.
 */
@Serializable
enum class SeatPreferenceType {
    WINDOW,
    AISLE,
    MIDDLE,
    FRONT,
    BACK,
    EXIT_ROW,
    SPECIFIC
}

/**
 * Boarding pass data for a checked-in passenger.
 */
@Serializable
data class BoardingPass(
    val pnr: PnrCode,
    val passengerName: String,
    val passengerType: PassengerType,
    val flightNumber: String,
    val origin: AirportCode,
    val destination: AirportCode,
    val originCity: String,
    val destinationCity: String,
    val departureDate: LocalDate,
    val departureTime: String,
    val boardingTime: String,
    val gate: String?,
    val seatNumber: String,
    val boardingGroup: String,
    val sequenceNumber: Int,
    val fareFamily: FareFamilyCode,
    val cabinClass: String,
    val barcodeData: String,
    val issuedAt: Instant
) {
    /**
     * Generates a display-friendly boarding time message.
     */
    fun boardingMessage(): String =
        "Please be at gate ${gate ?: "TBA"} by $boardingTime"
}

/**
 * Check-in completion response with boarding passes.
 */
@Serializable
data class CheckInCompletion(
    val pnr: PnrCode,
    val boardingPasses: List<BoardingPass>,
    val checkedInCount: Int,
    val message: String
)
