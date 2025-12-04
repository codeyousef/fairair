package com.fairair.e2e

import com.fairair.E2ETestBase
import com.fairair.contract.api.ApiRoutes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Check-In user stories (15.1 - 15.5).
 * 
 * Tests cover:
 * - 15.1 Access Check-In (retrieve booking for check-in)
 * - 15.2 View Check-In Eligibility
 * - 15.3 Select Passengers for Check-In
 * - 15.4 Complete Check-In Process
 * - 15.5 Receive Boarding Pass
 * 
 * Uses correct ApiRoutes:
 * - POST /api/v1/checkin (initiate check-in with PNR and lastName)
 * - POST /api/v1/checkin/{pnr}/complete (complete check-in for passengers)
 * - GET /api/v1/checkin/{pnr}/boarding-pass/{passengerIndex} (get boarding pass)
 */
@DisplayName("Check-In E2E Tests")
class CheckInE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("15.1 Access Check-In")
    inner class AccessCheckIn {

        @Test
        @DisplayName("Should retrieve booking for check-in with valid PNR and last name")
        fun `initiate check-in with valid credentials`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.flight").isNotEmpty
                .jsonPath("$.passengers").isArray
                .jsonPath("$.isCheckInOpen").exists()
        }

        @Test
        @DisplayName("Should reject check-in with invalid PNR")
        fun `initiate fails with invalid PNR`() {
            val request = mapOf(
                "pnr" to "XXXXXX",
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("BOOKING_NOT_FOUND")
        }

        @Test
        @DisplayName("Should reject check-in with wrong last name")
        fun `initiate fails with mismatched last name`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Smith"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should handle case-insensitive inputs")
        fun `initiate handles case-insensitive inputs`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr.lowercase(),
                "lastName" to "DOE"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should reject empty PNR")
        fun `initiate fails with empty PNR`() {
            val request = mapOf(
                "pnr" to "",
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject empty last name")
        fun `initiate fails with empty last name`() {
            val request = mapOf(
                "pnr" to "ABC123",
                "lastName" to ""
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("15.2 View Check-In Eligibility")
    inner class ViewCheckInEligibility {

        @Test
        @DisplayName("Should return passenger eligibility status")
        fun `check-in shows passenger eligibility`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.passengers").isArray
                .jsonPath("$.passengers[0].passengerIndex").exists()
                .jsonPath("$.passengers[0].fullName").isNotEmpty
                .jsonPath("$.passengers[0].isEligible").isBoolean
                .jsonPath("$.passengers[0].isCheckedIn").isBoolean
        }

        @Test
        @DisplayName("Should show check-in window times")
        fun `check-in shows window times`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.checkInOpenTime").isNotEmpty
                .jsonPath("$.checkInCloseTime").isNotEmpty
                .jsonPath("$.isCheckInOpen").isBoolean
        }

        @Test
        @DisplayName("Should show flight summary in eligibility response")
        fun `check-in shows flight summary`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flight").isNotEmpty
                .jsonPath("$.flight.flightNumber").isNotEmpty
                .jsonPath("$.flight.origin").isNotEmpty
                .jsonPath("$.flight.destination").isNotEmpty
        }

        @Test
        @DisplayName("Should indicate ineligibility reason when applicable")
        fun `check-in shows ineligibility reasons`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            // The response should include ineligibility reason if passenger is not eligible
            webClient.post()
                .uri(ApiRoutes.CheckIn.INITIATE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.passengers").isArray
        }
    }

    @Nested
    @DisplayName("15.3 Select Passengers for Check-In")
    inner class SelectPassengers {

        @Test
        @DisplayName("Should complete check-in for single passenger")
        fun `complete check-in for single passenger`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.boardingPasses").isArray
                .jsonPath("$.checkedInCount").isNumber
        }

        @Test
        @DisplayName("Should complete check-in for multiple passengers")
        fun `complete check-in for multiple passengers`() {
            // Create a booking with multiple passengers
            val searchResponse = searchFlights(adults = 2)
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val searchParsed = objectMapper.readTree(searchResponse)
            val searchId = searchParsed.get("searchId").asText()
            val flightNumber = searchParsed.get("flights").get(0).get("flightNumber").asText()

            val passenger2 = mapOf(
                "type" to "ADULT",
                "title" to "MS",
                "firstName" to "Jane",
                "lastName" to "Doe",
                "nationality" to "SA",
                "dateOfBirth" to "1992-03-22",
                "documentId" to "XYZ98765432"
            )

            val bookingRequest = createBookingRequest(
                searchId = searchId,
                flightNumber = flightNumber,
                passengers = listOf(VALID_ADULT_PASSENGER, passenger2)
            )

            val bookingResponse = webClient.post()
                .uri("/api/v1/booking")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(bookingRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val pnr = objectMapper.readTree(bookingResponse).get("pnr").asText()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0, 1),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.checkedInCount").isEqualTo(2)
        }

        @Test
        @DisplayName("Should reject check-in for invalid passenger index")
        fun `complete check-in fails with invalid passenger index`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(99),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject check-in for non-existent booking")
        fun `complete check-in fails for non-existent booking`() {
            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor("XXXXXX"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should apply seat preferences when completing check-in")
        fun `complete check-in with seat preferences`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "preferredSeat" to "12A",
                        "preferenceType" to "SPECIFIC"
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.boardingPasses[0].seatNumber").isNotEmpty
        }
    }

    @Nested
    @DisplayName("15.4 Complete Check-In Process")
    inner class CompleteCheckInProcess {

        @Test
        @DisplayName("Should generate boarding passes upon check-in completion")
        fun `check-in generates boarding passes`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.boardingPasses").isArray
                .jsonPath("$.boardingPasses[0].passengerName").isNotEmpty
                .jsonPath("$.boardingPasses[0].seatNumber").isNotEmpty
                .jsonPath("$.boardingPasses[0].boardingGroup").isNotEmpty
                .jsonPath("$.boardingPasses[0].barcodeData").isNotEmpty
        }

        @Test
        @DisplayName("Should return confirmation message")
        fun `check-in returns confirmation message`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.message").isNotEmpty
        }

        @Test
        @DisplayName("Should handle repeat check-in gracefully by returning existing boarding pass")
        fun `repeat check-in returns existing boarding pass`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            // First check-in
            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk

            // Second check-in returns the existing boarding pass (idempotent)
            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.boardingPasses").isArray
                .jsonPath("$.boardingPasses[0].passengerName").isNotEmpty
        }
    }

    @Nested
    @DisplayName("15.5 Receive Boarding Pass")
    inner class ReceiveBoardingPass {

        @Test
        @DisplayName("Should retrieve boarding pass after check-in")
        fun `get boarding pass for checked-in passenger`() {
            val (pnr, _) = createValidBooking()

            // First complete check-in
            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk

            // Then retrieve boarding pass
            webClient.get()
                .uri(ApiRoutes.CheckIn.boardingPassFor(pnr, 0))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.passengerName").isNotEmpty
                .jsonPath("$.flightNumber").isNotEmpty
                .jsonPath("$.seatNumber").isNotEmpty
                .jsonPath("$.boardingGroup").isNotEmpty
                .jsonPath("$.barcodeData").isNotEmpty
        }

        @Test
        @DisplayName("Boarding pass should include all required fields")
        fun `boarding pass has complete information`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk

            webClient.get()
                .uri(ApiRoutes.CheckIn.boardingPassFor(pnr, 0))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.origin").isNotEmpty
                .jsonPath("$.destination").isNotEmpty
                .jsonPath("$.departureDate").isNotEmpty
                .jsonPath("$.departureTime").isNotEmpty
                .jsonPath("$.boardingTime").isNotEmpty
                .jsonPath("$.sequenceNumber").isNumber
        }

        @Test
        @DisplayName("Should return 404 for non-checked-in passenger")
        fun `boarding pass not found for non-checked-in passenger`() {
            val (pnr, _) = createValidBooking()

            // Try to get boarding pass without checking in
            webClient.get()
                .uri(ApiRoutes.CheckIn.boardingPassFor(pnr, 0))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should return 404 for non-existent booking")
        fun `boarding pass not found for non-existent booking`() {
            webClient.get()
                .uri(ApiRoutes.CheckIn.boardingPassFor("XXXXXX", 0))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should return 404 for invalid passenger index")
        fun `boarding pass not found for invalid passenger index`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk

            // Try to get boarding pass for non-existent passenger
            webClient.get()
                .uri(ApiRoutes.CheckIn.boardingPassFor(pnr, 99))
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("Check-In Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle empty passenger indices list")
        fun `complete check-in fails with empty passenger list`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to emptyList<Int>(),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle window seat preference")
        fun `complete check-in with window seat preference`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "preferredSeat" to null,
                        "preferenceType" to "WINDOW"
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should handle aisle seat preference")
        fun `complete check-in with aisle seat preference`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "preferredSeat" to null,
                        "preferenceType" to "AISLE"
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should include gate information when available")
        fun `boarding pass includes gate info`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk

            webClient.get()
                .uri(ApiRoutes.CheckIn.boardingPassFor(pnr, 0))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                // Gate may be null or have a value, just verify the field exists
                .jsonPath("$.gate").exists()
        }

        @Test
        @DisplayName("Should include fare family and cabin class on boarding pass")
        fun `boarding pass includes fare and cabin info`() {
            val (pnr, _) = createValidBooking()

            val completeRequest = mapOf(
                "passengerIndices" to listOf(0),
                "seatPreferences" to null
            )

            webClient.post()
                .uri(ApiRoutes.CheckIn.completeFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(completeRequest))
                .exchange()
                .expectStatus().isOk

            webClient.get()
                .uri(ApiRoutes.CheckIn.boardingPassFor(pnr, 0))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.fareFamily").isNotEmpty
                .jsonPath("$.cabinClass").isNotEmpty
        }
    }
}
