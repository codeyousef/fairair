package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared configuration DTOs used by both backend and frontend.
 * These represent the API response format for configuration endpoints.
 */

/**
 * Route map response DTO.
 * Maps origin airport codes to lists of valid destination codes.
 */
@Serializable
data class RouteMapDto(
    val routes: Map<String, List<String>>
)

/**
 * Station (airport) DTO.
 */
@Serializable
data class StationDto(
    val code: String,
    val name: String,
    val city: String,
    val country: String
)
