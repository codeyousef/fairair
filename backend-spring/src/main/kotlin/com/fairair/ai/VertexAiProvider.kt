package com.fairair.ai

import com.fairair.config.FairairProperties
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.time.Duration
import java.util.UUID

/**
 * Vertex AI implementation of the GenAiProvider.
 * Uses Llama 3.1 70B Instruct via Vertex AI MaaS (Model as a Service) OpenAI-compatible API.
 */
@Service
@ConditionalOnProperty(
    name = ["fairair.ai.enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class VertexAiProvider(
    private val config: FairairProperties
) : GenAiProvider {

    private val log = LoggerFactory.getLogger(VertexAiProvider::class.java)

    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Chat message in OpenAI format.
     */
    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String
    )

    /**
     * Conversation state per session.
     */
    private data class ConversationState(
        val messages: MutableList<ChatMessage> = mutableListOf(),
        val systemPrompt: String
    )

    // Session cache for conversation history
    private val sessionCache: Cache<String, ConversationState> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(config.ai.sessionTimeoutSeconds))
        .maximumSize(1000)
        .build()

    // Google credentials for authentication
    private val credentials: GoogleCredentials by lazy {
        log.info("Initializing Google credentials for Vertex AI")
        GoogleCredentials.getApplicationDefault()
            .createScoped(listOf("https://www.googleapis.com/auth/cloud-platform"))
    }

    // WebClient for Vertex AI REST API
    private val webClient: WebClient by lazy {
        log.info("Initializing WebClient for Vertex AI MaaS API")
        WebClient.builder()
            .baseUrl("https://${config.ai.location}-aiplatform.googleapis.com")
            .build()
    }

    /**
     * Get the Vertex AI endpoint URL for Llama MaaS.
     * Uses the OpenAI-compatible chat completions endpoint.
     */
    private fun getEndpointUrl(): String {
        return "/v1beta1/projects/${config.ai.projectId}/locations/${config.ai.location}/endpoints/openapi/chat/completions"
    }

    /**
     * Get a fresh access token for API calls.
     */
    private fun getAccessToken(): String {
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    private fun createSystemPrompt(): String {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.of("Asia/Riyadh"))
            .date
            .toString()
        return PilotPrompts.createSystemPrompt(today) + "\n\n" + createToolsPrompt()
    }

    /**
     * Convert a Map<String, Any> to JsonElement recursively.
     */
    @Suppress("UNCHECKED_CAST")
    private fun mapToJsonElement(map: Map<String, Any>): JsonElement {
        return buildJsonObject {
            map.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is Map<*, *> -> put(key, mapToJsonElement(value as Map<String, Any>))
                    is List<*> -> put(key, buildJsonArray {
                        value.forEach { item ->
                            when (item) {
                                is String -> add(item)
                                is Number -> add(item)
                                is Boolean -> add(item)
                                is Map<*, *> -> add(mapToJsonElement(item as Map<String, Any>))
                                else -> add(item.toString())
                            }
                        }
                    })
                    else -> put(key, value.toString())
                }
            }
        }
    }

    /**
     * Create a tools description for the system prompt.
     * Since Llama doesn't have native function calling like Gemini/Claude,
     * we inject tool definitions into the system prompt.
     */
    private fun createToolsPrompt(): String {
        val toolsJson = PilotTools.allTools.map { tool ->
            buildJsonObject {
                put("name", tool.name)
                put("description", tool.description)
                put("parameters", mapToJsonElement(tool.parameters))
            }
        }
        
        return """
## Available Tools

You have access to the following tools. When you need to use a tool, respond with ONLY a JSON block like this:

```json
{"name": "tool_name", "arguments": {"arg1": "value1"}}
```

Available tools:
${json.encodeToString(toolsJson)}

CRITICAL RULES:
1. When you need to call a tool, output ONLY the json block - no other text before or after.
2. Do NOT show the tool call JSON to the user in your final response.
3. After receiving tool results, respond naturally in the user's language.
4. NEVER include raw JSON in your response to users - always use friendly natural language.
5. NEVER mention tool names, internal logic, or explain what you're doing behind the scenes.
6. For flight selections: When a user says "Select flight X", acknowledge their choice and offer next steps (extras like bags/meals, or proceed to booking).
7. Do NOT tell users to "tap on the flight card" after they've already selected a flight.
8. **BOOKING CONFIRMATIONS**: When user says "yes"/"ok"/"confirm" after you showed a booking summary with passenger details, you MUST call create_booking immediately. DO NOT call search_flights again. DO NOT show flights again.
""".trimIndent()
    }

    override suspend fun chat(
        sessionId: String,
        userMessage: String,
        systemPrompt: String?
    ): AiChatResponse = withContext(Dispatchers.IO) {
        log.info("Chat request for session $sessionId: ${userMessage.take(100)}...")
        
        try {
            // Get or create conversation state
            val state = sessionCache.get(sessionId) { 
                ConversationState(
                    messages = mutableListOf(),
                    systemPrompt = systemPrompt ?: createSystemPrompt()
                )
            }!!
            
            // Add user message to history
            state.messages.add(ChatMessage(role = "user", content = userMessage))
            
            // Call the Llama model via MaaS API
            val response = callLlamaModel(state)
            
            // Add assistant response to history
            state.messages.add(ChatMessage(role = "assistant", content = response.text))
            
            response
            
        } catch (e: Exception) {
            log.error("Error in chat for session $sessionId", e)
            throw e
        }
    }

    /**
     * Call the Llama model via Vertex AI MaaS OpenAI-compatible API.
     */
    private suspend fun callLlamaModel(state: ConversationState): AiChatResponse {
        // Build the messages array for OpenAI-compatible API
        val messagesArray = buildJsonArray {
            // System message first
            add(buildJsonObject {
                put("role", "system")
                put("content", state.systemPrompt)
            })
            
            // Add conversation history
            for (message in state.messages) {
                add(buildJsonObject {
                    put("role", message.role)
                    put("content", message.content)
                })
            }
        }
        
        // Create the request payload for OpenAI-compatible API
        val requestPayload = buildJsonObject {
            put("model", "meta/${config.ai.model}")
            put("messages", messagesArray)
            put("max_tokens", config.ai.maxTokens)
            put("temperature", config.ai.temperature)
            put("stream", false)
        }
        
        log.debug("Calling Llama MaaS with ${state.messages.size} messages")
        
        try {
            val responseBody = webClient.post()
                .uri(getEndpointUrl())
                .header("Authorization", "Bearer ${getAccessToken()}")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json.encodeToString(requestPayload))
                .retrieve()
                .bodyToMono<String>()
                .awaitSingle()
            
            log.debug("Received response from Vertex AI: ${responseBody.take(200)}...")
            
            // Parse the OpenAI-compatible response
            val responseJson = json.parseToJsonElement(responseBody).jsonObject
            val choices = responseJson["choices"]?.jsonArray
            val firstChoice = choices?.firstOrNull()?.jsonObject
            val messageObj = firstChoice?.get("message")?.jsonObject
            val responseText = messageObj?.get("content")?.jsonPrimitive?.content ?: ""
            
            // Parse for tool calls
            return parseResponse(responseText)
            
        } catch (e: Exception) {
            log.error("Vertex AI API error: ${e.message}", e)
            throw RuntimeException("Failed to call AI model: ${e.message}", e)
        }
    }

    /**
     * Parse the response text and extract any tool calls.
     * Supports:
     * 1. ```tool_call or ```json code blocks containing tool calls
     * 2. Bare JSON objects with "name" and "arguments" fields
     */
    private fun parseResponse(responseText: String): AiChatResponse {
        // First, look for code block format
        val codeBlockPattern = Regex("""```(?:tool_call|json)?\s*\n?(.+?)\n?```""", RegexOption.DOT_MATCHES_ALL)
        val codeBlockMatch = codeBlockPattern.find(responseText)
        
        if (codeBlockMatch != null) {
            val parsed = tryParseToolCall(codeBlockMatch.groupValues[1].trim())
            if (parsed != null) {
                return AiChatResponse(
                    text = responseText.replace(codeBlockMatch.value, "").trim(),
                    toolCalls = listOf(parsed),
                    isComplete = false
                )
            }
        }
        
        // If no code block, look for bare JSON that looks like a tool call
        // Find JSON by looking for {"name": and extracting balanced braces
        val toolCallJson = extractToolCallJson(responseText)
        if (toolCallJson != null) {
            val parsed = tryParseToolCall(toolCallJson)
            if (parsed != null) {
                log.info("Detected bare JSON tool call: ${parsed.name}")
                return AiChatResponse(
                    text = responseText.replace(toolCallJson, "").trim(),
                    toolCalls = listOf(parsed),
                    isComplete = false
                )
            }
        }
        
        return AiChatResponse(
            text = responseText.trim(),
            toolCalls = emptyList(),
            isComplete = true
        )
    }
    
    /**
     * Extract a JSON tool call object from text, handling nested braces.
     * Looks for {"name": and extracts the balanced JSON object.
     */
    private fun extractToolCallJson(text: String): String? {
        // Find the start of a potential tool call
        val namePattern = Regex("""\{"name"\s*:""")
        val match = namePattern.find(text) ?: return null
        
        val startIdx = match.range.first
        var braceCount = 0
        var endIdx = startIdx
        
        // Walk through the text counting braces
        for (i in startIdx until text.length) {
            when (text[i]) {
                '{' -> braceCount++
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        endIdx = i + 1
                        break
                    }
                }
            }
        }
        
        return if (braceCount == 0 && endIdx > startIdx) {
            text.substring(startIdx, endIdx)
        } else {
            null
        }
    }
    
    /**
     * Try to parse a JSON string as a tool call.
     */
    private fun tryParseToolCall(jsonText: String): ToolCall? {
        return try {
            val toolCallObj = json.parseToJsonElement(jsonText).jsonObject
            val toolName = toolCallObj["name"]?.jsonPrimitive?.content ?: return null
            val arguments = toolCallObj["arguments"]?.toString() ?: "{}"
            
            ToolCall(
                id = UUID.randomUUID().toString(),
                name = toolName,
                arguments = arguments
            )
        } catch (e: Exception) {
            log.debug("Failed to parse as tool call: ${e.message}")
            null
        }
    }

    override suspend fun continueWithToolResults(
        sessionId: String,
        toolResults: List<ToolResult>
    ): AiChatResponse = withContext(Dispatchers.IO) {
        log.info("Continuing with ${toolResults.size} tool results for session $sessionId")
        
        val state = sessionCache.getIfPresent(sessionId) 
            ?: throw IllegalStateException("Session $sessionId not found")
        
        try {
            // Format tool results as a message
            val toolResultsText = toolResults.joinToString("\n") { result ->
                """Tool result for ${result.toolCallId}:
${result.result}
${if (result.isError) "(Error occurred)" else ""}"""
            }
            
            // Add tool results as a user message (since Llama doesn't have native tool support)
            state.messages.add(ChatMessage(
                role = "user",
                content = """Tool execution results:
$toolResultsText

IMPORTANT: Use the ACTUAL DATA from the tool results above. Do NOT output placeholder text like "[list of travelers]". 
Extract the real names, passport numbers, and details from the JSON above and present them naturally to the user.
If get_saved_travelers returned travelers, list them BY NAME (e.g., "Jane Doe, Ahmed Doe").
If the user says "me" or "just me", use the traveler marked as isMainTraveler=true."""
            ))
            
            // Call the model again
            val response = callLlamaModel(state)
            
            // Add assistant response to history
            state.messages.add(ChatMessage(role = "assistant", content = response.text))
            
            response
            
        } catch (e: Exception) {
            log.error("Error continuing with tool results for session $sessionId", e)
            throw e
        }
    }

    override suspend fun clearSession(sessionId: String) {
        log.info("Clearing session $sessionId")
        sessionCache.invalidate(sessionId)
    }
}
