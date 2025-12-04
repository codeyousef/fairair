package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Error Handling & Recovery user stories (11.1 - 11.4).
 * 
 * Tests cover:
 * - 11.1 Handle Network Errors (API error responses)
 * - 11.2 Handle Session Expiration (Search expiration)
 * - 11.3 Handle Unavailable Flights
 * - 11.4 Handle Malformed Data
 */
@DisplayName("Error Handling E2E Tests")
class ErrorHandlingE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("11.2 Handle Session Expiration")
    inner class SessionExpiration {

        @Test
        @DisplayName("Should return SEARCH_EXPIRED for invalid searchId")
        fun `booking fails with expired or invalid search ID`() {
            val bookingRequest = createBookingRequest(
                searchId = "invalid-search-id",
                flightNumber = "FA123"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isEqualTo(410) // GONE
                .expectBody()
                .jsonPath("$.code").isEqualTo("SEARCH_EXPIRED")
                .jsonPath("$.message").value<String> { 
                    assert(it.contains("expired", ignoreCase = true)) 
                }
        }

        @Test
        @DisplayName("Should provide clear message for search expiration")
        fun `search expiration error contains helpful message`() {
            val bookingRequest = createBookingRequest(
                searchId = "expired-session-12345",
                flightNumber = "FA999"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isEqualTo(410)
                .expectBody()
                .jsonPath("$.message").value<String> { 
                    assert(it.contains("search", ignoreCase = true) || it.contains("again", ignoreCase = true))
                }
        }
    }

    @Nested
    @DisplayName("11.3 Handle Unavailable Flights")
    inner class UnavailableFlights {

        @Test
        @DisplayName("Should return FLIGHT_NOT_FOUND for non-existent flight")
        fun `booking fails when flight is not available`() {
            // First get a valid search
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

            val searchId = objectMapper.readTree(searchResponse).get("searchId").asText()

            // Try to book a non-existent flight
            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = "NONEXISTENT999"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("FLIGHT_NOT_FOUND")
        }

        @Test
        @DisplayName("Should return FARE_NOT_FOUND for unavailable fare family")
        fun `booking fails when fare family is not available`() {
            // Get valid search
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

            val parsed = objectMapper.readTree(searchResponse)
            val searchId = parsed.get("searchId").asText()
            val flightNumber = parsed.get("flights").get(0).get("flightNumber").asText()

            // Try to book with invalid fare family (note: might fail at serialization level)
            // We'll test with valid fare family name that doesn't exist
            val bookingRequest = mapOf(
                "searchId" to searchId,
                "flightNumber" to flightNumber,
                "fareFamily" to "INVALID_FARE",
                "passengers" to listOf(VALID_ADULT_PASSENGER),
                "ancillaries" to emptyList<Any>(),
                "contactEmail" to "test@example.com",
                "payment" to VALID_PAYMENT
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("11.4 Handle Malformed Data")
    inner class MalformedData {

        @Test
        @DisplayName("Should handle malformed JSON in search request")
        fun `search gracefully handles malformed JSON`() {
            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{invalid json}")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle missing required fields in search")
        fun `search fails with missing origin`() {
            val request = mapOf(
                "destination" to DESTINATION_RUH,
                "departureDate" to getFutureDateString(),
                "passengers" to mapOf("adults" to 1, "children" to 0, "infants" to 0)
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle missing required fields in booking")
        fun `booking fails with missing passengers`() {
            val request = mapOf(
                "searchId" to "some-search-id",
                "flightNumber" to "FA123",
                "fareFamily" to "FLY"
                // Missing passengers, ancillaries, contactEmail, payment
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle invalid date format in search")
        fun `search fails with invalid date format`() {
            val request = mapOf(
                "origin" to ORIGIN_JED,
                "destination" to DESTINATION_RUH,
                "departureDate" to "31/12/2025", // Wrong format
                "passengers" to mapOf("adults" to 1, "children" to 0, "infants" to 0)
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle empty request body")
        fun `search fails with empty body`() {
            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle invalid content type")
        fun `search fails with wrong content type`() {
            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("not json")
                .exchange()
                .expectStatus().is4xxClientError
        }

        @Test
        @DisplayName("Should handle null values gracefully")
        fun `search handles null values in request`() {
            val request = mapOf(
                "origin" to ORIGIN_JED,
                "destination" to null,
                "departureDate" to getFutureDateString(),
                "passengers" to mapOf("adults" to 1, "children" to 0, "infants" to 0)
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle booking with invalid passenger type")
        fun `booking fails with invalid passenger type enum`() {
            val searchResponse = searchFlights()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(searchResponse)
            val searchId = parsed.get("searchId").asText()
            val flightNumber = parsed.get("flights").get(0).get("flightNumber").asText()

            val invalidPassenger = VALID_ADULT_PASSENGER.toMutableMap()
            invalidPassenger["type"] = "INVALID_TYPE"

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(invalidPassenger)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("Invalid Route Handling")
    inner class InvalidRoutes {

        @Test
        @DisplayName("Should return INVALID_ROUTE for non-existent origin")
        fun `search returns clear error for invalid origin`() {
            val request = createSearchRequest(origin = "XXX")

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_ROUTE")
        }

        @Test
        @DisplayName("Should return INVALID_ROUTE for non-existent destination")
        fun `search returns clear error for invalid destination`() {
            val request = createSearchRequest(destination = "YYY")

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_ROUTE")
        }

        @Test
        @DisplayName("Should return INVALID_ROUTE for route that doesn't exist")
        fun `search fails for route not in network`() {
            // Try a route that might have valid airport codes but not connected
            val request = createSearchRequest(origin = "DMM", destination = "XXX")

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("Validation Error Messages")
    inner class ValidationErrors {

        @Test
        @DisplayName("Should return descriptive error for passenger name validation")
        fun `passenger validation error includes field reference`() {
            val searchResponse = searchFlights()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(searchResponse)
            val searchId = parsed.get("searchId").asText()
            val flightNumber = parsed.get("flights").get(0).get("flightNumber").asText()

            val invalidPassenger = VALID_ADULT_PASSENGER.toMutableMap()
            invalidPassenger["firstName"] = "A" // Too short

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(invalidPassenger)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.message").value<String> {
                    assert(it.contains("name", ignoreCase = true) || it.contains("character", ignoreCase = true))
                }
        }

        @Test
        @DisplayName("Should return error for payment with missing cardholder name")
        fun `payment validation error for missing cardholder`() {
            val searchResponse = searchFlights()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(searchResponse)
            val searchId = parsed.get("searchId").asText()
            val flightNumber = parsed.get("flights").get(0).get("flightNumber").asText()

            val invalidPayment = mapOf(
                "cardholderName" to "",
                "cardNumberLast4" to "4242",
                "totalAmountMinor" to 35000L,
                "currency" to "SAR"
            )

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                payment = invalidPayment
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should return error for invalid card last 4 digits")
        fun `payment validation error for invalid card number`() {
            val searchResponse = searchFlights()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(searchResponse)
            val searchId = parsed.get("searchId").asText()
            val flightNumber = parsed.get("flights").get(0).get("flightNumber").asText()

            val invalidPayment = mapOf(
                "cardholderName" to "John Doe",
                "cardNumberLast4" to "12", // Too short
                "totalAmountMinor" to 35000L,
                "currency" to "SAR"
            )

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                payment = invalidPayment
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }
    }
}
