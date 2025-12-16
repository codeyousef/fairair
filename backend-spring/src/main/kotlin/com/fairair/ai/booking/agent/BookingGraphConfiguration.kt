package com.fairair.ai.booking.agent

import com.fairair.ai.booking.exception.EntityExtractionException
import com.fairair.ai.booking.exception.RouteValidationException
import com.fairair.ai.booking.service.EntityExtractionService
import com.fairair.ai.booking.tool.NavitaireTools
import com.fairair.contract.dto.ChatUiType
import com.fairair.contract.model.FlightResponse
import com.fairair.koog.AIAgentGraph
import com.fairair.koog.AgentGraphBuilder
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Configuration
class BookingGraphConfiguration(
    private val entityExtractionService: EntityExtractionService,
    private val navitaireTools: NavitaireTools
) {
    private val logger = LoggerFactory.getLogger(BookingGraphConfiguration::class.java)

    @Bean
    fun flightBookingAgentGraph(): AIAgentGraph {
        return AgentGraphBuilder()
            .addNode("extract_and_validate_entities", ::extractAndValidateEntities)
            .addNode("execute_navitaire_search", ::executeNavitaireSearch)
            .addNode("generate_user_response", ::generateUserResponse)
            .build()
    }

    private suspend fun extractAndValidateEntities(context: MutableMap<String, Any>): MutableMap<String, Any> {
        val userInput = context["userInput"] as? String ?: throw IllegalArgumentException("userInput required")
        val chatContext = context["chatContext"] as? com.fairair.contract.dto.ChatContextDto
        
        val extracted = entityExtractionService.extractAndValidate(userInput, chatContext)
        return extracted.toMutableMap()
    }

    private suspend fun executeNavitaireSearch(context: MutableMap<String, Any>): MutableMap<String, Any> {
        val origin = context["origin"] as String
        val destination = context["destination"] as String
        val dateStr = context["date"] as String
        val passengers = context["passengers"] as Int
        
        val date = try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
             throw EntityExtractionException("Invalid date format: $dateStr")
        }
        
        logger.info("Searching flights: $origin -> $destination on $date for $passengers pax")
        
        val result = navitaireTools.searchFlights(origin, destination, date, passengers)
        
        return mutableMapOf(
            "flightResult" to result,
            "origin" to origin,
            "destination" to destination,
            "date" to dateStr,
            "passengers" to passengers
        )
    }

    private suspend fun generateUserResponse(context: MutableMap<String, Any>): MutableMap<String, Any> {
        val result = context["flightResult"] as FlightResponse
        val origin = context["origin"] as? String
        val destination = context["destination"] as? String
        val date = context["date"] as? String
        val passengers = context["passengers"] as? Int
        
        val response = if (result.isEmpty) {
            "I couldn't find any flights for that route and date. Please try another date."
        } else {
            val top3 = result.flights.take(3)
            val summary = top3.joinToString("\n") { 
                "${it.flightNumber} (${it.formatDuration()}) - ${it.lowestFare().price.formatDisplay()}" 
            }
            "I found ${result.count} flights! Here are the top options:\n$summary"
        }
        
        val output = mutableMapOf<String, Any>("userResponse" to response)
        
        if (!result.isEmpty) {
            output["uiType"] = ChatUiType.FLIGHT_LIST
            output["uiData"] = result
            // Use the searchId from the actual FlightResponse so it matches the cache
            output["pendingContext"] = com.fairair.contract.dto.PendingBookingContext(
                origin = origin,
                destination = destination,
                date = date,
                passengers = passengers,
                searchId = result.searchId
            )
        }
        
        return output
    }
}
