package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.model.*
import com.fairair.service.FlightService
import com.fairair.service.InvalidRouteException
import com.fairair.service.LowFareDate
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for flight search endpoints.
 */
@RestController
@RequestMapping(ApiRoutes.Search.BASE)
class SearchController(
    private val flightService: FlightService
) {
    private val log = LoggerFactory.getLogger(SearchController::class.java)

    /**
     * POST /api/v1/search
     *
     * Searches for available flights based on the provided criteria.
     * Supports both one-way and round-trip searches.
     * Results are cached for 5 minutes using the returned searchId.
     * Route-based caching shares results across users searching the same route.
     *
     * @param request Search criteria including origin, destination, date, and passengers
     * @return FlightResponse containing available flights and searchId
     */
    @PostMapping
    suspend fun searchFlights(@RequestBody request: FlightSearchRequestDto): ResponseEntity<Any> {
        log.info("POST /search: ${request.origin} -> ${request.destination} on ${request.departureDate}" +
                 if (request.returnDate != null) " (return: ${request.returnDate})" else "")

        return try {
            // Search outbound flights
            val outboundRequest = request.toModel()
            val outboundResponse = flightService.searchFlights(outboundRequest)
            
            // Search return flights if round-trip
            val returnResponse = request.toReturnModel()?.let { returnRequest ->
                flightService.searchFlights(returnRequest)
            }
            
            if (returnResponse != null) {
                // Round-trip response
                ResponseEntity.ok(RoundTripFlightResponseDto(
                    tripType = "ROUND_TRIP",
                    outbound = FlightResponseDto.from(outboundResponse),
                    return_ = FlightResponseDto.from(returnResponse),
                    searchId = outboundResponse.searchId
                ))
            } else {
                // One-way response
                ResponseEntity.ok(FlightResponseDto.from(outboundResponse))
            }
        } catch (e: InvalidRouteException) {
            log.warn("Invalid route: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SearchErrorResponse("INVALID_ROUTE", e.message ?: "Invalid route"))
        } catch (e: IllegalArgumentException) {
            log.warn("Validation error: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SearchErrorResponse("VALIDATION_ERROR", e.message ?: "Validation failed"))
        }
    }
    
    /**
     * GET /api/v1/search/low-fares
     *
     * Retrieves the lowest fare prices for a date range.
     * Useful for calendar displays showing price variations.
     * Results are cached per date to minimize backend calls.
     *
     * @param origin Origin airport code (e.g., "JED")
     * @param destination Destination airport code (e.g., "RUH")
     * @param startDate First date to check (ISO format: YYYY-MM-DD)
     * @param endDate Last date to check (ISO format: YYYY-MM-DD)
     * @param adults Number of adult passengers (default: 1)
     * @param children Number of child passengers (default: 0)
     * @param infants Number of infant passengers (default: 0)
     * @return List of LowFareDateDto for each date in range
     */
    @GetMapping("/low-fares")
    suspend fun getLowFares(
        @RequestParam origin: String,
        @RequestParam destination: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam(defaultValue = "1") adults: Int,
        @RequestParam(defaultValue = "0") children: Int,
        @RequestParam(defaultValue = "0") infants: Int
    ): ResponseEntity<Any> {
        log.info("GET /search/low-fares: $origin -> $destination from $startDate to $endDate")
        
        return try {
            val passengers = PassengerCounts(adults, children, infants)
            val lowFares = flightService.getLowFaresForDateRange(
                origin = AirportCode(origin),
                destination = AirportCode(destination),
                startDate = LocalDate.parse(startDate),
                endDate = LocalDate.parse(endDate),
                passengers = passengers
            )
            
            ResponseEntity.ok(LowFaresResponseDto(
                origin = origin,
                destination = destination,
                dates = lowFares.map { LowFareDateDto.from(it) }
            ))
        } catch (e: InvalidRouteException) {
            log.warn("Invalid route: ${e.message}")
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(SearchErrorResponse("INVALID_ROUTE", e.message ?: "Invalid route"))
        } catch (e: Exception) {
            log.error("Error fetching low fares: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(SearchErrorResponse("SERVER_ERROR", "Failed to fetch low fares"))
        }
    }
}

/**
 * Request DTO for flight search.
 * Supports one-way and round-trip searches.
 */
data class FlightSearchRequestDto(
    val origin: String,
    val destination: String,
    val departureDate: String,
    val returnDate: String? = null,  // For round-trip
    val tripType: String = "ONE_WAY", // ONE_WAY or ROUND_TRIP
    val passengers: PassengerCountsDto
) {
    fun toModel(): FlightSearchRequest {
        return FlightSearchRequest(
            origin = AirportCode(origin),
            destination = AirportCode(destination),
            departureDate = LocalDate.parse(departureDate),
            passengers = passengers.toModel()
        )
    }
    
    fun toReturnModel(): FlightSearchRequest? {
        if (returnDate == null) return null
        return FlightSearchRequest(
            origin = AirportCode(destination), // Swap origin/destination for return
            destination = AirportCode(origin),
            departureDate = LocalDate.parse(returnDate),
            passengers = passengers.toModel()
        )
    }
}

/**
 * DTO for passenger counts.
 */
data class PassengerCountsDto(
    val adults: Int,
    val children: Int,
    val infants: Int
) {
    fun toModel(): PassengerCounts {
        return PassengerCounts(
            adults = adults,
            children = children,
            infants = infants
        )
    }
}

/**
 * Response DTO for flight search.
 */
data class FlightResponseDto(
    val flights: List<FlightDto>,
    val searchId: String,
    val tripType: String = "ONE_WAY"
) {
    companion object {
        fun from(response: FlightResponse): FlightResponseDto {
            return FlightResponseDto(
                flights = response.flights.map { FlightDto.from(it) },
                searchId = response.searchId
            )
        }
    }
}

/**
 * Response DTO for round-trip flight search.
 */
data class RoundTripFlightResponseDto(
    val tripType: String,
    val outbound: FlightResponseDto,
    val return_: FlightResponseDto,
    val searchId: String
)

/**
 * DTO for individual flight.
 */
data class FlightDto(
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int,
    val durationFormatted: String,
    val aircraft: String,
    val fareFamilies: List<FareFamilyDto>,
    val seatsAvailable: Int = 0,
    val seatsBooked: Int = 0
) {
    companion object {
        fun from(flight: Flight): FlightDto {
            // Generate random seat availability for demo
            val totalSeats = when {
                flight.aircraft.contains("A320") -> 180
                flight.aircraft.contains("A321") -> 220
                flight.aircraft.contains("737") -> 160
                else -> 150
            }
            val booked = (totalSeats * (0.4 + Math.random() * 0.5)).toInt()
            
            return FlightDto(
                flightNumber = flight.flightNumber,
                origin = flight.origin.value,
                destination = flight.destination.value,
                departureTime = flight.departureTime.toString(),
                arrivalTime = flight.arrivalTime.toString(),
                durationMinutes = flight.durationMinutes,
                durationFormatted = flight.formatDuration(),
                aircraft = flight.aircraft,
                fareFamilies = flight.fareFamilies.map { FareFamilyDto.from(it) },
                seatsAvailable = totalSeats - booked,
                seatsBooked = booked
            )
        }
    }
}

/**
 * DTO for fare family.
 */
data class FareFamilyDto(
    val code: String,
    val name: String,
    val priceMinor: Long,
    val priceFormatted: String,
    val currency: String,
    val inclusions: FareInclusionsDto
) {
    companion object {
        fun from(fareFamily: FareFamily): FareFamilyDto {
            return FareFamilyDto(
                code = fareFamily.code.name,
                name = fareFamily.name,
                priceMinor = fareFamily.price.amountMinor,
                priceFormatted = fareFamily.price.formatAmount(),
                currency = fareFamily.price.currency.name,
                inclusions = FareInclusionsDto.from(fareFamily.inclusions)
            )
        }
    }
}

/**
 * DTO for fare inclusions.
 */
data class FareInclusionsDto(
    val carryOnBag: String,
    val checkedBag: String?,
    val seatSelection: String,
    val changePolicy: String,
    val cancellationPolicy: String,
    val priorityBoarding: Boolean,
    val loungeAccess: Boolean
) {
    companion object {
        fun from(inclusions: FareInclusions): FareInclusionsDto {
            return FareInclusionsDto(
                carryOnBag = inclusions.carryOnBag.formatDisplay(),
                checkedBag = inclusions.checkedBag?.formatDisplay(),
                seatSelection = inclusions.seatSelection.displayName,
                changePolicy = inclusions.changePolicy.formatDisplay(),
                cancellationPolicy = inclusions.cancellationPolicy.formatDisplay(),
                priorityBoarding = inclusions.priorityBoarding,
                loungeAccess = inclusions.loungeAccess
            )
        }
    }
}

/**
 * Error response for search endpoint.
 */
data class SearchErrorResponse(
    val code: String,
    val message: String
)

/**
 * Response DTO for low-fare calendar.
 */
data class LowFaresResponseDto(
    val origin: String,
    val destination: String,
    val dates: List<LowFareDateDto>
)

/**
 * DTO for a single date's lowest fare.
 */
data class LowFareDateDto(
    /** The date in ISO format (YYYY-MM-DD) */
    val date: String,
    /** The lowest fare price in minor units, or null if no flights */
    val priceMinor: Long?,
    /** Formatted price display (e.g., "350 SAR"), or null */
    val priceFormatted: String?,
    /** Currency code */
    val currency: String?,
    /** The fare family code of the lowest fare */
    val fareFamily: String?,
    /** Number of flights available on this date */
    val flightsAvailable: Int,
    /** Whether flights are available on this date */
    val available: Boolean
) {
    companion object {
        fun from(lowFareDate: LowFareDate): LowFareDateDto {
            return LowFareDateDto(
                date = lowFareDate.date.toString(),
                priceMinor = lowFareDate.lowestPrice?.amountMinor,
                priceFormatted = lowFareDate.lowestPrice?.formatAmount(),
                currency = lowFareDate.lowestPrice?.currency?.name,
                fareFamily = lowFareDate.fareFamily?.name,
                flightsAvailable = lowFareDate.flightsAvailable,
                available = lowFareDate.flightsAvailable > 0
            )
        }
    }
}
