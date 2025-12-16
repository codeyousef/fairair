package com.fairair.ai.booking.service

import com.fairair.ai.booking.exception.EntityExtractionException
import com.fairair.ai.booking.exception.RouteValidationException
import com.fairair.ai.booking.executor.BedrockLlamaExecutor
import com.fairair.ai.booking.executor.LocalModelExecutor
import com.fairair.contract.dto.ChatContextDto
import com.fairair.contract.dto.PendingBookingContext
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

    /**
     * Extracts and validates booking entities from user input.
     * Uses context from previous messages for conversation continuity.
     */
    suspend fun extractAndValidate(userInput: String, context: ChatContextDto? = null): Map<String, Any> {
        logger.info("Extracting entities from: '$userInput', pendingOrigin=${context?.pendingOrigin}, pendingDest=${context?.pendingDestination}, pendingDate=${context?.pendingDate}")
        
        // Build pending context for LLM hint
        val pendingCtx = context?.let {
            PendingBookingContext(it.pendingOrigin, it.pendingDestination, it.pendingDate, it.pendingPassengers)
        }
        
        // Step 1: LLM Extraction of Raw Entities from current message (with context hint)
        val rawEntities = extractRawEntities(userInput, pendingCtx)
        
        // Extract values from LLM response
        var rawOrigin = rawEntities["origin"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
        var rawDest = rawEntities["destination"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
        var rawDate = rawEntities["date"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() && it != "null" }
        var passengers = rawEntities["passengers"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        
        logger.info("LLM extracted: origin=$rawOrigin, dest=$rawDest, date=$rawDate")
        
        // Merge with context from previous messages (context takes precedence for missing fields)
        if (rawOrigin == null && context?.pendingOrigin != null) {
            rawOrigin = context.pendingOrigin
            logger.info("Using origin from context: $rawOrigin")
        }
        if (rawDest == null && context?.pendingDestination != null) {
            rawDest = context.pendingDestination
            logger.info("Using destination from context: $rawDest")
        }
        if (rawDate == null && context?.pendingDate != null) {
            rawDate = context.pendingDate
            logger.info("Using date from context: $rawDate")
        }
        if (passengers == 1 && context?.pendingPassengers != null) {
            passengers = context.pendingPassengers ?: 1
        }
        
        // Also check if user's geolocation provides origin
        if (rawOrigin == null && context?.userOriginAirport != null) {
            rawOrigin = context.userOriginAirport
            logger.info("Using origin from user geolocation: $rawOrigin")
        }
        
        logger.info("After merge: origin=$rawOrigin, dest=$rawDest, date=$rawDate")
        
        // Build list of missing fields
        val missingFields = mutableListOf<String>()
        if (rawOrigin == null) missingFields.add("origin")
        if (rawDest == null) missingFields.add("destination")
        if (rawDate == null) missingFields.add("date")
        
        if (missingFields.isNotEmpty()) {
            // Create pending context with what we have so we can continue the conversation
            val pendingContext = PendingBookingContext(
                origin = rawOrigin,
                destination = rawDest,
                date = rawDate,
                passengers = passengers
            )
            throw EntityExtractionException(
                buildMissingFieldsMessage(missingFields, rawOrigin, rawDest, rawDate),
                pendingContext
            )
        }
        
        // Step 2 & 3: Resolution (Direct -> Fuzzy -> LLM)
        val originCode = resolveCityCode(rawOrigin!!) 
        val destCode = resolveCityCode(rawDest!!)
        
        // Validation
        if (!referenceDataService.isValidRoute(originCode, destCode)) {
            throw RouteValidationException("Route $originCode-$destCode is not valid")
        }
        
        return mapOf(
            "origin" to originCode,
            "destination" to destCode,
            "date" to rawDate!!,
            "passengers" to passengers
        )
    }
    
    private fun buildMissingFieldsMessage(missing: List<String>, origin: String?, dest: String?, date: String?): String {
        val understood = mutableListOf<String>()
        if (origin != null) understood.add("from $origin")
        if (dest != null) understood.add("to $dest")
        if (date != null) understood.add("on $date")
        
        val understoodPart = if (understood.isNotEmpty()) {
            "I understood you want to fly ${understood.joinToString(" ")}. "
        } else ""
        
        val questions = missing.map { field ->
            when (field) {
                "origin" -> "Where would you like to fly from?"
                "destination" -> "Where would you like to go?"
                "date" -> "What date would you like to travel?"
                else -> ""
            }
        }.filter { it.isNotBlank() }
        
        return "${understoodPart}${questions.joinToString(" ")}"
    }

    private suspend fun extractRawEntities(userInput: String, pendingContext: PendingBookingContext? = null): JsonObject {
        val today = java.time.LocalDate.now().toString()
        val tomorrow = java.time.LocalDate.now().plusDays(1).toString()
        
        // Build context hint for the LLM so it knows what info we already have
        val contextHint = if (pendingContext != null && (pendingContext.origin != null || pendingContext.destination != null || pendingContext.date != null)) {
            val known = mutableListOf<String>()
            val missing = mutableListOf<String>()
            
            if (pendingContext.origin != null) known.add("origin=${pendingContext.origin}") else missing.add("origin")
            if (pendingContext.destination != null) known.add("destination=${pendingContext.destination}") else missing.add("destination")  
            if (pendingContext.date != null) known.add("date=${pendingContext.date}") else missing.add("date")
            
            """
            
            IMPORTANT CONTEXT: This is a follow-up message in a conversation.
            Already collected: ${known.joinToString(", ")}
            Still needed: ${missing.joinToString(", ")}
            The user's message "$userInput" is likely providing the MISSING information (${missing.joinToString(" or ")}).
            If the message is just a city name and we need origin, treat it as origin.
            If the message is just a city name and we need destination, treat it as destination.
            """
        } else ""
        
        val llmPrompt = """
            Extract flight booking details from: "$userInput"
            
            Today's date is $today.$contextHint
            
            Rules:
            - For "ASAP", "as soon as possible", "today" → use "$today"
            - For "tomorrow" → use "$tomorrow"  
            - For relative dates like "next week", "in 3 days" → calculate from today
            - If origin is not mentioned in this message, return null for origin
            - If destination is not mentioned in this message, return null for destination
            - If date is not mentioned or unclear in this message, return null for date
            - Default passengers to 1 if not specified
            
            Return ONLY valid JSON: {"origin": "city name or null", "destination": "city name or null", "date": "YYYY-MM-DD or null", "passengers": 1}
        """.trimIndent()

        logger.info("LLM prompt: $llmPrompt")
        
        val jsonStrRaw = try {
            bedrockExecutor.generate(llmPrompt)
        } catch (e: Exception) {
            logger.warn("Primary model failed, using local backup", e)
            localExecutor.generate(llmPrompt)
        }
        
        logger.info("LLM response: $jsonStrRaw")

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
