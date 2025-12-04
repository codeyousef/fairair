package com.fairair.contract.dto

import kotlinx.serialization.Serializable

/**
 * Shared authentication DTOs used by both backend and frontend.
 */

/**
 * Login request DTO.
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

/**
 * Login response DTO.
 */
@Serializable
data class LoginResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 900,
    val user: UserInfoDto? = null
) {
    /** Convenience property to get the access token */
    val token: String get() = accessToken
}

/**
 * User info DTO returned in login response.
 */
@Serializable
data class UserInfoDto(
    val id: String = "",
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)

/**
 * Refresh token request DTO.
 */
@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String
)

/**
 * Logout response DTO.
 */
@Serializable
data class LogoutResponseDto(
    val success: Boolean,
    val message: String
)

/**
 * Auth error response DTO.
 */
@Serializable
data class AuthErrorResponseDto(
    val code: String,
    val message: String
)
