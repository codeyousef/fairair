package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.model.*
import com.fairair.service.CheckInService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for check-in endpoints.
 * Handles online check-in flow and boarding pass generation.
 */
@RestController
@RequestMapping(ApiRoutes.CheckIn.BASE)
class CheckInController(
    private val checkInService: CheckInService
) {
    private val log = LoggerFactory.getLogger(CheckInController::class.java)

    /**
     * POST /api/v1/checkin
     *
     * Initiates check-in by verifying PNR and last name.
     * Returns check-in eligibility and passenger status.
     */
    @PostMapping
    suspend fun initiateCheckIn(
        @RequestBody request: CheckInRequestDto
    ): ResponseEntity<Any> {
        log.info("POST /checkin: pnr=${request.pnr}")

        return try {
            val eligibility = checkInService.getCheckInEligibility(
                pnr = request.pnr,
                lastName = request.lastName
            )
            ResponseEntity.ok(eligibility)
        } catch (e: IllegalArgumentException) {
            log.warn("Check-in validation failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CheckInErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: ${request.pnr}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CheckInErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR ${request.pnr} and the provided last name"))
        }
    }

    /**
     * POST /api/v1/checkin/{pnr}/complete
     *
     * Completes check-in for selected passengers and generates boarding passes.
     */
    @PostMapping("/{pnr}/complete")
    suspend fun completeCheckIn(
        @PathVariable pnr: String,
        @RequestBody request: CompleteCheckInRequestDto
    ): ResponseEntity<Any> {
        log.info("POST /checkin/$pnr/complete: passengers=${request.passengerIndices}")

        return try {
            val completion = checkInService.completeCheckIn(
                pnr = pnr,
                passengerIndices = request.passengerIndices,
                seatPreferences = request.seatPreferences?.map { it.toModel() }
            )
            ResponseEntity.ok(completion)
        } catch (e: IllegalArgumentException) {
            log.warn("Check-in completion failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CheckInErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: $pnr")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CheckInErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR $pnr"))
        } catch (e: CheckInNotAllowedException) {
            log.warn("Check-in not allowed: ${e.message}")
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CheckInErrorResponse("CHECK_IN_NOT_ALLOWED", e.message ?: "Check-in not allowed"))
        }
    }

    /**
     * GET /api/v1/checkin/{pnr}/boarding-pass/{passengerIndex}
     *
     * Retrieves boarding pass for a checked-in passenger.
     */
    @GetMapping("/{pnr}/boarding-pass/{passengerIndex}")
    suspend fun getBoardingPass(
        @PathVariable pnr: String,
        @PathVariable passengerIndex: Int
    ): ResponseEntity<Any> {
        log.info("GET /checkin/$pnr/boarding-pass/$passengerIndex")

        return try {
            val boardingPass = checkInService.getBoardingPass(pnr, passengerIndex)
            if (boardingPass != null) {
                ResponseEntity.ok(boardingPass)
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CheckInErrorResponse("NOT_FOUND", "Boarding pass not found"))
            }
        } catch (e: BookingNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CheckInErrorResponse("BOOKING_NOT_FOUND", "No booking found with PNR $pnr"))
        }
    }
}

/**
 * Request DTO for check-in initiation.
 */
data class CheckInRequestDto(
    val pnr: String,
    val lastName: String
)

/**
 * Request DTO for completing check-in.
 */
data class CompleteCheckInRequestDto(
    val passengerIndices: List<Int>,
    val seatPreferences: List<SeatPreferenceDto>?
)

/**
 * DTO for seat preference.
 */
data class SeatPreferenceDto(
    val passengerIndex: Int,
    val preferredSeat: String?,
    val preferenceType: String
) {
    fun toModel(): SeatPreference = SeatPreference(
        passengerIndex = passengerIndex,
        preferredSeat = preferredSeat,
        preferenceType = SeatPreferenceType.valueOf(preferenceType)
    )
}

/**
 * Error response for check-in endpoints.
 */
data class CheckInErrorResponse(
    val code: String,
    val message: String
)

/**
 * Exception for booking not found.
 */
class BookingNotFoundException(val pnr: String) : RuntimeException("Booking not found: $pnr")

/**
 * Exception for check-in not allowed.
 */
class CheckInNotAllowedException(override val message: String) : RuntimeException(message)
