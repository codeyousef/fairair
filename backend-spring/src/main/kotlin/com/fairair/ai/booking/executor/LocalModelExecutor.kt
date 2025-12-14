package com.fairair.ai.booking.executor

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Component
class LocalModelExecutor(
    private val webClientBuilder: WebClient.Builder
) {
    private val logger = LoggerFactory.getLogger(LocalModelExecutor::class.java)
    private val client = webClientBuilder.baseUrl("http://localhost:11434").build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generate(prompt: String): String {
        try {
            val request = OllamaRequest(
                model = "gemma2:2b",
                prompt = prompt,
                stream = false
            )
            
            val responseString = client.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .awaitBody<String>()
                
            val response = json.decodeFromString<OllamaResponse>(responseString)
            return response.response
        } catch (e: Exception) {
            logger.error("Local model execution failed", e)
            return "Please connect to the internet for complex searches."
        }
    }
}

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean
)

@Serializable
data class OllamaResponse(
    val response: String
)
