package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared seat map DTOs used by both backend and frontend.
 * These represent the API response format for seat map endpoints.
 */

/**
 * Seat map response DTO.
 */
@Serializable
data class SeatMapDto(
    val flightNumber: String,
    val aircraft: String,
    val rows: List<SeatRowDto>,
    val legend: List<SeatLegendDto>
)

/**
 * Seat row DTO.
 */
@Serializable
data class SeatRowDto(
    val rowNumber: Int,
    val seats: List<SeatDto>,
    val isExitRow: Boolean = false
)

/**
 * Seat DTO.
 */
@Serializable
data class SeatDto(
    val seatNumber: String,
    val status: String,
    val type: String,
    val priceMinor: Long = 0,
    val priceFormatted: String = "Free",
    val features: List<String> = emptyList()
)

/**
 * Seat legend DTO.
 */
@Serializable
data class SeatLegendDto(
    val type: String,
    val label: String,
    val color: String
)

/**
 * Seat selection request DTO.
 */
@Serializable
data class SeatSelectionRequestDto(
    val pnr: String,
    val passengerId: String,
    val seatNumber: String
)

/**
 * Seat selection response DTO.
 */
@Serializable
data class SeatSelectionResponseDto(
    val success: Boolean,
    val message: String,
    val seatNumber: String? = null,
    val priceFormatted: String? = null
)
