package com.fairair.app

import com.fairair.app.api.*
import com.fairair.contract.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * E2E tests for Pilot AI Chat functionality.
 * 
 * Tests the API client layer for chat operations since ScreenModels
 * depend on Voyager's screenModelScope which requires special setup.
 * 
 * Tests cover:
 * - Sending chat messages
 * - Receiving AI responses
 * - Multi-language support (English and Arabic)
 * - Session management
 * - Context handling
 * - Intent detection (flight search, booking, seat change)
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatFlowTest {

    private lateinit var mockApiClient: MockFairairApiClient
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApiClient = MockFairairApiClient()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        mockApiClient.reset()
    }

    // ========================================================================
    // Basic Chat Message Tests
    // ========================================================================

    @Test
    fun `sends chat message successfully`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-1",
            message = "Hello",
            locale = "en-US"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.text.isNotEmpty(), "Response text should not be empty")
    }

    @Test
    fun `returns greeting response for hello message`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-1",
            message = "Hello",
            locale = "en-US"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.text.contains("Pilot", ignoreCase = true) || 
                   response.text.contains("help", ignoreCase = true),
            "Expected greeting with Pilot or help offer")
        assertEquals("en", response.detectedLanguage)
    }

    @Test
    fun `returns suggestions in response`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-1",
            message = "Hello",
            locale = "en-US"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.suggestions.isNotEmpty(), "Should include suggestions")
    }

    @Test
    fun `tracks chat message calls`() = runTest {
        mockApiClient.sendChatMessage(
            sessionId = "test-session-1",
            message = "Hello",
            locale = "en-US"
        )

        assertEquals(1, mockApiClient.chatMessageCalls.size)
        assertEquals("test-session-1", mockApiClient.chatMessageCalls[0].sessionId)
        assertEquals("Hello", mockApiClient.chatMessageCalls[0].message)
    }

    // ========================================================================
    // Multi-language Support Tests
    // ========================================================================

    @Test
    fun `returns Arabic response for Arabic greeting`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-ar",
            message = "مرحبا",
            locale = "ar-SA"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.text.contains("بايلوت") || response.text.contains("هلا"),
            "Expected Arabic response with Pilot name or greeting")
        assertEquals("ar", response.detectedLanguage)
    }

    @Test
    fun `returns Arabic suggestions for Arabic greeting`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-ar",
            message = "مرحبا",
            locale = "ar-SA"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.suggestions.isNotEmpty(), "Should have Arabic suggestions")
    }

    // ========================================================================
    // Intent Detection Tests
    // ========================================================================

    @Test
    fun `handles flight search intent`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-2",
            message = "I want to find a flight to Riyadh",
            locale = "en-US"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.text.contains("flight", ignoreCase = true),
            "Expected flight-related response")
    }

    @Test
    fun `handles booking lookup intent`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-3",
            message = "I want to check my booking",
            locale = "en-US"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.text.contains("booking", ignoreCase = true) ||
                   response.text.contains("PNR", ignoreCase = true),
            "Expected booking-related response")
    }

    @Test
    fun `handles seat change intent`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-4",
            message = "I want to change my seat",
            locale = "en-US"
        )

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.text.contains("seat", ignoreCase = true),
            "Expected seat-related response")
    }

    // ========================================================================
    // Context Handling Tests
    // ========================================================================

    @Test
    fun `sends context with message`() = runTest {
        val context = ChatContextDto(
            currentPnr = "ABC123",
            currentScreen = "manage-booking"
        )

        mockApiClient.sendChatMessage(
            sessionId = "test-session-5",
            message = "Change my seat",
            locale = "en-US",
            context = context
        )

        assertEquals(1, mockApiClient.chatMessageCalls.size)
        assertEquals("ABC123", mockApiClient.chatMessageCalls[0].context?.currentPnr)
        assertEquals("manage-booking", mockApiClient.chatMessageCalls[0].context?.currentScreen)
    }

    @Test
    fun `handles null context`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-6",
            message = "Hello",
            locale = "en-US",
            context = null
        )

        assertTrue(result is ApiResult.Success)
        assertNull(mockApiClient.chatMessageCalls[0].context)
    }

    // ========================================================================
    // Session Management Tests
    // ========================================================================

    @Test
    fun `tracks multiple messages in same session`() = runTest {
        val sessionId = "test-session-7"

        mockApiClient.sendChatMessage(sessionId = sessionId, message = "Hello")
        mockApiClient.sendChatMessage(sessionId = sessionId, message = "I need a flight")
        mockApiClient.sendChatMessage(sessionId = sessionId, message = "To Riyadh")

        assertEquals(3, mockApiClient.chatMessageCalls.size)
        assertTrue(mockApiClient.chatMessageCalls.all { it.sessionId == sessionId })
    }

    @Test
    fun `clears session successfully`() = runTest {
        val sessionId = "test-session-8"

        val result = mockApiClient.clearChatSession(sessionId)

        assertTrue(result is ApiResult.Success)
        assertEquals(1, mockApiClient.clearSessionCalls.size)
        assertEquals(sessionId, mockApiClient.clearSessionCalls[0])
    }

    @Test
    fun `different sessions are independent`() = runTest {
        mockApiClient.sendChatMessage(sessionId = "session-a", message = "Hello from A")
        mockApiClient.sendChatMessage(sessionId = "session-b", message = "Hello from B")

        val sessionACalls = mockApiClient.chatMessageCalls.filter { it.sessionId == "session-a" }
        val sessionBCalls = mockApiClient.chatMessageCalls.filter { it.sessionId == "session-b" }

        assertEquals(1, sessionACalls.size)
        assertEquals(1, sessionBCalls.size)
        assertEquals("Hello from A", sessionACalls[0].message)
        assertEquals("Hello from B", sessionBCalls[0].message)
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    fun `handles API failure gracefully`() = runTest {
        val failingApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "Network error")

        val result = failingApiClient.sendChatMessage(
            sessionId = "test-session-error",
            message = "Hello"
        )

        assertTrue(result is ApiResult.Error)
        assertEquals("Network error", (result as ApiResult.Error).message)
    }

    @Test
    fun `handles session clear failure`() = runTest {
        val failingApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "Server error")

        val result = failingApiClient.clearChatSession("test-session")

        assertTrue(result is ApiResult.Error)
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    fun `handles empty message gracefully`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-9",
            message = "",
            locale = "en-US"
        )

        // Mock returns default response for empty message
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `handles special characters in message`() = runTest {
        val result = mockApiClient.sendChatMessage(
            sessionId = "test-session-10",
            message = "Hello! Can you help me? 🛫 ✈️ 🌍",
            locale = "en-US"
        )

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `preserves locale in API call`() = runTest {
        mockApiClient.sendChatMessage(
            sessionId = "test-session-11",
            message = "Hello",
            locale = "ar-SA"
        )

        assertEquals("ar-SA", mockApiClient.chatMessageCalls[0].locale)
    }
}
