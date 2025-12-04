package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.model.*
import com.fairair.service.MembershipService
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for membership subscription endpoints.
 * Handles plan listing, subscription, usage tracking, and cancellation.
 */
@RestController
@RequestMapping(ApiRoutes.Membership.BASE)
class MembershipController(
    private val membershipService: MembershipService
) {
    private val log = LoggerFactory.getLogger(MembershipController::class.java)

    /**
     * GET /api/v1/membership/plans
     *
     * Returns all available membership plans.
     */
    @GetMapping("/plans")
    suspend fun getPlans(): ResponseEntity<List<MembershipPlan>> {
        log.info("GET /membership/plans")
        val plans = membershipService.getAvailablePlans()
        return ResponseEntity.ok(plans)
    }

    /**
     * POST /api/v1/membership/subscribe
     *
     * Subscribes the authenticated user to a membership plan.
     */
    @PostMapping("/subscribe")
    suspend fun subscribe(
        @RequestBody request: SubscribeRequestDto
    ): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        log.info("POST /membership/subscribe: userId=$userId, planId=${request.planId}")

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MembershipErrorResponse("UNAUTHORIZED", "Authentication required"))
        }

        return try {
            val subscription = membershipService.subscribe(
                userId = userId,
                planId = request.planId,
                paymentMethod = request.paymentMethod.toModel(),
                billingAddress = request.billingAddress.toModel()
            )
            ResponseEntity.status(HttpStatus.CREATED).body(subscription)
        } catch (e: IllegalArgumentException) {
            log.warn("Subscription failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MembershipErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        } catch (e: AlreadySubscribedException) {
            log.warn("User already subscribed: $userId")
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MembershipErrorResponse("ALREADY_SUBSCRIBED", "You already have an active subscription"))
        } catch (e: PaymentFailedException) {
            log.warn("Payment failed: ${e.message}")
            ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(MembershipErrorResponse("PAYMENT_FAILED", e.message ?: "Payment processing failed"))
        }
    }

    /**
     * GET /api/v1/membership/status
     *
     * Returns the current user's subscription status.
     */
    @GetMapping("/status")
    suspend fun getStatus(): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        log.info("GET /membership/status: userId=$userId")

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MembershipErrorResponse("UNAUTHORIZED", "Authentication required"))
        }

        val subscription = membershipService.getSubscription(userId)
        return if (subscription != null) {
            ResponseEntity.ok(subscription)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MembershipErrorResponse("NOT_SUBSCRIBED", "No active subscription found"))
        }
    }

    /**
     * GET /api/v1/membership/usage
     *
     * Returns usage statistics for the current billing period.
     */
    @GetMapping("/usage")
    suspend fun getUsage(): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        log.info("GET /membership/usage: userId=$userId")

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MembershipErrorResponse("UNAUTHORIZED", "Authentication required"))
        }

        return try {
            val usage = membershipService.getUsage(userId)
            ResponseEntity.ok(usage)
        } catch (e: NotSubscribedException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MembershipErrorResponse("NOT_SUBSCRIBED", "No active subscription found"))
        }
    }

    /**
     * POST /api/v1/membership/cancel
     *
     * Cancels the current subscription.
     */
    @PostMapping("/cancel")
    suspend fun cancel(
        @RequestBody request: CancelSubscriptionRequestDto
    ): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        log.info("POST /membership/cancel: userId=$userId, reason=${request.reason}")

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MembershipErrorResponse("UNAUTHORIZED", "Authentication required"))
        }

        return try {
            val cancellation = membershipService.cancelSubscription(
                userId = userId,
                reason = SubscriptionCancelReason.valueOf(request.reason),
                feedback = request.feedback
            )
            ResponseEntity.ok(cancellation)
        } catch (e: NotSubscribedException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MembershipErrorResponse("NOT_SUBSCRIBED", "No active subscription found"))
        } catch (e: AlreadyCancelledException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(MembershipErrorResponse("ALREADY_CANCELLED", "Subscription is already cancelled"))
        }
    }

    /**
     * POST /api/v1/membership/book
     *
     * Books a flight using membership credits.
     */
    @PostMapping("/book")
    suspend fun bookWithMembership(
        @RequestBody request: MembershipBookingRequestDto
    ): ResponseEntity<Any> {
        val userId = getCurrentUserId()
        log.info("POST /membership/book: userId=$userId, flight=${request.flightNumber}")

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MembershipErrorResponse("UNAUTHORIZED", "Authentication required"))
        }

        return try {
            val confirmation = membershipService.bookWithMembership(
                userId = userId,
                flightNumber = request.flightNumber,
                departureDate = LocalDate.parse(request.departureDate),
                passengers = request.passengers.map { it.toModel() }
            )
            ResponseEntity.status(HttpStatus.CREATED).body(confirmation)
        } catch (e: NotSubscribedException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(MembershipErrorResponse("NOT_SUBSCRIBED", "No active subscription found"))
        } catch (e: NoCreditsAvailableException) {
            ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(MembershipErrorResponse("NO_CREDITS", "No trips remaining in current period"))
        } catch (e: IllegalArgumentException) {
            log.warn("Membership booking failed: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MembershipErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        }
    }

    private suspend fun getCurrentUserId(): String? {
        return try {
            val authentication = ReactiveSecurityContextHolder.getContext()
                .map { it.authentication }
                .awaitSingleOrNull()
            authentication?.principal as? String
        } catch (e: Exception) {
            log.debug("Could not get current user: ${e.message}")
            null
        }
    }
}

/**
 * Request DTO for subscription.
 */
data class SubscribeRequestDto(
    val planId: String,
    val paymentMethod: PaymentMethodRequestDto,
    val billingAddress: BillingAddressDto
)

/**
 * DTO for payment method.
 */
data class PaymentMethodRequestDto(
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val cardholderName: String
) {
    fun toModel(): PaymentMethodRequest = PaymentMethodRequest(
        cardNumber = cardNumber,
        expiryMonth = expiryMonth,
        expiryYear = expiryYear,
        cvv = cvv,
        cardholderName = cardholderName
    )
}

/**
 * DTO for billing address.
 */
data class BillingAddressDto(
    val line1: String,
    val line2: String?,
    val city: String,
    val state: String?,
    val postalCode: String,
    val country: String
) {
    fun toModel(): BillingAddress = BillingAddress(
        line1 = line1,
        line2 = line2,
        city = city,
        state = state,
        postalCode = postalCode,
        country = country
    )
}

/**
 * Request DTO for subscription cancellation.
 */
data class CancelSubscriptionRequestDto(
    val reason: String,
    val feedback: String?
)

/**
 * Request DTO for membership booking.
 */
data class MembershipBookingRequestDto(
    val flightNumber: String,
    val departureDate: String,
    val passengers: List<MembershipPassengerDto>
)

/**
 * DTO for membership passenger.
 */
data class MembershipPassengerDto(
    val title: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String
) {
    fun toModel(): MembershipPassenger = MembershipPassenger(
        title = Title.valueOf(title),
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.parse(dateOfBirth)
    )
}

/**
 * Error response for membership endpoints.
 */
data class MembershipErrorResponse(
    val code: String,
    val message: String
)

/**
 * Exception for already subscribed.
 */
class AlreadySubscribedException : RuntimeException("User already has an active subscription")

/**
 * Exception for payment failure.
 */
class PaymentFailedException(override val message: String) : RuntimeException(message)

/**
 * Exception for not subscribed.
 */
class NotSubscribedException : RuntimeException("User does not have an active subscription")

/**
 * Exception for no credits available.
 */
class NoCreditsAvailableException : RuntimeException("No trips remaining in current period")

/**
 * Exception for already cancelled.
 */
class AlreadyCancelledException : RuntimeException("Subscription is already cancelled")
