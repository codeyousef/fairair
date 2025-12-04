package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.service.SeatsService
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for seat selection endpoints.
 * Handles seat map display, seat reservation, and assignment.
 */
@RestController
@RequestMapping(ApiRoutes.Seats.BASE)
class SeatsController(
    private val seatsService: SeatsService
) {
    private val log = LoggerFactory.getLogger(SeatsController::class.java)

    /**
     * GET /api/v1/seats/{flightNumber}
     *
     * Returns the seat map for a specific flight.
     */
    @GetMapping("/{flightNumber}")
    suspend fun getSeatMap(
        @PathVariable flightNumber: String,
        @RequestParam date: String
    ): ResponseEntity<Any> {
        log.info("GET /seats/$flightNumber?date=$date")

        return try {
            val seatMap = seatsService.getSeatMap(flightNumber, LocalDate.parse(date))
            ResponseEntity.ok(seatMap)
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid request: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SeatsErrorResponse("VALIDATION_ERROR", e.message ?: "Invalid request"))
        } catch (e: SeatFlightNotFoundException) {
            log.warn("Flight not found: $flightNumber")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(SeatsErrorResponse("FLIGHT_NOT_FOUND", "Flight $flightNumber not found"))
        }
    }

    /**
     * POST /api/v1/seats/reserve
     *
     * Temporarily reserves seats during the booking flow.
     */
    @PostMapping("/reserve")
    suspend fun reserveSeats(
        @RequestBody request: SeatReservationRequest
    ): ResponseEntity<Any> {
        log.info("POST /seats/reserve: ${request.seats.size} seats for flight ${request.flightNumber}")

        return try {
            val reservation = seatsService.reserveSeats(
                flightNumber = request.flightNumber,
                date = LocalDate.parse(request.date),
                seats = request.seats,
                sessionId = request.sessionId
            )
            ResponseEntity.status(HttpStatus.CREATED).body(reservation)
        } catch (e: IllegalArgumentException) {
            log.warn("Reservation failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SeatsErrorResponse("VALIDATION_ERROR", e.message ?: "Invalid request"))
        } catch (e: SeatUnavailableException) {
            log.warn("Seats unavailable: ${e.message}")
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(SeatsErrorResponse("SEAT_UNAVAILABLE", e.message ?: "One or more seats are not available"))
        }
    }

    /**
     * POST /api/v1/seats/assign
     *
     * Assigns seats to an existing booking.
     */
    @PostMapping("/assign")
    suspend fun assignSeats(
        @RequestBody request: SeatAssignmentRequest
    ): ResponseEntity<Any> {
        log.info("POST /seats/assign: ${request.assignments.size} assignments for PNR ${request.pnr}")

        return try {
            val assignment = seatsService.assignSeats(
                pnr = request.pnr,
                assignments = request.assignments
            )
            ResponseEntity.ok(assignment)
        } catch (e: IllegalArgumentException) {
            log.warn("Assignment failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SeatsErrorResponse("VALIDATION_ERROR", e.message ?: "Invalid request"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: ${request.pnr}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(SeatsErrorResponse("BOOKING_NOT_FOUND", "Booking ${request.pnr} not found"))
        } catch (e: SeatUnavailableException) {
            log.warn("Seats unavailable: ${e.message}")
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(SeatsErrorResponse("SEAT_UNAVAILABLE", e.message ?: "One or more seats are not available"))
        }
    }
}

// Request DTOs
data class SeatReservationRequest(
    val flightNumber: String,
    val date: String,
    val seats: List<String>,
    val sessionId: String
)

data class SeatAssignmentRequest(
    val pnr: String,
    val assignments: List<PassengerSeatAssignment>
)

data class PassengerSeatAssignment(
    val passengerIndex: Int,
    val seatNumber: String,
    val segmentIndex: Int = 0
)

// Response DTOs
data class SeatsErrorResponse(
    val code: String,
    val message: String
)

// Exceptions
class SeatFlightNotFoundException(message: String) : RuntimeException(message)
class SeatUnavailableException(message: String) : RuntimeException(message)
