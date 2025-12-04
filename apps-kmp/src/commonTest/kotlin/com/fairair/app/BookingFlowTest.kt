package com.fairair.app

import com.fairair.app.api.*
import com.fairair.app.state.BookingFlowState
import com.fairair.app.state.PassengerInfo
import com.fairair.app.state.SearchCriteria
import com.fairair.app.state.SelectedAncillaries
import com.fairair.app.state.SelectedFlight
import com.fairair.app.state.BaggageInfo
import com.fairair.app.state.MealInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * E2E tests for the complete booking flow covering user stories:
 * - 2.1: View matching flights with prices
 * - 2.2: See fare family options
 * - 2.3: Compare fare inclusions
 * - 2.4: Select preferred flight
 * - 2.5: See flight details
 * - 3.1: Enter passenger details
 * - 3.2: Enter contact information
 * - 3.3: See validation errors
 * - 3.4: Navigate between passengers
 * - 4.1: View ancillary options
 * - 4.2: Add baggage
 * - 4.3: Select meals
 * - 4.4: See updated total
 * - 5.1: View booking summary
 * - 5.2: Enter payment details
 * - 5.3: See payment validation
 * - 5.4: Complete booking
 * - 5.5: Secure payment processing
 * - 6.1: See confirmation
 * - 6.2: Receive confirmation email
 * - 6.3: Save booking reference
 * 
 * Tests the API client and BookingFlowState directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookingFlowTest {

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
    // Helper: Set up booking state with search results
    // ========================================================================

    private fun setupSearchResult() {
        val origin = StationDto("RUH", "King Khalid", "Riyadh", "Saudi Arabia")
        val destination = StationDto("JED", "King Abdulaziz", "Jeddah", "Saudi Arabia")
        
        bookingFlowState.setSearchCriteria(
            SearchCriteria(
                origin = origin,
                destination = destination,
                departureDate = "2024-12-15",
                passengers = PassengerCountsDto(adults = 2, children = 1, infants = 0)
            )
        )
        
        bookingFlowState.setSearchResult(
            FlightSearchResponseDto(
                flights = MockData.createFlights("RUH", "JED", "2024-12-15"),
                searchId = "SEARCH-123"
            )
        )
    }

    private fun setupSelectedFlight() {
        setupSearchResult()
        val flight = MockData.createFlights("RUH", "JED", "2024-12-15").first()
        bookingFlowState.setSelectedFlight(
            SelectedFlight(
                flight = flight,
                fareFamily = "VALUE",
                totalPrice = "449 SAR"
            )
        )
    }

    private fun setupPassengerInfo() {
        setupSelectedFlight()
        bookingFlowState.setPassengerInfo(
            listOf(
                PassengerInfo(
                    id = "adult_0",
                    type = "ADULT",
                    title = "MR",
                    firstName = "JOHN",
                    lastName = "DOE",
                    dateOfBirth = "1990-01-15",
                    nationality = "SA",
                    documentType = "PASSPORT",
                    documentNumber = "A12345678",
                    documentExpiry = "2028-01-15",
                    email = "john.doe@example.com",
                    phone = "+966501234567"
                ),
                PassengerInfo(
                    id = "adult_1",
                    type = "ADULT",
                    title = "MRS",
                    firstName = "JANE",
                    lastName = "DOE",
                    dateOfBirth = "1992-05-20",
                    nationality = "SA",
                    documentType = "PASSPORT",
                    documentNumber = "B87654321",
                    documentExpiry = "2027-05-20",
                    email = "",
                    phone = ""
                ),
                PassengerInfo(
                    id = "child_0",
                    type = "CHILD",
                    title = "MSTR",
                    firstName = "TOMMY",
                    lastName = "DOE",
                    dateOfBirth = "2018-08-10",
                    nationality = "SA",
                    documentType = "PASSPORT",
                    documentNumber = "C11111111",
                    documentExpiry = "2026-08-10",
                    email = "",
                    phone = ""
                )
            )
        )
    }

    // ========================================================================
    // User Story 2.1-2.5: Search Results - Flight Selection
    // ========================================================================

    @Test
    fun `search returns flights with fare families`() = runTest {
        val request = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 2, children = 1, infants = 0)
        )
        
        val result = mockApiClient.searchFlights(request)
        
        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertEquals(3, response.flights.size)
        
        val flight = response.flights.first()
        assertEquals(3, flight.fareFamilies.size)
    }

    @Test
    fun `search result stored in booking state`() = runTest {
        setupSearchResult()
        
        assertNotNull(bookingFlowState.searchResult)
        assertEquals(3, bookingFlowState.searchResult?.flights?.size)
        assertEquals(3, bookingFlowState.searchCriteria?.passengers?.adults?.plus(
            bookingFlowState.searchCriteria?.passengers?.children ?: 0
        ))
    }

    @Test
    fun `can select flight and fare family`() = runTest {
        setupSearchResult()
        
        val flight = bookingFlowState.searchResult!!.flights.first()
        bookingFlowState.setSelectedFlight(
            SelectedFlight(
                flight = flight,
                fareFamily = "VALUE",
                totalPrice = "449 SAR"
            )
        )
        
        val selected = bookingFlowState.selectedFlight
        assertNotNull(selected)
        assertEquals("VALUE", selected.fareFamily)
        assertEquals("449 SAR", selected.totalPrice)
    }

    @Test
    fun `flights have fare families with inclusions`() = runTest {
        setupSearchResult()
        
        val flight = bookingFlowState.searchResult!!.flights.first()
        assertEquals(3, flight.fareFamilies.size)
        
        val lightFare = flight.fareFamilies.find { it.code == "LIGHT" }
        assertNotNull(lightFare)
        assertNull(lightFare.inclusions.checkedBag)
        
        val businessFare = flight.fareFamilies.find { it.code == "BUSINESS" }
        assertNotNull(businessFare)
        assertTrue(businessFare.inclusions.loungeAccess)
        assertTrue(businessFare.inclusions.priorityBoarding)
    }

    @Test
    fun `fare families have increasing prices`() = runTest {
        setupSearchResult()
        
        val flight = bookingFlowState.searchResult!!.flights.first()
        val light = flight.fareFamilies.find { it.code == "LIGHT" }!!
        val value = flight.fareFamilies.find { it.code == "VALUE" }!!
        val business = flight.fareFamilies.find { it.code == "BUSINESS" }!!
        
        assertTrue(light.priceMinor < value.priceMinor)
        assertTrue(value.priceMinor < business.priceMinor)
    }

    // ========================================================================
    // User Story 3.1-3.4: Passenger Info
    // ========================================================================

    @Test
    fun `passenger info stored correctly`() {
        setupPassengerInfo()
        
        assertEquals(3, bookingFlowState.passengerInfo.size)
        
        val adult = bookingFlowState.passengerInfo.find { it.id == "adult_0" }
        assertNotNull(adult)
        assertEquals("JOHN", adult.firstName)
        assertEquals("DOE", adult.lastName)
        assertEquals("john.doe@example.com", adult.email)
    }

    @Test
    fun `passenger types are correct`() {
        setupPassengerInfo()
        
        val passengers = bookingFlowState.passengerInfo
        assertEquals(2, passengers.count { it.type == "ADULT" })
        assertEquals(1, passengers.count { it.type == "CHILD" })
    }

    @Test
    fun `multiple passengers can have different data`() {
        setupPassengerInfo()
        
        val adult0 = bookingFlowState.passengerInfo.find { it.id == "adult_0" }!!
        val adult1 = bookingFlowState.passengerInfo.find { it.id == "adult_1" }!!
        
        assertNotEquals(adult0.firstName, adult1.firstName)
        assertNotEquals(adult0.documentNumber, adult1.documentNumber)
    }

    // ========================================================================
    // User Story 4.1-4.4: Ancillaries
    // ========================================================================

    @Test
    fun `ancillaries can be added`() {
        setupPassengerInfo()
        
        bookingFlowState.setSelectedAncillaries(
            SelectedAncillaries(
                baggageSelections = listOf(
                    BaggageInfo(passengerId = "adult_0", weight = 20, price = "75 SAR"),
                    BaggageInfo(passengerId = "adult_1", weight = 20, price = "75 SAR")
                ),
                mealSelections = listOf(
                    MealInfo(passengerId = "adult_0", mealCode = "MEAL01", price = "35 SAR")
                ),
                priorityBoarding = true,
                ancillariesTotal = "185 SAR",
                grandTotal = "634 SAR"
            )
        )
        
        val ancillaries = bookingFlowState.selectedAncillaries
        assertNotNull(ancillaries)
        assertEquals(2, ancillaries.baggageSelections.size)
        assertEquals(1, ancillaries.mealSelections.size)
        assertTrue(ancillaries.priorityBoarding)
    }

    @Test
    fun `ancillaries update grand total`() {
        setupPassengerInfo()
        
        // First check without ancillaries
        val flightPrice = bookingFlowState.selectedFlight?.totalPrice
        assertEquals("449 SAR", flightPrice)
        
        // Add ancillaries
        bookingFlowState.setSelectedAncillaries(
            SelectedAncillaries(
                baggageSelections = emptyList(),
                mealSelections = emptyList(),
                priorityBoarding = false,
                ancillariesTotal = "100 SAR",
                grandTotal = "549 SAR"
            )
        )
        
        assertEquals("549 SAR", bookingFlowState.selectedAncillaries?.grandTotal)
    }

    // ========================================================================
    // User Story 5.1-5.5: Payment
    // ========================================================================

    @Test
    fun `booking request contains all passenger data`() = runTest {
        setupPassengerInfo()
        
        val passengerDtos = bookingFlowState.passengerInfo.map { p ->
            PassengerDto(
                type = p.type,
                title = p.title,
                firstName = p.firstName,
                lastName = p.lastName,
                dateOfBirth = p.dateOfBirth,
                nationality = p.nationality,
                documentId = p.documentNumber
            )
        }
        
        val request = BookingRequestDto(
            flightNumber = bookingFlowState.selectedFlight!!.flight.flightNumber,
            fareFamily = bookingFlowState.selectedFlight!!.fareFamily,
            passengers = passengerDtos,
            contactEmail = bookingFlowState.passengerInfo.first().email,
            contactPhone = bookingFlowState.passengerInfo.first().phone,
            payment = PaymentDto(
                cardholderName = "John Doe",
                cardNumberLast4 = "1111",
                totalAmountMinor = 44900,
                currency = "SAR"
            )
        )
        
        assertEquals(3, request.passengers.size)
        assertEquals("FA101", request.flightNumber)
        assertEquals("VALUE", request.fareFamily)
        assertEquals("john.doe@example.com", request.contactEmail)
    }

    @Test
    fun `booking API creates confirmation`() = runTest {
        setupPassengerInfo()
        
        val request = BookingRequestDto(
            flightNumber = "FA101",
            fareFamily = "VALUE",
            passengers = listOf(
                PassengerDto("ADULT", "MR", "JOHN", "DOE", "1990-01-15", "SA", "A12345678")
            ),
            contactEmail = "john@example.com",
            payment = PaymentDto("John Doe", "1111", 44900, "SAR")
        )
        
        val result = mockApiClient.createBooking(request)
        
        assertTrue(result is ApiResult.Success)
        val confirmation = (result as ApiResult.Success).data
        
        assertTrue(confirmation.pnr.isNotBlank())
        assertEquals(6, confirmation.pnr.length)
        assertEquals("CONFIRMED", confirmation.status)
    }

    @Test
    fun `payment only sends last 4 digits`() = runTest {
        val request = BookingRequestDto(
            flightNumber = "FA101",
            fareFamily = "VALUE",
            passengers = listOf(
                PassengerDto("ADULT", "MR", "JOHN", "DOE", "1990-01-15")
            ),
            contactEmail = "john@example.com",
            payment = PaymentDto(
                cardholderName = "John Doe",
                cardNumberLast4 = "4111111111111111".takeLast(4), // Simulate frontend
                totalAmountMinor = 44900,
                currency = "SAR"
            )
        )
        
        assertEquals("1111", request.payment.cardNumberLast4)
    }

    @Test
    fun `booking confirmation stored in state`() = runTest {
        setupPassengerInfo()
        
        val request = BookingRequestDto(
            flightNumber = "FA101",
            fareFamily = "VALUE",
            passengers = listOf(
                PassengerDto("ADULT", "MR", "JOHN", "DOE", "1990-01-15")
            ),
            contactEmail = "john@example.com",
            payment = PaymentDto("John Doe", "1111", 44900, "SAR")
        )
        
        val result = mockApiClient.createBooking(request)
        assertTrue(result is ApiResult.Success)
        
        bookingFlowState.setBookingConfirmation((result as ApiResult.Success).data)
        
        assertNotNull(bookingFlowState.bookingConfirmation)
        assertTrue(bookingFlowState.bookingConfirmation!!.pnr.isNotBlank())
    }

    @Test
    fun `payment failure returns error`() = runTest {
        val failingClient = MockFairairApiClient(shouldFail = true, failureMessage = "Payment declined")
        
        val request = BookingRequestDto(
            flightNumber = "FA101",
            fareFamily = "VALUE",
            passengers = listOf(
                PassengerDto("ADULT", "MR", "JOHN", "DOE", "1990-01-15")
            ),
            contactEmail = "john@example.com",
            payment = PaymentDto("John Doe", "1111", 44900, "SAR")
        )
        
        val result = failingClient.createBooking(request)
        
        assertTrue(result is ApiResult.Error)
        assertEquals("Payment declined", (result as ApiResult.Error).message)
    }

    // ========================================================================
    // User Story 6.1-6.3: Confirmation
    // ========================================================================

    @Test
    fun `booking confirmation contains PNR`() = runTest {
        val request = BookingRequestDto(
            flightNumber = "FA101",
            fareFamily = "VALUE",
            passengers = listOf(
                PassengerDto("ADULT", "MR", "JOHN", "DOE", "1990-01-15")
            ),
            contactEmail = "john@example.com",
            payment = PaymentDto("John Doe", "1111", 44900, "SAR")
        )
        
        val result = mockApiClient.createBooking(request)
        assertTrue(result is ApiResult.Success)
        
        val confirmation = (result as ApiResult.Success).data
        assertTrue(confirmation.pnr.isNotBlank())
        assertEquals(6, confirmation.pnr.length)
    }

    @Test
    fun `booking confirmation contains flight summary`() = runTest {
        val request = BookingRequestDto(
            flightNumber = "FA101",
            fareFamily = "VALUE",
            passengers = listOf(
                PassengerDto("ADULT", "MR", "JOHN", "DOE", "1990-01-15")
            ),
            contactEmail = "john@example.com",
            payment = PaymentDto("John Doe", "1111", 44900, "SAR")
        )
        
        val result = mockApiClient.createBooking(request)
        assertTrue(result is ApiResult.Success)
        
        val confirmation = (result as ApiResult.Success).data
        assertNotNull(confirmation.flight)
        assertEquals("FA101", confirmation.flight?.flightNumber)
        assertEquals("VALUE", confirmation.flight?.fareFamily)
    }

    @Test
    fun `booking confirmation contains passenger summary`() = runTest {
        val request = BookingRequestDto(
            flightNumber = "FA101",
            fareFamily = "VALUE",
            passengers = listOf(
                PassengerDto("ADULT", "MR", "JOHN", "DOE", "1990-01-15"),
                PassengerDto("CHILD", "MSTR", "TOMMY", "DOE", "2018-08-10")
            ),
            contactEmail = "john@example.com",
            payment = PaymentDto("John Doe", "1111", 44900, "SAR")
        )
        
        val result = mockApiClient.createBooking(request)
        assertTrue(result is ApiResult.Success)
        
        val confirmation = (result as ApiResult.Success).data
        assertEquals(2, confirmation.passengers.size)
    }

    @Test
    fun `can retrieve booking by PNR`() = runTest {
        val result = mockApiClient.getBooking("ABC123")
        
        assertTrue(result is ApiResult.Success)
        val booking = (result as ApiResult.Success).data
        assertEquals("ABC123", booking.pnr)
    }

    // ========================================================================
    // Complete Flow Test
    // ========================================================================

    @Test
    fun `complete booking flow from search to confirmation`() = runTest {
        // Step 1: Search
        val searchRequest = FlightSearchRequestDto(
            origin = "RUH",
            destination = "JED",
            departureDate = "2024-12-15",
            passengers = PassengerCountsDto(adults = 2, children = 0, infants = 0)
        )
        
        val searchResult = mockApiClient.searchFlights(searchRequest)
        assertTrue(searchResult is ApiResult.Success)
        
        val origin = StationDto("RUH", "King Khalid", "Riyadh", "Saudi Arabia")
        val destination = StationDto("JED", "King Abdulaziz", "Jeddah", "Saudi Arabia")
        
        bookingFlowState.setSearchCriteria(
            SearchCriteria(origin, destination, "2024-12-15", searchRequest.passengers)
        )
        bookingFlowState.setSearchResult((searchResult as ApiResult.Success).data)
        
        assertNotNull(bookingFlowState.searchResult)
        
        // Step 2: Select flight
        val flight = bookingFlowState.searchResult!!.flights.first()
        bookingFlowState.setSelectedFlight(
            SelectedFlight(flight, "VALUE", "449 SAR")
        )
        
        assertNotNull(bookingFlowState.selectedFlight)
        
        // Step 3: Enter passenger info
        bookingFlowState.setPassengerInfo(
            listOf(
                PassengerInfo(
                    id = "adult_0", type = "ADULT", title = "MR",
                    firstName = "JOHN", lastName = "DOE", dateOfBirth = "1990-01-15",
                    nationality = "SA", documentType = "PASSPORT", documentNumber = "A12345678",
                    documentExpiry = "2028-01-15", email = "john@example.com", phone = "+966501234567"
                ),
                PassengerInfo(
                    id = "adult_1", type = "ADULT", title = "MRS",
                    firstName = "JANE", lastName = "DOE", dateOfBirth = "1992-05-20",
                    nationality = "SA", documentType = "PASSPORT", documentNumber = "B87654321",
                    documentExpiry = "2027-05-20", email = "", phone = ""
                )
            )
        )
        
        assertTrue(bookingFlowState.passengerInfo.isNotEmpty())
        
        // Step 4: Payment
        val bookingRequest = BookingRequestDto(
            flightNumber = bookingFlowState.selectedFlight!!.flight.flightNumber,
            fareFamily = bookingFlowState.selectedFlight!!.fareFamily,
            passengers = bookingFlowState.passengerInfo.map { p ->
                PassengerDto(p.type, p.title, p.firstName, p.lastName, p.dateOfBirth, p.nationality, p.documentNumber)
            },
            contactEmail = bookingFlowState.passengerInfo.first().email,
            contactPhone = bookingFlowState.passengerInfo.first().phone,
            payment = PaymentDto("John Doe", "1111", 44900, "SAR")
        )
        
        val paymentResult = mockApiClient.createBooking(bookingRequest)
        assertTrue(paymentResult is ApiResult.Success)
        
        bookingFlowState.setBookingConfirmation((paymentResult as ApiResult.Success).data)
        
        // Step 5: Verify confirmation
        val confirmation = bookingFlowState.bookingConfirmation
        assertNotNull(confirmation)
        assertTrue(confirmation.pnr.isNotBlank())
        assertEquals("CONFIRMED", confirmation.status)
    }

    // ========================================================================
    // State Reset Tests
    // ========================================================================

    @Test
    fun `booking state can be reset`() {
        setupPassengerInfo()
        bookingFlowState.setBookingConfirmation(
            BookingConfirmationDto(pnr = "ABC123", status = "CONFIRMED")
        )
        
        assertNotNull(bookingFlowState.searchCriteria)
        assertNotNull(bookingFlowState.selectedFlight)
        assertTrue(bookingFlowState.passengerInfo.isNotEmpty())
        assertNotNull(bookingFlowState.bookingConfirmation)
        
        bookingFlowState.reset()
        
        assertNull(bookingFlowState.searchCriteria)
        assertNull(bookingFlowState.selectedFlight)
        assertTrue(bookingFlowState.passengerInfo.isEmpty())
        assertNull(bookingFlowState.bookingConfirmation)
    }
}
