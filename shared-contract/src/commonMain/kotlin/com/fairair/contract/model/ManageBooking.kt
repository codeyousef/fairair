package com.fairair.contract.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Request to retrieve a booking for management.
 */
@Serializable
data class RetrieveBookingRequest(
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
 * Full booking details for management operations.
 */
@Serializable
data class ManagedBooking(
    val pnr: PnrCode,
    val bookingReference: String,
    val status: BookingStatus,
    val flight: ManagedFlightDetails,
    val passengers: List<ManagedPassenger>,
    val ancillaries: List<BookedAncillary>,
    val payment: PaymentSummary,
    val contact: ContactDetails,
    val createdAt: Instant,
    val lastModifiedAt: Instant,
    val allowedActions: List<BookingAction>
)

/**
 * Booking status enumeration.
 */
@Serializable
enum class BookingStatus {
    /**
     * Booking is confirmed and active.
     */
    CONFIRMED,
    
    /**
     * Passengers have checked in.
     */
    CHECKED_IN,
    
    /**
     * Flight has departed.
     */
    FLOWN,
    
    /**
     * Booking was cancelled by customer.
     */
    CANCELLED,
    
    /**
     * Booking was cancelled due to flight cancellation.
     */
    FLIGHT_CANCELLED,
    
    /**
     * Booking is pending payment confirmation.
     */
    PENDING_PAYMENT,
    
    /**
     * Booking has been modified and awaiting confirmation.
     */
    PENDING_MODIFICATION
}

/**
 * Detailed flight information for management screen.
 */
@Serializable
data class ManagedFlightDetails(
    val flightNumber: String,
    val origin: AirportCode,
    val destination: AirportCode,
    val originCity: String,
    val destinationCity: String,
    val originAirport: String,
    val destinationAirport: String,
    val departureTime: Instant,
    val arrivalTime: Instant,
    val durationMinutes: Int,
    val fareFamily: FareFamilyCode,
    val aircraft: String,
    val flightStatus: FlightStatus
)

/**
 * Flight status for display.
 */
@Serializable
enum class FlightStatus {
    SCHEDULED,
    ON_TIME,
    DELAYED,
    BOARDING,
    DEPARTED,
    IN_FLIGHT,
    LANDED,
    CANCELLED
}

/**
 * Passenger details with booking-specific information.
 */
@Serializable
data class ManagedPassenger(
    val index: Int,
    val type: PassengerType,
    val title: Title,
    val firstName: String,
    val lastName: String,
    val nationality: String,
    val dateOfBirth: LocalDate,
    val documentId: String,
    val seatNumber: String?,
    val isCheckedIn: Boolean,
    val specialRequests: List<SpecialRequest>,
    val ancillaries: List<BookedAncillary>
) {
    val fullName: String get() = "$firstName $lastName"
}

/**
 * Special service requests for a passenger.
 */
@Serializable
data class SpecialRequest(
    val type: SpecialRequestType,
    val details: String?,
    val status: RequestStatus
)

/**
 * Types of special service requests.
 */
@Serializable
enum class SpecialRequestType {
    WHEELCHAIR,
    BLIND_ASSISTANCE,
    DEAF_ASSISTANCE,
    UNACCOMPANIED_MINOR,
    MEDICAL_EQUIPMENT,
    SPECIAL_MEAL,
    BASSINET,
    PET_IN_CABIN
}

/**
 * Status of a special request.
 */
@Serializable
enum class RequestStatus {
    REQUESTED,
    CONFIRMED,
    PENDING_VERIFICATION,
    DENIED
}

/**
 * Booked ancillary with details.
 */
@Serializable
data class BookedAncillary(
    val type: AncillaryType,
    val description: String,
    val quantity: Int,
    val price: Money,
    val status: AncillaryStatus
)

/**
 * Ancillary status.
 */
@Serializable
enum class AncillaryStatus {
    CONFIRMED,
    PENDING,
    CANCELLED,
    USED
}

/**
 * Payment summary for booking management.
 */
@Serializable
data class PaymentSummary(
    val baseFare: Money,
    val taxes: Money,
    val ancillaries: Money,
    val fees: Money,
    val total: Money,
    val paid: Money,
    val refunded: Money,
    val paymentMethod: String,
    val lastFourDigits: String?
)

/**
 * Contact details for the booking.
 */
@Serializable
data class ContactDetails(
    val email: String,
    val phone: String?,
    val alternatePhone: String?
)

/**
 * Actions that can be performed on a booking.
 */
@Serializable
enum class BookingAction {
    /**
     * Can check in online.
     */
    CHECK_IN,
    
    /**
     * Can view/download boarding pass.
     */
    VIEW_BOARDING_PASS,
    
    /**
     * Can change flight date/time.
     */
    CHANGE_FLIGHT,
    
    /**
     * Can cancel for refund.
     */
    CANCEL_WITH_REFUND,
    
    /**
     * Can cancel but no refund.
     */
    CANCEL_NO_REFUND,
    
    /**
     * Can add ancillaries.
     */
    ADD_ANCILLARIES,
    
    /**
     * Can select/change seats.
     */
    SELECT_SEATS,
    
    /**
     * Can order meals.
     */
    ORDER_MEALS,
    
    /**
     * Can update passenger details.
     */
    UPDATE_PASSENGERS,
    
    /**
     * Can request special assistance.
     */
    REQUEST_ASSISTANCE,
    
    /**
     * Can upgrade fare family.
     */
    UPGRADE_FARE,
    
    /**
     * Can download e-ticket.
     */
    DOWNLOAD_TICKET
}

/**
 * Request to change flight.
 */
@Serializable
data class ChangeFlightRequest(
    val newDepartureDate: LocalDate,
    val preferredFlightNumber: String?
)

/**
 * Quote for flight change with any applicable fees.
 */
@Serializable
data class FlightChangeQuote(
    val originalFlight: FlightSummary,
    val availableFlights: List<FlightChangeOption>,
    val changeFee: Money?,
    val priceDifference: Money,
    val totalToPay: Money,
    val expiresAt: Instant
)

/**
 * Flight option for change.
 */
@Serializable
data class FlightChangeOption(
    val flightNumber: String,
    val departureTime: Instant,
    val arrivalTime: Instant,
    val priceDifference: Money,
    val seatsAvailable: Int
)

/**
 * Request to cancel booking.
 */
@Serializable
data class CancelBookingRequest(
    val reason: CancellationReason,
    val comments: String?
)

/**
 * Cancellation reasons.
 */
@Serializable
enum class CancellationReason {
    CHANGE_OF_PLANS,
    SCHEDULE_CONFLICT,
    ILLNESS,
    VISA_ISSUES,
    FOUND_BETTER_OPTION,
    OTHER
}

/**
 * Cancellation confirmation with refund details.
 */
@Serializable
data class CancellationConfirmation(
    val pnr: PnrCode,
    val cancelledAt: Instant,
    val refundAmount: Money,
    val refundMethod: String,
    val refundEstimatedDays: Int,
    val cancellationReference: String
)

/**
 * Request to add ancillaries to existing booking.
 */
@Serializable
data class AddAncillariesRequest(
    val ancillaries: List<AncillaryRequest>
)

/**
 * Single ancillary request.
 */
@Serializable
data class AncillaryRequest(
    val type: AncillaryType,
    val passengerIndex: Int,
    val quantity: Int
)

/**
 * Request to update passenger details.
 */
@Serializable
data class UpdatePassengersRequest(
    val updates: List<PassengerUpdate>
)

/**
 * Update for a single passenger.
 */
@Serializable
data class PassengerUpdate(
    val passengerIndex: Int,
    val firstName: String?,
    val lastName: String?,
    val documentId: String?,
    val nationality: String?
)
