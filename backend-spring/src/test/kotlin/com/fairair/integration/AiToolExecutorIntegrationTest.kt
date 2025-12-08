package com.fairair.integration

import com.fairair.ai.FarisPrompts
import com.fairair.ai.FarisTools
import com.fairair.contract.dto.ChatContextDto
import com.fairair.contract.dto.ChatUiType
import com.fairair.service.AiToolExecutor
import com.fairair.service.BookingService
import com.fairair.service.FlightService
import com.fairair.service.ManageBookingService
import com.fairair.service.ProfileService
import com.fairair.client.NavitaireClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

/**
 * Integration tests for AI tool execution.
 * 
 * These tests verify that tools are executed correctly and return
 * proper UI types for the chat responses.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AI Tool Executor Integration Tests")
class AiToolExecutorIntegrationTest {

    @Autowired
    private lateinit var toolExecutor: AiToolExecutor

    private val json = Json { ignoreUnknownKeys = true }

    @Nested
    @DisplayName("search_flights Tool Execution")
    inner class SearchFlightsToolTests {

        @Test
        @DisplayName("search_flights should return FLIGHT_LIST uiType")
        fun searchFlightsShouldReturnFlightListUiType() = runBlocking {
            val arguments = """
                {
                    "origin": "JED",
                    "destination": "RUH",
                    "date": "2025-12-15"
                }
            """.trimIndent()

            val result = toolExecutor.execute("search_flights", arguments, null)

            assertNotNull(result.data)
            assertEquals(
                ChatUiType.FLIGHT_LIST,
                result.uiType,
                "uiType should be FLIGHT_LIST"
            )
        }
        
        @Test
        @DisplayName("search_flights should return searchId in results")
        fun searchFlightsShouldReturnSearchId() = runBlocking {
            val arguments = """
                {
                    "origin": "JED",
                    "destination": "RUH",
                    "date": "2025-12-15"
                }
            """.trimIndent()

            val result = toolExecutor.execute("search_flights", arguments, null)
            val data = result.data as Map<*, *>
            
            assertTrue(
                data.containsKey("searchId"),
                "Result should contain searchId for later booking"
            )
        }
    }

    @Nested
    @DisplayName("select_flight Tool Execution")
    inner class SelectFlightToolTests {

        @Test
        @DisplayName("select_flight should return FLIGHT_SELECTED uiType")
        fun selectFlightShouldReturnFlightSelectedUiType() = runBlocking {
            val arguments = """{"flight_number": "F3100"}"""

            val result = toolExecutor.execute("select_flight", arguments, null)

            assertNotNull(result.data)
            assertEquals(
                ChatUiType.FLIGHT_SELECTED,
                result.uiType,
                "uiType should be FLIGHT_SELECTED"
            )
        }
    }

    @Nested
    @DisplayName("create_booking requires valid search context")
    inner class CreateBookingContextTests {

        @Test
        @DisplayName("create_booking should fail without searchId")
        fun createBookingRequiresSearchId() = runBlocking {
            // No searchId in context - should return error
            val context = ChatContextDto(
                userId = "test-user-123",
                userEmail = "test@example.com"
            )

            val arguments = """
                {
                    "flight_number": "F3100",
                    "passengers": [{
                        "firstName": "Jane",
                        "lastName": "Doe",
                        "dateOfBirth": "1985-03-15",
                        "gender": "FEMALE",
                        "documentNumber": "A12345678",
                        "nationality": "SA"
                    }]
                }
            """.trimIndent()

            val result = toolExecutor.execute("create_booking", arguments, context)
            
            val data = result.data as Map<*, *>
            assertTrue(
                data.containsKey("error") && (data["error"] as String).contains("search"),
                "Should return error about missing search: $data"
            )
        }
    }

    @Nested
    @DisplayName("Tool Definitions")
    inner class ToolDefinitionTests {

        @Test
        @DisplayName("create_booking description should mention confirmation context")
        fun createBookingDescriptionShouldMentionConfirmation() {
            val createBookingTool = FarisTools.createBooking
            
            assertTrue(
                createBookingTool.description.contains("yes", ignoreCase = true) ||
                createBookingTool.description.contains("confirm", ignoreCase = true) ||
                createBookingTool.description.contains("WHEN TO USE", ignoreCase = false),
                "create_booking description should mention when to use it (on confirmation)"
            )
        }

        @Test
        @DisplayName("select_flight description should clarify its purpose")
        fun selectFlightDescriptionShouldClarifyPurpose() {
            val selectFlightTool = FarisTools.selectFlight
            
            assertTrue(
                selectFlightTool.description.contains("NOT", ignoreCase = false) ||
                selectFlightTool.description.contains("ONLY", ignoreCase = false) ||
                selectFlightTool.description.contains("select", ignoreCase = true),
                "select_flight description should clarify when to use it"
            )
        }
    }

    @Nested
    @DisplayName("System Prompt Verification")
    inner class SystemPromptTests {

        @Test
        @DisplayName("System prompt should prioritize create_booking for confirmations")
        fun systemPromptShouldPrioritizeCreateBooking() {
            val prompt = FarisPrompts.systemPrompt
            
            // Check that the prompt mentions the correct behavior
            assertTrue(
                prompt.contains("create_booking", ignoreCase = false),
                "System prompt should mention create_booking tool"
            )
            
            assertTrue(
                prompt.contains("yes", ignoreCase = true) && 
                prompt.contains("confirm", ignoreCase = true),
                "System prompt should mention confirmation handling"
            )
        }

        @Test
        @DisplayName("System prompt should warn against search on confirmation")
        fun systemPromptShouldWarnAgainstSearch() {
            val prompt = FarisPrompts.systemPrompt
            
            assertTrue(
                prompt.contains("DO NOT") && 
                (prompt.contains("search_flights") || prompt.contains("select_flight")),
                "System prompt should warn against calling wrong tools on confirmation"
            )
        }

        @Test
        @DisplayName("System prompt should have HIGHEST PRIORITY section")
        fun systemPromptShouldHaveHighestPrioritySection() {
            val prompt = FarisPrompts.systemPrompt
            
            assertTrue(
                prompt.contains("HIGHEST PRIORITY", ignoreCase = true),
                "System prompt should have a HIGHEST PRIORITY section for critical rules"
            )
        }
    }
}
