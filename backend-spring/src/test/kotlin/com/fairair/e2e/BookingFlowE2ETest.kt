package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Passenger Information and Booking Creation user stories (3.1 - 3.4).
 * 
 * Tests cover:
 * - 3.1 Enter Passenger Details
 * - 3.2 Validate Passenger Information
 * - 3.3 Enter Contact Information
 * - 3.4 Navigate Between Passengers (API support for multiple passengers)
 * 
 * Also covers booking flow aspects from:
 * - 2.4 Select a Fare Family
 * - 4.2 Add Checked Baggage (Ancillaries)
 * - 5.1-5.5 Payment
 * - 6.1-6.3 Booking Confirmation
 */
@DisplayName("Booking Flow E2E Tests")
class BookingFlowE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("Complete Booking Flow")
    inner class CompleteBookingFlow {

        @Test
        @DisplayName("Should complete full booking flow: search -> select -> book")
        fun `complete booking flow with single adult passenger`() {
            // Step 1: Search for flights
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

            // Step 2: Create booking
            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                fareFamily = "FLY"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.pnr").isNotEmpty
                .jsonPath("$.pnr").value<String> { assert(it.length == 6) { "PNR should be 6 chars, got: $it" } }
                .jsonPath("$.bookingReference").isNotEmpty
                .jsonPath("$.flight.flightNumber").isEqualTo(flightNumber)
                .jsonPath("$.passengers").isArray
                .jsonPath("$.passengers.length()").isEqualTo(1)
                .jsonPath("$.totalPaidMinor").isNumber
                .jsonPath("$.totalPaidFormatted").isNotEmpty
                .jsonPath("$.currency").isEqualTo("SAR")
                .jsonPath("$.createdAt").isNotEmpty
        }

        @Test
        @DisplayName("Should complete booking with multiple passengers")
        fun `complete booking with adult, child, and infant`() {
            // Search for flights with multiple passengers
            val searchRequest = createSearchRequest(adults = 2, children = 1, infants = 1)
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

            // Create second adult
            val secondAdult = mapOf(
                "type" to "ADULT",
                "title" to "MRS",
                "firstName" to "Jane",
                "lastName" to "Doe",
                "nationality" to "SA",
                "dateOfBirth" to "1992-03-20",
                "documentId" to "XYZ98765432"
            )

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                fareFamily = "FLY_PLUS",
                passengers = listOf(
                    VALID_ADULT_PASSENGER,
                    secondAdult,
                    VALID_CHILD_PASSENGER,
                    VALID_INFANT_PASSENGER
                )
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.passengers.length()").isEqualTo(4)
        }

        @Test
        @DisplayName("Should book with FlyMax fare family")
        fun `complete booking with premium fare family`() {
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

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                fareFamily = "FLY_MAX"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.flight.fareFamily").isEqualTo("FLY_MAX")
        }
    }

    @Nested
    @DisplayName("3.1 Enter Passenger Details")
    inner class EnterPassengerDetails {

        @Test
        @DisplayName("Should accept valid passenger with all required fields")
        fun `booking accepts complete passenger details`() {
            val (_, _) = createValidBookingFromSearch()
        }

        @Test
        @DisplayName("Should accept all valid titles (MR, MRS, MS, MISS, MSTR)")
        fun `booking accepts all valid passenger titles`() {
            val titles = listOf("MR", "MRS", "MS", "MISS", "MSTR")
            
            titles.forEach { title ->
                val searchResponse = searchFlights()
                    .expectStatus().isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!

                val searchParsed = objectMapper.readTree(searchResponse)
                val searchId = searchParsed.get("searchId").asText()
                val flightNumber = searchParsed.get("flights").get(0).get("flightNumber").asText()

                val passenger = VALID_ADULT_PASSENGER.toMutableMap()
                passenger["title"] = title

                val bookingRequest = createBookingRequest(
                    searchId = searchId,
                    flightNumber = flightNumber,
                    passengers = listOf(passenger)
                )

                webClient.post()
                    .uri("/api/v1/booking")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                    .exchange()
                    .expectStatus().isCreated
            }
        }

        @Test
        @DisplayName("Should accept valid nationality codes")
        fun `booking accepts valid 2-letter nationality codes`() {
            val nationalities = listOf("SA", "US", "GB", "AE", "EG")
            
            nationalities.forEach { nationality ->
                val searchResponse = searchFlights()
                    .expectStatus().isOk
                    .expectBody(String::class.java)
                    .returnResult()
                    .responseBody!!

                val searchParsed = objectMapper.readTree(searchResponse)
                val searchId = searchParsed.get("searchId").asText()
                val flightNumber = searchParsed.get("flights").get(0).get("flightNumber").asText()

                val passenger = VALID_ADULT_PASSENGER.toMutableMap()
                passenger["nationality"] = nationality

                val bookingRequest = createBookingRequest(
                    searchId = searchId,
                    flightNumber = flightNumber,
                    passengers = listOf(passenger)
                )

                webClient.post()
                    .uri("/api/v1/booking")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                    .exchange()
                    .expectStatus().isCreated
            }
        }
    }

    @Nested
    @DisplayName("3.2 Validate Passenger Information")
    inner class ValidatePassengerInformation {

        @Test
        @DisplayName("Should reject passenger with first name too short")
        fun `booking fails with first name less than 2 characters`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val passenger = VALID_ADULT_PASSENGER.toMutableMap()
            passenger["firstName"] = "J"

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(passenger)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
        }

        @Test
        @DisplayName("Should reject passenger with last name too short")
        fun `booking fails with last name less than 2 characters`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val passenger = VALID_ADULT_PASSENGER.toMutableMap()
            passenger["lastName"] = "D"

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(passenger)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject passenger with document ID too short")
        fun `booking fails with document ID less than 5 characters`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val passenger = VALID_ADULT_PASSENGER.toMutableMap()
            passenger["documentId"] = "ABC1"

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(passenger)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject passenger with document ID too long")
        fun `booking fails with document ID more than 20 characters`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val passenger = VALID_ADULT_PASSENGER.toMutableMap()
            passenger["documentId"] = "A".repeat(21)

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(passenger)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject passenger with non-alphanumeric document ID")
        fun `booking fails with special characters in document ID`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val passenger = VALID_ADULT_PASSENGER.toMutableMap()
            passenger["documentId"] = "ABC-12345"

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(passenger)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject booking with no passengers")
        fun `booking fails with empty passengers list`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = emptyList()
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject booking with more than 9 passengers")
        fun `booking fails with too many passengers`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val passengers = (1..10).map { i ->
                mapOf(
                    "type" to "ADULT",
                    "title" to "MR",
                    "firstName" to "Passenger$i",
                    "lastName" to "Test",
                    "nationality" to "SA",
                    "dateOfBirth" to "1990-01-${"0$i".takeLast(2)}",
                    "documentId" to "DOC${100000 + i}"
                )
            }

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = passengers
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject booking without adult passenger")
        fun `booking fails without at least one adult`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(VALID_CHILD_PASSENGER)
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.message").value<String> { 
                    assert(it.contains("adult", ignoreCase = true)) 
                }
        }

        @Test
        @DisplayName("Should reject booking with more infants than adults")
        fun `booking fails when infants exceed adults`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val secondInfant = mapOf(
                "type" to "INFANT",
                "title" to "MSTR",
                "firstName" to "Bobby",
                "lastName" to "Doe",
                "nationality" to "SA",
                "dateOfBirth" to "2024-02-15",
                "documentId" to "INF22222222"
            )

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(
                    VALID_ADULT_PASSENGER,
                    VALID_INFANT_PASSENGER,
                    secondInfant
                )
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.message").value<String> { 
                    assert(it.contains("infant", ignoreCase = true)) 
                }
        }
    }

    @Nested
    @DisplayName("3.3 Enter Contact Information")
    inner class EnterContactInformation {

        @Test
        @DisplayName("Should accept valid email address")
        fun `booking accepts valid email format`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                contactEmail = "valid.email@example.com"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
        }

        @Test
        @DisplayName("Should reject invalid email format")
        fun `booking fails with invalid email`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                contactEmail = "invalid-email"
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject email without domain")
        fun `booking fails with email missing domain`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                contactEmail = "email@"
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
    @DisplayName("4.2 Add Checked Baggage (Ancillaries)")
    inner class AddAncillaries {

        @Test
        @DisplayName("Should accept booking with checked bag ancillary")
        fun `booking with checked bag succeeds`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val ancillaries = listOf(
                mapOf(
                    "type" to "CHECKED_BAG",
                    "passengerIndex" to 0,
                    "priceMinor" to 10000L,
                    "currency" to "SAR"
                )
            )

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                ancillaries = ancillaries
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
        }

        @Test
        @DisplayName("Should reject ancillary with invalid passenger index")
        fun `booking fails with ancillary referencing non-existent passenger`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val ancillaries = listOf(
                mapOf(
                    "type" to "CHECKED_BAG",
                    "passengerIndex" to 5, // Only 1 passenger
                    "priceMinor" to 10000L,
                    "currency" to "SAR"
                )
            )

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                ancillaries = ancillaries
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
    @DisplayName("5.1-5.5 Payment Validation")
    inner class PaymentValidation {

        @Test
        @DisplayName("Should accept valid payment details")
        fun `booking with valid payment succeeds`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                payment = VALID_PAYMENT
            )

            webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
        }

        @Test
        @DisplayName("Should reject payment with zero amount")
        fun `booking fails with zero payment amount`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val invalidPayment = mapOf(
                "cardholderName" to "John Doe",
                "cardNumberLast4" to "4242",
                "totalAmountMinor" to 0L,
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
                .expectBody()
                .jsonPath("$.code").isEqualTo("PAYMENT_ERROR")
        }

        @Test
        @DisplayName("Should reject payment with negative amount")
        fun `booking fails with negative payment amount`() {
            val (searchId, flightNumber) = getSearchIdAndFlight()

            val invalidPayment = mapOf(
                "cardholderName" to "John Doe",
                "cardNumberLast4" to "4242",
                "totalAmountMinor" to -1000L,
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

    @Nested
    @DisplayName("6.1-6.3 Booking Confirmation")
    inner class BookingConfirmation {

        @Test
        @DisplayName("Should return 6-character PNR code")
        fun `booking confirmation contains valid PNR`() {
            val (pnr, _) = createValidBookingFromSearch()
            assert(pnr.length == 6) { "PNR should be 6 characters, got ${pnr.length}" }
            assert(pnr.all { it.isUpperCase() || it.isDigit() }) { "PNR should be alphanumeric uppercase" }
        }

        @Test
        @DisplayName("Should retrieve booking by PNR")
        fun `get booking by PNR returns confirmation`() {
            val (pnr, _) = createValidBookingFromSearch()

            webClient.get()
                .uri("/api/v1/booking/$pnr")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.flight").isNotEmpty
                .jsonPath("$.passengers").isArray
                .jsonPath("$.totalPaidMinor").isNumber
        }

        @Test
        @DisplayName("Should return 404 for non-existent PNR")
        fun `get booking with invalid PNR returns not found`() {
            webClient.get()
                .uri("/api/v1/booking/XXXXXX")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("NOT_FOUND")
        }

        @Test
        @DisplayName("Should return 404 for malformed PNR")
        fun `get booking with malformed PNR returns not found`() {
            webClient.get()
                .uri("/api/v1/booking/ABC")
                .exchange()
                .expectStatus().isNotFound
        }
    }

    // Helper methods
    private fun getSearchIdAndFlight(): Pair<String, String> {
        val searchResponse = searchFlights()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        val parsed = objectMapper.readTree(searchResponse)
        return Pair(
            parsed.get("searchId").asText(),
            parsed.get("flights").get(0).get("flightNumber").asText()
        )
    }

    private fun createValidBookingFromSearch(): Pair<String, String> {
        val (searchId, flightNumber) = getSearchIdAndFlight()

        val bookingRequest = createBookingRequest(searchId, flightNumber)
        val response = webClient.post()
            .uri("/api/v1/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(bookingRequest))
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        val parsed = objectMapper.readTree(response)
        return Pair(parsed.get("pnr").asText(), searchId)
    }
}
