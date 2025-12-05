package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared meal DTOs used by both backend and frontend.
 * These represent the API response format for meal endpoints.
 */

/**
 * Meal options response DTO.
 */
@Serializable
data class MealOptionsResponseDto(
    val meals: List<MealOptionDto>
)

/**
 * Meal option DTO.
 */
@Serializable
data class MealOptionDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val priceMinor: Long,
    val priceFormatted: String,
    val dietaryInfo: List<String> = emptyList(),
    val imageUrl: String? = null
)

/**
 * Meal selection request DTO.
 */
@Serializable
data class MealSelectionRequestDto(
    val pnr: String,
    val passengerId: String,
    val flightNumber: String,
    val mealId: String
)

/**
 * Meal selection response DTO.
 */
@Serializable
data class MealSelectionResponseDto(
    val success: Boolean,
    val message: String,
    val mealId: String? = null,
    val mealName: String? = null,
    val priceFormatted: String? = null
)
