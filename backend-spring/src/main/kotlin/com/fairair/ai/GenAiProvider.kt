package com.fairair.ai

/**
 * Interface for AI/LLM providers.
 * Follows hexagonal architecture to allow swapping between providers (Vertex AI, Bedrock, etc.)
 */
interface GenAiProvider {
    /**
     * Send a message to the AI and get a response.
     * 
     * @param sessionId Unique session identifier for conversation continuity
     * @param userMessage The user's message
     * @param systemPrompt Optional system prompt override
     * @return The AI's response with optional tool calls
     */
    suspend fun chat(
        sessionId: String,
        userMessage: String,
        systemPrompt: String? = null
    ): AiChatResponse

    /**
     * Send a message with tool results back to the AI.
     * 
     * @param sessionId Session identifier
     * @param toolResults Results from tool executions
     * @return The AI's response after processing tool results
     */
    suspend fun continueWithToolResults(
        sessionId: String,
        toolResults: List<ToolResult>
    ): AiChatResponse

    /**
     * Clear a conversation session.
     */
    suspend fun clearSession(sessionId: String)
}

/**
 * Response from the AI provider.
 */
data class AiChatResponse(
    /** The text content of the response */
    val text: String,
    /** Tool calls requested by the AI (if any) */
    val toolCalls: List<ToolCall> = emptyList(),
    /** Whether the response is complete or partial (streaming) */
    val isComplete: Boolean = true,
    /** Stop reason if applicable */
    val stopReason: String? = null
)

/**
 * A tool call requested by the AI.
 */
data class ToolCall(
    /** Unique ID for this tool call */
    val id: String,
    /** Name of the tool to invoke */
    val name: String,
    /** Arguments as JSON string */
    val arguments: String
)

/**
 * Result of a tool execution to send back to the AI.
 */
data class ToolResult(
    /** The tool call ID this result corresponds to */
    val toolCallId: String,
    /** The result content (usually JSON) */
    val result: String,
    /** Whether the tool execution was successful */
    val isError: Boolean = false
)
