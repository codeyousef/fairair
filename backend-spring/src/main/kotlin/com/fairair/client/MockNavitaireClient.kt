package com.fairair.client

import com.fairair.config.FairairProperties
import com.fairair.contract.model.*
import com.fairair.entity.BookingEntity
import com.fairair.repository.BookingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.random.Random

/**
 * Mock implementation of NavitaireClient for development and testing.
 * Generates realistic flight data with configurable delays to simulate network latency.
 * 
 * Uses database-backed storage for bookings via BookingRepository.
 *
 * Activated when fairair.provider=mock (default).
 */
@Service
@ConditionalOnProperty(
    name = ["fairair.provider"],
    havingValue = "mock",
    matchIfMissing = true
)
class MockNavitaireClient(
    private val config: FairairProperties,
    private val bookingRepository: BookingRepository
) : NavitaireClient {

    private val log = LoggerFactory.getLogger(MockNavitaireClient::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Simulates network delay based on configuration.
     */
    private suspend fun simulateDelay() {
        val delayMs = Random.nextLong(config.mock.minDelay, config.mock.maxDelay)
        log.debug("Simulating ${delayMs}ms network delay")
        delay(delayMs)
    }

    override suspend fun getRouteMap(): RouteMap {
        simulateDelay()
        log.info("Fetching mock route map")

        // Flyadeal routes - domestic + select international
        return RouteMap(
            routes = mapOf(
                AirportCode("JED") to listOf(
                    AirportCode("RUH"),
                    AirportCode("DMM"),
                    AirportCode("AHB"),
                    AirportCode("GIZ"),
                    AirportCode("TUU"),
                    AirportCode("CAI")  // JED-CAI but no JED-DXB
                ),
                AirportCode("RUH") to listOf(
                    AirportCode("JED"),
                    AirportCode("DMM"),
                    AirportCode("AHB"),
                    AirportCode("GIZ"),
                    AirportCode("TUU"),
                    AirportCode("DXB"),  // RUH-DXB
                    AirportCode("CAI")   // RUH-CAI
                ),
                AirportCode("DMM") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH"),
                    AirportCode("DXB")   // DMM-DXB
                ),
                AirportCode("AHB") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                ),
                AirportCode("GIZ") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                ),
                AirportCode("TUU") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                ),
                AirportCode("DXB") to listOf(
                    AirportCode("RUH"),
                    AirportCode("DMM")
                ),
                AirportCode("CAI") to listOf(
                    AirportCode("JED"),
                    AirportCode("RUH")
                )
            )
        )
    }

    override suspend fun getStations(): List<Station> {
        simulateDelay()
        log.info("Fetching mock stations")

        // Saudi domestic + international destinations
        return listOf(
            Station(AirportCode("JED"), "King Abdulaziz International Airport", "Jeddah", "Saudi Arabia"),
            Station(AirportCode("RUH"), "King Khalid International Airport", "Riyadh", "Saudi Arabia"),
            Station(AirportCode("DMM"), "King Fahd International Airport", "Dammam", "Saudi Arabia"),
            Station(AirportCode("AHB"), "Abha International Airport", "Abha", "Saudi Arabia"),
            Station(AirportCode("GIZ"), "King Abdullah bin Abdulaziz Airport", "Jazan", "Saudi Arabia"),
            Station(AirportCode("TUU"), "Tabuk Regional Airport", "Tabuk", "Saudi Arabia"),
            Station(AirportCode("DXB"), "Dubai International Airport", "Dubai", "UAE"),
            Station(AirportCode("CAI"), "Cairo International Airport", "Cairo", "Egypt")
        )
    }

    override suspend fun searchFlights(request: FlightSearchRequest): FlightResponse {
        simulateDelay()
        log.info("Searching flights: ${request.origin} -> ${request.destination} on ${request.departureDate}")

        // Validate route exists
        val routeMap = getRouteMap()
        val validDestinations = routeMap.routes[request.origin] ?: emptyList()
        
        if (request.destination !in validDestinations) {
            log.info("Route ${request.origin} -> ${request.destination} does not exist")
            return FlightResponse(
                flights = emptyList(),
                searchId = UUID.randomUUID().toString()
            )
        }

        val searchId = UUID.randomUUID().toString()
        val flights = generateMockFlights(request)

        return FlightResponse(
            flights = flights,
            searchId = searchId
        )
    }

    override suspend fun createBooking(request: BookingRequest, userId: String?): BookingConfirmation {
        simulateDelay()
        log.info("Creating booking for flight ${request.flightNumber} with ${request.passengers.size} passengers, userId=$userId")

        val pnr = PnrCode.generate()
        val now = Clock.System.now()
        val departureTime = now.plus(1, DateTimeUnit.DAY, TimeZone.UTC)

        val confirmation = BookingConfirmation(
            pnr = pnr,
            bookingReference = UUID.randomUUID().toString(),
            flight = FlightSummary(
                flightNumber = request.flightNumber,
                origin = AirportCode("JED"),
                destination = AirportCode("RUH"),
                departureTime = departureTime,
                fareFamily = request.fareFamily
            ),
            passengers = request.passengers.map { passenger ->
                PassengerSummary(
                    fullName = passenger.fullName,
                    type = passenger.type
                )
            },
            totalPaid = request.payment.totalAmount,
            createdAt = now
        )

        // Persist to database
        val entity = BookingEntity.create(
            pnr = pnr.value,
            bookingReference = confirmation.bookingReference,
            userId = userId,
            flightNumber = request.flightNumber,
            origin = "JED",
            destination = "RUH",
            departureTime = java.time.Instant.ofEpochMilli(departureTime.toEpochMilliseconds()),
            fareFamily = request.fareFamily.name,
            passengersJson = json.encodeToString(confirmation.passengers),
            totalAmount = request.payment.totalAmount.amountAsDouble,
            currency = request.payment.totalAmount.currency.name,
            createdAt = java.time.Instant.ofEpochMilli(now.toEpochMilliseconds())
        )
        bookingRepository.save(entity)
        
        log.info("Booking ${pnr.value} persisted to database" + if (userId != null) ", associated with user $userId" else "")

        return confirmation
    }

    override suspend fun getBooking(pnr: String): BookingConfirmation? {
        simulateDelay()
        log.info("Retrieving booking for PNR=$pnr")
        
        val entity = bookingRepository.findByPnr(pnr) ?: return null
        return entity.toBookingConfirmation()
    }
    
    override suspend fun getBookingsByUser(userId: String): List<BookingConfirmation> {
        simulateDelay()
        log.info("Retrieving bookings for user=$userId")
        
        return bookingRepository.findByUserId(userId)
            .toList()
            .map { it.toBookingConfirmation() }
    }
    
    /**
     * Converts a BookingEntity to a BookingConfirmation.
     */
    private fun BookingEntity.toBookingConfirmation(): BookingConfirmation {
        val passengers: List<PassengerSummary> = try {
            json.decodeFromString(passengersJson)
        } catch (e: Exception) {
            log.warn("Failed to parse passengers JSON for PNR=$pnr", e)
            emptyList()
        }
        
        return BookingConfirmation(
            pnr = PnrCode(pnr),
            bookingReference = bookingReference,
            flight = FlightSummary(
                flightNumber = flightNumber,
                origin = AirportCode(origin),
                destination = AirportCode(destination),
                departureTime = Instant.fromEpochMilliseconds(departureTime.toEpochMilli()),
                fareFamily = FareFamilyCode.valueOf(fareFamily)
            ),
            passengers = passengers,
            totalPaid = Money.of(totalAmount, Currency.valueOf(currency)),
            createdAt = Instant.fromEpochMilliseconds(createdAt.toEpochMilli())
        )
    }

    /**
     * Generates mock flight data for the given search request.
     */
    private fun generateMockFlights(request: FlightSearchRequest): List<Flight> {
        val baseDate = request.departureDate.atStartOfDayIn(TimeZone.of("Asia/Riyadh"))
        val flightCount = Random.nextInt(3, 7)

        return (0 until flightCount).map { index ->
            val departureHour = 6 + (index * 3)
            val departureTime = baseDate.plus(departureHour, DateTimeUnit.HOUR)
            val durationMinutes = getDurationForRoute(request.origin, request.destination)
            val arrivalTime = departureTime.plus(durationMinutes, DateTimeUnit.MINUTE)

            val basePrice = getBasePriceForRoute(request.origin, request.destination)

            Flight(
                flightNumber = "F3${100 + index}",
                origin = request.origin,
                destination = request.destination,
                departureTime = departureTime,
                arrivalTime = arrivalTime,
                durationMinutes = durationMinutes,
                aircraft = if (index % 2 == 0) "A320neo" else "A321neo",
                fareFamilies = listOf(
                    FareFamily(
                        code = FareFamilyCode.FLY,
                        name = "Fly",
                        price = Money.sar(basePrice * (request.passengers.adults +
                            request.passengers.children * 0.75 +
                            request.passengers.infants * 0.1)),
                        inclusions = FareInclusions.FLY_INCLUSIONS
                    ),
                    FareFamily(
                        code = FareFamilyCode.FLY_PLUS,
                        name = "Fly+",
                        price = Money.sar(basePrice * 1.3 * (request.passengers.adults +
                            request.passengers.children * 0.75 +
                            request.passengers.infants * 0.1)),
                        inclusions = FareInclusions.FLY_PLUS_INCLUSIONS
                    ),
                    FareFamily(
                        code = FareFamilyCode.FLY_MAX,
                        name = "FlyMax",
                        price = Money.sar(basePrice * 1.8 * (request.passengers.adults +
                            request.passengers.children * 0.75 +
                            request.passengers.infants * 0.1)),
                        inclusions = FareInclusions.FLY_MAX_INCLUSIONS
                    )
                )
            )
        }
    }

    /**
     * Returns estimated flight duration in minutes for a route.
     */
    private fun getDurationForRoute(origin: AirportCode, destination: AirportCode): Int {
        return when {
            setOf(origin.value, destination.value) == setOf("JED", "RUH") -> 85
            setOf(origin.value, destination.value) == setOf("JED", "DMM") -> 120
            setOf(origin.value, destination.value) == setOf("RUH", "DMM") -> 65
            setOf(origin.value, destination.value) == setOf("RUH", "DXB") -> 120
            setOf(origin.value, destination.value) == setOf("DMM", "DXB") -> 90
            setOf(origin.value, destination.value) == setOf("JED", "CAI") -> 150
            setOf(origin.value, destination.value) == setOf("RUH", "CAI") -> 180
            setOf(origin.value, destination.value).contains("AHB") -> 75
            setOf(origin.value, destination.value).contains("GIZ") -> 80
            setOf(origin.value, destination.value).contains("TUU") -> 95
            else -> 90
        }
    }

    /**
     * Returns base price in SAR for a route (per adult).
     */
    private fun getBasePriceForRoute(origin: AirportCode, destination: AirportCode): Double {
        val randomFactor = Random.nextDouble(0.9, 1.1)
        val basePrice = when {
            setOf(origin.value, destination.value) == setOf("JED", "RUH") -> 350.0
            setOf(origin.value, destination.value) == setOf("JED", "DMM") -> 450.0
            setOf(origin.value, destination.value) == setOf("RUH", "DMM") -> 280.0
            setOf(origin.value, destination.value) == setOf("RUH", "DXB") -> 650.0
            setOf(origin.value, destination.value) == setOf("DMM", "DXB") -> 550.0
            setOf(origin.value, destination.value) == setOf("JED", "CAI") -> 850.0
            setOf(origin.value, destination.value) == setOf("RUH", "CAI") -> 900.0
            setOf(origin.value, destination.value).contains("AHB") -> 320.0
            setOf(origin.value, destination.value).contains("GIZ") -> 340.0
            setOf(origin.value, destination.value).contains("TUU") -> 380.0
            else -> 400.0
        }
        return basePrice * randomFactor
    }
}
