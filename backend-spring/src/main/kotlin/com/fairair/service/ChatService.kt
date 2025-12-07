package com.fairair.service

import com.fairair.ai.AiChatResponse
import com.fairair.ai.GenAiProvider
import com.fairair.ai.ToolCall
import com.fairair.ai.ToolResult
import com.fairair.contract.dto.*
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service that orchestrates AI chat interactions.
 * Handles the conversation flow, tool execution, and response formatting.
 */
@Service
class ChatService(
    private val aiProvider: GenAiProvider,
    private val toolExecutor: AiToolExecutor,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(ChatService::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    /**
     * Process a chat message from the user.
     * Handles tool calls in a loop until the AI provides a final response.
     */
    suspend fun processMessage(request: ChatMessageRequestDto): ChatResponseDto {
        log.info("Processing chat message for session ${request.sessionId}")
        
        var response = aiProvider.chat(
            sessionId = request.sessionId,
            userMessage = request.message
        )
        
        var iterations = 0
        val maxIterations = 5 // Prevent infinite loops
        
        // Track the last tool execution result for UI metadata
        var lastToolResult: ToolExecutionResult? = null
        
        // Process tool calls until we get a final response
        while (response.toolCalls.isNotEmpty() && iterations < maxIterations) {
            iterations++
            log.info("Processing ${response.toolCalls.size} tool calls (iteration $iterations)")
            
            val (toolResults, toolExecutionResults) = executeToolCalls(response.toolCalls, request.context)
            
            // Keep the last tool execution result for UI data
            lastToolResult = toolExecutionResults.lastOrNull { it.uiType != null } ?: lastToolResult
            
            response = aiProvider.continueWithToolResults(
                sessionId = request.sessionId,
                toolResults = toolResults
            )
        }
        
        if (iterations >= maxIterations) {
            log.warn("Max tool iterations reached for session ${request.sessionId}")
        }
        
        return createChatResponse(response, request.locale, lastToolResult)
    }

    /**
     * Execute tool calls and return results.
     * Returns both the ToolResult list (for AI) and ToolExecutionResult list (for UI metadata).
     */
    private suspend fun executeToolCalls(
        toolCalls: List<ToolCall>,
        context: ChatContextDto?
    ): Pair<List<ToolResult>, List<ToolExecutionResult>> {
        val toolResults = mutableListOf<ToolResult>()
        val executionResults = mutableListOf<ToolExecutionResult>()
        
        for (call in toolCalls) {
            try {
                log.info("Executing tool: ${call.name}")
                val result = toolExecutor.execute(call.name, call.arguments, context)
                executionResults.add(result)
                toolResults.add(ToolResult(
                    toolCallId = call.id,
                    result = result.toJson(),
                    isError = false
                ))
            } catch (e: Exception) {
                log.error("Tool execution failed for ${call.name}", e)
                toolResults.add(ToolResult(
                    toolCallId = call.id,
                    result = json.encodeToString(mapOf("error" to (e.message ?: "Unknown error"))),
                    isError = true
                ))
            }
        }
        
        return Pair(toolResults, executionResults)
    }

    /**
     * Create the final chat response with UI payload if applicable.
     */
    private fun createChatResponse(
        aiResponse: AiChatResponse,
        locale: String?,
        toolResult: ToolExecutionResult? = null
    ): ChatResponseDto {
        // Extract UI payload from tool execution result
        val uiType = toolResult?.uiType
        val uiData = toolResult?.let { serializeUiData(it.data) }
        
        // Generate suggestions based on context
        val suggestions = generateSuggestions(aiResponse, locale, uiType)
        
        return ChatResponseDto(
            text = aiResponse.text,
            uiType = uiType,
            uiData = uiData,
            suggestions = suggestions,
            isPartial = !aiResponse.isComplete,
            detectedLanguage = detectLanguage(aiResponse.text)
        )
    }

    /**
     * Serialize UI data to JSON string using Jackson ObjectMapper.
     */
    private fun serializeUiData(data: Any?): String? {
        if (data == null) return null
        return try {
            objectMapper.writeValueAsString(data)
        } catch (e: Exception) {
            log.error("Failed to serialize UI data", e)
            null
        }
    }

    /**
     * Generate quick reply suggestions based on context.
     */
    private fun generateSuggestions(
        response: AiChatResponse, 
        locale: String?,
        uiType: ChatUiType?
    ): List<String> {
        val isArabic = locale?.startsWith("ar") == true
        
        // Context-aware suggestions based on UI type
        return when (uiType) {
            ChatUiType.FLIGHT_LIST -> if (isArabic) {
                listOf("أريد الرحلة الأولى", "أظهر المزيد", "بحث مختلف")
            } else {
                listOf("I'll take the first one", "Show more options", "Different search")
            }
            ChatUiType.BOOKING_SUMMARY -> if (isArabic) {
                listOf("غير المقعد", "إلغاء الحجز", "تسجيل الدخول")
            } else {
                listOf("Change my seat", "Cancel booking", "Check in")
            }
            ChatUiType.SEAT_MAP -> if (isArabic) {
                listOf("مقعد نافذة", "مقعد ممر", "إلغاء")
            } else {
                listOf("Window seat", "Aisle seat", "Cancel")
            }
            ChatUiType.BOARDING_PASS -> if (isArabic) {
                listOf("أظهر الحجز", "مساعدة")
            } else {
                listOf("Show my booking", "Help")
            }
            else -> if (isArabic) {
                listOf("بحث عن رحلة", "إدارة حجز", "تسجيل دخول")
            } else {
                listOf("Search flights", "Manage booking", "Check in")
            }
        }
    }

    /**
     * Simple language detection based on character ranges.
     */
    private fun detectLanguage(text: String): String {
        val arabicPattern = Regex("[\\u0600-\\u06FF]")
        val arabicCount = arabicPattern.findAll(text).count()
        val totalChars = text.filter { it.isLetter() }.length
        
        return if (totalChars > 0 && arabicCount.toFloat() / totalChars > 0.3f) {
            "ar"
        } else {
            "en"
        }
    }

    /**
     * Clear a chat session.
     */
    suspend fun clearSession(sessionId: String) {
        log.info("Clearing session $sessionId")
        aiProvider.clearSession(sessionId)
    }

    /**
     * Get chat history for a session.
     * Note: Currently returns empty list as history is managed by the AI provider.
     * In a production system, we'd store messages separately for history retrieval.
     */
    suspend fun getHistory(sessionId: String): List<ChatResponseDto> {
        log.info("Getting history for session $sessionId")
        // History is maintained in the AI provider's session cache
        // For now, return empty list - frontend maintains its own message history
        return emptyList()
    }

    /**
     * Create a new chat session.
     */
    fun createSession(): ChatSessionDto {
        val sessionId = java.util.UUID.randomUUID().toString()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
        
        return ChatSessionDto(
            sessionId = sessionId,
            messages = emptyList(),
            createdAt = now,
            lastActivityAt = now
        )
    }
}

/**
 * Result from tool execution.
 */
data class ToolExecutionResult(
    val data: Any?,
    val uiType: ChatUiType? = null,
    val uiData: Any? = null
) {
    fun toJson(): String {
        val json = Json { encodeDefaults = true; prettyPrint = false }
        return when (data) {
            is String -> data
            null -> "{}"
            else -> {
                // Attempt to serialize, fall back to toString
                try {
                    @Suppress("UNCHECKED_CAST")
                    json.encodeToString(
                        kotlinx.serialization.serializer<Map<String, Any?>>(),
                        data as Map<String, Any?>
                    )
                } catch (e: Exception) {
                    data.toString()
                }
            }
        }
    }
}
