package com.fairair.service

import com.fairair.ai.booking.service.BookingOrchestrator
import com.fairair.contract.dto.*
import com.fairair.contract.model.FlightResponse
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val bookingOrchestrator: BookingOrchestrator,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(ChatService::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun processMessage(request: ChatMessageRequestDto): ChatResponseDto {
        log.info("Processing chat message for session ${request.sessionId}")
        
        val result = bookingOrchestrator.handleUserRequest(request.message, request.context)
        
        var uiDataJson: String? = null
        if (result.uiData != null) {
            uiDataJson = try {
                when {
                    result.uiType == ChatUiType.FLIGHT_LIST && result.uiData is FlightResponse -> {
                        val payload = mapToFlightListPayload(result.uiData)
                        json.encodeToString(payload)
                    }
                    result.uiData is String -> {
                        // Already serialized JSON string
                        result.uiData as String
                    }
                    else -> {
                        json.encodeToString(result.uiData.toString())
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to serialize UI data", e)
                null
            }
        }
        
        val aiResponseText = result.response
        val locale = request.locale
        val uiType = result.uiType
        
        // Use suggestions from the booking orchestrator if provided, otherwise fall back to generated suggestions
        val suggestions = if (result.suggestions.isNotEmpty()) {
            result.suggestions
        } else {
            generateSuggestions(locale, uiType)
        }

        return ChatResponseDto(
            text = aiResponseText,
            uiType = uiType,
            uiData = uiDataJson,
            suggestions = suggestions, 
            isPartial = false,
            detectedLanguage = detectLanguage(aiResponseText),
            pendingContext = result.pendingContext
        )
    }
    
    private fun mapToFlightListPayload(response: FlightResponse): FlightListPayloadDto {
        val firstFlight = response.flights.firstOrNull()
        return FlightListPayloadDto(
            flights = response.flights.map { flight ->
                val lowest = flight.lowestFare()
                FlightOptionDto(
                    flightNumber = flight.flightNumber,
                    departureTime = flight.departureTime.toString(),
                    arrivalTime = flight.arrivalTime.toString(),
                    priceFormatted = lowest.price.formatDisplay(),
                    priceMinor = lowest.price.amountMinor,
                    currency = lowest.price.currency.toString(),
                    duration = flight.formatDuration()
                )
            },
            origin = firstFlight?.origin?.value ?: "",
            destination = firstFlight?.destination?.value ?: "",
            date = firstFlight?.departureTime?.toString()?.substringBefore("T") ?: ""
        )
    }

    private fun generateSuggestions(
        locale: String?,
        uiType: ChatUiType?
    ): List<String> {
        val isArabic = locale?.startsWith("ar") == true
        
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

    fun detectLanguage(text: String): String {
        val arabicPattern = Regex("[\\u0600-\\u06FF]")
        val arabicCount = arabicPattern.findAll(text).count()
        val totalChars = text.filter { it.isLetter() }.length
        
        return if (totalChars > 0 && arabicCount.toFloat() / totalChars > 0.3f) {
            "ar"
        } else {
            "en"
        }
    }
    
    suspend fun clearSession(sessionId: String) {
        log.info("Clearing session $sessionId")
        // No-op for Koog for now
    }
    
    suspend fun getHistory(sessionId: String): List<ChatResponseDto> {
        return emptyList()
    }
    
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
 * Retained for compatibility with legacy AiToolExecutor if still used elsewhere.
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
