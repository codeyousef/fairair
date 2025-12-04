package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Seat Selection functionality.
 * 
 * Tests cover:
 * - Seat map retrieval
 * - Seat reservation
 * - Seat assignment
 * - Seat category pricing (standard, extra legroom, exit row)
 */
@DisplayName("Seat Selection E2E Tests")
class SeatsE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("Seat Map")
    inner class SeatMapTests {

        @Test
        @DisplayName("Should get seat map for valid flight")
        fun `get seat map returns A320 layout`() {
            webClient.get()
                .uri("/api/v1/seats/F3101?date=2025-02-15")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flightNumber").isEqualTo("F3101")
                .jsonPath("$.aircraft.type").isEqualTo("Airbus A320")
                .jsonPath("$.rows").isArray
                .jsonPath("$.rows.length()").value<Int> { assert(it >= 20) }
                .jsonPath("$.rows[0].rowNumber").isEqualTo(1)
                .jsonPath("$.rows[0].seats").isArray
        }

        @Test
        @DisplayName("Should return seat categories with pricing")
        fun `seat map includes pricing information`() {
            webClient.get()
                .uri("/api/v1/seats/F3101?date=2025-02-15")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.rows[0].seats[0].type").isNotEmpty
                .jsonPath("$.rows[0].seats[0].price").isNotEmpty
                .jsonPath("$.rows[0].seats[0].price.currency").isEqualTo("SAR")
        }

        @Test
        @DisplayName("Should indicate seat status")
        fun `seat map shows seat status`() {
            webClient.get()
                .uri("/api/v1/seats/F3101?date=2025-02-15")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.rows[0].seats[0].status").isNotEmpty
                .jsonPath("$.rows[0].seats[0].seatNumber").isNotEmpty
        }

        @Test
        @DisplayName("Should return 404 for invalid flight")
        fun `get seat map fails for non-existent flight`() {
            webClient.get()
                .uri("/api/v1/seats/INVALID?date=2025-02-15")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("FLIGHT_NOT_FOUND")
        }

        @Test
        @DisplayName("Should return 400 for invalid date format")
        fun `get seat map fails with invalid date`() {
            webClient.get()
                .uri("/api/v1/seats/F3101?date=invalid-date")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should include seat legend")
        fun `seat map includes legend`() {
            webClient.get()
                .uri("/api/v1/seats/F3101?date=2025-02-15")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.legend").isArray
                .jsonPath("$.legend[0].type").isNotEmpty
                .jsonPath("$.legend[0].label").isNotEmpty
        }

        @Test
        @DisplayName("Should include pricing tiers")
        fun `seat map includes pricing tiers`() {
            webClient.get()
                .uri("/api/v1/seats/F3101?date=2025-02-15")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pricing").isNotEmpty
                .jsonPath("$.pricing.standard").isNotEmpty
                .jsonPath("$.pricing.extraLegroom").isNotEmpty
        }
    }

    @Nested
    @DisplayName("Seat Reservation")
    inner class SeatReservationTests {

        @Test
        @DisplayName("Should reserve seats for session")
        fun `reserve seats returns reservation`() {
            val reserveRequest = mapOf(
                "flightNumber" to "F3101",
                "date" to "2025-02-15",
                "seats" to listOf("15A", "15B"),
                "sessionId" to "test-session-${System.currentTimeMillis()}"
            )

            webClient.post()
                .uri("/api/v1/seats/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(reserveRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.reservationId").isNotEmpty
                .jsonPath("$.seats").isArray
                .jsonPath("$.totalPrice").isNotEmpty
                .jsonPath("$.expiresAt").isNotEmpty
        }

        @Test
        @DisplayName("Should fail to reserve already taken seat")
        fun `reserve occupied seat returns conflict`() {
            val sessionId = "test-session-${System.currentTimeMillis()}"
            val reserveRequest = mapOf(
                "flightNumber" to "F3101",
                "date" to "2025-02-15",
                "seats" to listOf("20A"),
                "sessionId" to sessionId
            )

            // First reservation
            webClient.post()
                .uri("/api/v1/seats/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(reserveRequest))
                .exchange()
                .expectStatus().isCreated

            // Second reservation for same seat with different session
            val secondRequest = mapOf(
                "flightNumber" to "F3101",
                "date" to "2025-02-15",
                "seats" to listOf("20A"),
                "sessionId" to "different-session"
            )

            webClient.post()
                .uri("/api/v1/seats/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(secondRequest))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SEAT_UNAVAILABLE")
        }
    }

    @Nested
    @DisplayName("Seat Assignment")
    inner class SeatAssignmentTests {

        @Test
        @DisplayName("Should assign seats to booking")
        fun `assign seats returns success`() {
            val pnr = createBookingAndGetPnr()

            val assignRequest = mapOf(
                "pnr" to pnr,
                "assignments" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "seatNumber" to "25A"
                    )
                )
            )

            webClient.post()
                .uri("/api/v1/seats/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(assignRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.assignments").isArray
                .jsonPath("$.assignments[0].seatNumber").isEqualTo("25A")
                .jsonPath("$.message").isNotEmpty
        }

        @Test
        @DisplayName("Should fail with invalid PNR")
        fun `assign seats with invalid pnr returns not found`() {
            val assignRequest = mapOf(
                "pnr" to "INVALID",
                "assignments" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "seatNumber" to "15C"
                    )
                )
            )

            webClient.post()
                .uri("/api/v1/seats/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(assignRequest))
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("BOOKING_NOT_FOUND")
        }
    }

    // Helper method to create a booking and return the PNR
    private fun createBookingAndGetPnr(): String {
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

        val bookingRequest = createBookingRequest(searchId, flightNumber)
        val bookingResponse = webClient.post()
            .uri("/api/v1/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(bookingRequest))
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        val bookingParsed = objectMapper.readTree(bookingResponse)
        return bookingParsed.get("pnr").asText()
    }
}
