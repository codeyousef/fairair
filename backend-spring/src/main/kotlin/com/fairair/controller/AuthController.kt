package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.dto.*
import com.fairair.security.JwtTokenProvider
import com.fairair.security.TokenValidationResult
import com.fairair.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for authentication endpoints.
 * Handles login, token refresh, and logout.
 * 
 * Demo users:
 * - employee@fairair.com / password (Employee)
 * - jane@test.com / password (User)
 * - admin@test.com / password (Admin)
 */
@RestController
@RequestMapping(ApiRoutes.Auth.BASE)
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userService: UserService
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * POST /api/v1/auth/login
     * 
     * Authenticates a user and returns access/refresh tokens.
     */
    @PostMapping("/login")
    suspend fun login(@RequestBody request: LoginRequestDto): ResponseEntity<Any> {
        log.info("Login attempt for email: ${request.email}")
        
        if (!isValidEmail(request.email)) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponseDto("INVALID_EMAIL", "Invalid email format"))
        }
        
        // Validate credentials against database
        val user = userService.validateCredentials(request.email, request.password)
        
        if (user == null) {
            log.warn("Failed login attempt for email: ${request.email}")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthErrorResponseDto("INVALID_CREDENTIALS", "Invalid email or password"))
        }
        
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)
        
        log.info("Login successful for user: ${user.id} (${user.email}) - Role: ${user.role}")
        
        return ResponseEntity.ok(LoginResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = "Bearer",
            expiresIn = 900, // 15 minutes in seconds
            user = UserInfo(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                role = user.role.name
            )
        ))
    }

    /**
     * POST /api/v1/auth/refresh
     * 
     * Refreshes an access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    fun refreshToken(@RequestBody request: RefreshTokenRequestDto): ResponseEntity<Any> {
        log.debug("Token refresh requested")
        
        return when (val result = jwtTokenProvider.validateToken(request.refreshToken)) {
            is TokenValidationResult.Valid -> {
                if (result.tokenType != "refresh") {
                    return ResponseEntity.badRequest()
                        .body(AuthErrorResponseDto("INVALID_TOKEN_TYPE", "Not a refresh token"))
                }
                
                val newAccessToken = jwtTokenProvider.generateAccessToken(
                    result.userId, 
                    result.email ?: ""
                )
                val newRefreshToken = jwtTokenProvider.generateRefreshToken(result.userId)
                
                log.debug("Token refreshed for user: ${result.userId}")
                
                ResponseEntity.ok(LoginResponse(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    tokenType = "Bearer",
                    expiresIn = 900
                ))
            }
            is TokenValidationResult.Expired -> {
                log.debug("Refresh token expired")
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponseDto("TOKEN_EXPIRED", "Refresh token has expired"))
            }
            is TokenValidationResult.Invalid -> {
                log.warn("Invalid refresh token")
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponseDto("INVALID_TOKEN", "Invalid refresh token"))
            }
        }
    }

    /**
     * POST /api/v1/auth/logout
     * 
     * Logs out the current user.
     * In a production system with token revocation, this would invalidate the tokens.
     */
    @PostMapping("/logout")
    fun logout(): ResponseEntity<Any> {
        log.debug("Logout requested")
        // In production: add token to blacklist or revoke in database
        return ResponseEntity.ok(LogoutResponseDto(success = true, message = "Logged out successfully"))
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}

/**
 * JVM-specific login response DTO for proper Jackson serialization.
 * All fields are required (no defaults) to ensure they're always serialized.
 */
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserInfo? = null
)

/**
 * JVM-specific user info DTO for proper Jackson serialization.
 */
data class UserInfo(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)
