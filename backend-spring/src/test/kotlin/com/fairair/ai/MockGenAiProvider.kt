package com.fairair.ai

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

/**
 * Mock AI provider for testing.
 * Returns predictable responses without calling external AI services.
 */
@Service
@Primary
@Profile("test")
class MockGenAiProvider : GenAiProvider {

    private val sessions = mutableMapOf<String, MutableList<String>>()

    override suspend fun chat(
        sessionId: String,
        userMessage: String,
        systemPrompt: String?
    ): AiChatResponse {
        // Store message in session
        sessions.getOrPut(sessionId) { mutableListOf() }.add(userMessage)

        // Generate predictable responses based on message content
        return when {
            // Flight search intent
            userMessage.contains("flight", ignoreCase = true) ||
            userMessage.contains("fly", ignoreCase = true) ||
            userMessage.contains("riyadh", ignoreCase = true) ||
            userMessage.contains("jeddah", ignoreCase = true) -> {
                if (userMessage.contains("search", ignoreCase = true) ||
                    userMessage.contains("find", ignoreCase = true) ||
                    userMessage.contains("need", ignoreCase = true) ||
                    userMessage.contains("want", ignoreCase = true)) {
                    // Return a tool call for flight search
                    AiChatResponse(
                        text = "",
                        toolCalls = listOf(
                            ToolCall(
                                id = "tool-call-1",
                                name = "search_flights",
                                arguments = """{"origin": "JED", "destination": "RUH", "date": "2025-12-15"}"""
                            )
                        ),
                        isComplete = false
                    )
                } else {
                    AiChatResponse(
                        text = "I can help you find flights! Where would you like to fly from and to?",
                        toolCalls = emptyList(),
                        isComplete = true
                    )
                }
            }

            // Booking lookup intent (check/manage booking without PNR should ask for it)
            userMessage.contains("booking", ignoreCase = true) ||
            userMessage.contains("pnr", ignoreCase = true) ||
            userMessage.contains("reservation", ignoreCase = true) -> {
                // Always prompt for PNR - the test case doesn't provide one
                AiChatResponse(
                    text = "I can help you with your booking. Please provide your 6-character booking reference (PNR).",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }

            // Seat change intent
            userMessage.contains("seat", ignoreCase = true) ||
            userMessage.contains("window", ignoreCase = true) ||
            userMessage.contains("aisle", ignoreCase = true) -> {
                AiChatResponse(
                    text = "I can help you change your seat. Would you prefer a window or aisle seat?",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }

            // Cancel intent
            userMessage.contains("cancel", ignoreCase = true) -> {
                AiChatResponse(
                    text = "I understand you want to cancel. Please confirm the booking reference and passenger name you'd like to cancel.",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }

            // Arabic greeting
            userMessage.contains("مرحبا", ignoreCase = true) ||
            userMessage.contains("السلام", ignoreCase = true) ||
            userMessage.contains("هلا", ignoreCase = true) -> {
                AiChatResponse(
                    text = "هلا وغلا! أنا فارس، مساعدك الذكي من فلاي أديل. كيف أقدر أساعدك اليوم؟",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }

            // Default greeting/help
            userMessage.contains("hello", ignoreCase = true) ||
            userMessage.contains("hi", ignoreCase = true) ||
            userMessage.contains("help", ignoreCase = true) -> {
                AiChatResponse(
                    text = "Hello! I'm Faris, your FareAir assistant. I can help you:\n\n" +
                           "• Search for flights\n" +
                           "• Manage your booking\n" +
                           "• Change seats\n" +
                           "• Check-in\n\n" +
                           "How can I help you today?",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }

            // Default response
            else -> {
                AiChatResponse(
                    text = "I'm here to help with your travel needs. You can ask me to search for flights, " +
                           "check on a booking, change seats, or help with check-in. What would you like to do?",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }
        }
    }

    override suspend fun continueWithToolResults(
        sessionId: String,
        toolResults: List<ToolResult>
    ): AiChatResponse {
        // Generate response based on tool results
        val toolName = toolResults.firstOrNull()?.toolCallId ?: "unknown"
        
        // Check if any results contain specific content to provide contextual response
        val hasFlightData = toolResults.any { it.result.contains("flight", ignoreCase = true) }
        val hasBookingData = toolResults.any { it.result.contains("booking", ignoreCase = true) || it.result.contains("pnr", ignoreCase = true) }

        return when {
            hasBookingData -> AiChatResponse(
                text = "I found your booking details. Would you like to make any changes to your reservation?",
                toolCalls = emptyList(),
                isComplete = true
            )
            hasFlightData -> AiChatResponse(
                text = "I found several flights for your route. Would you like me to help you book one?",
                toolCalls = emptyList(),
                isComplete = true
            )
            else -> AiChatResponse(
                text = "Based on the search results, I found some great options for you. " +
                       "Would you like me to show you the details or help you with something else?",
                toolCalls = emptyList(),
                isComplete = true
            )
        }
    }

    override suspend fun clearSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    /**
     * Get the message history for a session (for testing).
     */
    fun getSessionHistory(sessionId: String): List<String> {
        return sessions[sessionId]?.toList() ?: emptyList()
    }

    /**
     * Clear all sessions (for test cleanup).
     */
    fun clearAllSessions() {
        sessions.clear()
    }
}
