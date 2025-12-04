package com.fairair.service

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import jakarta.annotation.PostConstruct

/**
 * User service for managing demo users.
 * In production, this would use a database.
 */
@Service
class UserService {

    private val passwordEncoder = BCryptPasswordEncoder()
    private val users = mutableMapOf<String, DemoUser>()

    @PostConstruct
    fun initDemoUsers() {
        // Create 3 demo users
        val hashedPassword = passwordEncoder.encode("password")
        
        // Use lowercase keys for consistent lookup
        users["employee@fairair.com".lowercase()] = DemoUser(
            id = "user-001",
            email = "employee@fairair.com",
            password = hashedPassword,
            firstName = "John",
            lastName = "Smith",
            role = UserRole.EMPLOYEE
        )
        
        users["jane@test.com".lowercase()] = DemoUser(
            id = "user-002",
            email = "jane@test.com",
            password = hashedPassword,
            firstName = "Jane",
            lastName = "Doe",
            role = UserRole.USER
        )
        
        users["admin@test.com".lowercase()] = DemoUser(
            id = "user-003",
            email = "admin@test.com",
            password = hashedPassword,
            firstName = "Admin",
            lastName = "User",
            role = UserRole.ADMIN
        )
    }

    fun findByEmail(email: String): DemoUser? {
        return users[email.lowercase()]
    }

    fun validateCredentials(email: String, password: String): DemoUser? {
        val user = findByEmail(email.lowercase()) ?: return null
        return if (passwordEncoder.matches(password, user.password)) {
            user
        } else {
            null
        }
    }

    fun getAllUsers(): List<DemoUser> = users.values.toList()
}

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
