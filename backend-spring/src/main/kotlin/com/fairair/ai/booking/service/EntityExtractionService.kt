package com.fairair.ai.booking.service

import com.fairair.ai.booking.exception.EntityExtractionException
import com.fairair.ai.booking.exception.RouteValidationException
import com.fairair.ai.booking.executor.BedrockLlamaExecutor
import com.fairair.ai.booking.executor.LocalModelExecutor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EntityExtractionService(
    private val referenceDataService: ReferenceDataService,
    private val levenshteinMatcher: LevenshteinMatcher,
    private val bedrockExecutor: BedrockLlamaExecutor,
    private val localExecutor: LocalModelExecutor
) {
    private val logger = LoggerFactory.getLogger(EntityExtractionService::class.java)
    private val jsonParser = Json { ignoreUnknownKeys = true }

    suspend fun extractAndValidate(userInput: String): Map<String, Any> {
        logger.debug("Extracting entities from: $userInput")
        
        // Step 1: LLM Extraction of Raw Entities
        val rawEntities = extractRawEntities(userInput)
        
        val rawOrigin = rawEntities["origin"]?.jsonPrimitive?.contentOrNull
        val rawDest = rawEntities["destination"]?.jsonPrimitive?.contentOrNull
        val rawDate = rawEntities["date"]?.jsonPrimitive?.contentOrNull
        val passengers = rawEntities["passengers"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        
        if (rawOrigin == null || rawDest == null || rawDate == null) {
             throw EntityExtractionException("Missing required entities in input. Please specify origin, destination, and date.")
        }
        
        // Step 2 & 3: Resolution (Direct -> Fuzzy -> LLM)
        val originCode = resolveCityCode(rawOrigin) 
        val destCode = resolveCityCode(rawDest)
        
        // Validation
        if (!referenceDataService.isValidRoute(originCode, destCode)) {
            throw RouteValidationException("Route $originCode-$destCode is not valid")
        }
        
        return mapOf(
            "origin" to originCode,
            "destination" to destCode,
            "date" to rawDate,
            "passengers" to passengers
        )
    }

    private suspend fun extractRawEntities(userInput: String): JsonObject {
        val llmPrompt = """
            Extract origin, destination, date (YYYY-MM-DD), and passengers count from: "$userInput".
            Return JSON: {"origin": "string", "destination": "string", "date": "string", "passengers": "int"}
        """.trimIndent()

        val jsonStrRaw = try {
            bedrockExecutor.generate(llmPrompt + "\nRespond with valid JSON only.")
        } catch (e: Exception) {
            logger.warn("Primary model failed, using local backup", e)
            localExecutor.generate(llmPrompt)
        }

        val jsonStr = jsonStrRaw.replace("```json", "").replace("```", "").trim()
        
        return try {
            jsonParser.parseToJsonElement(jsonStr) as JsonObject
        } catch (e: Exception) {
             throw EntityExtractionException("Failed to parse LLM output: $jsonStr")
        }
    }

    private suspend fun resolveCityCode(raw: String): String {
        // Stage 1: Direct Lookup
        referenceDataService.getCodeForAlias(raw)?.let { return it }
        
        // Stage 2: Fuzzy Matching
        levenshteinMatcher.findClosestMatch(raw)?.let { return it }
        
        // Stage 3: LLM Disambiguation
        // If regular extraction failed to find a code, ask LLM explicitly for the code
        return disambiguateWithLlama(raw)
    }
    
    private suspend fun disambiguateWithLlama(raw: String): String {
        logger.info("Attempting to disambiguate '$raw' via LLM")
        val validCodes = referenceDataService.getAllAliases().joinToString(", ")
        // We only provide codes, not all aliases, to keep prompt short? 
        // Or maybe asking "What is the IATA code for 'raw'?"
        
        val prompt = """
            Identify the 3-letter IATA airport code for: "$raw".
            Return ONLY the 3-letter code. If unknown, return "UNKNOWN".
        """.trimIndent()
        
        val code = try {
            bedrockExecutor.generate(prompt).trim().uppercase().take(3)
        } catch (e: Exception) {
            "UNKNOWN"
        }
        
        // Validate the LLM output against our valid codes
        // We can check if the returned code exists in our system
        // We can use referenceDataService.getCityCodesMap() to check keys?
        // Wait, alias map values are codes.
        
        // Let's check if the returned code is a valid destination in our DB
        // But I don't have a simple "isValidCode" method. 
        // I can use `referenceDataService.getCityCodesMap().containsKey(code)`? No that's code->aliases.
        
        if (referenceDataService.getCityCodesMap().containsKey(code)) {
            return code
        }
        
        throw EntityExtractionException("Could not resolve city: $raw")
    }
}
