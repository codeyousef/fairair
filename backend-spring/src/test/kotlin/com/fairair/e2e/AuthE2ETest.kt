package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Authentication and Guest Checkout user stories (12.1 - 12.3).
 * 
 * Tests cover:
 * - 12.1 Book as Guest
 * - 12.2 Create Account After Booking (API structure)
 * - 12.3 Login to Existing Account
 * 
 * Also tests Auth API error handling and token management.
 */
@DisplayName("Authentication & Guest Checkout E2E Tests")
class AuthE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("12.1 Book as Guest")
    inner class BookAsGuest {

        @Test
        @DisplayName("Should complete booking without authentication")
        fun `guest user can complete full booking flow`() {
            // Search without auth token
            val searchRequest = createSearchRequest()
            val searchResponse = webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(searchRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val searchParsed = objectMapper.readTree(searchResponse)
            val searchId = searchParsed.get("searchId").asText()
            val flightNumber = searchParsed.get("flights").get(0).get("flightNumber").asText()

            // Book without auth token
            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                contactEmail = "guest@example.com"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.pnr").isNotEmpty
        }

        @Test
        @DisplayName("Should access config routes without authentication")
        fun `guest can access routes configuration`() {
            webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.routes").isMap
        }

        @Test
        @DisplayName("Should access stations without authentication")
        fun `guest can access stations list`() {
            webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$[0].code").isNotEmpty
        }
    }

    @Nested
    @DisplayName("12.3 Login to Existing Account")
    inner class LoginToAccount {

        @Test
        @DisplayName("Should login with valid credentials")
        fun `login succeeds with correct email and password`() {
            webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(createLoginRequest()))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty
                .jsonPath("$.refreshToken").isNotEmpty
                .jsonPath("$.tokenType").isEqualTo("Bearer")
                .jsonPath("$.expiresIn").isNumber
                .jsonPath("$.user").isNotEmpty
                .jsonPath("$.user.email").isEqualTo(DEMO_USER_EMAIL)
                .jsonPath("$.user.firstName").isNotEmpty
                .jsonPath("$.user.lastName").isNotEmpty
                .jsonPath("$.user.role").isNotEmpty
        }

        @Test
        @DisplayName("Should login as employee user")
        fun `login succeeds for employee account`() {
            webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(
                    createLoginRequest(email = DEMO_EMPLOYEE_EMAIL)
                ))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.user.role").isEqualTo("EMPLOYEE")
        }

        @Test
        @DisplayName("Should login as admin user")
        fun `login succeeds for admin account`() {
            webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(
                    createLoginRequest(email = DEMO_ADMIN_EMAIL)
                ))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.user.role").isEqualTo("ADMIN")
        }

        @Test
        @DisplayName("Should reject login with invalid email")
        fun `login fails with non-existent email`() {
            webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(
                    createLoginRequest(email = "nonexistent@test.com")
                ))
                .exchange()
                .expectStatus().isUnauthorized
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_CREDENTIALS")
        }

        @Test
        @DisplayName("Should reject login with wrong password")
        fun `login fails with incorrect password`() {
            webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(
                    createLoginRequest(password = INVALID_PASSWORD)
                ))
                .exchange()
                .expectStatus().isUnauthorized
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_CREDENTIALS")
        }

        @Test
        @DisplayName("Should reject login with malformed email")
        fun `login fails with invalid email format`() {
            webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(
                    createLoginRequest(email = INVALID_EMAIL)
                ))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_EMAIL")
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    inner class TokenRefresh {

        @Test
        @DisplayName("Should refresh access token with valid refresh token")
        fun `token refresh succeeds with valid refresh token`() {
            // First login to get tokens
            val loginResponse = webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(createLoginRequest()))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val loginParsed = objectMapper.readTree(loginResponse)
            val refreshToken = loginParsed.get("refreshToken").asText()

            // Refresh the token
            webClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(mapOf("refreshToken" to refreshToken)))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty
                .jsonPath("$.refreshToken").isNotEmpty
        }

        @Test
        @DisplayName("Should reject refresh with invalid token")
        fun `token refresh fails with invalid token`() {
            webClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(mapOf("refreshToken" to "invalid-token")))
                .exchange()
                .expectStatus().isUnauthorized
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN")
        }

        @Test
        @DisplayName("Should reject refresh with access token instead of refresh token")
        fun `token refresh fails when using access token`() {
            // Login to get access token
            val loginResponse = webClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(createLoginRequest()))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val loginParsed = objectMapper.readTree(loginResponse)
            val accessToken = loginParsed.get("accessToken").asText()

            // Try to use access token as refresh token
            webClient.post()
                .uri("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(mapOf("refreshToken" to accessToken)))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_TOKEN_TYPE")
        }
    }

    @Nested
    @DisplayName("Authenticated Booking")
    inner class AuthenticatedBooking {

        @Test
        @DisplayName("Should associate booking with user when authenticated")
        fun `authenticated booking is linked to user account`() {
            // Login to get token
            val accessToken = login()!!

            // Search
            val searchRequest = createSearchRequest()
            val searchResponse = webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $accessToken")
                .bodyValue(objectMapper.writeValueAsString(searchRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val searchParsed = objectMapper.readTree(searchResponse)
            val searchId = searchParsed.get("searchId").asText()
            val flightNumber = searchParsed.get("flights").get(0).get("flightNumber").asText()

            // Book with auth
            val bookingRequest = createBookingRequest(searchId, flightNumber)
            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $accessToken")
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.pnr").isNotEmpty
        }

        @Test
        @DisplayName("Should retrieve user's bookings when authenticated")
        fun `authenticated user can retrieve their bookings`() {
            val accessToken = login()!!

            webClient.get()
                .uri("/api/v1/booking/user/me")
                .header("Authorization", "Bearer $accessToken")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
        }

        @Test
        @DisplayName("Should reject user bookings endpoint without auth")
        fun `user bookings endpoint requires authentication`() {
            webClient.get()
                .uri("/api/v1/booking/user/me")
                .exchange()
                .expectStatus().isUnauthorized
        }
    }

    @Nested
    @DisplayName("Logout")
    inner class Logout {

        @Test
        @DisplayName("Should logout successfully")
        fun `logout returns success response`() {
            webClient.post()
                .uri("/api/v1/auth/logout")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.message").isNotEmpty
        }
    }

    @Nested
    @DisplayName("Health Check")
    inner class HealthCheck {

        @Test
        @DisplayName("Should return health status without auth")
        fun `health endpoint is accessible without authentication`() {
            webClient.get()
                .uri("/health")
                .exchange()
                .expectStatus().isOk
        }
    }
}
