package com.fairair.app

import com.fairair.app.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * Tests for authentication, saved bookings, and local storage covering user stories:
 * - 7.1: View saved bookings (local)
 * - 7.2: Access booking from history
 * - 7.3: Delete saved booking
 * - 12.1: Login to account
 * - 12.2: View booking history (authenticated)
 * - 12.3: Logout
 * - 13.1: App startup and initialization
 * - 13.2: Offline mode
 * - 13.3: Language preference
 * 
 * Tests local storage operations directly without ScreenModel dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthAndSavedBookingsTest {

    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================================================
    // User Story 7.1-7.3: Saved Bookings (Local Storage)
    // ========================================================================

    @Test
    fun `local storage initially has no saved bookings`() = runTest {
        val localStorage = MockLocalStorage()
        
        val bookings = localStorage.getSavedBookingsList()
        assertTrue(bookings.isEmpty(), "Should have no bookings initially")
    }

    @Test
    fun `can save a booking to local storage`() = runTest {
        val localStorage = MockLocalStorage()
        
        val booking = MockData.createSavedBooking("ABC123")
        localStorage.saveBooking(booking)
        
        val saved = localStorage.getSavedBookingsList()
        assertEquals(1, saved.size, "Should have 1 booking")
        assertEquals("ABC123", saved[0].pnr)
    }

    @Test
    fun `can save multiple bookings`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.saveBooking(MockData.createSavedBooking("ABC123"))
        localStorage.saveBooking(MockData.createSavedBooking("XYZ789"))
        
        val saved = localStorage.getSavedBookingsList()
        assertEquals(2, saved.size, "Should have 2 bookings")
    }

    @Test
    fun `can delete a saved booking by PNR`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.saveBooking(MockData.createSavedBooking("ABC123"))
        localStorage.saveBooking(MockData.createSavedBooking("XYZ789"))
        assertEquals(2, localStorage.getSavedBookingsList().size)
        
        localStorage.deleteBooking("ABC123")
        
        val saved = localStorage.getSavedBookingsList()
        assertEquals(1, saved.size, "Should have 1 booking after delete")
        assertNull(saved.find { it.pnr == "ABC123" }, "Deleted booking should be gone")
        assertNotNull(saved.find { it.pnr == "XYZ789" }, "Other booking should remain")
    }

    @Test
    fun `delete non-existent booking is no-op`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.saveBooking(MockData.createSavedBooking("ABC123"))
        assertEquals(1, localStorage.getSavedBookingsList().size)
        
        localStorage.deleteBooking("NONEXISTENT")
        
        assertEquals(1, localStorage.getSavedBookingsList().size)
    }

    @Test
    fun `can clear all bookings`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.saveBooking(MockData.createSavedBooking("ABC123"))
        localStorage.saveBooking(MockData.createSavedBooking("XYZ789"))
        assertEquals(2, localStorage.getSavedBookingsList().size)
        
        localStorage.clearAllBookings()
        
        assertTrue(localStorage.getSavedBookingsList().isEmpty(), "Should be empty after clear")
    }

    @Test
    fun `saved booking contains all required data`() = runTest {
        val localStorage = MockLocalStorage()
        
        val booking = MockData.createSavedBooking("ABC123")
        localStorage.saveBooking(booking)
        
        val saved = localStorage.getSavedBookingsList().first()
        assertEquals("ABC123", saved.pnr)
        assertEquals("CONFIRMED", saved.status)
        assertNotNull(saved.totalPrice)
    }

    // ========================================================================
    // User Story 12.1-12.3: Authentication
    // ========================================================================

    @Test
    fun `login with valid credentials succeeds`() = runTest {
        val mockApiClient = MockFairairApiClient()
        
        val result = mockApiClient.login("test@example.com", "password123")
        
        assertTrue(result.isSuccess, "Login should succeed")
        val response = result.getOrNull()
        assertNotNull(response)
        assertTrue(response.accessToken.isNotBlank(), "Should have access token")
        assertEquals("Bearer", response.tokenType)
    }

    @Test
    fun `login with invalid credentials fails`() = runTest {
        val mockApiClient = MockFairairApiClient()
        
        val result = mockApiClient.login("wrong@example.com", "wrongpassword")
        
        assertTrue(result.isFailure, "Login should fail")
    }

    @Test
    fun `login tracks API calls`() = runTest {
        val mockApiClient = MockFairairApiClient()
        
        mockApiClient.login("test@example.com", "password123")
        
        assertEquals(1, mockApiClient.loginCalls.size)
        assertEquals("test@example.com" to "password123", mockApiClient.loginCalls.first())
    }

    @Test
    fun `auth token can be stored and retrieved`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.saveAuthToken("test-token-12345")
        
        val retrieved = localStorage.getAuthToken()
        assertEquals("test-token-12345", retrieved)
    }

    @Test
    fun `no auth token returns null`() = runTest {
        val localStorage = MockLocalStorage()
        
        val token = localStorage.getAuthToken()
        assertNull(token, "Should return null when no token stored")
    }

    @Test
    fun `logout clears auth data`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.saveAuthToken("test-token-12345")
        assertNotNull(localStorage.getAuthToken())
        
        localStorage.clearAuth()
        
        assertNull(localStorage.getAuthToken(), "Token should be cleared")
    }

    @Test
    fun `authenticated user can fetch their bookings`() = runTest {
        val mockApiClient = MockFairairApiClient()
        
        val result = mockApiClient.getMyBookings("mock-token")
        
        assertTrue(result is ApiResult.Success)
        val bookings = (result as ApiResult.Success).data
        assertEquals(2, bookings.size, "Should have 2 bookings")
    }

    @Test
    fun `fetching bookings without token returns error`() = runTest {
        val mockApiClient = MockFairairApiClient()
        
        val result = mockApiClient.getMyBookings("")
        
        assertTrue(result is ApiResult.Error)
    }

    // ========================================================================
    // User Story 13.1-13.3: App Initialization and Preferences
    // ========================================================================

    @Test
    fun `default language is English`() = runTest {
        val localStorage = MockLocalStorage()
        
        val language = localStorage.getCurrentLanguage()
        assertEquals("en", language)
    }

    @Test
    fun `can change language to Arabic`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.setCurrentLanguage("ar")
        
        val language = localStorage.getCurrentLanguage()
        assertEquals("ar", language)
    }

    @Test
    fun `invalid language throws error`() = runTest {
        val localStorage = MockLocalStorage()
        
        assertFailsWith<IllegalArgumentException> {
            localStorage.setCurrentLanguage("fr") // Not supported
        }
    }

    @Test
    fun `routes can be cached`() = runTest {
        val localStorage = MockLocalStorage()
        val routes = mapOf(
            "RUH" to listOf("JED", "DMM"),
            "JED" to listOf("RUH", "DMM")
        )
        
        localStorage.cacheRoutes(routes)
        
        val cached = localStorage.getCachedRoutes()
        assertNotNull(cached)
        assertEquals(routes, cached.routes)
    }

    @Test
    fun `cached routes returns null if not cached`() = runTest {
        val localStorage = MockLocalStorage()
        
        val cached = localStorage.getCachedRoutes()
        assertNull(cached)
    }

    @Test
    fun `search history is saved`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.addSearchToHistory("RUH", "JED", "2024-12-15")
        localStorage.addSearchToHistory("JED", "DMM", "2024-12-20")
        
        val history = localStorage.getSearchHistory()
        assertEquals(2, history.size)
        assertEquals("JED", history[0].origin) // Most recent first
        assertEquals("RUH", history[1].origin)
    }

    @Test
    fun `search history limits to 10 entries`() = runTest {
        val localStorage = MockLocalStorage()
        
        repeat(15) { i ->
            localStorage.addSearchToHistory("RUH", "DEST$i", "2024-12-${(i % 28) + 1}")
        }
        
        val history = localStorage.getSearchHistory()
        assertEquals(10, history.size, "Should only keep 10 entries")
    }

    @Test
    fun `duplicate searches are removed from history`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.addSearchToHistory("RUH", "JED", "2024-12-15")
        localStorage.addSearchToHistory("JED", "DMM", "2024-12-20")
        localStorage.addSearchToHistory("RUH", "JED", "2024-12-25") // Same route, new date
        
        val history = localStorage.getSearchHistory()
        assertEquals(2, history.size, "Duplicate route should be removed")
        assertEquals("RUH", history[0].origin) // Most recent
        assertEquals("2024-12-25", history[0].departureDate) // New date
    }

    @Test
    fun `can clear search history`() = runTest {
        val localStorage = MockLocalStorage()
        
        localStorage.addSearchToHistory("RUH", "JED", "2024-12-15")
        assertEquals(1, localStorage.getSearchHistory().size)
        
        localStorage.clearSearchHistory()
        
        assertTrue(localStorage.getSearchHistory().isEmpty())
    }

    @Test
    fun `clear all clears all data`() = runTest {
        val localStorage = MockLocalStorage()
        
        // Add various data
        localStorage.saveBooking(MockData.createSavedBooking("ABC123"))
        localStorage.addSearchToHistory("RUH", "JED", "2024-12-15")
        localStorage.setCurrentLanguage("ar")
        
        localStorage.clearAll()
        
        assertTrue(localStorage.getSavedBookingsList().isEmpty(), "Bookings should be cleared")
        assertTrue(localStorage.getSearchHistory().isEmpty(), "History should be cleared")
        assertEquals("en", localStorage.getCurrentLanguage(), "Language should reset to English")
    }

    // ========================================================================
    // User Story 13.2: Offline Mode
    // ========================================================================

    @Test
    fun `saved bookings available offline`() = runTest {
        val localStorage = MockLocalStorage()
        
        // Save booking while "online"
        localStorage.saveBooking(MockData.createSavedBooking("ABC123"))
        
        // Simulate offline - bookings should still be accessible
        val bookings = localStorage.getSavedBookingsList()
        assertEquals(1, bookings.size)
        assertEquals("ABC123", bookings[0].pnr)
    }

    @Test
    fun `cached routes available offline`() = runTest {
        val localStorage = MockLocalStorage()
        val routes = mapOf(
            "RUH" to listOf("JED", "DMM"),
            "JED" to listOf("RUH", "DMM")
        )
        
        // Cache routes while "online"
        localStorage.cacheRoutes(routes)
        
        // Simulate offline - routes should still be accessible
        val cached = localStorage.getCachedRoutes()
        assertNotNull(cached)
        assertEquals(2, cached.routes.size)
    }

    @Test
    fun `search history available offline`() = runTest {
        val localStorage = MockLocalStorage()
        
        // Add search history while "online"
        localStorage.addSearchToHistory("RUH", "JED", "2024-12-15")
        
        // Simulate offline - history should still be accessible
        val history = localStorage.getSearchHistory()
        assertEquals(1, history.size)
        assertEquals("RUH", history[0].origin)
    }

    // ========================================================================
    // API Client - Error Handling Tests
    // ========================================================================

    @Test
    fun `API failure returns error result`() = runTest {
        val mockApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "Network error")
        
        val result = mockApiClient.getStations()
        
        assertTrue(result is ApiResult.Error)
        assertEquals("Network error", (result as ApiResult.Error).message)
    }

    @Test
    fun `search failure returns error result`() = runTest {
        val mockApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "No flights found")
        
        val result = mockApiClient.searchFlights(
            FlightSearchRequestDto(
                origin = "RUH",
                destination = "JED",
                departureDate = "2024-12-15",
                passengers = PassengerCountsDto(1, 0, 0)
            )
        )
        
        assertTrue(result is ApiResult.Error)
    }

    @Test
    fun `booking failure returns error result`() = runTest {
        val mockApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "Payment failed")
        
        val result = mockApiClient.createBooking(
            BookingRequestDto(
                flightNumber = "FA101",
                fareFamily = "VALUE",
                passengers = listOf(
                    PassengerDto("ADULT", "MR", "John", "Doe", "1990-01-15")
                ),
                contactEmail = "test@example.com",
                payment = PaymentDto("John Doe", "1111", 44900, "SAR")
            )
        )
        
        assertTrue(result is ApiResult.Error)
        assertEquals("Payment failed", (result as ApiResult.Error).message)
    }

    @Test
    fun `mock API client can be reset`() = runTest {
        val mockApiClient = MockFairairApiClient()
        
        mockApiClient.login("test@example.com", "password123")
        mockApiClient.searchFlights(
            FlightSearchRequestDto("RUH", "JED", "2024-12-15", null, PassengerCountsDto(1, 0, 0))
        )
        
        assertEquals(1, mockApiClient.loginCalls.size)
        assertEquals(1, mockApiClient.searchFlightsCalls.size)
        
        mockApiClient.reset()
        
        assertTrue(mockApiClient.loginCalls.isEmpty())
        assertTrue(mockApiClient.searchFlightsCalls.isEmpty())
    }
}
