package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared baggage DTOs used by both backend and frontend.
 * These represent the API response format for baggage endpoints.
 */

/**
 * Baggage options response DTO.
 */
@Serializable
data class BaggageOptionsResponseDto(
    val options: List<BaggageOptionDto>
)

/**
 * Baggage option DTO.
 */
@Serializable
data class BaggageOptionDto(
    val id: String,
    val name: String,
    val description: String,
    val weightKg: Int,
    val priceMinor: Long,
    val priceFormatted: String
)

/**
 * Baggage selection request DTO.
 */
@Serializable
data class BaggageSelectionRequestDto(
    val pnr: String,
    val passengerId: String,
    val flightNumber: String,
    val baggageId: String,
    val quantity: Int = 1
)

/**
 * Baggage selection response DTO.
 */
@Serializable
data class BaggageSelectionResponseDto(
    val success: Boolean,
    val message: String,
    val baggageId: String? = null,
    val baggageName: String? = null,
    val priceFormatted: String? = null
)
