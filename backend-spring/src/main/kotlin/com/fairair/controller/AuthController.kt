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
 * Handles login, registration, password reset, token refresh, and logout.
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
     * POST /api/v1/auth/register
     * 
     * Registers a new user account.
     */
    @PostMapping("/register")
    suspend fun register(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        log.info("Registration attempt for email: ${request.email}")

        // Validate email format
        if (!isValidEmail(request.email)) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponseDto("INVALID_EMAIL", "Invalid email format"))
        }

        // Validate password strength
        if (request.password.length < 8) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponseDto("WEAK_PASSWORD", "Password must be at least 8 characters"))
        }

        // Validate required fields
        if (request.firstName.isBlank() || request.lastName.isBlank()) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponseDto("MISSING_FIELDS", "First name and last name are required"))
        }

        return try {
            val user = userService.registerUser(
                email = request.email,
                password = request.password,
                firstName = request.firstName,
                lastName = request.lastName,
                phone = request.phone
            )

            val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
            val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

            log.info("Registration successful for user: ${user.id} (${user.email})")

            ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse(
                success = true,
                message = "Account created successfully",
                accessToken = accessToken,
                refreshToken = refreshToken,
                user = UserInfo(
                    id = user.id,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    role = user.role.name
                )
            ))
        } catch (e: EmailAlreadyExistsException) {
            log.warn("Registration failed - email already exists: ${request.email}")
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(AuthErrorResponseDto("EMAIL_EXISTS", "An account with this email already exists"))
        }
    }

    /**
     * POST /api/v1/auth/forgot-password
     * 
     * Initiates password reset process by sending a reset email.
     */
    @PostMapping("/forgot-password")
    suspend fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<Any> {
        log.info("Password reset requested for email: ${request.email}")

        if (!isValidEmail(request.email)) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponseDto("INVALID_EMAIL", "Invalid email format"))
        }

        // Always return success to prevent email enumeration attacks
        userService.initiatePasswordReset(request.email)

        return ResponseEntity.ok(ForgotPasswordResponse(
            success = true,
            message = "If an account exists with this email, you will receive a password reset link shortly."
        ))
    }

    /**
     * POST /api/v1/auth/reset-password
     * 
     * Resets password using a valid reset token.
     */
    @PostMapping("/reset-password")
    suspend fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<Any> {
        log.info("Password reset with token attempted")

        // Validate new password
        if (request.newPassword.length < 8) {
            return ResponseEntity.badRequest()
                .body(AuthErrorResponseDto("WEAK_PASSWORD", "Password must be at least 8 characters"))
        }

        return try {
            userService.resetPassword(request.token, request.newPassword)

            log.info("Password reset successful")

            ResponseEntity.ok(ResetPasswordResponse(
                success = true,
                message = "Password has been reset successfully. You can now log in with your new password."
            ))
        } catch (e: InvalidResetTokenException) {
            log.warn("Password reset failed - invalid or expired token")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(AuthErrorResponseDto("INVALID_TOKEN", "Invalid or expired reset token"))
        }
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

    /**
     * GET /api/v1/auth/profile
     * 
     * Returns the current user's profile.
     */
    @GetMapping("/profile")
    suspend fun getProfile(
        @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<Any> {
        log.debug("Get profile requested")

        val token = authHeader.removePrefix("Bearer ").trim()
        val validationResult = jwtTokenProvider.validateToken(token)

        return when (validationResult) {
            is TokenValidationResult.Valid -> {
                val user = userService.findByEmail(validationResult.email ?: "")
                if (user != null) {
                    ResponseEntity.ok(ProfileResponse(
                        id = user.id,
                        email = user.email,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        role = user.role.name
                    ))
                } else {
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AuthErrorResponseDto("USER_NOT_FOUND", "User not found"))
                }
            }
            is TokenValidationResult.Expired -> {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponseDto("TOKEN_EXPIRED", "Token has expired"))
            }
            is TokenValidationResult.Invalid -> {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponseDto("INVALID_TOKEN", "Invalid token"))
            }
        }
    }

    /**
     * PUT /api/v1/auth/profile
     * 
     * Updates the current user's profile.
     */
    @PutMapping("/profile")
    suspend fun updateProfile(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<Any> {
        log.debug("Update profile requested")

        val token = authHeader.removePrefix("Bearer ").trim()
        val validationResult = jwtTokenProvider.validateToken(token)

        return when (validationResult) {
            is TokenValidationResult.Valid -> {
                val updatedUser = userService.updateProfile(
                    email = validationResult.email ?: "",
                    firstName = request.firstName,
                    lastName = request.lastName,
                    phone = request.phone
                )
                if (updatedUser != null) {
                    ResponseEntity.ok(ProfileResponse(
                        id = updatedUser.id,
                        email = updatedUser.email,
                        firstName = updatedUser.firstName,
                        lastName = updatedUser.lastName,
                        role = updatedUser.role.name
                    ))
                } else {
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AuthErrorResponseDto("USER_NOT_FOUND", "User not found"))
                }
            }
            is TokenValidationResult.Expired -> {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponseDto("TOKEN_EXPIRED", "Token has expired"))
            }
            is TokenValidationResult.Invalid -> {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthErrorResponseDto("INVALID_TOKEN", "Invalid token"))
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}

// Request DTOs
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

// Response DTOs
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserInfo? = null
)

data class UserInfo(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)

data class RegisterResponse(
    val success: Boolean,
    val message: String,
    val accessToken: String,
    val refreshToken: String,
    val user: UserInfo
)

data class ForgotPasswordResponse(
    val success: Boolean,
    val message: String
)

data class ResetPasswordResponse(
    val success: Boolean,
    val message: String
)

data class UpdateProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val phone: String?
)

data class ProfileResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String
)

// Exceptions
class EmailAlreadyExistsException(message: String) : RuntimeException(message)
class InvalidResetTokenException(message: String) : RuntimeException(message)
