package com.fairair.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

/**
 * Booking entity for database persistence.
 * Stores all booking confirmations.
 * 
 * Implements Persistable to allow manual ID assignment with proper INSERT behavior.
 * Uses a secondary constructor annotated with @PersistenceCreator for database reads.
 */
@Table("bookings")
class BookingEntity private constructor(
    @Id
    @Column("pnr")
    val pnr: String,
    
    @Column("booking_reference")
    val bookingReference: String,
    
    @Column("user_id")
    val userId: String?,
    
    @Column("flight_number")
    val flightNumber: String,
    
    @Column("origin")
    val origin: String,
    
    @Column("destination")
    val destination: String,
    
    @Column("departure_time")
    val departureTime: Instant,
    
    @Column("fare_family")
    val fareFamily: String,
    
    @Column("passengers_json")
    val passengersJson: String,
    
    @Column("total_amount")
    val totalAmount: Double,
    
    @Column("currency")
    val currency: String,
    
    @Column("created_at")
    val createdAt: Instant,
    
    @Transient
    private val _isNew: Boolean
) : Persistable<String> {
    
    override fun getId(): String = pnr
    
    override fun isNew(): Boolean = _isNew
    
    companion object {
        /**
         * Creates a new entity for insertion into the database.
         */
        fun create(
            pnr: String,
            bookingReference: String,
            userId: String? = null,
            flightNumber: String,
            origin: String,
            destination: String,
            departureTime: Instant,
            fareFamily: String,
            passengersJson: String,
            totalAmount: Double,
            currency: String,
            createdAt: Instant = Instant.now()
        ): BookingEntity = BookingEntity(
            pnr = pnr,
            bookingReference = bookingReference,
            userId = userId,
            flightNumber = flightNumber,
            origin = origin,
            destination = destination,
            departureTime = departureTime,
            fareFamily = fareFamily,
            passengersJson = passengersJson,
            totalAmount = totalAmount,
            currency = currency,
            createdAt = createdAt,
            _isNew = true
        )
        
        /**
         * Used by Spring Data R2DBC to reconstruct entities from the database.
         * Marks the entity as not new so saves become updates.
         */
        @PersistenceCreator
        @JvmStatic
        fun fromDatabase(
            pnr: String,
            bookingReference: String,
            userId: String?,
            flightNumber: String,
            origin: String,
            destination: String,
            departureTime: Instant,
            fareFamily: String,
            passengersJson: String,
            totalAmount: Double,
            currency: String,
            createdAt: Instant
        ): BookingEntity = BookingEntity(
            pnr = pnr,
            bookingReference = bookingReference,
            userId = userId,
            flightNumber = flightNumber,
            origin = origin,
            destination = destination,
            departureTime = departureTime,
            fareFamily = fareFamily,
            passengersJson = passengersJson,
            totalAmount = totalAmount,
            currency = currency,
            createdAt = createdAt,
            _isNew = false
        )
    }
}
