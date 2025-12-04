package com.fairair.app.api

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.dto.LoginRequestDto
import com.fairair.contract.dto.LoginResponseDto
import com.fairair.contract.dto.UserInfoDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.random.Random

/**
 * Configuration for retry behavior on transient failures.
 * Uses exponential backoff with jitter.
 */
data class RetryConfig(
    /** Maximum number of retry attempts (not counting the initial request) */
    val maxRetries: Int = 3,
    /** Initial delay before first retry in milliseconds */
    val initialDelayMs: Long = 500L,
    /** Maximum delay between retries in milliseconds */
    val maxDelayMs: Long = 10_000L,
    /** Multiplier for exponential backoff */
    val backoffMultiplier: Double = 2.0,
    /** Jitter factor (0.0-1.0) to randomize delays */
    val jitterFactor: Double = 0.25
) {
    companion object {
        /** Default retry configuration */
        val Default = RetryConfig()

        /** No retries - fail immediately */
        val NoRetry = RetryConfig(maxRetries = 0)

        /** Aggressive retry for critical operations */
        val Aggressive = RetryConfig(
            maxRetries = 5,
            initialDelayMs = 300L,
            maxDelayMs = 15_000L
        )
    }
}

/**
 * API client for the fairair backend.
 * Uses Ktor client for cross-platform HTTP requests with automatic retry for transient failures.
 */
class FairairApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
    private val retryConfig: RetryConfig = RetryConfig.Default
) {
    /**
     * Fetches the route map from the backend.
     * @return RouteMapResponse with available routes
     */
    suspend fun getRoutes(): ApiResult<RouteMapDto> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Config.ROUTES}").body()
        }
    }

    /**
     * Fetches all stations from the backend.
     * @return List of StationDto
     */
    suspend fun getStations(): ApiResult<List<StationDto>> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Config.STATIONS}").body()
        }
    }

    /**
     * Fetches available destinations for a given origin.
     * @param origin Origin airport code
     * @return List of StationDto for valid destinations
     */
    suspend fun getDestinationsForOrigin(origin: String): ApiResult<List<StationDto>> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Config.destinationsFor(origin)}").body()
        }
    }

    /**
     * Searches for flights based on the given criteria.
     * @param request Search parameters
     * @return FlightSearchResponseDto with available flights
     */
    suspend fun searchFlights(request: FlightSearchRequestDto): ApiResult<FlightSearchResponseDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Search.FLIGHTS}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }
    
    /**
     * Fetches lowest fare prices for a date range.
     * Used for calendar displays showing price variations.
     * 
     * @param origin Origin airport code
     * @param destination Destination airport code
     * @param startDate Start date (ISO format: YYYY-MM-DD)
     * @param endDate End date (ISO format: YYYY-MM-DD)
     * @param adults Number of adult passengers
     * @param children Number of child passengers
     * @param infants Number of infant passengers
     * @return LowFaresResponseDto with price per date
     */
    suspend fun getLowFares(
        origin: String,
        destination: String,
        startDate: String,
        endDate: String,
        adults: Int = 1,
        children: Int = 0,
        infants: Int = 0
    ): ApiResult<LowFaresResponseDto> {
        return safeApiCall {
            val url = "$baseUrl${ApiRoutes.Search.lowFaresUrl(origin, destination, startDate, endDate, adults, children, infants)}"
            httpClient.get(url).body()
        }
    }

    /**
     * Creates a booking.
     * @param request Booking details
     * @return BookingConfirmationDto on success
     */
    suspend fun createBooking(request: BookingRequestDto): ApiResult<BookingConfirmationDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Booking.CREATE}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    /**
     * Retrieves a booking by PNR.
     * @param pnr The 6-character PNR code
     * @return BookingConfirmationDto or error if not found
     */
    suspend fun getBooking(pnr: String): ApiResult<BookingConfirmationDto> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Booking.byPnr(pnr)}").body()
        }
    }

    /**
     * Login with email and password.
     * @param email User email
     * @param password User password
     * @return LoginResponseDto with JWT token on success
     */
    suspend fun login(email: String, password: String): Result<LoginResponseDto> {
        return try {
            val response = httpClient.post("$baseUrl${ApiRoutes.Auth.LOGIN}") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequestDto(email, password))
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Login failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves all bookings for the authenticated user.
     * @param authToken The JWT access token
     * @return List of BookingConfirmationDto or error if not authenticated
     */
    suspend fun getMyBookings(authToken: String): ApiResult<List<BookingConfirmationDto>> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Booking.USER_BOOKINGS}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }.body()
        }
    }

    // ============================================================================
    // Check-In API
    // ============================================================================

    /**
     * Looks up a booking for check-in.
     * @param pnr The 6-character PNR code
     * @param lastName Passenger last name
     * @return CheckInLookupResponseDto with booking details for check-in
     */
    suspend fun lookupForCheckIn(pnr: String, lastName: String): ApiResult<CheckInLookupResponseDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.CheckIn.INITIATE}") {
                contentType(ContentType.Application.Json)
                setBody(CheckInLookupRequestDto(pnr, lastName))
            }.body()
        }
    }

    /**
     * Processes check-in for selected passengers.
     * @param request Check-in details with passenger selections
     * @return CheckInResultDto with boarding passes
     */
    suspend fun processCheckIn(request: CheckInProcessRequestDto): ApiResult<CheckInResultDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.CheckIn.completeFor(request.pnr)}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    /**
     * Retrieves a boarding pass for a checked-in passenger.
     * @param pnr The PNR code
     * @param passengerIndex The passenger index (0-based)
     * @return BoardingPassDto with boarding details
     */
    suspend fun getBoardingPass(pnr: String, passengerIndex: Int): ApiResult<BoardingPassDto> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.CheckIn.boardingPassFor(pnr, passengerIndex)}").body()
        }
    }

    // ============================================================================
    // Manage Booking API
    // ============================================================================

    /**
     * Retrieves booking details for modification.
     * @param pnr The 6-character PNR code
     * @param lastName Passenger last name
     * @return ManageBookingResponseDto with full booking details
     */
    suspend fun retrieveBooking(pnr: String, lastName: String): ApiResult<ManageBookingResponseDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.ManageBooking.RETRIEVE}") {
                contentType(ContentType.Application.Json)
                setBody(RetrieveBookingRequestDto(pnr, lastName))
            }.body()
        }
    }

    /**
     * Modifies a booking (updates passengers).
     * @param pnr The PNR code
     * @param request Modification details
     * @return ModifyBookingResponseDto with updated booking
     */
    suspend fun modifyBooking(pnr: String, request: ModifyBookingRequestDto): ApiResult<ModifyBookingResponseDto> {
        return safeApiCall {
            httpClient.put("$baseUrl${ApiRoutes.ManageBooking.updatePassengersFor(pnr)}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    /**
     * Cancels a booking.
     * @param pnr The PNR code
     * @param request Cancellation details
     * @return CancelBookingResponseDto with refund information
     */
    suspend fun cancelBooking(pnr: String, request: CancelBookingRequestDto): ApiResult<CancelBookingResponseDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.ManageBooking.cancelFor(pnr)}") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    // ============================================================================
    // Membership API
    // ============================================================================

    /**
     * Gets all available membership plans.
     * @return List of MembershipPlanDto
     */
    suspend fun getMembershipPlans(): ApiResult<List<MembershipPlanDto>> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Membership.PLANS}").body()
        }
    }

    /**
     * Gets user's active subscription.
     * @param authToken JWT access token
     * @return SubscriptionDto or null if no active subscription
     */
    suspend fun getSubscription(authToken: String): ApiResult<SubscriptionDto?> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Membership.STATUS}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }.body()
        }
    }

    /**
     * Subscribes to a membership plan.
     * @param request Subscription details with payment info
     * @param authToken JWT access token
     * @return SubscriptionDto with new subscription details
     */
    suspend fun subscribe(request: SubscribeRequestDto, authToken: String): ApiResult<SubscriptionDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Membership.SUBSCRIBE}") {
                contentType(ContentType.Application.Json)
                setBody(request)
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }.body()
        }
    }

    /**
     * Cancels current subscription.
     * @param authToken JWT access token
     * @return Cancellation confirmation
     */
    suspend fun cancelSubscription(authToken: String): ApiResult<CancelSubscriptionResponseDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Membership.CANCEL}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }.body()
        }
    }

    /**
     * Gets subscription usage statistics.
     * @param authToken JWT access token
     * @return UsageStatsDto with usage details
     */
    suspend fun getUsageStats(authToken: String): ApiResult<UsageStatsDto> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Membership.USAGE}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }.body()
        }
    }

    // ============================================================================
    // Enhanced Ancillaries API
    // ============================================================================

    /**
     * Gets seat map for a flight.
     * @param flightNumber The flight number
     * @param departureDate The departure date (ISO format)
     * @return SeatMapDto with seat availability
     */
    suspend fun getSeatMap(flightNumber: String, departureDate: String): ApiResult<SeatMapDto> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Seats.mapFor(flightNumber, departureDate)}").body()
        }
    }

    /**
     * Gets available meal options for a flight.
     * @param origin Origin airport code
     * @param destination Destination airport code
     * @return List of MealOptionDto
     */
    suspend fun getMealOptions(origin: String, destination: String): ApiResult<List<MealOptionDto>> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Meals.availableFor(origin, destination)}").body()
        }
    }
    
    /**
     * Creates a booking with optional authentication.
     * @param request Booking details
     * @param authToken Optional JWT access token for logged-in users
     * @return BookingConfirmationDto on success
     */
    suspend fun createBookingAuthenticated(request: BookingRequestDto, authToken: String?): ApiResult<BookingConfirmationDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Booking.CREATE}") {
                contentType(ContentType.Application.Json)
                setBody(request)
                if (authToken != null) {
                    header(HttpHeaders.Authorization, "Bearer $authToken")
                }
            }.body()
        }
    }

    /**
     * Wraps API calls with error handling and automatic retry for transient failures.
     * Uses exponential backoff with jitter for retries.
     *
     * Retryable conditions:
     * - Network connectivity issues (IOException, timeout)
     * - Server errors (5xx status codes, except 501 Not Implemented)
     * - Rate limiting (429 Too Many Requests)
     *
     * Non-retryable conditions:
     * - Client errors (4xx status codes, except 429)
     * - Parse/serialization errors
     */
    private suspend inline fun <reified T> safeApiCall(
        customRetryConfig: RetryConfig? = null,
        crossinline block: suspend () -> T
    ): ApiResult<T> {
        val config = customRetryConfig ?: retryConfig
        var lastException: Exception? = null
        var lastErrorResult: ApiResult.Error? = null

        repeat(config.maxRetries + 1) { attempt ->
            try {
                return ApiResult.Success(block())
            } catch (e: ClientRequestException) {
                // Client errors (4xx) - check if retryable
                val statusCode = e.response.status.value
                if (isRetryableClientError(statusCode)) {
                    lastException = e
                    lastErrorResult = ApiResult.Error(
                        code = "HTTP_$statusCode",
                        message = "Rate limited. Retrying...",
                        isRetryable = true
                    )
                } else {
                    // Non-retryable client error - return immediately
                    val errorBody = try {
                        e.response.bodyAsText()
                    } catch (_: Exception) {
                        ""
                    }
                    return ApiResult.Error(
                        code = "HTTP_$statusCode",
                        message = errorBody.ifEmpty { e.message },
                        isRetryable = false
                    )
                }
            } catch (e: ServerResponseException) {
                // Server errors (5xx) - generally retryable
                val statusCode = e.response.status.value
                if (isRetryableServerError(statusCode)) {
                    lastException = e
                    lastErrorResult = ApiResult.Error(
                        code = "SERVER_ERROR",
                        message = "Server error: $statusCode",
                        isRetryable = true
                    )
                } else {
                    return ApiResult.Error(
                        code = "SERVER_ERROR_$statusCode",
                        message = "Server error: $statusCode",
                        isRetryable = false
                    )
                }
            } catch (e: HttpRequestTimeoutException) {
                // Timeout - retryable
                lastException = e
                lastErrorResult = ApiResult.Error(
                    code = "TIMEOUT",
                    message = "Request timed out",
                    isRetryable = true
                )
            } catch (e: Exception) {
                // Network and other errors - check if retryable
                if (isRetryableException(e)) {
                    lastException = e
                    lastErrorResult = ApiResult.Error(
                        code = "NETWORK_ERROR",
                        message = e.message ?: "Network error",
                        isRetryable = true
                    )
                } else {
                    return ApiResult.Error(
                        code = "ERROR",
                        message = e.message ?: "Unknown error",
                        isRetryable = false
                    )
                }
            }

            // If we have more retries, delay before next attempt
            if (attempt < config.maxRetries) {
                val delayMs = calculateBackoffDelay(attempt, config)
                delay(delayMs)
            }
        }

        // All retries exhausted - return the last error
        return lastErrorResult ?: ApiResult.Error(
            code = "NETWORK_ERROR",
            message = lastException?.message ?: "Request failed after ${config.maxRetries} retries",
            isRetryable = false
        )
    }

    /**
     * Calculates delay for exponential backoff with jitter.
     */
    private fun calculateBackoffDelay(attempt: Int, config: RetryConfig): Long {
        // Calculate base delay with exponential backoff
        var delayMs = config.initialDelayMs
        repeat(attempt) {
            delayMs = (delayMs * config.backoffMultiplier).toLong()
        }
        delayMs = min(delayMs, config.maxDelayMs)

        // Add jitter to prevent thundering herd
        val jitter = (delayMs * config.jitterFactor * Random.nextDouble()).toLong()
        return delayMs + jitter
    }

    /**
     * Determines if a client error (4xx) is retryable.
     * Only 429 (Too Many Requests) is retryable.
     */
    private fun isRetryableClientError(statusCode: Int): Boolean {
        return statusCode == 429 // Too Many Requests
    }

    /**
     * Determines if a server error (5xx) is retryable.
     * 501 (Not Implemented) is not retryable.
     */
    private fun isRetryableServerError(statusCode: Int): Boolean {
        return statusCode in 500..599 && statusCode != 501
    }

    /**
     * Determines if an exception is retryable.
     * Network-related exceptions are retryable.
     */
    private fun isRetryableException(e: Exception): Boolean {
        val className = e::class.simpleName ?: ""
        return className.contains("IOException", ignoreCase = true) ||
               className.contains("Connect", ignoreCase = true) ||
               className.contains("Socket", ignoreCase = true) ||
               className.contains("Timeout", ignoreCase = true) ||
               className.contains("Network", ignoreCase = true) ||
               className.contains("UnknownHost", ignoreCase = true) ||
               e.message?.contains("connection", ignoreCase = true) == true ||
               e.message?.contains("timeout", ignoreCase = true) == true ||
               e.message?.contains("network", ignoreCase = true) == true
    }

    companion object {
        /**
         * Creates a configured HttpClient for the API.
         */
        fun createHttpClient(engine: io.ktor.client.engine.HttpClientEngine? = null): HttpClient {
            val clientConfig: HttpClientConfig<*>.() -> Unit = {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = false
                    })
                }
                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 10_000
                    socketTimeoutMillis = 30_000
                }
                defaultRequest {
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }
            }

            return if (engine != null) {
                HttpClient(engine, clientConfig)
            } else {
                HttpClient(clientConfig)
            }
        }
    }
}

/**
 * Sealed class for API results.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val code: String,
        val message: String,
        val isRetryable: Boolean = false
    ) : ApiResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): Error? = this as? Error

    inline fun <R> map(transform: (T) -> R): ApiResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (Error) -> Unit): ApiResult<T> {
        if (this is Error) action(this)
        return this
    }
}

// DTO classes for API communication
// Auth DTOs are imported from com.fairair.contract.dto

@Serializable
data class RouteMapDto(
    val routes: Map<String, List<String>>
)

@Serializable
data class StationDto(
    val code: String,
    val name: String,
    val city: String,
    val country: String
)

@Serializable
data class FlightSearchRequestDto(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String? = null,
    val passengers: PassengerCountsDto
)

@Serializable
data class PassengerCountsDto(
    val adults: Int,
    val children: Int,
    val infants: Int
)

@Serializable
data class FlightSearchResponseDto(
    val flights: List<FlightDto>,
    val searchId: String = ""
)

/**
 * Response DTO for low-fare calendar.
 */
@Serializable
data class LowFaresResponseDto(
    val origin: String,
    val destination: String,
    val dates: List<LowFareDateDto>
)

/**
 * DTO for a single date's lowest fare.
 */
@Serializable
data class LowFareDateDto(
    /** The date in ISO format (YYYY-MM-DD) */
    val date: String,
    /** The lowest fare price in minor units, or null if no flights */
    val priceMinor: Long? = null,
    /** Formatted price display (e.g., "350 SAR"), or null */
    val priceFormatted: String? = null,
    /** Currency code */
    val currency: String? = null,
    /** The fare family code of the lowest fare */
    val fareFamily: String? = null,
    /** Number of flights available on this date */
    val flightsAvailable: Int = 0,
    /** Whether flights are available on this date */
    val available: Boolean = false
)

/**
 * Flight DTO matching backend SearchController.FlightDto
 */
@Serializable
data class FlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int = 0,
    val durationFormatted: String = "",
    val aircraft: String = "",
    val fareFamilies: List<FareFamilyDto> = emptyList(),
    val seatsAvailable: Int = 0,
    val seatsBooked: Int = 0
) {
    /** Convenience property for display */
    val duration: String get() = durationFormatted.ifEmpty { "${durationMinutes}m" }

    /** Convert fareFamilies to simpler fares for UI display */
    val fares: List<FareDto> get() = fareFamilies.map { ff ->
        FareDto(
            fareFamily = ff.name,
            fareFamilyCode = ff.code,
            basePrice = ff.priceFormatted,
            totalPrice = ff.priceFormatted,
            currency = ff.currency,
            inclusions = buildInclusionsList(ff.inclusions)
        )
    }

    private fun buildInclusionsList(inclusions: FareInclusionsDto): List<String> {
        val list = mutableListOf<String>()
        list.add("Carry-on: ${inclusions.carryOnBag}")
        inclusions.checkedBag?.let { list.add("Checked bag: $it") }
        list.add("Seat: ${inclusions.seatSelection}")
        if (inclusions.priorityBoarding) list.add("Priority boarding")
        if (inclusions.loungeAccess) list.add("Lounge access")
        return list
    }
}

/**
 * Fare family DTO matching backend SearchController.FareFamilyDto
 */
@Serializable
data class FareFamilyDto(
    val code: String,
    val name: String,
    val priceMinor: Long,
    val priceFormatted: String,
    val currency: String,
    val inclusions: FareInclusionsDto
)

/**
 * Fare inclusions DTO matching backend SearchController.FareInclusionsDto
 */
@Serializable
data class FareInclusionsDto(
    val carryOnBag: String,
    val checkedBag: String? = null,
    val seatSelection: String,
    val changePolicy: String,
    val cancellationPolicy: String,
    val priorityBoarding: Boolean,
    val loungeAccess: Boolean
)

/**
 * Simplified fare DTO for UI display.
 */
@Serializable
data class FareDto(
    val fareFamily: String,
    val fareFamilyCode: String = "",
    val basePrice: String = "0",
    val totalPrice: String = "0",
    val currency: String = "SAR",
    val inclusions: List<String> = emptyList()
)

@Serializable
data class BookingRequestDto(
    val searchId: String = "",
    val flightNumber: String,
    val fareFamily: String,
    val passengers: List<PassengerDto>,
    val ancillaries: List<AncillaryDto> = emptyList(),
    val contactEmail: String,
    val contactPhone: String = "",
    val payment: PaymentDto
)

@Serializable
data class PassengerDto(
    val type: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val nationality: String = "",
    val documentId: String = ""
)

@Serializable
data class AncillaryDto(
    val type: String,
    val passengerIndex: Int,
    val priceMinor: Long = 0,
    val currency: String = "SAR"
)

@Serializable
data class PaymentDto(
    val cardholderName: String,
    val cardNumberLast4: String,
    val totalAmountMinor: Long,
    val currency: String
)

@Serializable
data class FlightSummaryDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val fareFamily: String
)

@Serializable
data class PassengerSummaryDto(
    val fullName: String,
    val type: String
)

@Serializable
data class BookingConfirmationDto(
    val pnr: String = "",
    val bookingReference: String = "",
    val flight: FlightSummaryDto? = null,
    val passengers: List<PassengerSummaryDto> = emptyList(),
    val status: String = "CONFIRMED",
    val totalPaidMinor: Long = 0,
    val totalPaidFormatted: String = "0",
    val currency: String = "SAR",
    val createdAt: String = ""
) {
    /** Convenience property for display */
    val totalPrice: String get() = totalPaidFormatted.ifEmpty { (totalPaidMinor / 100.0).toString() }
}

// ============================================================================
// Check-In DTOs
// ============================================================================

@Serializable
data class CheckInLookupRequestDto(
    val pnr: String,
    val lastName: String
)

@Serializable
data class CheckInLookupResponseDto(
    val pnr: String,
    val flight: CheckInFlightDto,
    val passengers: List<CheckInPassengerDto>,
    val isEligibleForCheckIn: Boolean,
    val eligibilityMessage: String? = null
)

@Serializable
data class CheckInFlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val departureDate: String,
    val aircraft: String = ""
)

@Serializable
data class CheckInPassengerDto(
    val passengerId: String,
    val firstName: String,
    val lastName: String,
    val type: String,
    val isCheckedIn: Boolean = false,
    val seatAssignment: String? = null,
    val boardingGroup: String? = null
)

@Serializable
data class CheckInProcessRequestDto(
    val pnr: String,
    val passengerIds: List<String>,
    val seatPreferences: Map<String, SeatPreferenceDto> = emptyMap()
)

@Serializable
data class SeatPreferenceDto(
    val preferWindow: Boolean = false,
    val preferAisle: Boolean = false,
    val preferFront: Boolean = false
)

@Serializable
data class CheckInResultDto(
    val pnr: String,
    val checkedInPassengers: List<CheckedInPassengerDto>,
    val message: String
)

@Serializable
data class CheckedInPassengerDto(
    val passengerId: String,
    val name: String,
    val seatNumber: String,
    val boardingGroup: String,
    val boardingPassUrl: String? = null
)

@Serializable
data class BoardingPassDto(
    val pnr: String,
    val flightNumber: String,
    val passengerName: String,
    val seatNumber: String,
    val boardingGroup: String,
    val gate: String? = null,
    val boardingTime: String? = null,
    val departureTime: String,
    val origin: String,
    val destination: String,
    val barcodeData: String
)

// ============================================================================
// Manage Booking DTOs
// ============================================================================

@Serializable
data class RetrieveBookingRequestDto(
    val pnr: String,
    val lastName: String
)

@Serializable
data class ManageBookingResponseDto(
    val pnr: String,
    val status: String,
    val flight: ManageBookingFlightDto,
    val passengers: List<ManageBookingPassengerDto>,
    val ancillaries: List<BookedAncillaryDto>,
    val payment: PaymentSummaryDto,
    val allowedActions: List<String>
)

@Serializable
data class ManageBookingFlightDto(
    val flightNumber: String,
    val origin: String,
    val originName: String,
    val destination: String,
    val destinationName: String,
    val departureTime: String,
    val arrivalTime: String,
    val departureDate: String,
    val fareFamily: String
)

@Serializable
data class ManageBookingPassengerDto(
    val passengerId: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val type: String,
    val dateOfBirth: String? = null
)

@Serializable
data class BookedAncillaryDto(
    val type: String,
    val description: String,
    val passengerId: String,
    val priceFormatted: String
)

@Serializable
data class PaymentSummaryDto(
    val totalPaidMinor: Long,
    val totalPaidFormatted: String,
    val currency: String,
    val paymentMethod: String,
    val lastFourDigits: String? = null
)

@Serializable
data class ModifyBookingRequestDto(
    val pnr: String,
    val lastName: String,
    val modifications: BookingModificationsDto
)

@Serializable
data class BookingModificationsDto(
    val newFlightNumber: String? = null,
    val newDepartureDate: String? = null,
    val passengerUpdates: List<PassengerUpdateDto> = emptyList(),
    val addAncillaries: List<AncillaryDto> = emptyList(),
    val removeAncillaries: List<String> = emptyList()
)

@Serializable
data class PassengerUpdateDto(
    val passengerId: String,
    val firstName: String? = null,
    val lastName: String? = null
)

@Serializable
data class ModifyBookingResponseDto(
    val pnr: String,
    val success: Boolean,
    val message: String,
    val priceDifferenceFormatted: String? = null,
    val requiresPayment: Boolean = false
)

@Serializable
data class CancelBookingRequestDto(
    val pnr: String,
    val lastName: String,
    val reason: String? = null
)

@Serializable
data class CancelBookingResponseDto(
    val pnr: String,
    val success: Boolean,
    val message: String,
    val refundAmountFormatted: String? = null,
    val refundMethod: String? = null
)

// ============================================================================
// Membership DTOs
// ============================================================================

@Serializable
data class MembershipPlanDto(
    val id: String,
    val name: String,
    val tier: String,
    val monthlyPriceMinor: Long,
    val monthlyPriceFormatted: String,
    val annualPriceMinor: Long,
    val annualPriceFormatted: String,
    val currency: String,
    val benefits: List<String>,
    val flightsPerMonth: Int,
    val guestPasses: Int,
    val priorityBoarding: Boolean,
    val loungeAccess: Boolean,
    val flexibleChanges: Boolean,
    val baggageAllowance: String
)

@Serializable
data class SubscriptionDto(
    val id: String,
    val planId: String,
    val planName: String,
    val status: String,
    val billingCycle: String,
    val currentPeriodStart: String,
    val currentPeriodEnd: String,
    val flightsUsed: Int,
    val flightsRemaining: Int,
    val guestPassesUsed: Int,
    val guestPassesRemaining: Int,
    val autoRenew: Boolean
)

@Serializable
data class SubscribeRequestDto(
    val planId: String,
    val billingCycle: String,
    val paymentMethodId: String? = null
)

@Serializable
data class CancelSubscriptionResponseDto(
    val success: Boolean,
    val message: String,
    val effectiveDate: String
)

@Serializable
data class UsageStatsDto(
    val subscriptionId: String,
    val currentPeriod: String,
    val flightsUsed: Int,
    val flightsLimit: Int,
    val guestPassesUsed: Int,
    val guestPassesLimit: Int,
    val savingsThisPeriodFormatted: String,
    val recentFlights: List<UsageFlightDto>
)

@Serializable
data class UsageFlightDto(
    val flightNumber: String,
    val route: String,
    val date: String,
    val savedAmountFormatted: String
)

// ============================================================================
// Seat Map DTOs
// ============================================================================

@Serializable
data class SeatMapDto(
    val flightNumber: String,
    val aircraft: String,
    val rows: List<SeatRowDto>,
    val legend: List<SeatLegendDto>
)

@Serializable
data class SeatRowDto(
    val rowNumber: Int,
    val seats: List<SeatDto>,
    val isExitRow: Boolean = false
)

@Serializable
data class SeatDto(
    val seatNumber: String,
    val status: String,
    val type: String,
    val priceMinor: Long = 0,
    val priceFormatted: String = "Free",
    val features: List<String> = emptyList()
)

@Serializable
data class SeatLegendDto(
    val type: String,
    val label: String,
    val color: String
)

// ============================================================================
// Meal DTOs
// ============================================================================

@Serializable
data class MealOptionDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val priceMinor: Long,
    val priceFormatted: String,
    val imageUrl: String? = null,
    val dietaryInfo: List<String> = emptyList(),
    val available: Boolean = true
)
