package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.model.*
import com.fairair.service.ManageBookingService
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for manage booking endpoints.
 * Handles booking retrieval, modification, and cancellation.
 */
@RestController
@RequestMapping(ApiRoutes.ManageBooking.BASE)
class ManageBookingController(
    private val manageBookingService: ManageBookingService
) {
    private val log = LoggerFactory.getLogger(ManageBookingController::class.java)

    /**
     * POST /api/v1/manage
     *
     * Retrieves booking details for management using PNR and last name.
     */
    @PostMapping
    suspend fun retrieveBooking(
        @RequestBody request: RetrieveBookingRequestDto
    ): ResponseEntity<Any> {
        log.info("POST /manage: pnr=${request.pnr}")

        return try {
            val booking = manageBookingService.retrieveBooking(
                pnr = request.pnr,
                lastName = request.lastName
            )
            ResponseEntity.ok(booking)
        } catch (e: IllegalArgumentException) {
            log.warn("Validation failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ManageBookingErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: ${request.pnr}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ManageBookingErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR ${request.pnr} and the provided last name"))
        }
    }

    /**
     * PUT /api/v1/manage/{pnr}/passengers
     *
     * Updates passenger details for a booking.
     */
    @PutMapping("/{pnr}/passengers")
    suspend fun updatePassengers(
        @PathVariable pnr: String,
        @RequestBody request: UpdatePassengersRequestDto
    ): ResponseEntity<Any> {
        log.info("PUT /manage/$pnr/passengers: updates=${request.updates.size}")

        return try {
            val booking = manageBookingService.updatePassengers(
                pnr = pnr,
                updates = request.updates.map { it.toModel() }
            )
            ResponseEntity.ok(booking)
        } catch (e: IllegalArgumentException) {
            log.warn("Passenger update failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ManageBookingErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: $pnr")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ManageBookingErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR $pnr"))
        } catch (e: ModificationNotAllowedException) {
            log.warn("Modification not allowed: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ManageBookingErrorResponse("MODIFICATION_NOT_ALLOWED", e.message ?: "Modification not allowed"))
        }
    }

    /**
     * POST /api/v1/manage/{pnr}/change
     *
     * Gets a quote for changing the flight date/time.
     */
    @PostMapping("/{pnr}/change")
    suspend fun getChangeQuote(
        @PathVariable pnr: String,
        @RequestBody request: ChangeFlightRequestDto
    ): ResponseEntity<Any> {
        log.info("POST /manage/$pnr/change: newDate=${request.newDepartureDate}")

        return try {
            val quote = manageBookingService.getFlightChangeQuote(
                pnr = pnr,
                newDate = LocalDate.parse(request.newDepartureDate),
                preferredFlightNumber = request.preferredFlightNumber
            )
            ResponseEntity.ok(quote)
        } catch (e: IllegalArgumentException) {
            log.warn("Change quote failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ManageBookingErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: $pnr")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ManageBookingErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR $pnr"))
        } catch (e: ModificationNotAllowedException) {
            log.warn("Change not allowed: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ManageBookingErrorResponse("CHANGE_NOT_ALLOWED", e.message ?: "Flight change not allowed for this fare"))
        }
    }

    /**
     * POST /api/v1/manage/{pnr}/cancel
     *
     * Cancels a booking.
     */
    @PostMapping("/{pnr}/cancel")
    suspend fun cancelBooking(
        @PathVariable pnr: String,
        @RequestBody request: CancelBookingRequestDto
    ): ResponseEntity<Any> {
        log.info("POST /manage/$pnr/cancel: reason=${request.reason}")

        return try {
            val confirmation = manageBookingService.cancelBooking(
                pnr = pnr,
                reason = CancellationReason.valueOf(request.reason),
                comments = request.comments
            )
            ResponseEntity.ok(confirmation)
        } catch (e: IllegalArgumentException) {
            log.warn("Cancellation failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ManageBookingErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: $pnr")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ManageBookingErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR $pnr"))
        } catch (e: CancellationNotAllowedException) {
            log.warn("Cancellation not allowed: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ManageBookingErrorResponse("CANCELLATION_NOT_ALLOWED", e.message ?: "Cancellation not allowed for this booking"))
        }
    }

    /**
     * POST /api/v1/manage/{pnr}/ancillaries
     *
     * Adds ancillaries to an existing booking.
     */
    @PostMapping("/{pnr}/ancillaries")
    suspend fun addAncillaries(
        @PathVariable pnr: String,
        @RequestBody request: AddAncillariesRequestDto
    ): ResponseEntity<Any> {
        log.info("POST /manage/$pnr/ancillaries: items=${request.ancillaries.size}")

        return try {
            val booking = manageBookingService.addAncillaries(
                pnr = pnr,
                ancillaries = request.ancillaries.map { it.toModel() }
            )
            ResponseEntity.ok(booking)
        } catch (e: IllegalArgumentException) {
            log.warn("Add ancillaries failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ManageBookingErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: $pnr")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ManageBookingErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR $pnr"))
        }
    }
}

/**
 * Request DTO for booking retrieval.
 */
data class RetrieveBookingRequestDto(
    val pnr: String,
    val lastName: String
)

/**
 * Request DTO for updating passengers.
 */
data class UpdatePassengersRequestDto(
    val updates: List<PassengerUpdateDto>
)

/**
 * DTO for passenger update.
 */
data class PassengerUpdateDto(
    val passengerIndex: Int,
    val firstName: String?,
    val lastName: String?,
    val documentId: String?,
    val nationality: String?
) {
    fun toModel(): PassengerUpdate = PassengerUpdate(
        passengerIndex = passengerIndex,
        firstName = firstName,
        lastName = lastName,
        documentId = documentId,
        nationality = nationality
    )
}

/**
 * Request DTO for flight change.
 */
data class ChangeFlightRequestDto(
    val newDepartureDate: String,
    val preferredFlightNumber: String?
)

/**
 * Request DTO for booking cancellation.
 */
data class CancelBookingRequestDto(
    val reason: String,
    val comments: String?
)

/**
 * Request DTO for adding ancillaries.
 */
data class AddAncillariesRequestDto(
    val ancillaries: List<AncillaryRequestDto>
)

/**
 * DTO for ancillary request.
 */
data class AncillaryRequestDto(
    val type: String,
    val passengerIndex: Int,
    val quantity: Int
) {
    fun toModel(): AncillaryRequest = AncillaryRequest(
        type = AncillaryType.valueOf(type),
        passengerIndex = passengerIndex,
        quantity = quantity
    )
}

/**
 * Error response for manage booking endpoints.
 */
data class ManageBookingErrorResponse(
    val code: String,
    val message: String
)

/**
 * Exception for modification not allowed.
 */
class ModificationNotAllowedException(override val message: String) : RuntimeException(message)

/**
 * Exception for cancellation not allowed.
 */
class CancellationNotAllowedException(override val message: String) : RuntimeException(message)
