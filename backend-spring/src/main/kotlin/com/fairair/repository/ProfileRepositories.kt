package com.fairair.repository

import com.fairair.entity.SavedPaymentMethodEntity
import com.fairair.entity.SavedTravelerEntity
import com.fairair.entity.TravelDocumentEntity
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

/**
 * Repository for saved travelers.
 */
@Repository
interface SavedTravelerRepository : CoroutineCrudRepository<SavedTravelerEntity, String> {
    /**
     * Find all travelers for a user.
     */
    fun findByUserId(userId: String): Flow<SavedTravelerEntity>
    
    /**
     * Find a specific traveler by ID and user ID.
     */
    suspend fun findByIdAndUserId(id: String, userId: String): SavedTravelerEntity?
    
    /**
     * Delete all travelers for a user.
     */
    suspend fun deleteByUserId(userId: String)
    
    /**
     * Check if a traveler exists for a user.
     */
    suspend fun existsByIdAndUserId(id: String, userId: String): Boolean
}

/**
 * Repository for travel documents.
 */
@Repository
interface TravelDocumentRepository : CoroutineCrudRepository<TravelDocumentEntity, String> {
    /**
     * Find all documents for a traveler.
     */
    fun findByTravelerId(travelerId: String): Flow<TravelDocumentEntity>
    
    /**
     * Find a specific document.
     */
    suspend fun findByIdAndTravelerId(id: String, travelerId: String): TravelDocumentEntity?
    
    /**
     * Delete all documents for a traveler.
     */
    suspend fun deleteByTravelerId(travelerId: String)
}

/**
 * Repository for saved payment methods.
 */
@Repository
interface SavedPaymentMethodRepository : CoroutineCrudRepository<SavedPaymentMethodEntity, String> {
    /**
     * Find all payment methods for a user.
     */
    fun findByUserId(userId: String): Flow<SavedPaymentMethodEntity>
    
    /**
     * Find a specific payment method by ID and user ID.
     */
    suspend fun findByIdAndUserId(id: String, userId: String): SavedPaymentMethodEntity?
    
    /**
     * Delete all payment methods for a user.
     */
    suspend fun deleteByUserId(userId: String)
    
    /**
     * Check if a payment method exists for a user.
     */
    suspend fun existsByIdAndUserId(id: String, userId: String): Boolean
}
