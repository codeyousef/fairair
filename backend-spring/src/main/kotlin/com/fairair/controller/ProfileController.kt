package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.dto.*
import com.fairair.service.ProfileService
import com.fairair.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

/**
 * Controller for user profile management: saved travelers and payment methods.
 */
@RestController
@RequestMapping(ApiRoutes.Profile.BASE)
class ProfileController(
    private val profileService: ProfileService,
    private val userService: UserService
) {
    private val log = LoggerFactory.getLogger(ProfileController::class.java)

    // ============ Profile ============

    /**
     * Get the current user's complete profile.
     */
    @GetMapping
    suspend fun getProfile(principal: Principal): ResponseEntity<UserProfileDto> {
        val userId = principal.name
        val user = userService.getUserById(userId) 
            ?: return ResponseEntity.notFound().build()
        
        val travelers = profileService.getTravelers(userId)
        val paymentMethods = profileService.getPaymentMethods(userId)
        
        val profile = UserProfileDto(
            user = UserInfoDto(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                role = user.role.name
            ),
            savedTravelers = travelers,
            savedPaymentMethods = paymentMethods
        )
        
        return ResponseEntity.ok(profile)
    }

    // ============ Travelers ============

    /**
     * Get all saved travelers.
     */
    @GetMapping("/travelers")
    suspend fun getTravelers(principal: Principal): ResponseEntity<List<SavedTravelerDto>> {
        val travelers = profileService.getTravelers(principal.name)
        return ResponseEntity.ok(travelers)
    }

    /**
     * Get a specific traveler.
     */
    @GetMapping("/travelers/{travelerId}")
    suspend fun getTraveler(
        principal: Principal,
        @PathVariable travelerId: String
    ): ResponseEntity<SavedTravelerDto> {
        val traveler = profileService.getTraveler(principal.name, travelerId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(traveler)
    }

    /**
     * Add a new saved traveler.
     */
    @PostMapping("/travelers")
    suspend fun addTraveler(
        principal: Principal,
        @RequestBody request: SaveTravelerRequest
    ): ResponseEntity<SavedTravelerDto> {
        val traveler = profileService.addTraveler(principal.name, request)
        log.info("User {} added traveler {}", principal.name, traveler.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(traveler)
    }

    /**
     * Update an existing traveler.
     */
    @PutMapping("/travelers/{travelerId}")
    suspend fun updateTraveler(
        principal: Principal,
        @PathVariable travelerId: String,
        @RequestBody request: SaveTravelerRequest
    ): ResponseEntity<SavedTravelerDto> {
        val traveler = profileService.updateTraveler(principal.name, travelerId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(traveler)
    }

    /**
     * Delete a traveler.
     */
    @DeleteMapping("/travelers/{travelerId}")
    suspend fun deleteTraveler(
        principal: Principal,
        @PathVariable travelerId: String
    ): ResponseEntity<Void> {
        val deleted = profileService.deleteTraveler(principal.name, travelerId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ============ Documents ============

    /**
     * Get all documents for a traveler.
     */
    @GetMapping("/travelers/{travelerId}/documents")
    suspend fun getDocuments(
        principal: Principal,
        @PathVariable travelerId: String
    ): ResponseEntity<List<TravelDocumentDto>> {
        val documents = profileService.getDocuments(principal.name, travelerId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(documents)
    }

    /**
     * Add a document to a traveler.
     */
    @PostMapping("/travelers/{travelerId}/documents")
    suspend fun addDocument(
        principal: Principal,
        @PathVariable travelerId: String,
        @RequestBody request: AddDocumentRequest
    ): ResponseEntity<TravelDocumentDto> {
        val document = profileService.addDocument(principal.name, travelerId, request)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.status(HttpStatus.CREATED).body(document)
    }

    /**
     * Delete a document.
     */
    @DeleteMapping("/travelers/{travelerId}/documents/{documentId}")
    suspend fun deleteDocument(
        principal: Principal,
        @PathVariable travelerId: String,
        @PathVariable documentId: String
    ): ResponseEntity<Void> {
        val deleted = profileService.deleteDocument(principal.name, travelerId, documentId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ============ Payment Methods ============

    /**
     * Get all saved payment methods.
     */
    @GetMapping("/payment-methods")
    suspend fun getPaymentMethods(principal: Principal): ResponseEntity<List<SavedPaymentMethodDto>> {
        val methods = profileService.getPaymentMethods(principal.name)
        return ResponseEntity.ok(methods)
    }

    /**
     * Add a new payment method.
     */
    @PostMapping("/payment-methods")
    suspend fun addPaymentMethod(
        principal: Principal,
        @RequestBody request: SavePaymentMethodRequest
    ): ResponseEntity<SavedPaymentMethodDto> {
        val method = profileService.addPaymentMethod(principal.name, request)
        log.info("User {} added payment method {}", principal.name, method.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(method)
    }

    /**
     * Delete a payment method.
     */
    @DeleteMapping("/payment-methods/{paymentMethodId}")
    suspend fun deletePaymentMethod(
        principal: Principal,
        @PathVariable paymentMethodId: String
    ): ResponseEntity<Void> {
        val deleted = profileService.deletePaymentMethod(principal.name, paymentMethodId)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Set a payment method as default.
     */
    @PutMapping("/payment-methods/{paymentMethodId}/default")
    suspend fun setDefaultPaymentMethod(
        principal: Principal,
        @PathVariable paymentMethodId: String
    ): ResponseEntity<SavedPaymentMethodDto> {
        val method = profileService.setDefaultPaymentMethod(principal.name, paymentMethodId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(method)
    }
}
