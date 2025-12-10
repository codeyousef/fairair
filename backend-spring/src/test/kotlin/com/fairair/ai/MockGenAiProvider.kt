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

        // Check if there's a pre-configured scenario for this session
        val scenario = nextResponses.remove(sessionId)
        if (scenario != null) {
            return handleScenario(scenario, userMessage)
        }

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
                    text = "Hello! I'm Pilot, your FareAir assistant. I can help you:\n\n" +
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

    /**
     * Handle pre-configured test scenarios.
     */
    private fun handleScenario(scenario: TestScenario, userMessage: String): AiChatResponse {
        return when (scenario) {
            is BookingConfirmationScenario -> {
                // Simulate calling create_booking tool
                AiChatResponse(
                    text = "",
                    toolCalls = listOf(
                        ToolCall(
                            id = "tool-create-booking",
                            name = "create_booking",
                            arguments = """
                                {
                                    "flight_number": "${scenario.flightNumber}",
                                    "passengers": [{
                                        "firstName": "${scenario.passengerName.split(" ").firstOrNull() ?: "Test"}",
                                        "lastName": "${scenario.passengerName.split(" ").lastOrNull() ?: "User"}",
                                        "dateOfBirth": "${scenario.dateOfBirth}",
                                        "gender": "MALE",
                                        "documentNumber": "${scenario.passportNumber}",
                                        "nationality": "SA"
                                    }]
                                }
                            """.trimIndent()
                        )
                    ),
                    isComplete = false
                )
            }
            is FlightSearchScenario -> {
                AiChatResponse(
                    text = "",
                    toolCalls = listOf(
                        ToolCall(
                            id = "tool-search-flights",
                            name = "search_flights",
                            arguments = """{"origin": "JED", "destination": "RUH", "date": "2025-12-15"}"""
                        )
                    ),
                    isComplete = false
                )
            }
            is FlightSelectedScenario -> {
                AiChatResponse(
                    text = "",
                    toolCalls = listOf(
                        ToolCall(
                            id = "tool-select-flight",
                            name = "select_flight",
                            arguments = """{"flight_number": "${scenario.flightNumber}"}"""
                        )
                    ),
                    isComplete = false
                )
            }
            is BookingSummaryScenario -> {
                AiChatResponse(
                    text = "Here's a summary of your booking:\n\n" +
                           "* Flight: ${scenario.flightNumber}\n" +
                           "* Passenger: ${scenario.passengerName}\n" +
                           "* Passport: ${scenario.passportNumber}\n\n" +
                           "Shall I proceed with this booking?",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }
            is BookingConfirmedScenario -> {
                AiChatResponse(
                    text = "Your booking is confirmed! Your PNR is ${scenario.pnr}. " +
                           "Flight ${scenario.flightNumber} has been booked successfully.",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }
            is CancellationScenario -> {
                AiChatResponse(
                    text = "No problem, I've cancelled the booking process. Is there anything else I can help you with?",
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
        // Check tool call ID to determine the response type
        val toolCallId = toolResults.firstOrNull()?.toolCallId ?: "unknown"
        val resultContent = toolResults.firstOrNull()?.result ?: ""
        
        // Check if any results contain specific content to provide contextual response
        val hasFlightData = resultContent.contains("flight", ignoreCase = true)
        val hasBookingConfirmation = resultContent.contains("pnr", ignoreCase = true) && 
                                     resultContent.contains("success", ignoreCase = true)
        val hasFlightSelected = toolCallId.contains("select", ignoreCase = true) ||
                                resultContent.contains("selected", ignoreCase = true)

        return when {
            // Booking was created successfully
            hasBookingConfirmation || toolCallId.contains("create-booking", ignoreCase = true) -> {
                // Extract PNR from result if available
                val pnrMatch = Regex(""""pnr":\s*"([A-Z0-9]+)"""").find(resultContent)
                val pnr = pnrMatch?.groupValues?.get(1) ?: "ABC123"
                AiChatResponse(
                    text = "Your booking is confirmed! Your PNR is $pnr. You can use this reference to manage your booking.",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }
            // Flight was selected
            hasFlightSelected -> {
                AiChatResponse(
                    text = "Flight selected. Let me get your passenger details.",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }
            // Flight search results
            hasFlightData -> {
                AiChatResponse(
                    text = "I found several flights for your route. Would you like me to help you book one?",
                    toolCalls = emptyList(),
                    isComplete = true
                )
            }
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
        nextResponses.remove(sessionId)
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
        nextResponses.clear()
    }

    // ==================== Test Scenario Support ====================
    
    private val nextResponses = mutableMapOf<String, TestScenario>()

    /**
     * Set the next response for a specific session.
     * Used by tests to control AI behavior.
     */
    fun setNextResponse(sessionId: String, scenario: TestScenario) {
        nextResponses[sessionId] = scenario
    }

    /**
     * Base interface for test scenarios.
     */
    sealed interface TestScenario

    /**
     * Scenario: User confirms booking → AI calls create_booking
     */
    data class BookingConfirmationScenario(
        val flightNumber: String,
        val passengerName: String,
        val passportNumber: String,
        val dateOfBirth: String
    ) : TestScenario

    /**
     * Scenario: AI performs flight search
     */
    class FlightSearchScenario : TestScenario

    /**
     * Scenario: User selects a flight
     */
    data class FlightSelectedScenario(val flightNumber: String) : TestScenario

    /**
     * Scenario: AI shows booking summary asking for confirmation
     */
    data class BookingSummaryScenario(
        val flightNumber: String,
        val passengerName: String,
        val passportNumber: String
    ) : TestScenario

    /**
     * Scenario: Booking is confirmed with PNR
     */
    data class BookingConfirmedScenario(
        val pnr: String,
        val flightNumber: String
    ) : TestScenario

    /**
     * Scenario: User cancels/declines
     */
    class CancellationScenario : TestScenario
}
