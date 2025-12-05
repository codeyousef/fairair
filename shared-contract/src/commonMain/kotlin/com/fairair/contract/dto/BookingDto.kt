package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared booking DTOs used by both backend and frontend.
 * These represent the API request/response format for booking endpoints.
 */

/**
 * Booking request DTO.
 */
@Serializable
data class BookingRequestDto(
    val searchId: String = "",
    val flightNumber: String,
    val fareFamily: String,
    val passengers: List<PassengerDto>,
    val ancillaries: List<AncillaryDto> = emptyList(),
    val contactEmail: String,
    val contactPhone: String = "",
    val payment: PaymentDto
)

/**
 * Passenger DTO for booking request.
 */
@Serializable
data class PassengerDto(
    val type: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val nationality: String = "",
    val documentId: String = ""
)

/**
 * Ancillary DTO for booking request.
 */
@Serializable
data class AncillaryDto(
    val type: String,
    val passengerIndex: Int,
    val priceMinor: Long = 0,
    val currency: String = "SAR"
)

/**
 * Payment DTO for booking request.
 */
@Serializable
data class PaymentDto(
    val cardholderName: String,
    val cardNumberLast4: String,
    val totalAmountMinor: Long,
    val currency: String
)

/**
 * Booking confirmation response DTO.
 */
@Serializable
data class BookingConfirmationDto(
    val pnr: String = "",
    val bookingReference: String = "",
    val flight: FlightSummaryDto? = null,
    val passengers: List<PassengerSummaryDto> = emptyList(),
    val status: String = "CONFIRMED",
    val totalPaidMinor: Long = 0,
    val totalPaidFormatted: String = "0",
    val currency: String = "SAR",
    val createdAt: String = ""
) {
    /** Convenience property for display */
    val totalPrice: String get() = totalPaidFormatted.ifEmpty { (totalPaidMinor / 100.0).toString() }
}

/**
 * Flight summary DTO for booking confirmation.
 */
@Serializable
data class FlightSummaryDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val fareFamily: String
)

/**
 * Passenger summary DTO for booking confirmation.
 */
@Serializable
data class PassengerSummaryDto(
    val fullName: String,
    val type: String
)

/**
 * Error response DTO for booking endpoints.
 */
@Serializable
data class BookingErrorDto(
    val code: String,
    val message: String
)
