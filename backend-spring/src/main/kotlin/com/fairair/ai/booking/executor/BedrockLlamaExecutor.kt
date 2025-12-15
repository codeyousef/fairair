package com.fairair.ai.booking.executor

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import software.amazon.awssdk.core.SdkBytes
import kotlinx.serialization.json.*
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import kotlinx.coroutines.future.await
import org.slf4j.LoggerFactory

@Component
class BedrockLlamaExecutor(
    private val client: BedrockRuntimeAsyncClient,
    @Value("\${koog.primary-model}") private val modelId: String
) {
    private val logger = LoggerFactory.getLogger(BedrockLlamaExecutor::class.java)
    
    suspend fun generate(prompt: String): String {
        // Llama 3 on Bedrock uses the standard Llama prompt format
        val formattedPrompt = """
<|begin_of_text|><|start_header_id|>user<|end_header_id|}

$prompt<|eot_id|><|start_header_id|>assistant<|end_header_id|>

""".trimIndent()

        // Build JSON payload for Llama 3 model
        val payload = buildJsonObject {
            put("prompt", formattedPrompt)
            put("max_gen_len", 512)
            put("temperature", 0.1)
            put("top_p", 0.9)
        }.toString()

        logger.debug("Invoking Bedrock model: $modelId")

        val request = InvokeModelRequest.builder()
            .modelId(modelId)
            .body(SdkBytes.fromUtf8String(payload))
            .contentType("application/json")
            .accept("application/json")
            .build()

        val response = client.invokeModel(request).await()
        val responseBody = response.body().asUtf8String()
        
        logger.debug("Bedrock response: $responseBody")
        
        val json = Json.parseToJsonElement(responseBody) as JsonObject
        return json["generation"]?.jsonPrimitive?.contentOrNull ?: ""
    }
}
