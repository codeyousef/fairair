package com.fairair.ai.booking.tool

import com.fairair.service.FlightService
import com.fairair.contract.model.FlightSearchRequest
import com.fairair.contract.model.FlightResponse
import com.fairair.contract.model.AirportCode
import com.fairair.contract.model.PassengerCounts
import kotlinx.coroutines.reactor.awaitSingle
import reactor.core.publisher.Mono
import org.springframework.stereotype.Component
import kotlinx.datetime.LocalDate

@Component
class NavitaireTools(
    private val flightService: FlightService
) {
    
    // Extension function as requested
    fun <T> Mono<T>.toKoogTool(): suspend () -> T = { awaitSingle() }

    suspend fun searchFlights(
        origin: String, 
        destination: String, 
        date: LocalDate, 
        passengers: Int = 1
    ): FlightResponse {
        val request = FlightSearchRequest(
            origin = AirportCode(origin.uppercase()),
            destination = AirportCode(destination.uppercase()),
            departureDate = date,
            passengers = PassengerCounts(adults = passengers, children = 0, infants = 0)
        )
        
        // Use FlightService which handles caching
        return flightService.searchFlights(request)
    }
}
