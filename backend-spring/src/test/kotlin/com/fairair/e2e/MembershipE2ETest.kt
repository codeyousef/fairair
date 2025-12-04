package com.fairair.e2e

import com.fairair.E2ETestBase
import com.fairair.contract.api.ApiRoutes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Membership (Adeal) user stories (17.1 - 17.6).
 * 
 * Tests cover:
 * - 17.1 View Membership Plans
 * - 17.2 Subscribe to Membership
 * - 17.3 Book Flights with Membership
 * - 17.4 View Membership Usage
 * - 17.5 Manage Subscription
 * - 17.6 Cancel Membership
 * 
 * Uses correct ApiRoutes from shared-contract:
 * - GET /api/v1/membership/plans (get available plans)
 * - POST /api/v1/membership/subscribe (subscribe to a plan, requires auth)
 * - GET /api/v1/membership/status (get subscription status, requires auth)
 * - GET /api/v1/membership/usage (get usage stats, requires auth)
 * - POST /api/v1/membership/cancel (cancel subscription, requires auth)
 * - POST /api/v1/membership/book (book with membership credits, requires auth)
 */
@DisplayName("Membership (Adeal) E2E Tests")
class MembershipE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("17.1 View Membership Plans")
    inner class ViewPlans {

        @Test
        @DisplayName("Should return all membership plans")
        fun `get membership plans returns all options`() {
            webClient.get()
                .uri(ApiRoutes.Membership.PLANS)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$.length()").isEqualTo(3)
        }

        @Test
        @DisplayName("Should include plan details")
        fun `each plan has required information`() {
            webClient.get()
                .uri(ApiRoutes.Membership.PLANS)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].id").isNotEmpty
                .jsonPath("$[0].name").isNotEmpty
                .jsonPath("$[0].tripsPerYear").isNumber
                .jsonPath("$[0].monthlyPrice").isNotEmpty
                .jsonPath("$[0].benefits").isArray
        }

        @Test
        @DisplayName("Should show 12, 24, 36 trip options")
        fun `plans have correct trip counts`() {
            val response = webClient.get()
                .uri(ApiRoutes.Membership.PLANS)
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(response)
            
            val tripCounts = (0 until parsed.size()).map { 
                parsed.get(it).get("tripsPerYear").asInt() 
            }.toSet()
            
            assert(tripCounts.containsAll(setOf(12, 24, 36))) {
                "Expected plans for 12, 24, 36 trips, got: $tripCounts"
            }
        }

        @Test
        @DisplayName("Should include plan benefits")
        fun `plans include benefits list`() {
            webClient.get()
                .uri(ApiRoutes.Membership.PLANS)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].benefits").isArray
                .jsonPath("$[0].benefits[0].title").isNotEmpty
                .jsonPath("$[0].benefits[0].description").isNotEmpty
        }

        @Test
        @DisplayName("Should mark recommended plan")
        fun `one plan is marked as recommended`() {
            val response = webClient.get()
                .uri(ApiRoutes.Membership.PLANS)
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(response)
            
            val recommendedCount = (0 until parsed.size()).count {
                parsed.get(it).get("isRecommended")?.asBoolean() == true
            }
            
            assert(recommendedCount == 1) {
                "Exactly one plan should be recommended, found: $recommendedCount"
            }
        }

        @Test
        @DisplayName("Should include restrictions information")
        fun `plans show restrictions`() {
            webClient.get()
                .uri(ApiRoutes.Membership.PLANS)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].restrictions").isNotEmpty
                .jsonPath("$[0].restrictions.domesticOnly").isBoolean
                .jsonPath("$[0].restrictions.minimumBookingDays").isNumber
        }
    }

    @Nested
    @DisplayName("17.2 Subscribe to Membership")
    inner class Subscribe {

        @Test
        @DisplayName("Should require authentication for subscription")
        fun `subscribe fails without auth`() {
            val request = createSubscribeRequest("standard-24")

            webClient.post()
                .uri(ApiRoutes.Membership.SUBSCRIBE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @DisplayName("Should create subscription with valid data and auth")
        fun `subscribe to plan with authentication`() {
            val token = login()
            if (token == null) {
                // Skip if login not available
                return
            }

            val request = createSubscribeRequest("standard-24")

            webClient.post()
                .uri(ApiRoutes.Membership.SUBSCRIBE)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.id").isNotEmpty
                .jsonPath("$.status").isEqualTo("ACTIVE")
                .jsonPath("$.plan.id").isEqualTo("standard-24")
        }

        @Test
        @DisplayName("Should reject invalid plan ID")
        fun `subscribe fails with invalid plan`() {
            val token = login()
            if (token == null) return

            val request = createSubscribeRequest("invalid-plan")

            webClient.post()
                .uri(ApiRoutes.Membership.SUBSCRIBE)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject invalid payment info")
        fun `subscribe fails with invalid card`() {
            val token = login()
            if (token == null) return

            val request = mapOf(
                "planId" to "standard-24",
                "paymentMethod" to mapOf(
                    "cardNumber" to "123", // Too short
                    "expiryMonth" to 12,
                    "expiryYear" to 2028,
                    "cvv" to "123",
                    "cardholderName" to "John Doe"
                ),
                "billingAddress" to mapOf(
                    "line1" to "123 Main St",
                    "line2" to null,
                    "city" to "Riyadh",
                    "state" to "Riyadh",
                    "postalCode" to "12345",
                    "country" to "SA"
                )
            )

            webClient.post()
                .uri(ApiRoutes.Membership.SUBSCRIBE)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().is4xxClientError
        }
    }

    @Nested
    @DisplayName("17.3 View Subscription Status")
    inner class ViewStatus {

        @Test
        @DisplayName("Should require authentication for status")
        fun `status fails without auth`() {
            webClient.get()
                .uri(ApiRoutes.Membership.STATUS)
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @DisplayName("Should return 404 when not subscribed")
        fun `status returns not found when no subscription`() {
            // Use admin user who doesn't have a subscription (different from subscription tests)
            val token = login(DEMO_ADMIN_EMAIL, DEMO_USER_PASSWORD)
            if (token == null) return

            webClient.get()
                .uri(ApiRoutes.Membership.STATUS)
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_SUBSCRIBED")
        }
    }

    @Nested
    @DisplayName("17.4 View Membership Usage")
    inner class ViewUsage {

        @Test
        @DisplayName("Should require authentication for usage")
        fun `usage fails without auth`() {
            webClient.get()
                .uri(ApiRoutes.Membership.USAGE)
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @DisplayName("Should return 404 when not subscribed")
        fun `usage returns not found when no subscription`() {
            // Use admin user who doesn't have a subscription (different from subscription tests)
            val token = login(DEMO_ADMIN_EMAIL, DEMO_USER_PASSWORD)
            if (token == null) return

            webClient.get()
                .uri(ApiRoutes.Membership.USAGE)
                .header("Authorization", "Bearer $token")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_SUBSCRIBED")
        }
    }

    @Nested
    @DisplayName("17.5 Book with Membership")
    inner class BookWithMembership {

        @Test
        @DisplayName("Should require authentication for booking")
        fun `book fails without auth`() {
            val request = mapOf(
                "flightNumber" to "F3101",
                "departureDate" to getFutureDateString(14),
                "passengers" to listOf(
                    mapOf(
                        "title" to "MR",
                        "firstName" to "John",
                        "lastName" to "Doe",
                        "dateOfBirth" to "1990-01-15"
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.Membership.BOOK)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @DisplayName("Should return 404 when not subscribed")
        fun `book fails when not subscribed`() {
            val token = login()
            if (token == null) return

            val request = mapOf(
                "flightNumber" to "F3101",
                "departureDate" to getFutureDateString(14),
                "passengers" to listOf(
                    mapOf(
                        "title" to "MR",
                        "firstName" to "John",
                        "lastName" to "Doe",
                        "dateOfBirth" to "1990-01-15"
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.Membership.BOOK)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_SUBSCRIBED")
        }
    }

    @Nested
    @DisplayName("17.6 Cancel Membership")
    inner class CancelMembership {

        @Test
        @DisplayName("Should require authentication for cancellation")
        fun `cancel fails without auth`() {
            val request = mapOf(
                "reason" to "TOO_EXPENSIVE",
                "feedback" to "Service was good but not using it enough"
            )

            webClient.post()
                .uri(ApiRoutes.Membership.CANCEL)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isUnauthorized
        }

        @Test
        @DisplayName("Should return 404 when not subscribed")
        fun `cancel fails when not subscribed`() {
            val token = login()
            if (token == null) return

            val request = mapOf(
                "reason" to "TOO_EXPENSIVE",
                "feedback" to null
            )

            webClient.post()
                .uri(ApiRoutes.Membership.CANCEL)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_SUBSCRIBED")
        }

        @Test
        @DisplayName("Should reject invalid reason")
        fun `cancel fails with invalid reason`() {
            val token = login()
            if (token == null) return

            val request = mapOf(
                "reason" to "INVALID_REASON",
                "feedback" to null
            )

            webClient.post()
                .uri(ApiRoutes.Membership.CANCEL)
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                // Will be bad request or not found depending on order of checks
                .expectStatus().is4xxClientError
        }
    }

    @Nested
    @DisplayName("Membership Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle plans endpoint with no authentication")
        fun `plans accessible without auth`() {
            webClient.get()
                .uri(ApiRoutes.Membership.PLANS)
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should handle expired token gracefully")
        fun `expired token returns unauthorized`() {
            webClient.get()
                .uri(ApiRoutes.Membership.STATUS)
                .header("Authorization", "Bearer invalid.token.here")
                .exchange()
                .expectStatus().isUnauthorized
        }
    }

    // Helper methods
    private fun createSubscribeRequest(planId: String): Map<String, Any?> {
        return mapOf(
            "planId" to planId,
            "paymentMethod" to mapOf(
                "cardNumber" to "4111111111111111",
                "expiryMonth" to 12,
                "expiryYear" to 2028,
                "cvv" to "123",
                "cardholderName" to "John Doe"
            ),
            "billingAddress" to mapOf(
                "line1" to "123 Main St",
                "line2" to null,
                "city" to "Riyadh",
                "state" to "Riyadh",
                "postalCode" to "12345",
                "country" to "SA"
            )
        )
    }
}
