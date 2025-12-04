package com.fairair.service

import com.fairair.contract.model.*
import com.fairair.controller.*
import com.fairair.repository.MembershipRepository
import kotlinx.datetime.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service for membership subscription operations.
 * Handles plan management, subscriptions, usage tracking, and booking with credits.
 * 
 * Production Notes:
 * - Integrate with payment gateway (Stripe, etc.) for recurring billing
 * - Store subscription data in PostgreSQL
 * - Implement proper proration for plan changes
 * - Add webhook handlers for payment events
 */
@Service
class MembershipService(
    private val membershipRepository: MembershipRepository,
    private val bookingService: BookingService
) {
    private val log = LoggerFactory.getLogger(MembershipService::class.java)

    companion object {
        private val PLANS = listOf(
            MembershipPlan(
                id = "basic-12",
                name = "FairAir Basic",
                description = "12 round trips per year - perfect for occasional travelers",
                tripsPerYear = 12,
                tripsPerMonth = 1,
                monthlyPrice = Money.sar(299.0),
                benefits = listOf(
                    MembershipBenefit("✈️", "Monthly Flight", "One round trip flight per month"),
                    MembershipBenefit("🏷️", "Domestic Routes", "Valid on all domestic destinations"),
                    MembershipBenefit("📅", "3-Day Booking", "Book up to 3 days before departure"),
                    MembershipBenefit("🎒", "Cabin Bag", "7kg under-seat bag included")
                ),
                restrictions = MembershipRestrictions(
                    domesticOnly = true,
                    minimumBookingDays = 3,
                    blackoutDates = emptyList(),
                    commitmentMonths = 12,
                    fareFamily = FareFamilyCode.FLY
                ),
                isRecommended = false
            ),
            MembershipPlan(
                id = "standard-24",
                name = "FairAir Standard",
                description = "24 round trips per year - ideal for regular travelers",
                tripsPerYear = 24,
                tripsPerMonth = 2,
                monthlyPrice = Money.sar(549.0),
                benefits = listOf(
                    MembershipBenefit("✈️", "2 Monthly Flights", "Two round trip flights per month"),
                    MembershipBenefit("🏷️", "Domestic Routes", "Valid on all domestic destinations"),
                    MembershipBenefit("📅", "3-Day Booking", "Book up to 3 days before departure"),
                    MembershipBenefit("🎒", "Cabin Bag", "7kg under-seat bag included"),
                    MembershipBenefit("💼", "Checked Bag", "20kg checked bag included")
                ),
                restrictions = MembershipRestrictions(
                    domesticOnly = true,
                    minimumBookingDays = 3,
                    blackoutDates = emptyList(),
                    commitmentMonths = 12,
                    fareFamily = FareFamilyCode.FLY_PLUS
                ),
                isRecommended = true
            ),
            MembershipPlan(
                id = "premium-36",
                name = "FairAir Premium",
                description = "36 round trips per year - for frequent flyers",
                tripsPerYear = 36,
                tripsPerMonth = 3,
                monthlyPrice = Money.sar(799.0),
                benefits = listOf(
                    MembershipBenefit("✈️", "3 Monthly Flights", "Three round trip flights per month"),
                    MembershipBenefit("🏷️", "Domestic Routes", "Valid on all domestic destinations"),
                    MembershipBenefit("📅", "Same-Day Booking", "Book same day if seats available"),
                    MembershipBenefit("🎒", "Cabin Bag", "7kg under-seat bag included"),
                    MembershipBenefit("💼", "Checked Bag", "30kg checked bag included"),
                    MembershipBenefit("🪑", "Seat Selection", "Free seat selection included")
                ),
                restrictions = MembershipRestrictions(
                    domesticOnly = true,
                    minimumBookingDays = 0,
                    blackoutDates = emptyList(),
                    commitmentMonths = 12,
                    fareFamily = FareFamilyCode.FLY_MAX
                ),
                isRecommended = false
            )
        )
    }

    /**
     * Returns all available membership plans.
     */
    suspend fun getAvailablePlans(): List<MembershipPlan> {
        log.info("Fetching available membership plans")
        return PLANS
    }

    /**
     * Subscribes a user to a membership plan.
     */
    suspend fun subscribe(
        userId: String,
        planId: String,
        paymentMethod: PaymentMethodRequest,
        billingAddress: BillingAddress
    ): Subscription {
        log.info("Subscribing user=$userId to plan=$planId")

        // Check for existing subscription
        val existing = membershipRepository.findByUserId(userId)
        if (existing != null && existing.status == SubscriptionStatus.ACTIVE) {
            throw AlreadySubscribedException()
        }

        // Validate plan
        val plan = PLANS.find { it.id == planId }
            ?: throw IllegalArgumentException("Invalid plan ID: $planId")

        // Validate payment method (mock)
        if (paymentMethod.cardNumber.length < 15) {
            throw PaymentFailedException("Invalid card number")
        }

        // In production: 
        // 1. Create customer in Stripe
        // 2. Add payment method
        // 3. Create subscription
        // 4. Handle initial charge

        val now = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date
        val periodEnd = LocalDate(now.year, now.monthNumber, 1)
            .plus(1, DateTimeUnit.MONTH)
            .minus(1, DateTimeUnit.DAY)

        val subscription = Subscription(
            id = UUID.randomUUID().toString(),
            userId = userId,
            plan = plan,
            status = SubscriptionStatus.ACTIVE,
            startDate = now,
            currentPeriodStart = now,
            currentPeriodEnd = periodEnd,
            nextBillingDate = periodEnd.plus(1, DateTimeUnit.DAY),
            nextBillingAmount = plan.monthlyPrice,
            paymentMethod = PaymentMethodSummary(
                type = "card",
                lastFour = paymentMethod.cardNumber.takeLast(4),
                expiryMonth = paymentMethod.expiryMonth,
                expiryYear = paymentMethod.expiryYear,
                brand = detectCardBrand(paymentMethod.cardNumber)
            ),
            createdAt = Clock.System.now()
        )

        membershipRepository.save(subscription)
        log.info("Subscription created: ${subscription.id}")

        return subscription
    }

    /**
     * Gets the current subscription for a user.
     */
    suspend fun getSubscription(userId: String): Subscription? {
        log.info("Getting subscription for user=$userId")
        return membershipRepository.findByUserId(userId)
    }

    /**
     * Gets usage statistics for the current billing period.
     */
    suspend fun getUsage(userId: String): MembershipUsage {
        log.info("Getting usage for user=$userId")

        val subscription = membershipRepository.findByUserId(userId)
            ?: throw NotSubscribedException()

        val bookings = membershipRepository.getBookingsForPeriod(
            userId = userId,
            periodStart = subscription.currentPeriodStart,
            periodEnd = subscription.currentPeriodEnd
        )

        val tripsUsed = bookings.size
        val tripsRemaining = subscription.plan.tripsPerMonth - tripsUsed

        // Calculate savings (difference between regular price and membership)
        val totalEquivalentValue = bookings.sumOf { it.equivalentPrice.amountAsDouble }
        val monthlyPaid = subscription.plan.monthlyPrice.amountAsDouble
        val savingsThisPeriod = maxOf(0.0, totalEquivalentValue - monthlyPaid)

        return MembershipUsage(
            subscriptionId = subscription.id,
            periodStart = subscription.currentPeriodStart,
            periodEnd = subscription.currentPeriodEnd,
            tripsAllowed = subscription.plan.tripsPerMonth,
            tripsUsed = tripsUsed,
            tripsRemaining = tripsRemaining,
            bookings = bookings,
            savingsThisPeriod = Money.sar(savingsThisPeriod),
            totalSavingsAllTime = Money.sar(savingsThisPeriod * 3) // Mock historical
        )
    }

    /**
     * Cancels a subscription.
     */
    suspend fun cancelSubscription(
        userId: String,
        reason: SubscriptionCancelReason,
        feedback: String?
    ): SubscriptionCancellation {
        log.info("Cancelling subscription for user=$userId, reason=$reason")

        val subscription = membershipRepository.findByUserId(userId)
            ?: throw NotSubscribedException()

        if (subscription.status == SubscriptionStatus.CANCELLED) {
            throw AlreadyCancelledException()
        }

        // In production:
        // 1. Cancel in Stripe (at period end)
        // 2. Update database status to CANCELLING
        // 3. Send confirmation email

        val usage = getUsage(userId)

        membershipRepository.updateStatus(subscription.id, SubscriptionStatus.CANCELLING)

        return SubscriptionCancellation(
            subscriptionId = subscription.id,
            effectiveDate = subscription.currentPeriodEnd,
            refundAmount = null, // No pro-rated refund
            remainingTrips = usage.tripsRemaining,
            message = "Your subscription will end on ${subscription.currentPeriodEnd}. " +
                "You can still use your remaining ${usage.tripsRemaining} trips until then."
        )
    }

    /**
     * Books a flight using membership credits.
     */
    suspend fun bookWithMembership(
        userId: String,
        flightNumber: String,
        departureDate: LocalDate,
        passengers: List<MembershipPassenger>
    ): BookingConfirmation {
        log.info("Booking with membership: user=$userId, flight=$flightNumber")

        val subscription = membershipRepository.findByUserId(userId)
            ?: throw NotSubscribedException()

        if (subscription.status != SubscriptionStatus.ACTIVE) {
            throw NotSubscribedException()
        }

        // Check available credits
        val usage = getUsage(userId)
        if (usage.tripsRemaining <= 0) {
            throw NoCreditsAvailableException()
        }

        // Check booking restrictions
        val plan = subscription.plan
        val daysUntilDeparture = departureDate.toEpochDays() - 
            Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date.toEpochDays()
        
        if (daysUntilDeparture < plan.restrictions.minimumBookingDays) {
            throw IllegalArgumentException(
                "Booking must be at least ${plan.restrictions.minimumBookingDays} days before departure"
            )
        }

        // In production:
        // 1. Search for the specific flight
        // 2. Validate availability for fare family
        // 3. Create booking in Navitaire
        // 4. Record membership usage

        // Mock booking creation
        val confirmation = BookingConfirmation(
            pnr = PnrCode.generate(),
            bookingReference = UUID.randomUUID().toString(),
            flight = FlightSummary(
                flightNumber = flightNumber,
                origin = AirportCode("JED"),
                destination = AirportCode("RUH"),
                departureTime = departureDate.atStartOfDayIn(TimeZone.of("Asia/Riyadh"))
                    .plus(10, DateTimeUnit.HOUR),
                fareFamily = plan.restrictions.fareFamily
            ),
            passengers = passengers.map { 
                PassengerSummary(
                    fullName = "${it.firstName} ${it.lastName}",
                    type = PassengerType.ADULT
                )
            },
            totalPaid = Money.sar(0.0), // Membership booking - no additional charge
            createdAt = Clock.System.now()
        )

        // Record the membership booking
        membershipRepository.recordBooking(
            userId = userId,
            subscriptionId = subscription.id,
            pnr = confirmation.pnr,
            flightNumber = flightNumber,
            origin = AirportCode("JED"),
            destination = AirportCode("RUH"),
            departureDate = departureDate,
            equivalentPrice = Money.sar(350.0) // Mock equivalent value
        )

        log.info("Membership booking created: ${confirmation.pnr.value}")
        return confirmation
    }

    private fun detectCardBrand(cardNumber: String): String {
        val digits = cardNumber.filter { it.isDigit() }
        return when {
            digits.startsWith("4") -> "Visa"
            digits.startsWith("5") -> "Mastercard"
            digits.startsWith("34") || digits.startsWith("37") -> "Amex"
            else -> "Card"
        }
    }
}
