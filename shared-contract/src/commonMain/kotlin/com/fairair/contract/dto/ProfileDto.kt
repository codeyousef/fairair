package com.fairair.contract.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================================
// Saved Travelers - Frequently used passengers
// ============================================================================

/**
 * A saved traveler in the user's profile.
 * Used to quickly fill passenger information during booking.
 */
@Serializable
data class SavedTravelerDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String, // ISO-8601 date: "1990-05-15"
    val nationality: String, // ISO 3166-1 alpha-2: "SA", "US", etc.
    val gender: Gender,
    val email: String? = null,
    val phone: String? = null,
    val isMainTraveler: Boolean = false, // The account holder themselves
    val documents: List<TravelDocumentDto> = emptyList()
)

/**
 * Gender enum for traveler profiles.
 */
@Serializable
enum class Gender {
    @SerialName("MALE")
    MALE,
    @SerialName("FEMALE")
    FEMALE
}

/**
 * A travel document (passport, national ID) associated with a traveler.
 */
@Serializable
data class TravelDocumentDto(
    val id: String,
    val type: DocumentType,
    val number: String,
    val issuingCountry: String, // ISO 3166-1 alpha-2
    val expiryDate: String, // ISO-8601 date
    val isDefault: Boolean = false
)

/**
 * Type of travel document.
 */
@Serializable
enum class DocumentType {
    @SerialName("PASSPORT")
    PASSPORT,
    @SerialName("NATIONAL_ID")
    NATIONAL_ID,
    @SerialName("IQAMA")
    IQAMA // Saudi residence permit
}

// ============================================================================
// Saved Payment Methods
// ============================================================================

/**
 * A saved payment method in the user's profile.
 * Card numbers are masked for security (only last 4 digits stored).
 */
@Serializable
data class SavedPaymentMethodDto(
    val id: String,
    val type: PaymentType,
    val nickname: String? = null, // User-friendly name like "My Visa"
    val lastFourDigits: String, // "4242"
    val cardBrand: CardBrand? = null,
    val expiryMonth: Int? = null, // 1-12
    val expiryYear: Int? = null, // 2025
    val holderName: String,
    val isDefault: Boolean = false
)

/**
 * Type of payment method.
 */
@Serializable
enum class PaymentType {
    @SerialName("CREDIT_CARD")
    CREDIT_CARD,
    @SerialName("DEBIT_CARD")
    DEBIT_CARD,
    @SerialName("MADA")
    MADA, // Saudi debit network
    @SerialName("APPLE_PAY")
    APPLE_PAY,
    @SerialName("STC_PAY")
    STC_PAY // Saudi mobile payment
}

/**
 * Credit/debit card brand.
 */
@Serializable
enum class CardBrand {
    @SerialName("VISA")
    VISA,
    @SerialName("MASTERCARD")
    MASTERCARD,
    @SerialName("AMEX")
    AMEX,
    @SerialName("MADA")
    MADA
}

// ============================================================================
// User Profile - Complete profile with travelers and payment methods
// ============================================================================

/**
 * Complete user profile including saved travelers and payment methods.
 */
@Serializable
data class UserProfileDto(
    val user: UserInfoDto,
    val savedTravelers: List<SavedTravelerDto> = emptyList(),
    val savedPaymentMethods: List<SavedPaymentMethodDto> = emptyList()
)

// ============================================================================
// API Requests
// ============================================================================

/**
 * Request to create or update a saved traveler.
 */
@Serializable
data class SaveTravelerRequest(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val nationality: String,
    val gender: Gender,
    val email: String? = null,
    val phone: String? = null,
    val isMainTraveler: Boolean = false
)

/**
 * Request to add a travel document to a traveler.
 */
@Serializable
data class AddDocumentRequest(
    val type: DocumentType,
    val number: String,
    val issuingCountry: String,
    val expiryDate: String,
    val isDefault: Boolean = false
)

/**
 * Request to save a payment method.
 * In production, this would use a payment tokenization service.
 */
@Serializable
data class SavePaymentMethodRequest(
    val type: PaymentType,
    val nickname: String? = null,
    val cardToken: String, // Tokenized card from payment provider
    val isDefault: Boolean = false
)
