package com.fairair.ai.booking.agent

import com.fairair.koog.AIAgentGraph
import com.fairair.koog.KoogAgent
import com.fairair.contract.dto.ChatUiType
import org.springframework.stereotype.Component

data class BookingAgentResult(
    val response: String,
    val uiType: ChatUiType? = null,
    val uiData: Any? = null
)

@KoogAgent
@Component
class FlightBookingAgent(
    private val flightBookingAgentGraph: AIAgentGraph
) {
    suspend fun process(userInput: String): BookingAgentResult {
        val initialContext = mapOf("userInput" to userInput)
        val finalContext = flightBookingAgentGraph.execute(initialContext)
        
        val response = finalContext["userResponse"] as? String ?: "I'm sorry, something went wrong."
        val uiType = finalContext["uiType"] as? ChatUiType
        val uiData = finalContext["uiData"]
        
        return BookingAgentResult(response, uiType, uiData)
    }
}
