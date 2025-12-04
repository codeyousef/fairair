package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.service.MealsService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for meal pre-order endpoints.
 * Handles meal options display and meal ordering.
 */
@RestController
@RequestMapping(ApiRoutes.Meals.BASE)
class MealsController(
    private val mealsService: MealsService
) {
    private val log = LoggerFactory.getLogger(MealsController::class.java)

    /**
     * GET /api/v1/meals
     *
     * Returns all available meals, optionally filtered.
     */
    @GetMapping
    suspend fun getAvailableMeals(
        @RequestParam(required = false) dietary: String?, // vegetarian, vegan, halal, gluten-free
        @RequestParam(required = false) category: String? // hot-meal, cold-meal, snack, beverage
    ): ResponseEntity<Any> {
        log.info("GET /meals?dietary=$dietary&category=$category")

        return try {
            val meals = mealsService.getAllMeals(dietary, category)
            ResponseEntity.ok(mapOf("meals" to meals))
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid request: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MealsErrorResponse("VALIDATION_ERROR", e.message ?: "Invalid request"))
        }
    }

    /**
     * POST /api/v1/meals/add
     *
     * Adds a meal to an existing booking.
     */
    @PostMapping("/add")
    suspend fun addMeal(
        @RequestBody request: AddMealRequest
    ): ResponseEntity<Any> {
        log.info("POST /meals/add: ${request.mealId} for PNR ${request.pnr}")

        return try {
            val confirmation = mealsService.addMealToBooking(
                pnr = request.pnr,
                passengerIndex = request.passengerIndex,
                segmentIndex = request.segmentIndex,
                mealId = request.mealId
            )
            ResponseEntity.ok(confirmation)
        } catch (e: IllegalArgumentException) {
            log.warn("Add meal failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MealsErrorResponse("VALIDATION_ERROR", e.message ?: "Invalid request"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: ${request.pnr}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MealsErrorResponse("BOOKING_NOT_FOUND", "Booking ${request.pnr} not found"))
        } catch (e: MealNotFoundException) {
            log.warn("Meal not found: ${request.mealId}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MealsErrorResponse("MEAL_NOT_FOUND", "Meal ${request.mealId} not found"))
        }
    }

    /**
     * POST /api/v1/meals/remove
     *
     * Removes a meal from a booking.
     */
    @PostMapping("/remove")
    suspend fun removeMeal(
        @RequestBody request: RemoveMealRequest
    ): ResponseEntity<Any> {
        log.info("POST /meals/remove for PNR ${request.pnr}, passenger ${request.passengerIndex}")

        return try {
            mealsService.removeMealFromBooking(request.pnr, request.passengerIndex, request.segmentIndex)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Meal removed successfully"))
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: ${request.pnr}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MealsErrorResponse("BOOKING_NOT_FOUND", "Booking ${request.pnr} not found"))
        }
    }

    /**
     * GET /api/v1/meals/booking/{pnr}
     *
     * Gets meals ordered for a booking.
     */
    @GetMapping("/booking/{pnr}")
    suspend fun getBookingMeals(
        @PathVariable pnr: String,
        @RequestParam lastName: String
    ): ResponseEntity<Any> {
        log.info("GET /meals/booking/$pnr?lastName=$lastName")

        return try {
            val meals = mealsService.getBookingMeals(pnr, lastName)
            ResponseEntity.ok(meals)
        } catch (e: BookingNotFoundException) {
            log.warn("Booking not found: $pnr")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MealsErrorResponse("BOOKING_NOT_FOUND", "Booking $pnr not found"))
        }
    }
}

// Request DTOs
data class AddMealRequest(
    val pnr: String,
    val passengerIndex: Int,
    val segmentIndex: Int = 0,
    val mealId: String
)

data class RemoveMealRequest(
    val pnr: String,
    val passengerIndex: Int,
    val segmentIndex: Int = 0
)

// Response DTOs
data class MealsErrorResponse(
    val code: String,
    val message: String
)

// Exceptions
class MealNotFoundException(message: String) : RuntimeException(message)
