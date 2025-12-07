package com.fairair.app.api

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.dto.LoginRequestDto
import com.fairair.contract.dto.LoginResponseDto
import com.fairair.contract.dto.UserInfoDto
import com.fairair.contract.dto.RouteMapDto
import com.fairair.contract.dto.StationDto
import com.fairair.contract.dto.FlightSearchRequestDto
import com.fairair.contract.dto.PassengerCountsDto
import com.fairair.contract.dto.FlightSearchResponseDto
import com.fairair.contract.dto.FlightDto
import com.fairair.contract.dto.FareFamilyDto
import com.fairair.contract.dto.FareInclusionsDto
import com.fairair.contract.dto.LowFaresResponseDto
import com.fairair.contract.dto.LowFareDateDto
import com.fairair.contract.dto.BookingRequestDto
import com.fairair.contract.dto.PassengerDto
import com.fairair.contract.dto.AncillaryDto
import com.fairair.contract.dto.PaymentDto
import com.fairair.contract.dto.BookingConfirmationDto
import com.fairair.contract.dto.FlightSummaryDto
import com.fairair.contract.dto.PassengerSummaryDto
import com.fairair.contract.dto.BookingErrorDto
import com.fairair.contract.dto.CheckInLookupRequestDto
import com.fairair.contract.dto.CheckInLookupResponseDto
import com.fairair.contract.dto.CheckInFlightDto
import com.fairair.contract.dto.CheckInPassengerDto
import com.fairair.contract.dto.CheckInProcessRequestDto
import com.fairair.contract.dto.SeatPreferenceDto
import com.fairair.contract.dto.CheckInResultDto
import com.fairair.contract.dto.CheckedInPassengerDto
import com.fairair.contract.dto.BoardingPassDto
import com.fairair.contract.dto.RetrieveBookingRequestDto
import com.fairair.contract.dto.ManageBookingResponseDto
import com.fairair.contract.dto.ManageBookingFlightDto
import com.fairair.contract.dto.ManageBookingPassengerDto
import com.fairair.contract.dto.BookedAncillaryDto
import com.fairair.contract.dto.PaymentSummaryDto
import com.fairair.contract.dto.ModifyBookingRequestDto
import com.fairair.contract.dto.BookingModificationsDto
import com.fairair.contract.dto.PassengerUpdateDto
import com.fairair.contract.dto.ModifyBookingResponseDto
import com.fairair.contract.dto.CancelBookingRequestDto
import com.fairair.contract.dto.CancelBookingResponseDto
import com.fairair.contract.dto.SeatMapDto
import com.fairair.contract.dto.SeatRowDto
import com.fairair.contract.dto.SeatDto
import com.fairair.contract.dto.SeatLegendDto
import com.fairair.contract.dto.MealOptionDto
import com.fairair.contract.dto.ChatMessageRequestDto
import com.fairair.contract.dto.ChatResponseDto
import com.fairair.contract.dto.ChatContextDto
import com.fairair.contract.model.MembershipPlan
import com.fairair.contract.model.Subscription
import com.fairair.contract.model.SubscribeRequest
import com.fairair.contract.model.SubscriptionCancellation
import com.fairair.contract.model.MembershipUsage
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
     * @return List of MembershipPlan from shared contract
     */
    suspend fun getMembershipPlans(): ApiResult<List<MembershipPlan>> {
        return safeApiCall {
            httpClient.get("$baseUrl${ApiRoutes.Membership.PLANS}").body()
        }
    }

    /**
     * Gets user's active subscription.
     * @param authToken JWT access token
     * @return Subscription or null if no active subscription
     */
    suspend fun getSubscription(authToken: String): ApiResult<Subscription?> {
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
     * @return Subscription with new subscription details
     */
    suspend fun subscribe(request: SubscribeRequest, authToken: String): ApiResult<Subscription> {
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
     * @return SubscriptionCancellation confirmation
     */
    suspend fun cancelSubscription(authToken: String): ApiResult<SubscriptionCancellation> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Membership.CANCEL}") {
                header(HttpHeaders.Authorization, "Bearer $authToken")
            }.body()
        }
    }

    /**
     * Gets subscription usage statistics.
     * @param authToken JWT access token
     * @return MembershipUsage with usage details
     */
    suspend fun getUsageStats(authToken: String): ApiResult<MembershipUsage> {
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

    // ============================================================================
    // Chat / Faris AI Assistant Endpoints
    // ============================================================================

    /**
     * Sends a message to the Faris AI assistant.
     * @param sessionId Unique session ID for conversation continuity
     * @param message The user's message
     * @param locale User's locale (e.g., "en-US", "ar-SA")
     * @param context Optional context about current app state
     * @return ChatResponseDto with the AI's response
     */
    suspend fun sendChatMessage(
        sessionId: String,
        message: String,
        locale: String? = null,
        context: ChatContextDto? = null
    ): ApiResult<ChatResponseDto> {
        return safeApiCall {
            httpClient.post("$baseUrl${ApiRoutes.Chat.MESSAGE}") {
                contentType(ContentType.Application.Json)
                setBody(
                    ChatMessageRequestDto(
                        sessionId = sessionId,
                        message = message,
                        locale = locale,
                        context = context
                    )
                )
            }.body()
        }
    }

    /**
     * Clears a chat session history.
     * @param sessionId The session ID to clear
     * @return Success with Unit on success
     */
    suspend fun clearChatSession(sessionId: String): ApiResult<Unit> {
        return safeApiCall {
            httpClient.delete("$baseUrl${ApiRoutes.Chat.session(sessionId)}").body()
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

// DTO classes are now imported from com.fairair.contract.dto
// Only keep local helper types that extend shared DTOs

/**
 * Simplified fare DTO for UI display.
 * This is a frontend-specific type that adapts shared DTOs for UI rendering.
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

/**
 * Computed property to get fares list from FlightDto.
 * This is a convenience property for UI display.
 */
val FlightDto.fares: List<FareDto>
    get() = fareFamilies.map { ff ->
        FareDto(
            fareFamily = ff.name,
            fareFamilyCode = ff.code,
            basePrice = ff.priceFormatted,
            totalPrice = ff.priceFormatted,
            currency = ff.currency,
            inclusions = buildInclusionsList(ff.inclusions)
        )
    }

/**
 * Extension function to convert FlightDto's fareFamilies to simpler fares for UI display.
 */
fun FlightDto.toFares(): List<FareDto> = fares

private fun buildInclusionsList(inclusions: FareInclusionsDto): List<String> {
    val list = mutableListOf<String>()
    list.add("Carry-on: ${inclusions.carryOnBag}")
    inclusions.checkedBag?.let { list.add("Checked bag: $it") }
    list.add("Seat: ${inclusions.seatSelection}")
    if (inclusions.priorityBoarding) list.add("Priority boarding")
    if (inclusions.loungeAccess) list.add("Lounge access")
    return list
}


