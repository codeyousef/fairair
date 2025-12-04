package com.fairair.repository

import com.fairair.contract.model.*
import kotlinx.datetime.LocalDate
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for membership data.
 * 
 * Production Notes:
 * - Replace with Spring Data R2DBC repositories
 * - Add SubscriptionEntity, MembershipBookingEntity
 * - Consider event sourcing for subscription state changes
 */
@Repository
class MembershipRepository {
    
    // In-memory storage for mock implementation
    private val subscriptions = ConcurrentHashMap<String, Subscription>()
    private val membershipBookings = ConcurrentHashMap<String, MutableList<MembershipBooking>>()
    
    /**
     * Find subscription by user ID.
     */
    fun findByUserId(userId: String): Subscription? {
        return subscriptions.values.find { it.userId == userId }
    }
    
    /**
     * Save a subscription.
     */
    fun save(subscription: Subscription) {
        subscriptions[subscription.id] = subscription
    }
    
    /**
     * Update subscription status.
     */
    fun updateStatus(subscriptionId: String, status: SubscriptionStatus) {
        val existing = subscriptions[subscriptionId] ?: return
        subscriptions[subscriptionId] = existing.copy(status = status)
    }
    
    /**
     * Get bookings for a billing period.
     */
    fun getBookingsForPeriod(
        userId: String,
        periodStart: LocalDate,
        periodEnd: LocalDate
    ): List<MembershipBooking> {
        return membershipBookings[userId]?.filter { booking ->
            booking.departureDate >= periodStart && booking.departureDate <= periodEnd
        } ?: emptyList()
    }
    
    /**
     * Record a membership booking.
     */
    fun recordBooking(
        userId: String,
        subscriptionId: String,
        pnr: PnrCode,
        flightNumber: String,
        origin: AirportCode,
        destination: AirportCode,
        departureDate: LocalDate,
        equivalentPrice: Money
    ) {
        val booking = MembershipBooking(
            pnr = pnr,
            flightNumber = flightNumber,
            origin = origin,
            destination = destination,
            departureDate = departureDate,
            status = BookingStatus.CONFIRMED,
            equivalentPrice = equivalentPrice
        )
        
        membershipBookings.computeIfAbsent(userId) { mutableListOf() }.add(booking)
    }
}
