package com.fairair.ai.booking.agent

import com.fairair.koog.AIAgentGraph
import com.fairair.koog.KoogAgent
import com.fairair.contract.dto.ChatContextDto
import com.fairair.contract.dto.ChatUiType
import com.fairair.contract.dto.PendingBookingContext
import org.springframework.stereotype.Component

data class BookingAgentResult(
    val response: String,
    val uiType: ChatUiType? = null,
    val uiData: Any? = null,
    val pendingContext: PendingBookingContext? = null,
    val suggestions: List<String> = emptyList()
)

@KoogAgent
@Component
class FlightBookingAgent(
    private val flightBookingAgentGraph: AIAgentGraph
) {
    suspend fun process(userInput: String, chatContext: ChatContextDto? = null): BookingAgentResult {
        val initialContext = mutableMapOf<String, Any>("userInput" to userInput)
        if (chatContext != null) {
            initialContext["chatContext"] = chatContext
        }
        val finalContext = flightBookingAgentGraph.execute(initialContext)
        
        val response = finalContext["userResponse"] as? String ?: "I'm sorry, something went wrong."
        val uiType = finalContext["uiType"] as? ChatUiType
        val uiData = finalContext["uiData"]
        val pendingContext = finalContext["pendingContext"] as? PendingBookingContext
        
        return BookingAgentResult(response, uiType, uiData, pendingContext)
    }
}
