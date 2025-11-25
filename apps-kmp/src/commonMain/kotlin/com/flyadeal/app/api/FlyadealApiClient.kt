package com.flyadeal.app.api

import com.flyadeal.contract.api.ApiRoutes
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * API client for the flyadeal backend.
 * Uses Ktor client for cross-platform HTTP requests.
 */
class FlyadealApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient
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
     * Wraps API calls with error handling.
     */
    private suspend inline fun <reified T> safeApiCall(block: () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(block())
        } catch (e: ClientRequestException) {
            val errorBody = e.response.bodyAsText()
            ApiResult.Error(
                code = "HTTP_${e.response.status.value}",
                message = errorBody.ifEmpty { e.message }
            )
        } catch (e: ServerResponseException) {
            ApiResult.Error(
                code = "SERVER_ERROR",
                message = "Server error: ${e.response.status.value}"
            )
        } catch (e: Exception) {
            ApiResult.Error(
                code = "NETWORK_ERROR",
                message = e.message ?: "Unknown error"
            )
        }
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
    data class Error(val code: String, val message: String) : ApiResult<Nothing>()

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

@Serializable
data class FlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String = "",
    val aircraft: String = "",
    val fares: List<FareDto> = emptyList()
)

@Serializable
data class FareDto(
    val fareFamily: String,
    val basePrice: String = "0",
    val totalPrice: String = "0",
    val currency: String = "SAR",
    val inclusions: List<String> = emptyList()
)

@Serializable
data class BookingRequestDto(
    val flightNumber: String,
    val fareFamily: String,
    val passengers: List<PassengerDto>,
    val contactEmail: String,
    val contactPhone: String,
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
    val documentType: String = "",
    val documentNumber: String = "",
    val documentExpiry: String = ""
)

@Serializable
data class PaymentDto(
    val cardNumber: String,
    val cardholderName: String,
    val amount: String,
    val currency: String
)

@Serializable
data class BookingConfirmationDto(
    val pnr: String,
    val status: String = "CONFIRMED",
    val totalPrice: String = "0",
    val currency: String = "SAR",
    val flightNumber: String = "",
    val passengers: List<String> = emptyList(),
    val createdAt: String = ""
)
