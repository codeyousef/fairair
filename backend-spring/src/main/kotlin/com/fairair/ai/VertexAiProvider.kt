package com.fairair.ai

import com.fairair.config.FairairProperties
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.cloud.vertexai.VertexAI
import com.google.cloud.vertexai.api.*
import com.google.cloud.vertexai.generativeai.GenerativeModel
import com.google.cloud.vertexai.generativeai.ContentMaker
import com.google.cloud.vertexai.generativeai.ChatSession
import com.google.protobuf.Struct
import com.google.protobuf.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Duration

/**
 * Vertex AI implementation of the GenAiProvider.
 * Uses Claude 3.5 Sonnet via Vertex AI for the Faris assistant.
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
    }

    // Session cache for chat sessions
    private val sessionCache: Cache<String, ChatSession> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(config.ai.sessionTimeoutSeconds))
        .maximumSize(1000)
        .build()

    // Lazy initialization of Vertex AI client
    private val vertexAi: VertexAI by lazy {
        log.info("Initializing Vertex AI client for project: ${config.ai.projectId}, location: ${config.ai.location}")
        VertexAI(config.ai.projectId, config.ai.location)
    }

    private val generativeModel: GenerativeModel by lazy {
        log.info("Creating generative model: ${config.ai.model}")
        
        val generationConfig = GenerationConfig.newBuilder()
            .setMaxOutputTokens(config.ai.maxTokens)
            .setTemperature(config.ai.temperature.toFloat())
            .build()

        GenerativeModel.Builder()
            .setModelName(config.ai.model)
            .setVertexAi(vertexAi)
            .setGenerationConfig(generationConfig)
            .setSystemInstruction(ContentMaker.fromString(createSystemPrompt()))
            .setTools(listOf(createToolsDefinition()))
            .build()
    }

    private fun createSystemPrompt(): String {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.of("Asia/Riyadh"))
            .date
            .toString()
        return FarisPrompts.createSystemPrompt(today)
    }

    private fun createToolsDefinition(): Tool {
        val functionDeclarations = FarisTools.allTools.map { tool ->
            createFunctionDeclaration(tool)
        }
        
        return Tool.newBuilder()
            .addAllFunctionDeclarations(functionDeclarations)
            .build()
    }

    private fun createFunctionDeclaration(tool: ToolDefinition): FunctionDeclaration {
        val parametersSchema = convertToSchema(tool.parameters)
        
        return FunctionDeclaration.newBuilder()
            .setName(tool.name)
            .setDescription(tool.description)
            .setParameters(parametersSchema)
            .build()
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertToSchema(params: Map<String, Any>): Schema {
        val builder = Schema.newBuilder()
        
        val type = params["type"] as? String
        if (type == "object") {
            builder.setType(Type.OBJECT)
        }
        
        val properties = params["properties"] as? Map<String, Map<String, Any>>
        properties?.forEach { (name, propDef) ->
            val propSchema = Schema.newBuilder()
            when (propDef["type"]) {
                "string" -> propSchema.setType(Type.STRING)
                "integer" -> propSchema.setType(Type.INTEGER)
                "number" -> propSchema.setType(Type.NUMBER)
                "boolean" -> propSchema.setType(Type.BOOLEAN)
            }
            propDef["description"]?.let { propSchema.setDescription(it as String) }
            builder.putProperties(name, propSchema.build())
        }
        
        val required = params["required"] as? List<String>
        required?.let { builder.addAllRequired(it) }
        
        return builder.build()
    }

    override suspend fun chat(
        sessionId: String,
        userMessage: String,
        systemPrompt: String?
    ): AiChatResponse = withContext(Dispatchers.IO) {
        log.info("Chat request for session $sessionId: ${userMessage.take(100)}...")
        
        try {
            // Get or create chat session
            val chatSession = sessionCache.get(sessionId) { 
                generativeModel.startChat()
            }!!
            
            // Send message and get response
            val response = chatSession.sendMessage(userMessage)
            
            // Process response
            processResponse(response)
            
        } catch (e: Exception) {
            log.error("Error in chat for session $sessionId", e)
            throw e
        }
    }

    override suspend fun continueWithToolResults(
        sessionId: String,
        toolResults: List<ToolResult>
    ): AiChatResponse = withContext(Dispatchers.IO) {
        log.info("Continuing with ${toolResults.size} tool results for session $sessionId")
        
        val chatSession = sessionCache.getIfPresent(sessionId) 
            ?: throw IllegalStateException("Session $sessionId not found")
        
        try {
            // Create function response content
            val functionResponses = toolResults.map { result ->
                val responseStruct = Struct.newBuilder()
                    .putFields("result", Value.newBuilder().setStringValue(result.result).build())
                    .putFields("is_error", Value.newBuilder().setBoolValue(result.isError).build())
                    .build()
                
                Part.newBuilder()
                    .setFunctionResponse(
                        FunctionResponse.newBuilder()
                            .setName(result.toolCallId)
                            .setResponse(responseStruct)
                            .build()
                    )
                    .build()
            }
            
            val toolResultContent = Content.newBuilder()
                .setRole("function")
                .addAllParts(functionResponses)
                .build()
            
            // Send function responses
            val response = chatSession.sendMessage(toolResultContent)
            
            // Process response
            processResponse(response)
            
        } catch (e: Exception) {
            log.error("Error continuing with tool results for session $sessionId", e)
            throw e
        }
    }

    override suspend fun clearSession(sessionId: String) {
        log.info("Clearing session $sessionId")
        sessionCache.invalidate(sessionId)
    }

    private fun processResponse(response: GenerateContentResponse): AiChatResponse {
        if (response.candidatesList.isEmpty()) {
            return AiChatResponse(
                text = "I apologize, but I couldn't generate a response. Please try again.",
                isComplete = true
            )
        }
        
        val candidate = response.candidatesList[0]
        val content = candidate.content
        
        val textParts = mutableListOf<String>()
        val toolCalls = mutableListOf<ToolCall>()
        
        content.partsList.forEach { part ->
            if (part.hasText()) {
                textParts.add(part.text)
            }
            if (part.hasFunctionCall()) {
                val funcCall = part.functionCall
                toolCalls.add(
                    ToolCall(
                        id = funcCall.name, // Using name as ID for Vertex AI
                        name = funcCall.name,
                        arguments = structToJson(funcCall.args)
                    )
                )
            }
        }
        
        val stopReason = candidate.finishReason?.name
        
        return AiChatResponse(
            text = textParts.joinToString("\n"),
            toolCalls = toolCalls,
            isComplete = toolCalls.isEmpty(),
            stopReason = stopReason
        )
    }

    private fun structToJson(struct: Struct): String {
        val map = struct.fieldsMap.mapValues { (_, value) ->
            when (value.kindCase) {
                Value.KindCase.STRING_VALUE -> JsonPrimitive(value.stringValue)
                Value.KindCase.NUMBER_VALUE -> JsonPrimitive(value.numberValue)
                Value.KindCase.BOOL_VALUE -> JsonPrimitive(value.boolValue)
                Value.KindCase.NULL_VALUE -> JsonNull
                else -> JsonPrimitive(value.toString())
            }
        }
        return json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), 
            kotlinx.serialization.json.JsonObject(map))
    }
}
