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
 * E2E tests for the Pilot AI Chat functionality.
 * 
 * Tests cover:
 * - Chat message sending and receiving
 * - Session management
 * - Multi-language support (English and Arabic)
 * - Tool call handling
 * - Error handling
 */
@DisplayName("Chat E2E Tests")
class ChatE2ETest : E2ETestBase() {

    @Autowired
    private lateinit var mockAiProvider: MockGenAiProvider

    @BeforeEach
    fun setup() {
        mockAiProvider.clearAllSessions()
    }

    @Nested
    @DisplayName("POST /api/v1/chat/message - Send Message")
    inner class SendMessageTests {

        @Test
        @DisplayName("Should return greeting response for hello message")
        fun shouldReturnGreetingForHello() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Hello",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
                .jsonPath("$.text").value<String> { text ->
                    assert(text.contains("Pilot") || text.contains("help")) {
                        "Expected greeting with Pilot or help, got: $text"
                    }
                }
        }

        @Test
        @DisplayName("Should return Arabic response for Arabic greeting")
        fun shouldReturnArabicResponseForArabicGreeting() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "مرحبا",
                        "locale": "ar-SA"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
                .jsonPath("$.detectedLanguage").isEqualTo("ar")
        }

        @Test
        @DisplayName("Should handle flight search intent")
        fun shouldHandleFlightSearchIntent() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "I need to find a flight to Riyadh",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
        }

        @Test
        @DisplayName("Should handle booking lookup intent")
        fun shouldHandleBookingLookupIntent() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "I want to check my booking",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
                .jsonPath("$.text").value<String> { text ->
                    assert(text.contains("PNR") || text.contains("booking") || text.contains("reference")) {
                        "Expected booking-related response, got: $text"
                    }
                }
        }

        @Test
        @DisplayName("Should handle seat change intent")
        fun shouldHandleSeatChangeIntent() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "I want to change my seat to a window seat",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
                .jsonPath("$.text").value<String> { text ->
                    assert(text.contains("seat", ignoreCase = true) || 
                           text.contains("window", ignoreCase = true) ||
                           text.contains("aisle", ignoreCase = true)) {
                        "Expected seat-related response, got: $text"
                    }
                }
        }

        @Test
        @DisplayName("Should maintain session context across messages")
        fun shouldMaintainSessionContext() {
            val sessionId = UUID.randomUUID().toString()

            // First message
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Hello",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk

            // Second message in same session
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "I need a flight",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk

            // Verify session has both messages
            val history = mockAiProvider.getSessionHistory(sessionId)
            assert(history.size == 2) { "Expected 2 messages in session, got ${history.size}" }
            assert(history[0] == "Hello") { "First message should be 'Hello'" }
            assert(history[1] == "I need a flight") { "Second message should be 'I need a flight'" }
        }

        @Test
        @DisplayName("Should include suggestions in response")
        fun shouldIncludeSuggestionsInResponse() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Hello",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.suggestions").isArray
                .jsonPath("$.suggestions").isNotEmpty
        }

        @Test
        @DisplayName("Should handle context with current PNR")
        fun shouldHandleContextWithPnr() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Change my seat",
                        "locale": "en-US",
                        "context": {
                            "currentPnr": "ABC123",
                            "currentScreen": "manage-booking"
                        }
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
        }

        @Test
        @DisplayName("Should return 400 for missing sessionId")
        fun shouldReturn400ForMissingSessionId() {
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "message": "Hello"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should return 400 for missing message")
        fun shouldReturn400ForMissingMessage() {
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "${UUID.randomUUID()}"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should return 400 for empty message")
        fun shouldReturn400ForEmptyMessage() {
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "${UUID.randomUUID()}",
                        "message": ""
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/chat/sessions/{sessionId} - Clear Session")
    inner class ClearSessionTests {

        @Test
        @DisplayName("Should clear existing session")
        fun shouldClearExistingSession() {
            val sessionId = UUID.randomUUID().toString()

            // Create session with a message
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Hello",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk

            // Clear session
            webClient.delete()
                .uri("/api/v1/chat/sessions/$sessionId")
                .exchange()
                .expectStatus().isNoContent()

            // Verify session is cleared
            val history = mockAiProvider.getSessionHistory(sessionId)
            assert(history.isEmpty()) { "Session should be cleared" }
        }

        @Test
        @DisplayName("Should handle clearing non-existent session")
        fun shouldHandleClearingNonExistentSession() {
            val sessionId = UUID.randomUUID().toString()

            webClient.delete()
                .uri("/api/v1/chat/sessions/$sessionId")
                .exchange()
                .expectStatus().isNoContent() // Should not error for non-existent session
        }
    }

    @Nested
    @DisplayName("Conversation Flow Tests")
    inner class ConversationFlowTests {

        @Test
        @DisplayName("Should handle multi-turn conversation")
        fun shouldHandleMultiTurnConversation() {
            val sessionId = UUID.randomUUID().toString()

            // Turn 1: Greeting
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Hi",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty

            // Turn 2: Flight search intent
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "I want to fly to Jeddah",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty

            // Turn 3: More specific request
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Search for flights tomorrow",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty

            // Verify all 3 messages are in session
            val history = mockAiProvider.getSessionHistory(sessionId)
            assert(history.size == 3) { "Expected 3 messages in conversation" }
        }

        @Test
        @DisplayName("Should handle language switching mid-conversation")
        fun shouldHandleLanguageSwitching() {
            val sessionId = UUID.randomUUID().toString()

            // English message
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Hello",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk

            // Arabic message
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "أريد حجز رحلة",
                        "locale": "ar-SA"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
        }
    }

    @Nested
    @DisplayName("Tool Execution Flow Tests")
    inner class ToolExecutionTests {

        @Test
        @DisplayName("Should execute flight search tool")
        fun shouldExecuteFlightSearchTool() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Search for flights from Jeddah to Riyadh",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
        }

        @Test
        @DisplayName("Should execute booking lookup tool with PNR")
        fun shouldExecuteBookingLookupWithPnr() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Check booking ABC123",
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
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        fun shouldHandleMalformedJson() {
            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{ invalid json }")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle very long messages")
        fun shouldHandleVeryLongMessages() {
            val sessionId = UUID.randomUUID().toString()
            val longMessage = "I need help with " + "my flight ".repeat(500) // ~5000 chars

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "$longMessage",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
        }

        @Test
        @DisplayName("Should handle special characters in message")
        fun shouldHandleSpecialCharacters() {
            val sessionId = UUID.randomUUID().toString()

            webClient.post()
                .uri("/api/v1/chat/message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                        "sessionId": "$sessionId",
                        "message": "Hello! Can you help me? 🛫 ✈️ 🌍",
                        "locale": "en-US"
                    }
                """.trimIndent())
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.text").isNotEmpty
        }
    }
}
