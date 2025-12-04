package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.model.AirportCode
import com.fairair.contract.model.RouteMap
import com.fairair.contract.model.Station
import com.fairair.service.FlightService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller for configuration endpoints.
 * Provides route map and station data for the frontend.
 */
@RestController
@RequestMapping(ApiRoutes.Config.BASE)
class ConfigController(
    private val flightService: FlightService
) {
    private val log = LoggerFactory.getLogger(ConfigController::class.java)

    /**
     * GET /api/v1/config/routes
     *
     * Returns the route map defining valid origin-destination pairs.
     * This data is cached for 24 hours.
     *
     * @return RouteMap containing all available routes
     */
    @GetMapping("/routes")
    suspend fun getRoutes(): RouteMapResponse {
        log.info("GET /config/routes")
        val routeMap = flightService.getRouteMap()
        return RouteMapResponse.from(routeMap)
    }

    /**
     * GET /api/v1/config/stations
     *
     * Returns all available stations (airports).
     * This data is cached for 24 hours.
     *
     * @return List of all available stations
     */
    @GetMapping("/stations")
    suspend fun getStations(): List<StationResponse> {
        log.info("GET /config/stations")
        return flightService.getStations().map { StationResponse.from(it) }
    }
    
    /**
     * GET /api/v1/config/destinations/{origin}
     *
     * Returns all valid destination stations for a given origin airport.
     * This filters the route map and returns full station details.
     *
     * @param origin The origin airport code (e.g., "JED", "RUH")
     * @return List of destination stations, or empty list if origin has no routes
     */
    @GetMapping("/destinations/{origin}")
    suspend fun getDestinationsForOrigin(@PathVariable origin: String): ResponseEntity<List<StationResponse>> {
        log.info("GET /config/destinations/$origin")
        
        val originCode = try {
            AirportCode(origin.uppercase())
        } catch (e: Exception) {
            log.warn("Invalid origin code: $origin")
            return ResponseEntity.badRequest().build()
        }
        
        val routeMap = flightService.getRouteMap()
        val destinationCodes = routeMap.routes[originCode] ?: emptyList()
        
        val allStations = flightService.getStations()
        val destinationStations = allStations.filter { station ->
            destinationCodes.any { it.value == station.code.value }
        }
        
        return ResponseEntity.ok(destinationStations.map { StationResponse.from(it) })
    }
}

/**
 * Response DTO for route map endpoint.
 * Serializes AirportCode values as strings for JSON.
 */
data class RouteMapResponse(
    val routes: Map<String, List<String>>
) {
    companion object {
        fun from(routeMap: RouteMap): RouteMapResponse {
            return RouteMapResponse(
                routes = routeMap.routes.mapKeys { it.key.value }
                    .mapValues { entry -> entry.value.map { it.value } }
            )
        }
    }
}

/**
 * Response DTO for station endpoint.
 */
data class StationResponse(
    val code: String,
    val name: String,
    val city: String,
    val country: String
) {
    companion object {
        fun from(station: Station): StationResponse {
            return StationResponse(
                code = station.code.value,
                name = station.name,
                city = station.city,
                country = station.country
            )
        }
    }
}
