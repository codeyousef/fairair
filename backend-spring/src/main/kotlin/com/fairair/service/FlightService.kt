package com.fairair.service

import com.fairair.cache.CacheService
import com.fairair.client.NavitaireClient
import com.fairair.contract.model.*
import com.fairair.exception.FareNotFoundException
import com.fairair.exception.FlightNotFoundException
import com.fairair.exception.SearchExpiredException
import com.fairair.exception.ValidationException
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service layer for flight-related operations.
 * Handles caching, validation, and orchestration between controllers and clients.
 */
@Service
class FlightService(
    private val navitaireClient: NavitaireClient,
    private val cacheService: CacheService
) {
    private val log = LoggerFactory.getLogger(FlightService::class.java)

    /**
     * Gets the route map with caching.
     * Uses suspend-aware cache retrieval to avoid blocking Netty threads.
     * @return The RouteMap defining valid origin-destination pairs
     */
    suspend fun getRouteMap(): RouteMap {
        log.debug("Getting route map")
        return cacheService.getRouteMapSuspend {
            navitaireClient.getRouteMap()
        }
    }

    /**
     * Gets all stations with caching.
     * Uses suspend-aware cache retrieval to avoid blocking Netty threads.
     * @return List of all available stations
     */
    suspend fun getStations(): List<Station> {
        log.debug("Getting stations")
        return cacheService.getStationsSuspend {
            navitaireClient.getStations()
        }
    }

    /**
     * Gets a station by its airport code.
     * @param code The airport code
     * @return The Station or null if not found
     */
    suspend fun getStation(code: AirportCode): Station? {
        return getStations().find { it.code == code }
    }

    /**
     * Searches for flights and caches the result.
     * Uses route-based caching to share results across users searching the same route.
     * 
     * @param request The search criteria
     * @return FlightResponse containing available flights
     * @throws ValidationException if the route is invalid
     */
    suspend fun searchFlights(request: FlightSearchRequest): FlightResponse {
        log.info("Searching flights: ${request.origin} -> ${request.destination} on ${request.departureDate}")

        val routeMap = getRouteMap()
        if (!routeMap.isValidRoute(request.origin, request.destination)) {
            log.warn("Invalid route requested: ${request.origin} -> ${request.destination}")
            throw InvalidRouteException(request.origin, request.destination)
        }

        // Use route-based caching - if another user recently searched the same route,
        // return the cached result instead of hitting the backend again
        return cacheService.getRouteSearchSuspend(request) {
            val response = navitaireClient.searchFlights(request)
            log.info("Found ${response.count} flights for search ${response.searchId}")
            response
        }
    }
    
    /**
     * Gets the lowest fare prices for a date range.
     * Useful for calendar displays showing price variations across dates.
     * 
     * Uses route-based caching for each date to minimize backend calls.
     * 
     * @param origin Origin airport code
     * @param destination Destination airport code
     * @param startDate First date to check
     * @param endDate Last date to check (inclusive)
     * @param passengers Passenger counts for pricing
     * @return Map of date to lowest fare info
     */
    suspend fun getLowFaresForDateRange(
        origin: AirportCode,
        destination: AirportCode,
        startDate: LocalDate,
        endDate: LocalDate,
        passengers: PassengerCounts
    ): List<LowFareDate> {
        log.info("Getting low fares: $origin -> $destination from $startDate to $endDate")
        
        val routeMap = getRouteMap()
        if (!routeMap.isValidRoute(origin, destination)) {
            throw InvalidRouteException(origin, destination)
        }
        
        val results = mutableListOf<LowFareDate>()
        var currentDate = startDate
        
        while (currentDate <= endDate) {
            val request = FlightSearchRequest(
                origin = origin,
                destination = destination,
                departureDate = currentDate,
                passengers = passengers
            )
            
            try {
                val response = cacheService.getRouteSearchSuspend(request) {
                    navitaireClient.searchFlights(request)
                }
                
                // Find the lowest price across all flights and fare families
                val lowestFare = response.flights
                    .flatMap { it.fareFamilies }
                    .minByOrNull { it.price.amountMinor }
                
                if (lowestFare != null) {
                    results.add(
                        LowFareDate(
                            date = currentDate,
                            lowestPrice = lowestFare.price,
                            fareFamily = lowestFare.code,
                            flightsAvailable = response.flights.size
                        )
                    )
                } else {
                    results.add(
                        LowFareDate(
                            date = currentDate,
                            lowestPrice = null,
                            fareFamily = null,
                            flightsAvailable = 0
                        )
                    )
                }
            } catch (e: Exception) {
                log.warn("Failed to get fares for $currentDate: ${e.message}")
                results.add(
                    LowFareDate(
                        date = currentDate,
                        lowestPrice = null,
                        fareFamily = null,
                        flightsAvailable = 0
                    )
                )
            }
            
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
        
        return results
    }

    /**
     * Retrieves a cached search result by ID.
     * @param searchId The search identifier
     * @return The cached FlightResponse or null if expired/not found
     */
    fun getCachedSearch(searchId: String): FlightResponse? {
        return cacheService.getSearchResult(searchId)
    }

    /**
     * Validates that a search session is still valid and the selected flight exists.
     * @param searchId The search identifier
     * @param flightNumber The selected flight number
     * @param fareFamily The selected fare family
     * @return The selected FareFamily if valid
     * @throws SearchExpiredException if the search is no longer cached
     * @throws FlightNotFoundException if the flight is not in the search results
     * @throws FareNotFoundException if the fare family is not available
     */
    fun validateSelection(
        searchId: String,
        flightNumber: String,
        fareFamily: FareFamilyCode
    ): FareFamily {
        val cachedSearch = getCachedSearch(searchId)
            ?: throw SearchExpiredException(searchId)

        val flight = cachedSearch.flights.find { it.flightNumber == flightNumber }
            ?: throw FlightNotFoundException(flightNumber)

        return flight.getFareFamily(fareFamily)
            ?: throw FareNotFoundException(fareFamily.name)
    }
}

/**
 * Exception thrown when an invalid route is requested.
 */
class InvalidRouteException(
    val origin: AirportCode,
    val destination: AirportCode
) : RuntimeException("Route not available: ${origin.value} -> ${destination.value}")

/**
 * Data class representing the lowest fare for a specific date.
 * Used for the low-fare calendar display.
 */
data class LowFareDate(
    /** The date */
    val date: LocalDate,
    /** The lowest fare price, or null if no flights available */
    val lowestPrice: Money?,
    /** The fare family code of the lowest fare */
    val fareFamily: FareFamilyCode?,
    /** Number of flights available on this date */
    val flightsAvailable: Int
)
