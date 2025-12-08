package com.fairair.service

import com.fairair.contract.dto.*
import com.fairair.entity.SavedPaymentMethodEntity
import com.fairair.entity.SavedTravelerEntity
import com.fairair.entity.TravelDocumentEntity
import com.fairair.repository.SavedPaymentMethodRepository
import com.fairair.repository.SavedTravelerRepository
import com.fairair.repository.TravelDocumentRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

/**
 * Service for managing user profile data: saved travelers and payment methods.
 */
@Service
class ProfileService(
    private val travelerRepository: SavedTravelerRepository,
    private val documentRepository: TravelDocumentRepository,
    private val paymentMethodRepository: SavedPaymentMethodRepository
) {
    private val log = LoggerFactory.getLogger(ProfileService::class.java)

    // ============ Travelers ============

    /**
     * Get all saved travelers for a user with their documents.
     */
    suspend fun getTravelers(userId: String): List<SavedTravelerDto> {
        return travelerRepository.findByUserId(userId)
            .map { entity -> entityToDto(entity) }
            .toList()
    }

    /**
     * Get a specific traveler with documents.
     */
    suspend fun getTraveler(userId: String, travelerId: String): SavedTravelerDto? {
        val entity = travelerRepository.findByIdAndUserId(travelerId, userId) ?: return null
        return entityToDto(entity)
    }

    /**
     * Add a new saved traveler.
     */
    suspend fun addTraveler(userId: String, request: SaveTravelerRequest): SavedTravelerDto {
        val entity = SavedTravelerEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            firstName = request.firstName,
            lastName = request.lastName,
            dateOfBirth = LocalDate.parse(request.dateOfBirth),
            nationality = request.nationality.uppercase(),
            gender = request.gender.name,
            email = request.email,
            phone = request.phone,
            isMainTraveler = request.isMainTraveler
        )
        
        val saved = travelerRepository.save(entity)
        log.info("Added traveler {} for user {}", saved.id, userId)
        return entityToDto(saved)
    }

    /**
     * Update an existing traveler.
     */
    suspend fun updateTraveler(
        userId: String, 
        travelerId: String, 
        request: SaveTravelerRequest
    ): SavedTravelerDto? {
        val existing = travelerRepository.findByIdAndUserId(travelerId, userId) ?: return null
        
        val updated = existing.copy(
            firstName = request.firstName,
            lastName = request.lastName,
            dateOfBirth = LocalDate.parse(request.dateOfBirth),
            nationality = request.nationality.uppercase(),
            gender = request.gender.name,
            email = request.email,
            phone = request.phone,
            isMainTraveler = request.isMainTraveler
        )
        
        val saved = travelerRepository.save(updated)
        log.info("Updated traveler {} for user {}", travelerId, userId)
        return entityToDto(saved)
    }

    /**
     * Delete a traveler and all associated documents.
     */
    suspend fun deleteTraveler(userId: String, travelerId: String): Boolean {
        if (!travelerRepository.existsByIdAndUserId(travelerId, userId)) {
            return false
        }
        
        // Delete associated documents first
        documentRepository.deleteByTravelerId(travelerId)
        travelerRepository.deleteById(travelerId)
        
        log.info("Deleted traveler {} for user {}", travelerId, userId)
        return true
    }

    // ============ Documents ============

    /**
     * Get all documents for a traveler.
     */
    suspend fun getDocuments(userId: String, travelerId: String): List<TravelDocumentDto>? {
        // Verify traveler belongs to user
        if (!travelerRepository.existsByIdAndUserId(travelerId, userId)) {
            return null
        }
        
        return documentRepository.findByTravelerId(travelerId)
            .map { documentEntityToDto(it) }
            .toList()
    }

    /**
     * Add a document to a traveler.
     */
    suspend fun addDocument(
        userId: String, 
        travelerId: String, 
        request: AddDocumentRequest
    ): TravelDocumentDto? {
        // Verify traveler belongs to user
        if (!travelerRepository.existsByIdAndUserId(travelerId, userId)) {
            return null
        }
        
        val entity = TravelDocumentEntity(
            id = UUID.randomUUID().toString(),
            travelerId = travelerId,
            type = request.type.name,
            number = request.number,
            issuingCountry = request.issuingCountry.uppercase(),
            expiryDate = LocalDate.parse(request.expiryDate),
            isDefault = request.isDefault
        )
        
        val saved = documentRepository.save(entity)
        log.info("Added document {} to traveler {}", saved.id, travelerId)
        return documentEntityToDto(saved)
    }

    /**
     * Delete a document from a traveler.
     */
    suspend fun deleteDocument(userId: String, travelerId: String, documentId: String): Boolean {
        // Verify traveler belongs to user
        if (!travelerRepository.existsByIdAndUserId(travelerId, userId)) {
            return false
        }
        
        val document = documentRepository.findByIdAndTravelerId(documentId, travelerId) ?: return false
        documentRepository.delete(document)
        
        log.info("Deleted document {} from traveler {}", documentId, travelerId)
        return true
    }

    // ============ Payment Methods ============

    /**
     * Get all saved payment methods for a user.
     */
    suspend fun getPaymentMethods(userId: String): List<SavedPaymentMethodDto> {
        return paymentMethodRepository.findByUserId(userId)
            .map { paymentEntityToDto(it) }
            .toList()
    }

    /**
     * Add a new payment method.
     * In production, cardToken would be validated with payment provider.
     */
    suspend fun addPaymentMethod(
        userId: String, 
        request: SavePaymentMethodRequest
    ): SavedPaymentMethodDto {
        // In production, we would:
        // 1. Validate the token with the payment provider
        // 2. Get card details (last 4, brand, expiry) from the provider
        // For demo, we'll extract mock data from the token
        
        val (lastFour, brand, holderName, expiryMonth, expiryYear) = parseCardToken(request.cardToken)
        
        val entity = SavedPaymentMethodEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = request.type.name,
            nickname = request.nickname,
            lastFourDigits = lastFour,
            cardBrand = brand,
            expiryMonth = expiryMonth,
            expiryYear = expiryYear,
            holderName = holderName,
            isDefault = request.isDefault,
            paymentToken = request.cardToken
        )
        
        val saved = paymentMethodRepository.save(entity)
        log.info("Added payment method {} for user {}", saved.id, userId)
        return paymentEntityToDto(saved)
    }

    /**
     * Delete a payment method.
     */
    suspend fun deletePaymentMethod(userId: String, paymentMethodId: String): Boolean {
        if (!paymentMethodRepository.existsByIdAndUserId(paymentMethodId, userId)) {
            return false
        }
        
        paymentMethodRepository.deleteById(paymentMethodId)
        log.info("Deleted payment method {} for user {}", paymentMethodId, userId)
        return true
    }

    /**
     * Set a payment method as default.
     */
    suspend fun setDefaultPaymentMethod(userId: String, paymentMethodId: String): SavedPaymentMethodDto? {
        val paymentMethod = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId) ?: return null
        
        // Unset all other defaults
        paymentMethodRepository.findByUserId(userId)
            .collect { pm ->
                if (pm.isDefault && pm.id != paymentMethodId) {
                    paymentMethodRepository.save(pm.copy(isDefault = false))
                }
            }
        
        // Set this one as default
        val updated = paymentMethodRepository.save(paymentMethod.copy(isDefault = true))
        log.info("Set payment method {} as default for user {}", paymentMethodId, userId)
        return paymentEntityToDto(updated)
    }

    // ============ Helpers ============

    private suspend fun entityToDto(entity: SavedTravelerEntity): SavedTravelerDto {
        val documents = documentRepository.findByTravelerId(entity.id)
            .map { documentEntityToDto(it) }
            .toList()
        
        return SavedTravelerDto(
            id = entity.id,
            firstName = entity.firstName,
            lastName = entity.lastName,
            dateOfBirth = entity.dateOfBirth.toString(),
            nationality = entity.nationality,
            gender = Gender.valueOf(entity.gender),
            email = entity.email,
            phone = entity.phone,
            isMainTraveler = entity.isMainTraveler,
            documents = documents
        )
    }

    private fun documentEntityToDto(entity: TravelDocumentEntity): TravelDocumentDto {
        return TravelDocumentDto(
            id = entity.id,
            type = DocumentType.valueOf(entity.type),
            number = entity.number,
            issuingCountry = entity.issuingCountry,
            expiryDate = entity.expiryDate.toString(),
            isDefault = entity.isDefault
        )
    }

    private fun paymentEntityToDto(entity: SavedPaymentMethodEntity): SavedPaymentMethodDto {
        return SavedPaymentMethodDto(
            id = entity.id,
            type = PaymentType.valueOf(entity.type),
            nickname = entity.nickname,
            lastFourDigits = entity.lastFourDigits,
            cardBrand = entity.cardBrand?.let { CardBrand.valueOf(it) },
            expiryMonth = entity.expiryMonth,
            expiryYear = entity.expiryYear,
            holderName = entity.holderName,
            isDefault = entity.isDefault
        )
    }

    /**
     * Parse mock card token for demo purposes.
     * In production, this would call the payment provider's API.
     * Token format: "tok_{last4}_{brand}_{holderName}_{MM}_{YYYY}"
     */
    private fun parseCardToken(token: String): CardTokenData {
        // Default mock data
        return try {
            val parts = token.removePrefix("tok_").split("_")
            CardTokenData(
                lastFour = parts.getOrElse(0) { "4242" },
                brand = parts.getOrElse(1) { "VISA" },
                holderName = parts.getOrElse(2) { "Card Holder" }.replace("-", " "),
                expiryMonth = parts.getOrElse(3) { "12" }.toIntOrNull() ?: 12,
                expiryYear = parts.getOrElse(4) { "2028" }.toIntOrNull() ?: 2028
            )
        } catch (e: Exception) {
            CardTokenData("4242", "VISA", "Card Holder", 12, 2028)
        }
    }

    private data class CardTokenData(
        val lastFour: String,
        val brand: String,
        val holderName: String,
        val expiryMonth: Int,
        val expiryYear: Int
    )
}
