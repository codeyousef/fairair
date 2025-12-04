package com.fairair.e2e

import com.fairair.E2ETestBase
import com.fairair.contract.api.ApiRoutes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Manage Booking user stories (16.1 - 16.6).
 * 
 * Tests cover:
 * - 16.1 Retrieve Existing Booking
 * - 16.2 View Booking Details
 * - 16.3 Modify Flight Date or Time
 * - 16.4 Update Passenger Information
 * - 16.5 Cancel Booking
 * - 16.6 Add Ancillaries to Existing Booking
 * 
 * Uses correct ApiRoutes from shared-contract:
 * - POST /api/v1/manage (retrieve booking with PNR and lastName)
 * - PUT /api/v1/manage/{pnr}/passengers (update passengers)
 * - POST /api/v1/manage/{pnr}/change (get flight change quote)
 * - POST /api/v1/manage/{pnr}/cancel (cancel booking)
 * - POST /api/v1/manage/{pnr}/ancillaries (add ancillaries)
 */
@DisplayName("Manage Booking E2E Tests")
class ManageBookingE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("16.1 Retrieve Existing Booking")
    inner class RetrieveBooking {

        @Test
        @DisplayName("Should retrieve booking with valid PNR and last name")
        fun `retrieve booking with valid credentials`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.status").isNotEmpty
                .jsonPath("$.flight").isNotEmpty
                .jsonPath("$.passengers").isArray
        }

        @Test
        @DisplayName("Should reject retrieval with invalid PNR")
        fun `retrieve fails with invalid PNR`() {
            val request = mapOf(
                "pnr" to "XXXXXX",
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("BOOKING_NOT_FOUND")
        }

        @Test
        @DisplayName("Should reject retrieval with wrong last name")
        fun `retrieve fails with mismatched last name`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Smith"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should handle case-insensitive last name and PNR")
        fun `retrieve handles case-insensitive inputs`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr.lowercase(),
                "lastName" to "DOE"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should reject empty PNR")
        fun `retrieve fails with empty PNR`() {
            val request = mapOf(
                "pnr" to "",
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject empty last name")
        fun `retrieve fails with empty last name`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to ""
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("16.2 View Booking Details")
    inner class ViewBookingDetails {

        @Test
        @DisplayName("Should return complete flight information")
        fun `booking details include flight info`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flight.flightNumber").isNotEmpty
                .jsonPath("$.flight.origin").isNotEmpty
                .jsonPath("$.flight.destination").isNotEmpty
                .jsonPath("$.flight.departureTime").isNotEmpty
        }

        @Test
        @DisplayName("Should return all passenger details")
        fun `booking details include passenger info`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.passengers").isArray
                .jsonPath("$.passengers[0].firstName").isNotEmpty
                .jsonPath("$.passengers[0].lastName").isNotEmpty
        }

        @Test
        @DisplayName("Should return fare family information")
        fun `booking details include fare family`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flight.fareFamily").isNotEmpty
        }

        @Test
        @DisplayName("Should return payment summary")
        fun `booking details include payment info`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.payment").isNotEmpty
                .jsonPath("$.payment.total").isNotEmpty
        }

        @Test
        @DisplayName("Should return booking status")
        fun `booking details include status`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isNotEmpty
        }

        @Test
        @DisplayName("Should return allowed actions")
        fun `booking details include allowed actions`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.allowedActions").isArray
        }
    }

    @Nested
    @DisplayName("16.3 Modify Flight Date or Time")
    inner class ModifyFlight {

        @Test
        @DisplayName("Should return flight change quote")
        fun `get flight change quote`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "newDepartureDate" to getFutureDateString(14),
                "preferredFlightNumber" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.changeFlightFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.originalFlight").isNotEmpty
                .jsonPath("$.availableFlights").isArray
                .jsonPath("$.totalToPay").isNotEmpty
        }

        @Test
        @DisplayName("Should show available alternative flights")
        fun `change quote includes alternatives`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "newDepartureDate" to getFutureDateString(14),
                "preferredFlightNumber" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.changeFlightFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.availableFlights[0].flightNumber").isNotEmpty
                .jsonPath("$.availableFlights[0].departureTime").isNotEmpty
                .jsonPath("$.availableFlights[0].priceDifference").isNotEmpty
        }

        @Test
        @DisplayName("Should show change fee in quote")
        fun `change quote includes fee`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "newDepartureDate" to getFutureDateString(14),
                "preferredFlightNumber" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.changeFlightFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.totalToPay").isNotEmpty
                .jsonPath("$.expiresAt").isNotEmpty
        }

        @Test
        @DisplayName("Should reject change for non-existent booking")
        fun `change fails for invalid PNR`() {
            val request = mapOf(
                "newDepartureDate" to getFutureDateString(14),
                "preferredFlightNumber" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.changeFlightFor("XXXXXX"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should reject change with invalid date format")
        fun `change fails with invalid date`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "newDepartureDate" to "invalid-date",
                "preferredFlightNumber" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.changeFlightFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("16.4 Update Passenger Information")
    inner class UpdatePassengers {

        @Test
        @DisplayName("Should update passenger details")
        fun `update passenger info`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "updates" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "firstName" to "Jonathan",
                        "lastName" to "Doe",
                        "documentId" to "NEW123456",
                        "nationality" to "SA"
                    )
                )
            )

            webClient.put()
                .uri(ApiRoutes.ManageBooking.updatePassengersFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.passengers").isArray
        }

        @Test
        @DisplayName("Should reject update for non-existent booking")
        fun `update fails for invalid PNR`() {
            val request = mapOf(
                "updates" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "firstName" to "Test",
                        "lastName" to null,
                        "documentId" to null,
                        "nationality" to null
                    )
                )
            )

            webClient.put()
                .uri(ApiRoutes.ManageBooking.updatePassengersFor("XXXXXX"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should allow partial passenger updates")
        fun `partial update succeeds`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "updates" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "firstName" to null,
                        "lastName" to null,
                        "documentId" to "UPDATED123",
                        "nationality" to null
                    )
                )
            )

            webClient.put()
                .uri(ApiRoutes.ManageBooking.updatePassengersFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should handle empty updates list")
        fun `empty updates returns current booking`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "updates" to emptyList<Map<String, Any?>>()
            )

            webClient.put()
                .uri(ApiRoutes.ManageBooking.updatePassengersFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }
    }

    @Nested
    @DisplayName("16.5 Cancel Booking")
    inner class CancelBooking {

        @Test
        @DisplayName("Should cancel booking successfully")
        fun `cancel booking with valid request`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "reason" to "CHANGE_OF_PLANS",
                "comments" to "Travel plans changed"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.cancelFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.cancelledAt").isNotEmpty
                .jsonPath("$.cancellationReference").isNotEmpty
        }

        @Test
        @DisplayName("Should return refund amount")
        fun `cancellation shows refund info`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "reason" to "CHANGE_OF_PLANS",
                "comments" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.cancelFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.refundAmount").isNotEmpty
                .jsonPath("$.refundMethod").isNotEmpty
        }

        @Test
        @DisplayName("Should reject cancellation for non-existent booking")
        fun `cancel fails for invalid PNR`() {
            val request = mapOf(
                "reason" to "CHANGE_OF_PLANS",
                "comments" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.cancelFor("XXXXXX"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should require valid reason")
        fun `cancel fails without valid reason`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "reason" to "INVALID_REASON",
                "comments" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.cancelFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle cancellation with optional comments")
        fun `cancel without comments succeeds`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "reason" to "SCHEDULE_CONFLICT",
                "comments" to null
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.cancelFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }
    }

    @Nested
    @DisplayName("16.6 Add Ancillaries to Existing Booking")
    inner class AddAncillaries {

        @Test
        @DisplayName("Should add ancillary to booking")
        fun `add ancillary to booking`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "ancillaries" to listOf(
                    mapOf(
                        "type" to "CHECKED_BAG",
                        "passengerIndex" to 0,
                        "quantity" to 1
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.addAncillariesFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
        }

        @Test
        @DisplayName("Should add multiple ancillaries")
        fun `add multiple ancillaries`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "ancillaries" to listOf(
                    mapOf(
                        "type" to "CHECKED_BAG",
                        "passengerIndex" to 0,
                        "quantity" to 1
                    ),
                    mapOf(
                        "type" to "CHECKED_BAG",
                        "passengerIndex" to 0,
                        "quantity" to 1
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.addAncillariesFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should reject ancillary for non-existent booking")
        fun `add ancillary fails for invalid PNR`() {
            val request = mapOf(
                "ancillaries" to listOf(
                    mapOf(
                        "type" to "CHECKED_BAG",
                        "passengerIndex" to 0,
                        "quantity" to 1
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.addAncillariesFor("XXXXXX"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("Should reject invalid ancillary type")
        fun `add ancillary fails with invalid type`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "ancillaries" to listOf(
                    mapOf(
                        "type" to "INVALID_TYPE",
                        "passengerIndex" to 0,
                        "quantity" to 1
                    )
                )
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.addAncillariesFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle empty ancillaries list")
        fun `empty ancillaries returns current booking`() {
            val (pnr, _) = createValidBooking()

            val request = mapOf(
                "ancillaries" to emptyList<Map<String, Any>>()
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.addAncillariesFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }
    }

    @Nested
    @DisplayName("Manage Booking Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle special characters in last name")
        fun `retrieve handles special characters`() {
            val request = mapOf(
                "pnr" to "SPECNM",
                "lastName" to "O'Brien-Smith"
            )

            webClient.post()
                .uri(ApiRoutes.ManageBooking.BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                // Just verify no server error
                .expectStatus().is4xxClientError
        }

        @Test
        @DisplayName("Should handle concurrent requests gracefully")
        fun `sequential modifications work`() {
            val (pnr, _) = createValidBooking()

            // First update
            val request1 = mapOf(
                "updates" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "firstName" to "Test1",
                        "lastName" to null,
                        "documentId" to null,
                        "nationality" to null
                    )
                )
            )

            webClient.put()
                .uri(ApiRoutes.ManageBooking.updatePassengersFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request1))
                .exchange()
                .expectStatus().isOk

            // Second update
            val request2 = mapOf(
                "updates" to listOf(
                    mapOf(
                        "passengerIndex" to 0,
                        "firstName" to "Test2",
                        "lastName" to null,
                        "documentId" to null,
                        "nationality" to null
                    )
                )
            )

            webClient.put()
                .uri(ApiRoutes.ManageBooking.updatePassengersFor(pnr))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request2))
                .exchange()
                .expectStatus().isOk
        }
    }
}
