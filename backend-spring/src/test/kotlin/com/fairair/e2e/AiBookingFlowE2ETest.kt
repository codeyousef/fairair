package com.fairair.e2e

import com.fairair.E2ETestBase
import com.fairair.ai.MockGenAiProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.util.UUID

/**
 * E2E tests specifically for AI booking flow behavior.
 * 
 * These tests verify the critical booking flow scenarios:
 * 1. User confirms booking → AI calls create_booking (not search_flights or select_flight)
 * 2. Proper tool sequence: search → select → get_travelers → confirm → create_booking
 * 3. Session context is maintained across messages
 */
@DisplayName("AI Booking Flow E2E Tests")
class AiBookingFlowE2ETest : E2ETestBase() {

    @Autowired
    private lateinit var mockAiProvider: MockGenAiProvider

    @BeforeEach
    fun setup() {
        mockAiProvider.clearAllSessions()
    }

    @Nested
    @DisplayName("Booking Confirmation Flow")
    inner class BookingConfirmationTests {

        @Test
        @DisplayName("Should call create_booking when user confirms with 'yes'")
        fun shouldCallCreateBookingOnYesConfirmation() {
            val sessionId = UUID.randomUUID().toString()

            // Set up mock to simulate the booking confirmation scenario
            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.BookingConfirmationScenario(
                flightNumber = "F3102",
                passengerName = "Jane Doe",
                passportNumber = "A12345678",
                dateOfBirth = "1985-03-15"
            ))

            // User says "yes" after seeing booking summary
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "yes",
                        "locale": "en-US",
                        "context": {
                            "userId": "test-user-123",
                            "userEmail": "test@example.com",
                            "lastSearchId": "search-123",
                            "lastFlightNumber": "F3102"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    // Should contain booking confirmation, NOT flight search results
                    assert(
                        text.contains("booking", ignoreCase = true) ||
                        text.contains("confirmed", ignoreCase = true) ||
                        text.contains("PNR", ignoreCase = true) ||
                        text.contains("booked", ignoreCase = true)
                    ) {
                        "Expected booking confirmation response, got: $text"
                    }
                    assert(!text.contains("Available Flights", ignoreCase = true)) {
                        "Should NOT show flight search results after confirmation, got: $text"
                    }
                }
        }

        @Test
        @DisplayName("Should call create_booking when user confirms with 'ok'")
        fun shouldCallCreateBookingOnOkConfirmation() {
            val sessionId = UUID.randomUUID().toString()

            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.BookingConfirmationScenario(
                flightNumber = "F3100",
                passengerName = "Ahmed Ali",
                passportNumber = "B98765432",
                dateOfBirth = "1990-06-20"
            ))

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "ok",
                        "locale": "en-US",
                        "context": {
                            "userId": "test-user-456",
                            "lastSearchId": "search-456",
                            "lastFlightNumber": "F3100"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    assert(!text.contains("Available Flights", ignoreCase = true)) {
                        "Should NOT show flight search results after 'ok' confirmation"
                    }
                }
        }

        @Test
        @DisplayName("Should call create_booking when user confirms with Arabic 'نعم'")
        fun shouldCallCreateBookingOnArabicConfirmation() {
            val sessionId = UUID.randomUUID().toString()

            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.BookingConfirmationScenario(
                flightNumber = "F3101",
                passengerName = "محمد علي",
                passportNumber = "C11111111",
                dateOfBirth = "1988-01-15"
            ))

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "نعم",
                        "locale": "ar-SA",
                        "context": {
                            "userId": "test-user-789",
                            "lastSearchId": "search-789",
                            "lastFlightNumber": "F3101"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    assert(!text.contains("Available Flights", ignoreCase = true)) {
                        "Should NOT show flight search results after Arabic confirmation"
                    }
                }
        }

        @Test
        @DisplayName("Should call create_booking when user confirms with 'book it'")
        fun shouldCallCreateBookingOnBookItConfirmation() {
            val sessionId = UUID.randomUUID().toString()

            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.BookingConfirmationScenario(
                flightNumber = "F3103",
                passengerName = "Sara Khan",
                passportNumber = "D22222222",
                dateOfBirth = "1992-11-30"
            ))

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "book it",
                        "locale": "en-US",
                        "context": {
                            "userId": "test-user-abc",
                            "lastSearchId": "search-abc",
                            "lastFlightNumber": "F3103"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    assert(!text.contains("Available Flights", ignoreCase = true)) {
                        "Should NOT show flight search results after 'book it' confirmation"
                    }
                }
        }
    }

    @Nested
    @DisplayName("Full Booking Flow Sequence")
    inner class FullBookingFlowTests {

        @Test
        @DisplayName("Complete booking flow: search → select → travelers → confirm → book")
        fun shouldCompleteFullBookingFlow() {
            val sessionId = UUID.randomUUID().toString()
            val userId = "test-user-flow-123"

            // Step 1: User requests flight search
            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.FlightSearchScenario())
            
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "find flights from jeddah to riyadh tomorrow",
                        "locale": "en-US",
                        "context": { "userId": "$userId" }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty

            // Step 2: User selects a flight
            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.FlightSelectedScenario("F3100"))
            
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "I'll take F3100",
                        "locale": "en-US",
                        "context": { 
                            "userId": "$userId",
                            "lastSearchId": "search-id-123"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty

            // Step 3: AI gets travelers and shows booking summary
            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.BookingSummaryScenario(
                flightNumber = "F3100",
                passengerName = "Jane Doe",
                passportNumber = "A12345678"
            ))
            
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "me",
                        "locale": "en-US",
                        "context": { 
                            "userId": "$userId",
                            "lastSearchId": "search-id-123",
                            "lastFlightNumber": "F3100"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    assert(text.contains("proceed", ignoreCase = true) || 
                           text.contains("confirm", ignoreCase = true) ||
                           text.contains("summary", ignoreCase = true) ||
                           text.contains("booking", ignoreCase = true)) {
                        "Should show booking summary asking for confirmation, got: $text"
                    }
                }

            // Step 4: User confirms → should create booking
            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.BookingConfirmedScenario(
                pnr = "ABC123",
                flightNumber = "F3100"
            ))
            
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "yes",
                        "locale": "en-US",
                        "context": { 
                            "userId": "$userId",
                            "lastSearchId": "search-id-123",
                            "lastFlightNumber": "F3100"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    assert(text.contains("ABC123") || text.contains("confirmed", ignoreCase = true) ||
                           text.contains("booked", ignoreCase = true) || text.contains("PNR", ignoreCase = true)) {
                        "Should contain PNR or booking confirmation, got: $text"
                    }
                    assert(!text.contains("Available Flights", ignoreCase = true)) {
                        "Should NOT show flight search results, got: $text"
                    }
                }
        }
    }

    @Nested
    @DisplayName("Negative Cases - Should NOT create booking")
    inner class NegativeCases {

        @Test
        @DisplayName("Should NOT create booking when user says 'no'")
        fun shouldNotCreateBookingOnNoResponse() {
            val sessionId = UUID.randomUUID().toString()

            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.CancellationScenario())

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "no",
                        "locale": "en-US",
                        "context": {
                            "userId": "test-user-no",
                            "lastFlightNumber": "F3100"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    assert(text.contains("cancel", ignoreCase = true) || 
                           text.contains("else", ignoreCase = true) ||
                           text.contains("help", ignoreCase = true)) {
                        "Should ask if user needs anything else, got: $text"
                    }
                    assert(!text.contains("PNR", ignoreCase = true)) {
                        "Should NOT contain PNR when user says no, got: $text"
                    }
                }
        }

        @Test
        @DisplayName("Should search flights when user asks to search (not confirm)")
        fun shouldSearchFlightsWhenAsked() {
            val sessionId = UUID.randomUUID().toString()

            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.FlightSearchScenario())

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "search flights to dubai",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
        }
    }

    @Nested
    @DisplayName("Context Preservation")
    inner class ContextPreservationTests {

        @Test
        @DisplayName("Should preserve flight context across messages")
        fun shouldPreserveFlightContext() {
            val sessionId = UUID.randomUUID().toString()
            val flightNumber = "F3102"

            // First message selects flight
            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.FlightSelectedScenario(flightNumber))
            
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "I want $flightNumber",
                        "locale": "en-US",
                        "context": { "lastSearchId": "search-123" }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty

            // Second message confirms - should use the same flight
            mockAiProvider.setNextResponse(sessionId, MockGenAiProvider.BookingConfirmedScenario(
                pnr = "XYZ789",
                flightNumber = flightNumber
            ))
            
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "yes, book it",
                        "locale": "en-US",
                        "context": { 
                            "lastSearchId": "search-123",
                            "lastFlightNumber": "$flightNumber",
                            "userId": "user-123"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").value<String> { text ->
                    assert(text.contains("XYZ789") || text.contains("confirmed", ignoreCase = true) ||
                           text.contains("booked", ignoreCase = true)) {
                        "Should contain PNR or booking confirmation, got: $text"
                    }
                }
        }
    }
}
