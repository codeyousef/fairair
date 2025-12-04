package com.fairair.contract.model

import kotlinx.serialization.Serializable

/**
 * Complete seat map for an aircraft/flight.
 */
@Serializable
data class SeatMap(
    val flightNumber: String,
    val aircraft: String,
    val cabins: List<Cabin>,
    val legend: SeatLegend
)

/**
 * Aircraft cabin section.
 */
@Serializable
data class Cabin(
    val name: String,
    val rows: List<SeatRow>,
    val aislePositions: List<Int>
)

/**
 * Row of seats.
 */
@Serializable
data class SeatRow(
    val rowNumber: Int,
    val seats: List<Seat>,
    val isExitRow: Boolean,
    val hasExtraLegroom: Boolean
)

/**
 * Individual seat.
 */
@Serializable
data class Seat(
    val seatNumber: String,
    val column: String,
    val type: SeatType,
    val status: SeatStatus,
    val price: Money?,
    val features: List<SeatFeature>
)

/**
 * Seat type classification.
 */
@Serializable
enum class SeatType {
    /**
     * Standard economy seat.
     */
    STANDARD,
    
    /**
     * Extra legroom seat.
     */
    EXTRA_LEGROOM,
    
    /**
     * Exit row seat (extra legroom, restrictions apply).
     */
    EXIT_ROW,
    
    /**
     * Bulkhead seat (extra legroom, no under-seat storage).
     */
    BULKHEAD,
    
    /**
     * Premium economy seat.
     */
    PREMIUM,
    
    /**
     * Window seat.
     */
    WINDOW,
    
    /**
     * Aisle seat.
     */
    AISLE,
    
    /**
     * Middle seat.
     */
    MIDDLE
}

/**
 * Seat availability status.
 */
@Serializable
enum class SeatStatus {
    /**
     * Seat is available for selection.
     */
    AVAILABLE,
    
    /**
     * Seat is occupied by another passenger.
     */
    OCCUPIED,
    
    /**
     * Seat is blocked (crew, equipment, etc.).
     */
    BLOCKED,
    
    /**
     * Seat is selected by current user.
     */
    SELECTED,
    
    /**
     * Seat is reserved (temporary hold).
     */
    RESERVED,
    
    /**
     * Seat is not available for this fare type.
     */
    RESTRICTED
}

/**
 * Additional seat features.
 */
@Serializable
enum class SeatFeature {
    WINDOW,
    AISLE,
    EXTRA_LEGROOM,
    POWER_OUTLET,
    NO_RECLINE,
    LIMITED_RECLINE,
    BASSINET_CAPABLE,
    NEAR_LAVATORY,
    NEAR_GALLEY,
    WING_VIEW,
    NO_WINDOW
}

/**
 * Legend explaining seat map symbols and pricing.
 */
@Serializable
data class SeatLegend(
    val items: List<SeatLegendItem>
)

/**
 * Single legend item.
 */
@Serializable
data class SeatLegendItem(
    val type: SeatType,
    val status: SeatStatus?,
    val label: String,
    val color: String,
    val priceRange: String?
)

/**
 * Request to reserve seats during booking.
 */
@Serializable
data class SeatReservationRequest(
    val searchId: String,
    val flightNumber: String,
    val seats: List<PassengerSeatSelection>
)

/**
 * Seat selection for a passenger.
 */
@Serializable
data class PassengerSeatSelection(
    val passengerIndex: Int,
    val seatNumber: String
)

/**
 * Seat reservation confirmation.
 */
@Serializable
data class SeatReservation(
    val reservationId: String,
    val flightNumber: String,
    val seats: List<ReservedSeat>,
    val totalPrice: Money,
    val expiresAt: kotlinx.datetime.Instant
)

/**
 * Reserved seat details.
 */
@Serializable
data class ReservedSeat(
    val passengerIndex: Int,
    val seatNumber: String,
    val price: Money
)

/**
 * Request to assign seats to an existing booking.
 */
@Serializable
data class SeatAssignmentRequest(
    val pnr: String,
    val seats: List<PassengerSeatSelection>
)

/**
 * Seat assignment confirmation.
 */
@Serializable
data class SeatAssignment(
    val pnr: PnrCode,
    val assignments: List<PassengerSeatAssignment>,
    val totalCharged: Money
)

/**
 * Assigned seat for a passenger.
 */
@Serializable
data class PassengerSeatAssignment(
    val passengerName: String,
    val passengerIndex: Int,
    val seatNumber: String,
    val price: Money
)
