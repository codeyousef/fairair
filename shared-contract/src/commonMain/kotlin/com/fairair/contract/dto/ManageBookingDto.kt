package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared manage booking DTOs used by both backend and frontend.
 * These represent the API request/response format for manage booking endpoints.
 */

/**
 * Retrieve booking request DTO.
 */
@Serializable
data class RetrieveBookingRequestDto(
    val pnr: String,
    val lastName: String
)

/**
 * Manage booking response DTO.
 */
@Serializable
data class ManageBookingResponseDto(
    val pnr: String,
    val status: String,
    val flight: ManageBookingFlightDto,
    val passengers: List<ManageBookingPassengerDto>,
    val ancillaries: List<BookedAncillaryDto>,
    val payment: PaymentSummaryDto,
    val allowedActions: List<String>
)

/**
 * Flight DTO for manage booking.
 */
@Serializable
data class ManageBookingFlightDto(
    val flightNumber: String,
    val origin: String,
    val originName: String,
    val destination: String,
    val destinationName: String,
    val departureTime: String,
    val arrivalTime: String,
    val departureDate: String,
    val fareFamily: String
)

/**
 * Passenger DTO for manage booking.
 */
@Serializable
data class ManageBookingPassengerDto(
    val passengerId: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val type: String,
    val dateOfBirth: String? = null
)

/**
 * Booked ancillary DTO.
 */
@Serializable
data class BookedAncillaryDto(
    val type: String,
    val description: String,
    val passengerId: String,
    val priceFormatted: String
)

/**
 * Payment summary DTO.
 */
@Serializable
data class PaymentSummaryDto(
    val totalPaidMinor: Long,
    val totalPaidFormatted: String,
    val currency: String,
    val paymentMethod: String,
    val lastFourDigits: String? = null
)

/**
 * Modify booking request DTO.
 */
@Serializable
data class ModifyBookingRequestDto(
    val pnr: String,
    val lastName: String,
    val modifications: BookingModificationsDto
)

/**
 * Booking modifications DTO.
 */
@Serializable
data class BookingModificationsDto(
    val newFlightNumber: String? = null,
    val newDepartureDate: String? = null,
    val passengerUpdates: List<PassengerUpdateDto> = emptyList(),
    val addAncillaries: List<AncillaryDto> = emptyList(),
    val removeAncillaries: List<String> = emptyList()
)

/**
 * Passenger update DTO.
 */
@Serializable
data class PassengerUpdateDto(
    val passengerId: String,
    val firstName: String? = null,
    val lastName: String? = null
)

/**
 * Modify booking response DTO.
 */
@Serializable
data class ModifyBookingResponseDto(
    val pnr: String,
    val success: Boolean,
    val message: String,
    val priceDifferenceFormatted: String? = null,
    val requiresPayment: Boolean = false
)

/**
 * Cancel booking request DTO.
 */
@Serializable
data class CancelBookingRequestDto(
    val pnr: String,
    val lastName: String,
    val reason: String? = null
)

/**
 * Cancel booking response DTO.
 */
@Serializable
data class CancelBookingResponseDto(
    val pnr: String,
    val success: Boolean,
    val message: String,
    val refundAmountFormatted: String? = null,
    val refundMethod: String? = null
)
