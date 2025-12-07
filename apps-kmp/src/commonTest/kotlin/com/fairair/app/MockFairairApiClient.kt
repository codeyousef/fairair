package com.fairair.app

import com.fairair.app.api.*
import com.fairair.contract.dto.*
import kotlinx.datetime.Clock

/**
 * Mock API client for testing.
 * Returns predictable data for all API calls.
 * 
 * Note: This is a standalone mock that doesn't extend FairairApiClient
 * to avoid issues with HttpClient initialization in tests.
 */
class MockFairairApiClient(
    private val shouldFail: Boolean = false,
    private val failureMessage: String = "Mock API failure"
) {

    // Track API calls for verification
    val searchFlightsCalls = mutableListOf<FlightSearchRequestDto>()
    val createBookingCalls = mutableListOf<BookingRequestDto>()
    val getBookingCalls = mutableListOf<String>()
    val loginCalls = mutableListOf<Pair<String, String>>()
    val chatMessageCalls = mutableListOf<ChatMessageCall>()
    val clearSessionCalls = mutableListOf<String>()

    data class ChatMessageCall(
        val sessionId: String,
        val message: String,
        val locale: String,
        val context: ChatContextDto?
    )

    suspend fun getRoutes(): ApiResult<RouteMapDto> {
        if (shouldFail) return mockError()
        return ApiResult.Success(
            RouteMapDto(
                routes = mapOf(
                    "RUH" to listOf("JED", "DMM", "MED"),
                    "JED" to listOf("RUH", "DMM", "MED"),
                    "DMM" to listOf("RUH", "JED"),
                    "MED" to listOf("RUH", "JED")
                )
            )
        )
    }

    suspend fun getStations(): ApiResult<List<StationDto>> {
        if (shouldFail) return mockError()
        return ApiResult.Success(MockData.stations)
    }

    suspend fun getDestinationsForOrigin(origin: String): ApiResult<List<StationDto>> {
        if (shouldFail) return mockError()
        val routeMap = mapOf(
            "RUH" to listOf("JED", "DMM", "MED"),
            "JED" to listOf("RUH", "DMM", "MED"),
            "DMM" to listOf("RUH", "JED"),
            "MED" to listOf("RUH", "JED")
        )
        val destinationCodes = routeMap[origin] ?: emptyList()
        return ApiResult.Success(
            MockData.stations.filter { it.code in destinationCodes }
        )
    }

    suspend fun searchFlights(request: FlightSearchRequestDto): ApiResult<FlightSearchResponseDto> {
        searchFlightsCalls.add(request)
        if (shouldFail) return mockError()
        return ApiResult.Success(
            FlightSearchResponseDto(
                flights = MockData.createFlights(request.origin, request.destination, request.departureDate),
                searchId = "SEARCH-${Clock.System.now().toEpochMilliseconds()}"
            )
        )
    }

    suspend fun getLowFares(
        origin: String,
        destination: String,
        startDate: String,
        endDate: String,
        adults: Int = 1,
        children: Int = 0,
        infants: Int = 0
    ): ApiResult<LowFaresResponseDto> {
        if (shouldFail) return mockError()
        return ApiResult.Success(MockData.createLowFares(origin, destination, startDate, endDate))
    }

    suspend fun createBooking(request: BookingRequestDto): ApiResult<BookingConfirmationDto> {
        createBookingCalls.add(request)
        if (shouldFail) return mockError()
        return ApiResult.Success(MockData.createBookingConfirmation(request))
    }

    suspend fun getBooking(pnr: String): ApiResult<BookingConfirmationDto> {
        getBookingCalls.add(pnr)
        if (shouldFail) return mockError()
        return ApiResult.Success(MockData.createSavedBooking(pnr))
    }

    suspend fun login(email: String, password: String): Result<LoginResponseDto> {
        loginCalls.add(email to password)
        if (shouldFail) return Result.failure(Exception(failureMessage))
        return if (email == "test@example.com" && password == "password123") {
            Result.success(
                LoginResponseDto(
                    accessToken = "mock-access-token-${Clock.System.now().toEpochMilliseconds()}",
                    refreshToken = "mock-refresh-token-${Clock.System.now().toEpochMilliseconds()}",
                    expiresIn = 3600,
                    tokenType = "Bearer"
                )
            )
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    suspend fun getMyBookings(authToken: String): ApiResult<List<BookingConfirmationDto>> {
        if (shouldFail) return mockError()
        if (authToken.isBlank()) return ApiResult.Error("UNAUTHORIZED", "No auth token provided", false)
        return ApiResult.Success(
            listOf(
                MockData.createSavedBooking("ABC123"),
                MockData.createSavedBooking("XYZ789")
            )
        )
    }

    suspend fun createBookingAuthenticated(
        request: BookingRequestDto,
        authToken: String?
    ): ApiResult<BookingConfirmationDto> {
        createBookingCalls.add(request)
        if (shouldFail) return mockError()
        return ApiResult.Success(MockData.createBookingConfirmation(request))
    }

    // ========================================================================
    // Chat API Methods
    // ========================================================================

    suspend fun sendChatMessage(
        sessionId: String,
        message: String,
        locale: String = "en-US",
        context: ChatContextDto? = null
    ): ApiResult<ChatResponseDto> {
        chatMessageCalls.add(ChatMessageCall(sessionId, message, locale, context))
        if (shouldFail) return mockError()
        
        // Generate mock response based on message content
        val response = when {
            // Arabic greeting
            message.contains("مرحبا") || message.contains("السلام") -> ChatResponseDto(
                text = "هلا وغلا! أنا فارس، مساعدك الذكي من فلاي أديل. كيف أقدر أساعدك اليوم؟",
                suggestions = listOf("البحث عن رحلة", "إدارة الحجز", "تسجيل الوصول"),
                detectedLanguage = "ar"
            )
            
            // Flight search intent
            message.contains("flight", ignoreCase = true) || 
            message.contains("fly", ignoreCase = true) -> ChatResponseDto(
                text = "I can help you find a flight! Where would you like to travel from and to?",
                suggestions = listOf("Riyadh to Jeddah", "Jeddah to Dammam", "Show popular routes"),
                detectedLanguage = "en",
                uiType = null
            )
            
            // Booking lookup intent
            message.contains("booking", ignoreCase = true) ||
            message.contains("pnr", ignoreCase = true) -> ChatResponseDto(
                text = "I can help you with your booking. Please provide your 6-character booking reference (PNR).",
                suggestions = listOf("ABC123", "Find my booking"),
                detectedLanguage = "en"
            )
            
            // Seat change intent
            message.contains("seat", ignoreCase = true) -> ChatResponseDto(
                text = "I can help you change your seat. Would you prefer a window or aisle seat?",
                suggestions = listOf("Window seat", "Aisle seat", "Extra legroom"),
                detectedLanguage = "en"
            )
            
            // English greeting/help
            message.contains("hello", ignoreCase = true) ||
            message.contains("hi", ignoreCase = true) ||
            message.contains("help", ignoreCase = true) -> ChatResponseDto(
                text = "Hello! I'm Faris, your FareAir assistant. I can help you:\n\n" +
                       "• Search for flights\n" +
                       "• Manage your booking\n" +
                       "• Change seats\n" +
                       "• Check-in\n\n" +
                       "How can I help you today?",
                suggestions = listOf("Search for a flight", "Check my booking", "Help me check in"),
                detectedLanguage = "en"
            )
            
            // Default response
            else -> ChatResponseDto(
                text = "I'm here to help with your travel needs. What would you like to do?",
                suggestions = listOf("Search flights", "Manage booking", "Help"),
                detectedLanguage = "en"
            )
        }
        
        return ApiResult.Success(response)
    }

    suspend fun clearChatSession(sessionId: String): ApiResult<Unit> {
        clearSessionCalls.add(sessionId)
        if (shouldFail) return mockError()
        return ApiResult.Success(Unit)
    }

    private fun <T> mockError(): ApiResult<T> {
        return ApiResult.Error(
            code = "MOCK_ERROR",
            message = failureMessage,
            isRetryable = false
        )
    }

    fun reset() {
        searchFlightsCalls.clear()
        createBookingCalls.clear()
        getBookingCalls.clear()
        loginCalls.clear()
        chatMessageCalls.clear()
        clearSessionCalls.clear()
    }
}

/**
 * Mock data factory for tests.
 */
object MockData {
    val stations = listOf(
        StationDto("RUH", "King Khalid International Airport", "Riyadh", "Saudi Arabia"),
        StationDto("JED", "King Abdulaziz International Airport", "Jeddah", "Saudi Arabia"),
        StationDto("DMM", "King Fahd International Airport", "Dammam", "Saudi Arabia"),
        StationDto("MED", "Prince Mohammad Bin Abdulaziz International Airport", "Medina", "Saudi Arabia")
    )

    fun createFlights(origin: String, destination: String, departureDate: String): List<FlightDto> {
        return listOf(
            FlightDto(
                flightNumber = "FA101",
                origin = origin,
                destination = destination,
                departureTime = "${departureDate}T08:00:00",
                arrivalTime = "${departureDate}T10:00:00",
                durationMinutes = 120,
                durationFormatted = "2h 0m",
                aircraft = "A320",
                fareFamilies = createFareFamilies(),
                seatsAvailable = 150,
                seatsBooked = 50
            ),
            FlightDto(
                flightNumber = "FA102",
                origin = origin,
                destination = destination,
                departureTime = "${departureDate}T14:00:00",
                arrivalTime = "${departureDate}T16:00:00",
                durationMinutes = 120,
                durationFormatted = "2h 0m",
                aircraft = "A320",
                fareFamilies = createFareFamilies(),
                seatsAvailable = 150,
                seatsBooked = 75
            ),
            FlightDto(
                flightNumber = "FA103",
                origin = origin,
                destination = destination,
                departureTime = "${departureDate}T20:00:00",
                arrivalTime = "${departureDate}T22:00:00",
                durationMinutes = 120,
                durationFormatted = "2h 0m",
                aircraft = "A320",
                fareFamilies = createFareFamilies(),
                seatsAvailable = 150,
                seatsBooked = 30
            )
        )
    }

    private fun createFareFamilies(): List<FareFamilyDto> {
        return listOf(
            FareFamilyDto(
                code = "LIGHT",
                name = "Light",
                priceMinor = 29900,
                priceFormatted = "299 SAR",
                currency = "SAR",
                inclusions = FareInclusionsDto(
                    carryOnBag = "7 kg",
                    checkedBag = null,
                    seatSelection = "Standard",
                    changePolicy = "Fee applies",
                    cancellationPolicy = "Non-refundable",
                    priorityBoarding = false,
                    loungeAccess = false
                )
            ),
            FareFamilyDto(
                code = "VALUE",
                name = "Value",
                priceMinor = 44900,
                priceFormatted = "449 SAR",
                currency = "SAR",
                inclusions = FareInclusionsDto(
                    carryOnBag = "7 kg",
                    checkedBag = "20 kg",
                    seatSelection = "Standard + Extra Legroom",
                    changePolicy = "One free change",
                    cancellationPolicy = "Partial refund",
                    priorityBoarding = false,
                    loungeAccess = false
                )
            ),
            FareFamilyDto(
                code = "BUSINESS",
                name = "Business",
                priceMinor = 89900,
                priceFormatted = "899 SAR",
                currency = "SAR",
                inclusions = FareInclusionsDto(
                    carryOnBag = "10 kg",
                    checkedBag = "30 kg",
                    seatSelection = "Free selection + Priority",
                    changePolicy = "Unlimited changes",
                    cancellationPolicy = "Full refund",
                    priorityBoarding = true,
                    loungeAccess = true
                )
            )
        )
    }

    fun createLowFares(origin: String, destination: String, startDate: String, endDate: String): LowFaresResponseDto {
        // Generate low fares for a week
        val dates = (0..6).map { dayOffset ->
            val priceBase = 29900L + (dayOffset * 2000)
            LowFareDateDto(
                date = startDate, // In real code would offset the date
                priceMinor = priceBase,
                priceFormatted = "${priceBase / 100} SAR",
                currency = "SAR",
                fareFamily = "LIGHT",
                flightsAvailable = 3,
                available = true
            )
        }
        return LowFaresResponseDto(
            origin = origin,
            destination = destination,
            dates = dates
        )
    }

    fun createBookingConfirmation(request: BookingRequestDto): BookingConfirmationDto {
        val pnr = generatePnr()
        return BookingConfirmationDto(
            pnr = pnr,
            bookingReference = pnr,
            flight = FlightSummaryDto(
                flightNumber = request.flightNumber,
                origin = "RUH",
                destination = "JED",
                departureTime = "2024-12-01T08:00:00",
                fareFamily = request.fareFamily
            ),
            passengers = request.passengers.map { passenger ->
                PassengerSummaryDto(
                    fullName = "${passenger.firstName} ${passenger.lastName}",
                    type = passenger.type
                )
            },
            status = "CONFIRMED",
            totalPaidMinor = request.payment.totalAmountMinor,
            totalPaidFormatted = "${request.payment.totalAmountMinor / 100} ${request.payment.currency}",
            currency = request.payment.currency,
            createdAt = "2024-12-01T00:00:00Z"
        )
    }

    fun createSavedBooking(pnr: String): BookingConfirmationDto {
        return BookingConfirmationDto(
            pnr = pnr,
            bookingReference = pnr,
            flight = FlightSummaryDto(
                flightNumber = "FA101",
                origin = "RUH",
                destination = "JED",
                departureTime = "2024-12-01T08:00:00",
                fareFamily = "VALUE"
            ),
            passengers = listOf(
                PassengerSummaryDto(fullName = "John Doe", type = "ADULT")
            ),
            status = "CONFIRMED",
            totalPaidMinor = 44900,
            totalPaidFormatted = "449 SAR",
            currency = "SAR",
            createdAt = "2024-11-15T10:30:00Z"
        )
    }

    private var pnrCounter = 0
    
    private fun generatePnr(): String {
        // Use deterministic PNR generation for tests
        pnrCounter++
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val base = pnrCounter.toString().padStart(6, '0')
        return base.take(6).map { 
            if (it.isDigit()) chars[it.digitToInt()] else it 
        }.joinToString("")
    }
}

/**
 * In-memory mock local storage for testing.
 * Does not depend on platform-specific Settings implementation.
 */
class MockLocalStorage {
    
    private val savedBookings = mutableListOf<BookingConfirmationDto>()
    private val searchHistory = mutableListOf<SearchHistoryEntry>()
    private var authToken: String? = null
    private var currentLanguage = "en"
    private var cachedRoutes: CachedRoutes? = null
    
    // ============ Authentication ============
    
    fun saveAuthToken(token: String) {
        authToken = token
    }
    
    fun getAuthToken(): String? = authToken
    
    fun clearAuth() {
        authToken = null
    }
    
    // ============ Saved Bookings ============
    
    suspend fun saveBooking(booking: BookingConfirmationDto) {
        savedBookings.removeAll { it.pnr == booking.pnr }
        savedBookings.add(booking)
    }
    
    fun getSavedBookingsList(): List<BookingConfirmationDto> = savedBookings.toList()
    
    suspend fun deleteBooking(pnr: String) {
        savedBookings.removeAll { it.pnr == pnr }
    }
    
    suspend fun clearAllBookings() {
        savedBookings.clear()
    }
    
    // ============ Language ============
    
    suspend fun getCurrentLanguage(): String = currentLanguage
    
    suspend fun setCurrentLanguage(language: String) {
        require(language in listOf("en", "ar")) { "Language must be 'en' or 'ar'" }
        currentLanguage = language
    }
    
    // ============ Route Cache ============
    
    data class CachedRoutes(
        val routes: Map<String, List<String>>,
        val timestamp: Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    )
    
    suspend fun cacheRoutes(routes: Map<String, List<String>>) {
        cachedRoutes = CachedRoutes(routes)
    }
    
    suspend fun getCachedRoutes(): CachedRoutes? = cachedRoutes
    
    // ============ Search History ============
    
    data class SearchHistoryEntry(
        val origin: String,
        val destination: String,
        val departureDate: String,
        val timestamp: Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    )
    
    suspend fun addSearchToHistory(origin: String, destination: String, departureDate: String) {
        // Remove duplicates
        searchHistory.removeAll { it.origin == origin && it.destination == destination }
        // Add at the beginning
        searchHistory.add(0, SearchHistoryEntry(origin, destination, departureDate))
        // Keep only 10
        while (searchHistory.size > 10) {
            searchHistory.removeAt(searchHistory.size - 1)
        }
    }
    
    fun getSearchHistory(): List<SearchHistoryEntry> = searchHistory.toList()
    
    suspend fun clearSearchHistory() {
        searchHistory.clear()
    }
    
    // ============ Clear All ============
    
    suspend fun clearAll() {
        savedBookings.clear()
        searchHistory.clear()
        currentLanguage = "en"
        cachedRoutes = null
    }
}
