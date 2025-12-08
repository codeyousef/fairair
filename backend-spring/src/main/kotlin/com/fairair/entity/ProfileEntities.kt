package com.fairair.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate

/**
 * Saved traveler entity for database persistence.
 * Users can save frequently used travelers to speed up booking.
 */
@Table("saved_travelers")
data class SavedTravelerEntity(
    @Id
    val id: String,
    
    @Column("user_id")
    val userId: String,
    
    @Column("first_name")
    val firstName: String,
    
    @Column("last_name")
    val lastName: String,
    
    @Column("date_of_birth")
    val dateOfBirth: LocalDate,
    
    @Column("nationality")
    val nationality: String,
    
    @Column("gender")
    val gender: String, // MALE, FEMALE
    
    @Column("email")
    val email: String? = null,
    
    @Column("phone")
    val phone: String? = null,
    
    @Column("relationship")
    val relationship: String? = null, // SELF, SPOUSE, CHILD, PARENT, etc.
    
    @Column("is_primary")
    val isMainTraveler: Boolean = false,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now(),
    
    @Column("updated_at")
    val updatedAt: Instant = Instant.now()
)

/**
 * Travel document entity (passport, national ID, etc.).
 */
@Table("travel_documents")
data class TravelDocumentEntity(
    @Id
    val id: String,
    
    @Column("traveler_id")
    val travelerId: String,
    
    @Column("document_type")
    val type: String, // PASSPORT, NATIONAL_ID, IQAMA
    
    @Column("document_number")
    val number: String,
    
    @Column("issuing_country")
    val issuingCountry: String,
    
    @Column("expiry_date")
    val expiryDate: LocalDate,
    
    @Column("is_primary")
    val isDefault: Boolean = false,
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
)

/**
 * Saved payment method entity.
 * Only stores masked card info - actual payment tokens are with payment provider.
 */
@Table("saved_payment_methods")
data class SavedPaymentMethodEntity(
    @Id
    val id: String,
    
    @Column("user_id")
    val userId: String,
    
    @Column("type")
    val type: String, // CREDIT_CARD, DEBIT_CARD, MADA, APPLE_PAY, STC_PAY
    
    @Column("nickname")
    val nickname: String? = null,
    
    @Column("last_four_digits")
    val lastFourDigits: String,
    
    @Column("card_brand")
    val cardBrand: String? = null, // VISA, MASTERCARD, AMEX, MADA
    
    @Column("expiry_month")
    val expiryMonth: Int? = null,
    
    @Column("expiry_year")
    val expiryYear: Int? = null,
    
    @Column("holder_name")
    val holderName: String,
    
    @Column("is_default")
    val isDefault: Boolean = false,
    
    @Column("payment_token")
    val paymentToken: String, // Token from payment provider for charging
    
    @Column("created_at")
    val createdAt: Instant = Instant.now()
)
