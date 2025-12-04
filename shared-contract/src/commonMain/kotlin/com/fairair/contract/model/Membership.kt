package com.fairair.contract.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Membership plan available for subscription.
 */
@Serializable
data class MembershipPlan(
    val id: String,
    val name: String,
    val description: String,
    val tripsPerYear: Int,
    val tripsPerMonth: Int,
    val monthlyPrice: Money,
    val benefits: List<MembershipBenefit>,
    val restrictions: MembershipRestrictions,
    val isRecommended: Boolean
)

/**
 * Benefits included in a membership plan.
 */
@Serializable
data class MembershipBenefit(
    val icon: String,
    val title: String,
    val description: String
)

/**
 * Restrictions and terms for membership.
 */
@Serializable
data class MembershipRestrictions(
    val domesticOnly: Boolean,
    val minimumBookingDays: Int,
    val blackoutDates: List<LocalDate>,
    val commitmentMonths: Int,
    val fareFamily: FareFamilyCode
)

/**
 * Request to subscribe to a membership plan.
 */
@Serializable
data class SubscribeRequest(
    val planId: String,
    val paymentMethod: PaymentMethodRequest,
    val billingAddress: BillingAddress
)

/**
 * Payment method for subscription.
 */
@Serializable
data class PaymentMethodRequest(
    val cardNumber: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val cvv: String,
    val cardholderName: String
)

/**
 * Billing address for subscription.
 */
@Serializable
data class BillingAddress(
    val line1: String,
    val line2: String?,
    val city: String,
    val state: String?,
    val postalCode: String,
    val country: String
)

/**
 * Active subscription details.
 */
@Serializable
data class Subscription(
    val id: String,
    val userId: String,
    val plan: MembershipPlan,
    val status: SubscriptionStatus,
    val startDate: LocalDate,
    val currentPeriodStart: LocalDate,
    val currentPeriodEnd: LocalDate,
    val nextBillingDate: LocalDate,
    val nextBillingAmount: Money,
    val paymentMethod: PaymentMethodSummary,
    val createdAt: Instant
)

/**
 * Subscription status.
 */
@Serializable
enum class SubscriptionStatus {
    /**
     * Subscription is active and in good standing.
     */
    ACTIVE,
    
    /**
     * Payment failed, subscription at risk.
     */
    PAST_DUE,
    
    /**
     * Subscription cancelled but still in paid period.
     */
    CANCELLING,
    
    /**
     * Subscription has ended.
     */
    CANCELLED,
    
    /**
     * Subscription paused by user.
     */
    PAUSED,
    
    /**
     * Trial period.
     */
    TRIAL
}

/**
 * Payment method summary (no sensitive data).
 */
@Serializable
data class PaymentMethodSummary(
    val type: String,
    val lastFour: String,
    val expiryMonth: Int,
    val expiryYear: Int,
    val brand: String
)

/**
 * Membership usage statistics for current billing period.
 */
@Serializable
data class MembershipUsage(
    val subscriptionId: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val tripsAllowed: Int,
    val tripsUsed: Int,
    val tripsRemaining: Int,
    val bookings: List<MembershipBooking>,
    val savingsThisPeriod: Money,
    val totalSavingsAllTime: Money
)

/**
 * Booking made using membership.
 */
@Serializable
data class MembershipBooking(
    val pnr: PnrCode,
    val flightNumber: String,
    val origin: AirportCode,
    val destination: AirportCode,
    val departureDate: LocalDate,
    val status: BookingStatus,
    val equivalentPrice: Money
)

/**
 * Request to book a flight using membership credits.
 */
@Serializable
data class MembershipBookingRequest(
    val flightNumber: String,
    val departureDate: LocalDate,
    val passengers: List<MembershipPassenger>
)

/**
 * Passenger for membership booking (simpler than regular).
 */
@Serializable
data class MembershipPassenger(
    val title: Title,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
)

/**
 * Subscription cancellation request.
 */
@Serializable
data class CancelSubscriptionRequest(
    val reason: SubscriptionCancelReason,
    val feedback: String?
)

/**
 * Reasons for subscription cancellation.
 */
@Serializable
enum class SubscriptionCancelReason {
    TOO_EXPENSIVE,
    NOT_TRAVELING_ENOUGH,
    POOR_SERVICE,
    FOUND_ALTERNATIVE,
    TEMPORARY_PAUSE,
    OTHER
}

/**
 * Cancellation confirmation for subscription.
 */
@Serializable
data class SubscriptionCancellation(
    val subscriptionId: String,
    val effectiveDate: LocalDate,
    val refundAmount: Money?,
    val remainingTrips: Int,
    val message: String
)
