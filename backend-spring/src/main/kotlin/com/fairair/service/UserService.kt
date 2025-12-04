package com.fairair.service

import com.fairair.controller.EmailAlreadyExistsException
import com.fairair.controller.InvalidResetTokenException
import com.fairair.entity.UserEntity
import com.fairair.repository.UserRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * User service for managing users.
 * Uses database-backed storage via Spring Data R2DBC.
 * 
 * Demo users are seeded via data.sql on startup.
 * 
 * Production Notes:
 * - Add email verification flow
 * - Use proper email service for password reset
 * - Store reset tokens in database with expiry
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val r2dbcEntityTemplate: R2dbcEntityTemplate
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)
    private val passwordEncoder = BCryptPasswordEncoder()
    
    // In-memory storage for password reset tokens (mock)
    private val resetTokens = ConcurrentHashMap<String, PasswordResetToken>()

    suspend fun findByEmail(email: String): DemoUser? {
        return userRepository.findByEmailIgnoreCase(email)?.toDemoUser()
    }

    suspend fun validateCredentials(email: String, password: String): DemoUser? {
        val user = userRepository.findByEmailIgnoreCase(email) ?: return null
        return if (passwordEncoder.matches(password, user.passwordHash)) {
            user.toDemoUser()
        } else {
            null
        }
    }

    suspend fun getAllUsers(): List<DemoUser> {
        return userRepository.findAll().toList().map { it.toDemoUser() }
    }

    /**
     * Registers a new user account.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?
    ): DemoUser {
        log.info("Registering new user: $email")

        // Check if email already exists
        val existing = userRepository.findByEmailIgnoreCase(email)
        if (existing != null) {
            throw EmailAlreadyExistsException(email)
        }

        val now = Instant.now()
        val user = UserEntity(
            id = "user-${UUID.randomUUID().toString().take(8)}",
            email = email.lowercase(),
            passwordHash = passwordEncoder.encode(password),
            firstName = firstName,
            lastName = lastName,
            role = "USER",
            createdAt = now,
            updatedAt = now
        )

        // Use insert instead of save to ensure we're creating a new entity
        val saved = r2dbcEntityTemplate.insert(user).awaitSingle()
        log.info("User registered successfully: ${saved.id}")

        return saved.toDemoUser()
    }

    /**
     * Initiates password reset by generating a reset token.
     * In production: send email with reset link.
     */
    suspend fun initiatePasswordReset(email: String) {
        log.info("Password reset initiated for: $email")

        val user = userRepository.findByEmailIgnoreCase(email)
        if (user == null) {
            // Silently return to prevent email enumeration
            log.debug("Password reset requested for non-existent email: $email")
            return
        }

        // Generate reset token
        val token = UUID.randomUUID().toString()
        val resetToken = PasswordResetToken(
            token = token,
            userId = user.id,
            email = email,
            expiresAt = System.currentTimeMillis() + (60 * 60 * 1000) // 1 hour
        )

        resetTokens[token] = resetToken

        // In production: send email with reset link
        log.info("Password reset token generated for user ${user.id}: $token")
        log.info("Reset link would be: https://fairair.com/reset-password?token=$token")
    }

    /**
     * Resets password using a valid reset token.
     */
    suspend fun resetPassword(token: String, newPassword: String) {
        log.info("Password reset attempted with token")

        val resetToken = resetTokens[token]
        if (resetToken == null || resetToken.expiresAt < System.currentTimeMillis()) {
            throw InvalidResetTokenException("Invalid or expired reset token")
        }

        val user = userRepository.findByEmailIgnoreCase(resetToken.email)
            ?: throw InvalidResetTokenException("User not found")

        // Update password
        val updatedUser = user.copy(
            passwordHash = passwordEncoder.encode(newPassword),
            updatedAt = Instant.now()
        )
        userRepository.save(updatedUser)

        // Remove used token
        resetTokens.remove(token)

        log.info("Password reset successful for user: ${user.id}")
    }

    /**
     * Updates user profile.
     */
    suspend fun updateProfile(
        email: String,
        firstName: String?,
        lastName: String?,
        phone: String?
    ): DemoUser? {
        log.info("Updating profile for: $email")

        val user = userRepository.findByEmailIgnoreCase(email)
            ?: return null

        val updatedUser = user.copy(
            firstName = firstName ?: user.firstName,
            lastName = lastName ?: user.lastName,
            updatedAt = Instant.now()
        )
        val saved = userRepository.save(updatedUser)
        log.info("Profile updated for user: ${saved.id}")

        return saved.toDemoUser()
    }
    
    private fun UserEntity.toDemoUser() = DemoUser(
        id = id,
        email = email,
        password = passwordHash,
        firstName = firstName,
        lastName = lastName,
        role = UserRole.valueOf(role)
    )
}

/**
 * Password reset token data.
 */
data class PasswordResetToken(
    val token: String,
    val userId: String,
    val email: String,
    val expiresAt: Long
)

/**
 * Demo user data class.
 */
data class DemoUser(
    val id: String,
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole
)

/**
 * User roles.
 */
enum class UserRole {
    USER,
    EMPLOYEE,
    ADMIN
}
