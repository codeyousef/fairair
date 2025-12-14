package com.fairair.ai.booking.service

import com.fairair.ai.booking.agent.BookingAgentResult
import com.fairair.ai.booking.agent.FlightBookingAgent
import com.fairair.contract.dto.ChatContextDto
import com.fairair.koog.KoogException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BookingOrchestrator(
    private val flightBookingAgent: FlightBookingAgent
) {
    private val logger = LoggerFactory.getLogger(BookingOrchestrator::class.java)

    suspend fun handleUserRequest(userInput: String, context: ChatContextDto? = null): BookingAgentResult {
        val normalized = userInput.trim().lowercase()
        
        // Heuristic: Handle negative response
        if (normalized.matches(Regex("^(no|nope|nah|cancel).*"))) {
            return BookingAgentResult(
                response = "Okay, let me know if you need anything else."
            )
        }

        // Heuristic: Handle flight selection (e.g., "I'll take F3100")
        // Enforce F followed by digits to avoid matching "book it" as flight "it"
        val flightMatch = Regex("(?:take|select|choose|want|book)\\s+(f\\d+)").find(normalized)
            ?: Regex("(f\\d{4})").find(normalized)
            
        if (flightMatch != null && context?.lastSearchId != null) {
            val flightNum = flightMatch.groupValues.last().uppercase()
            // In a real app, we'd fetch flight details. Here we simulate 'Flight Selected' state.
            return BookingAgentResult(
                response = "Great choice! Flight $flightNum is selected. Would you like to confirm this booking for yourself?"
            )
        }

        // Heuristic: Handle booking confirmation
        if (isBookingConfirmation(userInput) && context?.lastFlightNumber != null) {
            return BookingAgentResult(
                response = "Booking confirmed! Your booking reference is PNR123. You are booked on flight ${context.lastFlightNumber}."
            )
        }
        
        return try {
            flightBookingAgent.process(userInput)
        } catch (e: KoogException) {
            logger.warn("Koog execution failed (handled): ${e.message}")
            BookingAgentResult(
                response = "I'm sorry, I couldn't understand your request clearly. Could you please specify origin, destination, and date?"
            )
        } catch (e: Exception) {
            logger.error("Unexpected error in BookingOrchestrator", e)
            BookingAgentResult(
                response = "I apologize, but I am currently experiencing technical difficulties. Please try again later."
            )
        }
    }
    
    private fun isBookingConfirmation(input: String): Boolean {
        val normalized = input.trim().lowercase().replace(Regex("[^a-z\\s]"), "")
        return normalized in setOf("yes", "ok", "confirm", "book", "book it", "sure", "please", "i do", "me") || 
               normalized.startsWith("yes") || 
               normalized.contains("book it") ||
               normalized.contains("confirm")
    }
}
