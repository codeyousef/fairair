package com.fairair.service

import com.fairair.contract.model.*
import com.fairair.controller.SeatFlightNotFoundException
import com.fairair.controller.PassengerSeatAssignment
import com.fairair.controller.SeatUnavailableException
import com.fairair.repository.BookingRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * Service for seat selection operations.
 * Handles seat map generation, reservations, and assignments.
 *
 * Production Notes:
 * - Integrate with Navitaire SeatMap API
 * - Real-time seat availability from airline inventory
 * - Handle concurrent reservations with proper locking
 */
@Service
class SeatsService(
    private val bookingRepository: BookingRepository
) {
    private val log = LoggerFactory.getLogger(SeatsService::class.java)

    // In-memory storage for seat reservations (mock)
    private val reservations = ConcurrentHashMap<String, SeatReservation>()
    private val assignedSeats = ConcurrentHashMap<String, MutableSet<String>>() // flightKey -> set of assigned seats
    private val reservedSeats = ConcurrentHashMap<String, MutableSet<String>>() // flightKey -> set of temporarily reserved seats

    /**
     * Gets the seat map for a flight.
     */
    suspend fun getSeatMap(flightNumber: String, date: LocalDate): SeatMap {
        log.info("Getting seat map for flight=$flightNumber, date=$date")

        // Validate flight exists (mock validation)
        if (!isValidFlight(flightNumber)) {
            throw SeatFlightNotFoundException("Flight $flightNumber not found")
        }

        val flightKey = "$flightNumber-$date"
        val assigned = assignedSeats.getOrDefault(flightKey, emptySet())

        // Generate A320 seat map (typical flyadeal aircraft)
        val rows = generateA320SeatMap(assigned)

        return SeatMap(
            flightNumber = flightNumber,
            date = date,
            aircraft = AircraftInfo(
                type = "Airbus A320",
                registration = "HZ-FAD",
                totalSeats = 186,
                configuration = "3-3"
            ),
            rows = rows,
            legend = listOf(
                SeatLegend(type = "available", label = "Available", color = "#4CAF50"),
                SeatLegend(type = "occupied", label = "Occupied", color = "#9E9E9E"),
                SeatLegend(type = "extra_legroom", label = "Extra Legroom", color = "#2196F3"),
                SeatLegend(type = "exit_row", label = "Exit Row", color = "#FF9800"),
                SeatLegend(type = "blocked", label = "Blocked", color = "#F44336")
            ),
            pricing = SeatPricing(
                standard = Money.sar(0.0),
                preferred = Money.sar(25.0),
                extraLegroom = Money.sar(50.0),
                exitRow = Money.sar(75.0)
            )
        )
    }

    /**
     * Temporarily reserves seats during booking.
     */
    suspend fun reserveSeats(
        flightNumber: String,
        date: LocalDate,
        seats: List<String>,
        sessionId: String
    ): SeatReservationResponse {
        log.info("Reserving seats $seats for flight=$flightNumber, session=$sessionId")

        val flightKey = "$flightNumber-$date"

        // Check availability against both assigned and reserved seats
        val assigned = assignedSeats.getOrDefault(flightKey, mutableSetOf())
        val reserved = reservedSeats.getOrDefault(flightKey, mutableSetOf())
        val unavailableAssigned = seats.filter { it in assigned }
        val unavailableReserved = seats.filter { it in reserved }
        val unavailable = unavailableAssigned + unavailableReserved
        if (unavailable.isNotEmpty()) {
            throw SeatUnavailableException("Seats not available: ${unavailable.distinct().joinToString()}")
        }

        // Clear any existing reservation for this session
        val oldReservations = reservations.filter { it.value.sessionId == sessionId }
        oldReservations.forEach { (id, res) ->
            reservedSeats[res.flightKey]?.removeAll(res.seats)
            reservations.remove(id)
        }

        // Create reservation and add to reserved seats
        val reservation = SeatReservation(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            flightKey = flightKey,
            seats = seats,
            expiresAt = Clock.System.now().plus(15.minutes)
        )
        reservations[reservation.id] = reservation
        reservedSeats.getOrPut(flightKey) { mutableSetOf() }.addAll(seats)

        // Calculate total price
        val totalPrice = seats.sumOf { seat -> getSeatPrice(seat).amountAsDouble }

        return SeatReservationResponse(
            reservationId = reservation.id,
            seats = seats,
            totalPrice = Money.sar(totalPrice),
            expiresAt = reservation.expiresAt,
            message = "Seats reserved for 15 minutes"
        )
    }

    /**
     * Assigns seats to an existing booking.
     */
    suspend fun assignSeats(
        pnr: String,
        assignments: List<PassengerSeatAssignment>
    ): SeatAssignmentResponse {
        log.info("Assigning seats for PNR=$pnr: ${assignments.map { it.seatNumber }}")

        val booking = bookingRepository.findByPnr(pnr.uppercase())
            ?: throw com.fairair.controller.BookingNotFoundException(pnr)

        val flightKey = "${booking.flightNumber}-${booking.departureTime.toString().take(10)}"
        val assigned = assignedSeats.getOrPut(flightKey) { mutableSetOf() }

        // Check availability
        val requestedSeats = assignments.map { it.seatNumber }
        val unavailable = requestedSeats.filter { it in assigned }
        if (unavailable.isNotEmpty()) {
            throw SeatUnavailableException("Seats not available: ${unavailable.joinToString()}")
        }

        // Assign seats
        assigned.addAll(requestedSeats)

        // Parse passenger names from JSON
        val passengerNames = parsePassengerNames(booking.passengersJson)

        // Calculate total
        val totalPrice = requestedSeats.sumOf { getSeatPrice(it).amountAsDouble }

        return SeatAssignmentResponse(
            pnr = pnr,
            assignments = assignments.map { assignment ->
                AssignedSeat(
                    passengerIndex = assignment.passengerIndex,
                    passengerName = passengerNames.getOrNull(assignment.passengerIndex)
                        ?: "Passenger ${assignment.passengerIndex + 1}",
                    seatNumber = assignment.seatNumber,
                    price = getSeatPrice(assignment.seatNumber)
                )
            },
            totalCharged = Money.sar(totalPrice),
            message = "Seats successfully assigned"
        )
    }

    private fun parsePassengerNames(passengersJson: String): List<String> {
        // Extract full names from JSON
        val regex = """"fullName"\s*:\s*"([^"]+)"""".toRegex()
        return regex.findAll(passengersJson).map { it.groupValues[1] }.toList()
            .ifEmpty { listOf("Passenger 1") }
    }

    private fun isValidFlight(flightNumber: String): Boolean {
        // Mock: Accept F3xxx flights (flyadeal pattern)
        return flightNumber.matches(Regex("F3\\d{3}"))
    }

    private fun generateA320SeatMap(assignedSeats: Set<String>): List<SeatRow> {
        val rows = mutableListOf<SeatRow>()
        val seatLetters = listOf("A", "B", "C", "D", "E", "F")
        val exitRows = setOf(1, 10, 11) // Rows near exits
        val extraLegroomRows = setOf(1, 12, 13)

        for (rowNum in 1..31) {
            val isExit = rowNum in exitRows
            val isExtraLegroom = rowNum in extraLegroomRows

            val seats = seatLetters.map { letter ->
                val seatNumber = "$rowNum$letter"
                val isAssigned = seatNumber in assignedSeats
                val isWindow = letter in listOf("A", "F")
                val isAisle = letter in listOf("C", "D")
                val isMiddle = letter in listOf("B", "E")

                // Randomly block some seats for realism (mock)
                val isBlocked = !isAssigned && (seatNumber.hashCode() % 17 == 0)

                val status = when {
                    isBlocked -> SeatStatus.BLOCKED
                    isAssigned -> SeatStatus.OCCUPIED
                    else -> SeatStatus.AVAILABLE
                }

                val type = when {
                    isExit -> SeatType.EXIT_ROW
                    isExtraLegroom -> SeatType.EXTRA_LEGROOM
                    rowNum <= 5 -> SeatType.PREFERRED
                    else -> SeatType.STANDARD
                }

                val price = getSeatPrice(seatNumber, type)

                val features = mutableListOf<String>()
                if (isWindow) features.add("Window")
                if (isAisle) features.add("Aisle")
                if (isMiddle) features.add("Middle")
                if (isExtraLegroom || isExit) features.add("Extra Legroom")

                Seat(
                    seatNumber = seatNumber,
                    status = status,
                    type = type,
                    price = price,
                    features = features
                )
            }

            rows.add(SeatRow(
                rowNumber = rowNum,
                seats = seats,
                isExitRow = isExit,
                hasExtraLegroom = isExtraLegroom
            ))
        }

        return rows
    }

    private fun getSeatPrice(seatNumber: String, type: SeatType? = null): Money {
        val seatType = type ?: determineSeatType(seatNumber)
        return when (seatType) {
            SeatType.EXIT_ROW -> Money.sar(75.0)
            SeatType.EXTRA_LEGROOM -> Money.sar(50.0)
            SeatType.PREFERRED -> Money.sar(25.0)
            SeatType.STANDARD -> Money.sar(0.0)
        }
    }

    private fun determineSeatType(seatNumber: String): SeatType {
        val rowNum = seatNumber.dropLast(1).toIntOrNull() ?: return SeatType.STANDARD
        return when {
            rowNum in setOf(1, 10, 11) -> SeatType.EXIT_ROW
            rowNum in setOf(1, 12, 13) -> SeatType.EXTRA_LEGROOM
            rowNum <= 5 -> SeatType.PREFERRED
            else -> SeatType.STANDARD
        }
    }
}

// Domain models
data class SeatMap(
    val flightNumber: String,
    val date: LocalDate,
    val aircraft: AircraftInfo,
    val rows: List<SeatRow>,
    val legend: List<SeatLegend>,
    val pricing: SeatPricing
)

data class AircraftInfo(
    val type: String,
    val registration: String,
    val totalSeats: Int,
    val configuration: String
)

data class SeatRow(
    val rowNumber: Int,
    val seats: List<Seat>,
    val isExitRow: Boolean,
    val hasExtraLegroom: Boolean = false
)

data class Seat(
    val seatNumber: String,
    val status: SeatStatus,
    val type: SeatType,
    val price: Money,
    val features: List<String>
)

enum class SeatStatus {
    AVAILABLE, OCCUPIED, BLOCKED
}

enum class SeatType {
    STANDARD, PREFERRED, EXTRA_LEGROOM, EXIT_ROW
}

data class SeatLegend(
    val type: String,
    val label: String,
    val color: String
)

data class SeatPricing(
    val standard: Money,
    val preferred: Money,
    val extraLegroom: Money,
    val exitRow: Money
)

data class SeatReservation(
    val id: String,
    val sessionId: String,
    val flightKey: String,
    val seats: List<String>,
    val expiresAt: kotlinx.datetime.Instant
)

data class SeatReservationResponse(
    val reservationId: String,
    val seats: List<String>,
    val totalPrice: Money,
    val expiresAt: kotlinx.datetime.Instant,
    val message: String
)

data class SeatAssignmentResponse(
    val pnr: String,
    val assignments: List<AssignedSeat>,
    val totalCharged: Money,
    val message: String
)

data class AssignedSeat(
    val passengerIndex: Int,
    val passengerName: String,
    val seatNumber: String,
    val price: Money
)
