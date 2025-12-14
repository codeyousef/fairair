package com.fairair.ai.booking.executor

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import software.amazon.awssdk.core.SdkBytes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import kotlinx.coroutines.future.await

@Component
class BedrockLlamaExecutor(
    private val client: BedrockRuntimeAsyncClient,
    @Value("\${koog.primary-model}") private val modelId: String
) {
    suspend fun generate(prompt: String): String {
        val formattedPrompt = """
            <|begin_of_text|><|start_header_id|>user<|end_header_id|>
            $prompt
            <|eot_id|><|start_header_id|>assistant<|end_header_id|>
        """.trimIndent()

        val escapedPrompt = formattedPrompt.replace("\"", "\\\"").replace("\n", "\\n")

        val payload = """
            {
                "prompt": "$escapedPrompt",
                "temperature": 0.1,
                "top_p": 0.9,
                "max_gen_len": 512
            }
        """.trimIndent()

        val request = InvokeModelRequest.builder()
            .modelId(modelId)
            .body(SdkBytes.fromUtf8String(payload))
            .contentType("application/json")
            .accept("application/json")
            .build()

        val response = client.invokeModel(request).await()
        val responseBody = response.body().asUtf8String()
        
        val json = Json.parseToJsonElement(responseBody) as JsonObject
        return json["generation"]?.jsonPrimitive?.contentOrNull ?: ""
    }
}
