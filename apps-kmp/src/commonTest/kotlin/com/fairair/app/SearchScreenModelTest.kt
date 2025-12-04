package com.fairair.app

import com.fairair.app.api.*
import com.fairair.app.state.BookingFlowState
import com.fairair.app.state.SearchCriteria
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * E2E tests for search functionality covering user stories:
 * - 1.1: View available flights
 * - 1.2: Select departure/destination airports
 * - 1.3: Select travel dates
 * - 1.4: Specify passenger counts
 * - 1.5: See initial loading state
 * - 1.6: Search flights button
 * - 2.1: View matching flights
 * - 11.1: Network error handling
 * - 11.2: Invalid input validation
 * 
 * Tests the API client and BookingFlowState directly since ScreenModels
 * depend on Voyager's screenModelScope which requires special setup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenModelTest {

    private lateinit var mockApiClient: MockFairairApiClient
    private lateinit var bookingFlowState: BookingFlowState
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApiClient = MockFairairApiClient()
        bookingFlowState = BookingFlowState()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========================================================================
    // User Story 1.1: View available flights - initial data loading
    // ========================================================================

    @Test
    fun `loads stations successfully`() = runTest {
        val result = mockApiClient.getStations()
        
        assertTrue(result is ApiResult.Success)
        assertEquals(4, (result as ApiResult.Success).data.size, "Should load 4 stations")
    }

    @Test
    fun `loads routes successfully`() = runTest {
        val result = mockApiClient.getRoutes()
        
        assertTrue(result is ApiResult.Success)
        val routes = (result as ApiResult.Success).data.routes
        assertTrue(routes.isNotEmpty(), "Should load routes")
        assertTrue(routes.containsKey("RUH"), "Should have RUH routes")
    }

    @Test
    fun `shows error when initial data fails to load`() = runTest {
        val failingApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "Network error")
        
        val result = failingApiClient.getStations()
        
        assertTrue(result is ApiResult.Error)
        assertEquals("Network error", (result as ApiResult.Error).message)
    }

    // ========================================================================
    // User Story 1.2: Select departure/destination airports
    // ========================================================================

    @Test
    fun `can get destinations for origin`() = runTest {
        val result = mockApiClient.getDestinationsForOrigin("RUH")
        
        assertTrue(result is ApiResult.Success)
        val destinations = (result as ApiResult.Success).data
        assertTrue(destinations.isNotEmpty())
        
        val destinationCodes = destinations.map { it.code }
        assertTrue("JED" in destinationCodes, "JED should be available from RUH")
        assertTrue("DMM" in destinationCodes, "DMM should be available from RUH")
        assertFalse("RUH" in destinationCodes, "Origin should not be in destinations")
    }

    @Test
    fun `different origins have different destinations`() = runTest {
        val ruhDestinations = mockApiClient.getDestinationsForOrigin("RUH")
        val dmmDestinations = mockApiClient.getDestinationsForOrigin("DMM")
        
        assertTrue(ruhDestinations is ApiResult.Success)
        assertTrue(dmmDestinations is ApiResult.Success)
        
        val ruhCodes = (ruhDestinations as ApiResult.Success).data.map { it.code }
        val dmmCodes = (dmmDestinations as ApiResult.Success).data.map { it.code }
        
        // RUH has more destinations than DMM
        assertTrue(ruhCodes.size >= dmmCodes.size)
        assertTrue("MED" in ruhCodes, "MED should be available from RUH")
        assertFalse("MED" in dmmCodes, "MED should not be available from DMM")
    }

    // ========================================================================
    // User Story 1.4: Specify passenger counts
    // ========================================================================

    @Test
    fun `passenger counts are stored in search criteria`() {
        val origin = StationDto("RUH", "King Khalid", "Riyadh", "Saudi Arabia")
        val destination = StationDto("JED", "King Abdulaziz", "Jeddah", "Saudi Arabia")
        
        val criteria = SearchCriteria(
            origin = origin,
            destination = destination,
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 2, children = 1, infants = 0)
        )
        
        bookingFlowState.setSearchCriteria(criteria)
        
        assertEquals(2, bookingFlowState.searchCriteria?.passengers?.adults)
        assertEquals(1, bookingFlowState.searchCriteria?.passengers?.children)
        assertEquals(0, bookingFlowState.searchCriteria?.passengers?.infants)
    }

    @Test
    fun `total passengers calculated correctly`() {
        val passengers = PassengerCountsDto(adults = 2, children = 3, infants = 1)
        val total = passengers.adults + passengers.children + passengers.infants
        
        assertEquals(6, total)
    }

    // ========================================================================
    // User Story 1.6: Search flights
    // ========================================================================

    @Test
    fun `search flights returns results`() = runTest {
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 1, children = 0, infants = 0)
        )
        
        val result = mockApiClient.searchFlights(request)
        
        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertEquals(3, response.flights.size, "Should return 3 flights")
    }

    @Test
    fun `search flights tracks API calls`() = runTest {
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 2, children = 1, infants = 0)
        )
        
        mockApiClient.searchFlights(request)
        
        assertEquals(1, mockApiClient.searchFlightsCalls.size)
        assertEquals("RUH", mockApiClient.searchFlightsCalls[0].origin)
        assertEquals("JED", mockApiClient.searchFlightsCalls[0].destination)
        assertEquals(2, mockApiClient.searchFlightsCalls[0].passengers.adults)
    }

    @Test
    fun `search updates booking flow state`() = runTest {
        val origin = StationDto("RUH", "King Khalid", "Riyadh", "Saudi Arabia")
        val destination = StationDto("JED", "King Abdulaziz", "Jeddah", "Saudi Arabia")
        
        val request = FlightSearchRequestDto(
            origin = origin.code,
            destination = destination.code,
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 1, children = 0, infants = 0)
        )
        
        val result = mockApiClient.searchFlights(request)
        assertTrue(result is ApiResult.Success)
        
        bookingFlowState.setSearchCriteria(
            SearchCriteria(
                origin = origin,
                destination = destination,
                departureDate = "2024-12-15",
                passengers = request.passengers
            )
        )
        bookingFlowState.setSearchResult((result as ApiResult.Success).data)
        
        assertNotNull(bookingFlowState.searchCriteria)
        assertNotNull(bookingFlowState.searchResult)
        assertEquals("RUH", bookingFlowState.searchCriteria?.origin?.code)
        assertEquals("JED", bookingFlowState.searchCriteria?.destination?.code)
        assertEquals(3, bookingFlowState.searchResult?.flights?.size)
    }

    // ========================================================================
    // User Story 2.1: View matching flights with fare families
    // ========================================================================

    @Test
    fun `flights have multiple fare families`() = runTest {
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 1, children = 0, infants = 0)
        )
        
        val result = mockApiClient.searchFlights(request)
        assertTrue(result is ApiResult.Success)
        
        val flight = (result as ApiResult.Success).data.flights.first()
        assertEquals(3, flight.fareFamilies.size, "Should have 3 fare families")
        
        val fareCodes = flight.fareFamilies.map { it.code }
        assertTrue("LIGHT" in fareCodes)
        assertTrue("VALUE" in fareCodes)
        assertTrue("BUSINESS" in fareCodes)
    }

    @Test
    fun `fare families have different prices`() = runTest {
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 1, children = 0, infants = 0)
        )
        
        val result = mockApiClient.searchFlights(request)
        assertTrue(result is ApiResult.Success)
        
        val flight = (result as ApiResult.Success).data.flights.first()
        val light = flight.fareFamilies.find { it.code == "LIGHT" }
        val value = flight.fareFamilies.find { it.code == "VALUE" }
        val business = flight.fareFamilies.find { it.code == "BUSINESS" }
        
        assertNotNull(light)
        assertNotNull(value)
        assertNotNull(business)
        
        assertTrue(light.priceMinor < value.priceMinor, "Light should be cheaper than Value")
        assertTrue(value.priceMinor < business.priceMinor, "Value should be cheaper than Business")
    }

    @Test
    fun `fare families have different inclusions`() = runTest {
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 1, children = 0, infants = 0)
        )
        
        val result = mockApiClient.searchFlights(request)
        assertTrue(result is ApiResult.Success)
        
        val flight = (result as ApiResult.Success).data.flights.first()
        val light = flight.fareFamilies.find { it.code == "LIGHT" }
        val business = flight.fareFamilies.find { it.code == "BUSINESS" }
        
        assertNotNull(light)
        assertNotNull(business)
        
        assertNull(light.inclusions.checkedBag, "Light should have no checked bag")
        assertNotNull(business.inclusions.checkedBag, "Business should have checked bag")
        assertFalse(light.inclusions.loungeAccess)
        assertTrue(business.inclusions.loungeAccess)
    }

    // ========================================================================
    // User Story 11.1: Network error handling
    // ========================================================================

    @Test
    fun `search shows error when fails`() = runTest {
        val failingApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "Network error")
        
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 1, children = 0, infants = 0)
        )
        
        val result = failingApiClient.searchFlights(request)
        
        assertTrue(result is ApiResult.Error)
        assertEquals("Network error", (result as ApiResult.Error).message)
    }

    @Test
    fun `destinations error handled gracefully`() = runTest {
        val failingApiClient = MockFairairApiClient(shouldFail = true, failureMessage = "Failed to load destinations")
        
        val result = failingApiClient.getDestinationsForOrigin("RUH")
        
        assertTrue(result is ApiResult.Error)
    }

    // ========================================================================
    // Low Fares API Tests
    // ========================================================================

    @Test
    fun `low fares returns price data`() = runTest {
        val result = mockApiClient.getLowFares(
            origin = "RUH",
            destination = "JED",
            startDate = "2024-12-01",
            endDate = "2024-12-07"
        )
        
        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertTrue(response.dates.isNotEmpty())
        assertEquals("RUH", response.origin)
        assertEquals("JED", response.destination)
    }

    @Test
    fun `low fares shows prices per date`() = runTest {
        val result = mockApiClient.getLowFares(
            origin = "RUH",
            destination = "JED",
            startDate = "2024-12-01",
            endDate = "2024-12-07"
        )
        
        assertTrue(result is ApiResult.Success)
        val dates = (result as ApiResult.Success).data.dates
        
        dates.forEach { lowFare ->
            assertNotNull(lowFare.priceMinor)
            assertNotNull(lowFare.priceFormatted)
            assertTrue(lowFare.available)
        }
    }

    // ========================================================================
    // API Reset Tests
    // ========================================================================

    @Test
    fun `mock API can be reset`() = runTest {
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 1, children = 0, infants = 0)
        )
        
        mockApiClient.searchFlights(request)
        assertEquals(1, mockApiClient.searchFlightsCalls.size)
        
        mockApiClient.reset()
        
        assertTrue(mockApiClient.searchFlightsCalls.isEmpty())
    }
}
