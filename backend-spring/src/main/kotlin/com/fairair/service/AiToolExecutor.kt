package com.fairair.service

import com.fairair.client.NavitaireClient
import com.fairair.contract.dto.ChatContextDto
import com.fairair.contract.dto.ChatUiType
import com.fairair.contract.model.*
import kotlinx.datetime.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Executes AI tool calls by delegating to the appropriate services.
 * Acts as a bridge between the AI's tool calls and the backend services.
 */
@Service
class AiToolExecutor(
    private val navitaireClient: NavitaireClient,
    private val flightService: FlightService,
    private val manageBookingService: ManageBookingService,
    private val bookingService: BookingService,
    private val profileService: ProfileService,
    private val weatherService: WeatherService
) {
    private val log = LoggerFactory.getLogger(AiToolExecutor::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * City metadata for destination suggestions.
     * Maps airport code to (cityName, country, imageUrl)
     */
    private val cityMetadata = mapOf(
        // Saudi Arabia
        "RUH" to Triple("Riyadh", "Saudi Arabia", "https://images.unsplash.com/photo-1586724237569-f3d0c1dee8c6?w=400"),
        "JED" to Triple("Jeddah", "Saudi Arabia", "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=400"),
        "DMM" to Triple("Dammam", "Saudi Arabia", "https://images.unsplash.com/photo-1578895101408-1a36b834405b?w=400"),
        "MED" to Triple("Madinah", "Saudi Arabia", "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=400"),
        "AHB" to Triple("Abha", "Saudi Arabia", "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400"),
        "GIZ" to Triple("Jazan", "Saudi Arabia", "https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=400"),
        "TUU" to Triple("Tabuk", "Saudi Arabia", "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400"),
        "TIF" to Triple("Taif", "Saudi Arabia", "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400"),
        "ELQ" to Triple("Qassim", "Saudi Arabia", "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400"),
        "AJF" to Triple("Al Jouf", "Saudi Arabia", "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=400"),
        // UAE
        "DXB" to Triple("Dubai", "UAE", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=400"),
        "AUH" to Triple("Abu Dhabi", "UAE", "https://images.unsplash.com/photo-1512632578888-169bbbc64f33?w=400"),
        "SHJ" to Triple("Sharjah", "UAE", "https://images.unsplash.com/photo-1578895101408-1a36b834405b?w=400"),
        // Egypt
        "CAI" to Triple("Cairo", "Egypt", "https://images.unsplash.com/photo-1572252009286-268acec5ca0a?w=400"),
        "SSH" to Triple("Sharm El Sheikh", "Egypt", "https://images.unsplash.com/photo-1539650116574-75c0c6d73f6e?w=400"),
        "HRG" to Triple("Hurghada", "Egypt", "https://images.unsplash.com/photo-1539650116574-75c0c6d73f6e?w=400"),
        // Jordan
        "AMM" to Triple("Amman", "Jordan", "https://images.unsplash.com/photo-1580834341580-8c17a3a630ca?w=400"),
        // Turkey
        "IST" to Triple("Istanbul", "Turkey", "https://images.unsplash.com/photo-1541432901042-2d8bd64b4a9b?w=400"),
        // Bahrain
        "BAH" to Triple("Bahrain", "Bahrain", "https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=400"),
        // Kuwait
        "KWI" to Triple("Kuwait City", "Kuwait", "https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=400"),
        // Qatar
        "DOH" to Triple("Doha", "Qatar", "https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=400"),
        // Oman
        "MCT" to Triple("Muscat", "Oman", "https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=400"),
        // Morocco
        "CMN" to Triple("Casablanca", "Morocco", "https://images.unsplash.com/photo-1489749798305-4fea3ae63d43?w=400")
    )

    /**
     * Execute a tool by name with the given arguments.
     */
    suspend fun execute(
        toolName: String,
        argumentsJson: String,
        context: ChatContextDto?
    ): ToolExecutionResult {
        log.info("Executing tool: $toolName with args: $argumentsJson")
        
        val args = try {
            json.parseToJsonElement(argumentsJson).jsonObject
        } catch (e: Exception) {
            log.warn("Failed to parse arguments: $argumentsJson", e)
            JsonObject(emptyMap())
        }
        
        return try {
            when (toolName) {
                "search_flights" -> searchFlights(args, context)
                "select_flight" -> selectFlight(args)
                "get_saved_travelers" -> getSavedTravelers(args, context)
                "create_booking" -> createBooking(args, context)
                "get_booking" -> getBooking(args)
                "cancel_specific_passenger" -> cancelSpecificPassenger(args)
                "calculate_change_fees" -> calculateChangeFees(args)
                "change_flight" -> changeFlight(args)
                "get_seat_map" -> getSeatMap(args)
                "change_seat" -> changeSeat(args)
                "get_available_meals" -> getAvailableMeals(args)
                "add_meal" -> addMeal(args)
                "add_baggage" -> addBaggage(args)
                "check_in" -> checkIn(args)
                "get_boarding_pass" -> getBoardingPass(args)
                "find_weather_destinations" -> findWeatherDestinations(args, context)
                "find_cheapest_flights" -> findCheapestFlights(args, context)
                "get_popular_destinations" -> getPopularDestinations(args, context)
                else -> {
                    log.warn("Unknown tool: $toolName")
                    ToolExecutionResult(
                        data = mapOf("error" to "Unknown tool: $toolName")
                    )
                }
            }
        } catch (e: Exception) {
            log.error("Tool execution failed for $toolName", e)
            ToolExecutionResult(
                data = mapOf("error" to (e.message ?: "Tool execution failed"))
            )
        }
    }

    private suspend fun searchFlights(args: JsonObject, context: ChatContextDto?): ToolExecutionResult {
        // Get origin from args, or from context if user has location, otherwise require it
        val originArg = args["origin"]?.jsonPrimitive?.contentOrNull
        val origin = originArg 
            ?: context?.userOriginAirport
            ?: return ToolExecutionResult(
                data = mapOf(
                    "error" to "origin_required",
                    "message" to "I need to know where you're flying from. Could you tell me your departure city, or enable location services so I can detect it automatically?"
                )
            )
        val destination = args["destination"]?.jsonPrimitive?.content 
            ?: return ToolExecutionResult(data = mapOf("error" to "destination is required"))
        val dateStr = args["date"]?.jsonPrimitive?.contentOrNull
        val passengers = args["passengers"]?.jsonPrimitive?.intOrNull ?: 1
        
        // Calculate date if relative or use provided
        val date = dateStr?.let { parseFlexibleDate(it) }
            ?: Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date.plus(1, DateTimeUnit.DAY)
        
        val request = FlightSearchRequest(
            origin = AirportCode(origin.uppercase()),
            destination = AirportCode(destination.uppercase()),
            departureDate = date,
            passengers = PassengerCounts(adults = passengers, children = 0, infants = 0)
        )
        
        val response = navitaireClient.searchFlights(request)
        
        val flightsList = response.flights.map { flight ->
            mapOf(
                "flightNumber" to flight.flightNumber,
                "departureTime" to flight.departureTime.toString(),
                "arrivalTime" to flight.arrivalTime.toString(),
                "duration" to "${flight.durationMinutes} minutes",
                "lowestPrice" to flight.fareFamilies.minOfOrNull { it.price.amountAsDouble },
                "currency" to "SAR"
            )
        }
        
        return ToolExecutionResult(
            data = mapOf(
                "searchId" to response.searchId,
                "origin" to origin.uppercase(),
                "destination" to destination.uppercase(),
                "date" to date.toString(),
                "flightCount" to response.flights.size,
                "flights" to flightsList
            ),
            uiType = ChatUiType.FLIGHT_LIST
        )
    }

    /**
     * Handle flight selection from search results.
     * This confirms the selection and returns flight details.
     */
    private suspend fun selectFlight(args: JsonObject): ToolExecutionResult {
        val flightNumber = args["flight_number"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "flight_number is required"))
        
        log.info("Flight selected: $flightNumber")
        
        // Return confirmation that the flight was selected
        // The actual booking flow will be handled by the frontend
        return ToolExecutionResult(
            data = mapOf(
                "status" to "selected",
                "flightNumber" to flightNumber,
                "message" to "Flight $flightNumber has been selected. Ready to proceed with booking."
            ),
            uiType = ChatUiType.FLIGHT_SELECTED
        )
    }

    /**
     * Get saved travelers for the current user.
     * Returns list of travelers with their documents for booking.
     */
    private suspend fun getSavedTravelers(args: JsonObject, context: ChatContextDto?): ToolExecutionResult {
        val userId = context?.userId
            ?: return ToolExecutionResult(data = mapOf("error" to "User not logged in. Please log in to access saved travelers."))
        
        log.info("Getting saved travelers for user: $userId")
        
        val travelers = profileService.getTravelers(userId)
        
        if (travelers.isEmpty()) {
            return ToolExecutionResult(
                data = mapOf(
                    "travelers" to emptyList<Any>(),
                    "message" to "No saved travelers found. Please add travelers in your profile."
                )
            )
        }
        
        val travelersList = travelers.map { traveler ->
            mapOf(
                "id" to traveler.id,
                "firstName" to traveler.firstName,
                "lastName" to traveler.lastName,
                "fullName" to "${traveler.firstName} ${traveler.lastName}",
                "dateOfBirth" to traveler.dateOfBirth,
                "nationality" to traveler.nationality,
                "gender" to traveler.gender.name,
                "email" to traveler.email,
                "phone" to traveler.phone,
                "isMainTraveler" to traveler.isMainTraveler,
                "documents" to traveler.documents.map { doc ->
                    mapOf(
                        "type" to doc.type.name,
                        "number" to doc.number,
                        "issuingCountry" to doc.issuingCountry,
                        "expiryDate" to doc.expiryDate,
                        "isDefault" to doc.isDefault
                    )
                }
            )
        }
        
        return ToolExecutionResult(
            data = mapOf(
                "travelers" to travelersList,
                "count" to travelers.size,
                "message" to "Found ${travelers.size} saved traveler(s)"
            )
        )
    }

    /**
     * Create a booking with the selected flight and passengers.
     * This actually creates the booking in the system.
     */
    private suspend fun createBooking(args: JsonObject, context: ChatContextDto?): ToolExecutionResult {
        val userId = context?.userId
        val searchId = context?.lastSearchId
            ?: return ToolExecutionResult(data = mapOf("error" to "No search found. Please search for flights first."))
        
        val flightNumber = args["flight_number"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "flight_number is required"))
        
        val fareFamilyStr = args["fare_family"]?.jsonPrimitive?.content?.uppercase() ?: "FLY"
        val fareFamily = try {
            FareFamilyCode.valueOf(fareFamilyStr)
        } catch (e: Exception) {
            FareFamilyCode.FLY
        }
        
        val passengersJson = args["passengers"]?.jsonArray
            ?: return ToolExecutionResult(data = mapOf("error" to "passengers array is required"))
        
        val contactEmail = args["contact_email"]?.jsonPrimitive?.content
            ?: context?.userEmail
            ?: return ToolExecutionResult(data = mapOf("error" to "contact_email is required"))
        
        log.info("Creating booking: flight=$flightNumber, fareFamily=$fareFamily, passengers=${passengersJson.size}")
        
        // Parse passengers from JSON
        val passengers = try {
            passengersJson.map { passengerElement ->
                val p = passengerElement.jsonObject
                val firstName = p["firstName"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("firstName is required for each passenger")
                val lastName = p["lastName"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("lastName is required for each passenger")
                val dateOfBirthStr = p["dateOfBirth"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("dateOfBirth is required for each passenger")
                val genderStr = p["gender"]?.jsonPrimitive?.content?.uppercase() ?: "MALE"
                val documentNumber = p["documentNumber"]?.jsonPrimitive?.content
                    ?: throw IllegalArgumentException("documentNumber is required for each passenger")
                val nationality = p["nationality"]?.jsonPrimitive?.content?.uppercase() ?: "SA"
                
                // Parse date of birth
                val dateOfBirth = LocalDate.parse(dateOfBirthStr)
                
                // Determine passenger type based on age
                val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date
                val age = today.year - dateOfBirth.year
                val passengerType = when {
                    age < 2 -> PassengerType.INFANT
                    age < 12 -> PassengerType.CHILD
                    else -> PassengerType.ADULT
                }
                
                // Determine title based on gender and age
                val title = when {
                    passengerType == PassengerType.CHILD || passengerType == PassengerType.INFANT -> 
                        if (genderStr == "MALE") Title.MSTR else Title.MISS
                    genderStr == "MALE" -> Title.MR
                    else -> Title.MS
                }
                
                Passenger(
                    type = passengerType,
                    title = title,
                    firstName = firstName,
                    lastName = lastName,
                    nationality = nationality,
                    dateOfBirth = dateOfBirth,
                    documentId = documentNumber
                )
            }
        } catch (e: Exception) {
            log.error("Failed to parse passengers", e)
            return ToolExecutionResult(data = mapOf("error" to "Invalid passenger data: ${e.message}"))
        }
        
        if (passengers.isEmpty()) {
            return ToolExecutionResult(data = mapOf("error" to "At least one passenger is required"))
        }
        
        // Create the booking request
        val totalAmount = Money.sar(500.0) // 500 SAR as default, will be overridden by service
        val bookingRequest = BookingRequest(
            searchId = searchId,
            flightNumber = flightNumber,
            fareFamily = fareFamily,
            passengers = passengers,
            ancillaries = emptyList(),
            contactEmail = contactEmail,
            payment = PaymentDetails(
                cardholderName = passengers.firstOrNull()?.fullName ?: "Card Holder",
                cardNumberLast4 = "1111",
                totalAmount = totalAmount
            )
        )
        
        return try {
            val confirmation = bookingService.createBooking(bookingRequest, userId)
            
            log.info("Booking created successfully: PNR=${confirmation.pnr.value}")
            
            ToolExecutionResult(
                data = mapOf(
                    "success" to true,
                    "pnr" to confirmation.pnr.value,
                    "bookingReference" to confirmation.bookingReference,
                    "flightNumber" to confirmation.flight.flightNumber,
                    "origin" to confirmation.flight.origin.value,
                    "destination" to confirmation.flight.destination.value,
                    "departureTime" to confirmation.flight.departureTime.toString(),
                    "passengers" to confirmation.passengers.map { p ->
                        mapOf("name" to p.fullName, "type" to p.type.name)
                    },
                    "totalPaid" to confirmation.totalPaid.amountAsDouble,
                    "currency" to confirmation.totalPaid.currency.name,
                    "message" to "Booking confirmed! Your PNR is ${confirmation.pnr.value}"
                ),
                uiType = ChatUiType.BOOKING_CONFIRMED
            )
        } catch (e: Exception) {
            log.error("Booking creation failed", e)
            ToolExecutionResult(
                data = mapOf(
                    "success" to false,
                    "error" to (e.message ?: "Booking creation failed")
                )
            )
        }
    }

    private suspend fun getBooking(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        
        val booking = navitaireClient.getBooking(pnr)
            ?: return ToolExecutionResult(
                data = mapOf("error" to "Booking not found with PNR: $pnr")
            )
        
        return ToolExecutionResult(
            data = mapOf(
                "pnr" to pnr,
                "flightNumber" to booking.flight.flightNumber,
                "origin" to booking.flight.origin.value,
                "destination" to booking.flight.destination.value,
                "departureTime" to booking.flight.departureTime.toString(),
                "passengers" to booking.passengers.map { p ->
                    mapOf("name" to p.fullName, "type" to p.type.name)
                },
                "totalPaid" to booking.totalPaid.amountAsDouble,
                "currency" to booking.totalPaid.currency.name
            ),
            uiType = ChatUiType.BOOKING_SUMMARY
        )
    }

    private suspend fun cancelSpecificPassenger(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val passengerName = args["passenger_name"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "passenger_name is required"))
        
        val result = manageBookingService.cancelPassenger(pnr, passengerName)
        
        return ToolExecutionResult(
            data = mapOf(
                "success" to true,
                "cancelledPassenger" to result.cancelledPassenger,
                "originalPnr" to result.originalPnr,
                "newPnr" to result.newPnr,
                "refundAmount" to result.refundAmount,
                "message" to "Successfully cancelled ${result.cancelledPassenger} from booking $pnr"
            )
        )
    }

    private suspend fun calculateChangeFees(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val newFlightNumber = args["new_flight_number"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "new_flight_number is required"))
        
        val result = manageBookingService.calculateChangeFees(pnr, newFlightNumber)
        
        return ToolExecutionResult(
            data = mapOf(
                "pnr" to pnr,
                "currentFlight" to result.currentFlightNumber,
                "newFlight" to newFlightNumber,
                "changeFee" to result.changeFee,
                "priceDifference" to result.priceDifference,
                "totalDue" to result.totalDue,
                "currency" to "SAR"
            ),
            uiType = ChatUiType.FLIGHT_COMPARISON
        )
    }

    private suspend fun changeFlight(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val newFlightNumber = args["new_flight_number"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "new_flight_number is required"))
        val passengerName = args["passenger_name"]?.jsonPrimitive?.contentOrNull
        
        val result = manageBookingService.changeFlight(pnr, newFlightNumber, passengerName)
        
        return ToolExecutionResult(
            data = mapOf(
                "success" to true,
                "pnr" to pnr,
                "newFlight" to result.newFlightNumber,
                "passengers" to result.affectedPassengers,
                "message" to "Successfully changed flight to ${result.newFlightNumber}"
            )
        )
    }

    private suspend fun getSeatMap(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        
        // Mock seat map data
        val availableSeats = listOf("10A", "10F", "11A", "11C", "11D", "11F", "12A", "12F", "13C", "13D")
        
        return ToolExecutionResult(
            data = mapOf(
                "pnr" to pnr,
                "availableSeats" to availableSeats,
                "windowSeats" to availableSeats.filter { it.endsWith("A") || it.endsWith("F") },
                "aisleSeats" to availableSeats.filter { it.endsWith("C") || it.endsWith("D") }
            ),
            uiType = ChatUiType.SEAT_MAP
        )
    }

    private suspend fun changeSeat(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val passengerName = args["passenger_name"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "passenger_name is required"))
        val newSeat = args["new_seat"]?.jsonPrimitive?.contentOrNull
        val preference = args["preference"]?.jsonPrimitive?.contentOrNull
        
        // Assign seat based on preference if not specified
        val seatToAssign = newSeat ?: when (preference?.lowercase()) {
            "window" -> "12A"
            "aisle" -> "12C"
            else -> "12B"
        }
        
        return ToolExecutionResult(
            data = mapOf(
                "success" to true,
                "pnr" to pnr,
                "passengerName" to passengerName,
                "newSeat" to seatToAssign,
                "seatType" to when {
                    seatToAssign.endsWith("A") || seatToAssign.endsWith("F") -> "window"
                    seatToAssign.endsWith("C") || seatToAssign.endsWith("D") -> "aisle"
                    else -> "middle"
                },
                "message" to "Successfully changed $passengerName's seat to $seatToAssign"
            )
        )
    }

    private suspend fun getAvailableMeals(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        
        // Mock meal data
        val meals = listOf(
            mapOf("code" to "CHKN", "name" to "Grilled Chicken with Rice", "price" to 45.0, "isHalal" to true),
            mapOf("code" to "BEEF", "name" to "Beef Pasta", "price" to 55.0, "isHalal" to true),
            mapOf("code" to "VEGE", "name" to "Vegetable Curry", "price" to 40.0, "isVegetarian" to true),
            mapOf("code" to "FISH", "name" to "Grilled Fish Fillet", "price" to 60.0, "isHalal" to true)
        )
        
        return ToolExecutionResult(
            data = mapOf("pnr" to pnr, "meals" to meals, "currency" to "SAR"),
            uiType = ChatUiType.ANCILLARY_OPTIONS
        )
    }

    private suspend fun addMeal(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val passengerName = args["passenger_name"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "passenger_name is required"))
        val mealCode = args["meal_code"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "meal_code is required"))
        
        return ToolExecutionResult(
            data = mapOf(
                "success" to true,
                "pnr" to pnr,
                "passengerName" to passengerName,
                "mealCode" to mealCode,
                "message" to "Successfully added meal to booking"
            )
        )
    }

    private suspend fun addBaggage(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val passengerName = args["passenger_name"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "passenger_name is required"))
        val weightKg = args["weight_kg"]?.jsonPrimitive?.intOrNull
            ?: return ToolExecutionResult(data = mapOf("error" to "weight_kg is required"))
        
        val price = when (weightKg) {
            20 -> 75.0
            25 -> 100.0
            30 -> 125.0
            else -> 75.0
        }
        
        return ToolExecutionResult(
            data = mapOf(
                "success" to true,
                "pnr" to pnr,
                "passengerName" to passengerName,
                "baggageWeight" to weightKg,
                "price" to price,
                "message" to "Successfully added ${weightKg}kg baggage allowance"
            )
        )
    }

    private suspend fun checkIn(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val passengerName = args["passenger_name"]?.jsonPrimitive?.contentOrNull
        
        // Mock check-in result
        val checkedIn = listOf(
            mapOf("name" to (passengerName ?: "John Doe"), "seat" to "12A", "boardingGroup" to "A")
        )
        
        return ToolExecutionResult(
            data = mapOf(
                "success" to true,
                "pnr" to pnr,
                "checkedInPassengers" to checkedIn,
                "message" to "Successfully checked in"
            )
        )
    }

    private suspend fun getBoardingPass(args: JsonObject): ToolExecutionResult {
        val pnr = args["pnr"]?.jsonPrimitive?.content?.uppercase()
            ?: return ToolExecutionResult(data = mapOf("error" to "pnr is required"))
        val passengerName = args["passenger_name"]?.jsonPrimitive?.content
            ?: return ToolExecutionResult(data = mapOf("error" to "passenger_name is required"))
        
        return ToolExecutionResult(
            data = mapOf(
                "pnr" to pnr,
                "passengerName" to passengerName,
                "flightNumber" to "F3100",
                "origin" to "RUH",
                "destination" to "JED",
                "departureTime" to "2024-12-08T14:30:00",
                "gate" to "B12",
                "seat" to "12A",
                "boardingGroup" to "A",
                "qrCode" to "M1${pnr}${passengerName.take(3).uppercase()}"
            ),
            uiType = ChatUiType.BOARDING_PASS
        )
    }

    /**
     * Find destinations with nice weather from the user's origin.
     * Returns destinations with weather information for vacation planning.
     * Uses real weather data from Open-Meteo API.
     */
    private suspend fun findWeatherDestinations(args: JsonObject, context: ChatContextDto?): ToolExecutionResult {
        val originArg = args["origin"]?.jsonPrimitive?.contentOrNull
        val origin = originArg
            ?: context?.userOriginAirport
            ?: return ToolExecutionResult(
                data = mapOf(
                    "error" to "origin_required",
                    "message" to "I need to know where you're located to find destinations with nice weather. Could you tell me your city, or enable location services?"
                )
            )
        val weatherPreference = args["weather_preference"]?.jsonPrimitive?.contentOrNull ?: "sunny"
        val maxResults = args["max_results"]?.jsonPrimitive?.intOrNull ?: 5
        
        log.info("Finding weather destinations from $origin with preference: $weatherPreference")
        
        // Get available routes from origin
        val routeMap = navitaireClient.getRouteMap()
        val destinations = routeMap.getDestinationsFor(AirportCode(origin.uppercase()))
            .map { it.value }
            .distinct()
        
        // Fetch real weather data for destinations
        val weatherDataMap = weatherService.getWeatherForCities(destinations)
        
        // Filter destinations based on weather preference
        val filteredDestinations = destinations.mapNotNull { code ->
            weatherDataMap[code]?.let { weather ->
                val matches = when (weatherPreference.lowercase()) {
                    "sunny" -> weather.condition == "sunny" || weather.condition == "partly_cloudy"
                    "warm" -> weather.temperature >= 25
                    "cool" -> weather.temperature < 20
                    "beach" -> weather.condition == "sunny" && weather.temperature >= 25
                    else -> true
                }
                if (matches) Pair(code, weather) else null
            }
        }.take(maxResults)
        
        // Get prices for filtered destinations
        val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date
        val searchDate = today.plus(1, DateTimeUnit.DAY)
        
        val suggestionsWithPrices = buildList {
            for ((code, weather) in filteredDestinations) {
                // Get lowest price for this destination
                val request = FlightSearchRequest(
                    origin = AirportCode(origin.uppercase()),
                    destination = AirportCode(code),
                    departureDate = searchDate,
                    passengers = PassengerCounts(adults = 1, children = 0, infants = 0)
                )
                val searchResult = try {
                    navitaireClient.searchFlights(request)
                } catch (e: Exception) {
                    null
                }
                val lowestPrice = searchResult?.flights?.flatMap { it.fareFamilies }?.minOfOrNull { it.price.amountAsDouble }
                
                // Get city metadata (country and image)
                val metadata = cityMetadata[code]
                
                add(mapOf(
                    "destinationCode" to code,
                    "destinationName" to (metadata?.first ?: weather.cityName),
                    "country" to (metadata?.second ?: ""),
                    "imageUrl" to (metadata?.third ?: ""),
                    "temperature" to weather.temperature,
                    "weatherCondition" to weather.condition,
                    "weatherDescription" to weather.description,
                    "lowestPrice" to lowestPrice,
                    "currency" to "SAR",
                    "reason" to "${weather.temperature}°C - ${weather.description}"
                ))
            }
        }
        
        return ToolExecutionResult(
            data = mapOf(
                "suggestionType" to "weather",
                "originCode" to origin.uppercase(),
                "suggestions" to suggestionsWithPrices,
                "count" to suggestionsWithPrices.size,
                "preference" to weatherPreference
            ),
            uiType = ChatUiType.DESTINATION_SUGGESTIONS
        )
    }

    /**
     * Find the cheapest flights from origin to any destination.
     * Useful for budget travelers looking for deals.
     */
    private suspend fun findCheapestFlights(args: JsonObject, context: ChatContextDto?): ToolExecutionResult {
        val origin = args["origin"]?.jsonPrimitive?.contentOrNull 
            ?: context?.userOriginAirport
            ?: return ToolExecutionResult(
                data = mapOf("error" to "I need to know where you're flying from. Please share your location or tell me your departure city."),
                uiType = null
            )
        val maxResults = args["max_results"]?.jsonPrimitive?.intOrNull ?: 5
        
        val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date
        val dateFrom = args["date_from"]?.jsonPrimitive?.contentOrNull?.let { parseFlexibleDate(it) }
            ?: today.plus(1, DateTimeUnit.DAY)
        val dateTo = args["date_to"]?.jsonPrimitive?.contentOrNull?.let { parseFlexibleDate(it) }
            ?: today.plus(7, DateTimeUnit.DAY)
        
        log.info("Finding cheapest flights from $origin between $dateFrom and $dateTo")
        
        // Get all available destinations from this origin
        val routeMap = navitaireClient.getRouteMap()
        val destinations = routeMap.getDestinationsFor(AirportCode(origin.uppercase()))
            .map { it.value }
            .distinct()
        
        // Destination names for display
        val destinationNames = mapOf(
            "DXB" to "Dubai",
            "JED" to "Jeddah",
            "RUH" to "Riyadh",
            "MED" to "Madinah",
            "DMM" to "Dammam",
            "CAI" to "Cairo",
            "AMM" to "Amman",
            "BAH" to "Bahrain",
            "KWI" to "Kuwait City",
            "MCT" to "Muscat",
            "IST" to "Istanbul",
            "BKK" to "Bangkok"
        )
        
        // Search each destination and find lowest prices
        val cheapestFlightsList = mutableListOf<Map<String, Any?>>()
        for (destCode in destinations) {
            try {
                val request = FlightSearchRequest(
                    origin = AirportCode(origin.uppercase()),
                    destination = AirportCode(destCode),
                    departureDate = dateFrom,
                    passengers = PassengerCounts(adults = 1, children = 0, infants = 0)
                )
                val result = navitaireClient.searchFlights(request)
                val cheapestFare = result.flights.flatMap { it.fareFamilies }.minByOrNull { it.price.amountAsDouble }
                val cheapestFlight = result.flights.find { fl -> fl.fareFamilies.any { ff -> ff.price == cheapestFare?.price } }
                
                // Get city metadata
                val metadata = cityMetadata[destCode]
                
                if (cheapestFlight != null && cheapestFare != null) {
                    cheapestFlightsList.add(mapOf(
                        "destinationCode" to destCode,
                        "destinationName" to (metadata?.first ?: destCode),
                        "country" to (metadata?.second ?: ""),
                        "imageUrl" to (metadata?.third ?: ""),
                        "flightNumber" to cheapestFlight.flightNumber,
                        "departureTime" to cheapestFlight.departureTime.toString(),
                        "arrivalTime" to cheapestFlight.arrivalTime.toString(),
                        "duration" to "${cheapestFlight.durationMinutes} minutes",
                        "lowestPrice" to cheapestFare.price.amountAsDouble,
                        "currency" to "SAR",
                        "fareFamily" to cheapestFare.code.name
                    ))
                }
            } catch (e: Exception) {
                log.warn("Failed to search flights to $destCode: ${e.message}")
            }
        }
        val cheapestFlights = cheapestFlightsList
            .sortedBy { (it["lowestPrice"] as? Double) ?: Double.MAX_VALUE }
            .take(maxResults)
        
        return ToolExecutionResult(
            data = mapOf(
                "suggestionType" to "cheapest",
                "originCode" to origin.uppercase(),
                "dateRange" to "$dateFrom to $dateTo",
                "suggestions" to cheapestFlights,
                "count" to cheapestFlights.size
            ),
            uiType = ChatUiType.DESTINATION_SUGGESTIONS
        )
    }

    /**
     * Get popular destinations from origin.
     * Returns trending/popular destinations for general browsing.
     */
    private suspend fun getPopularDestinations(args: JsonObject, context: ChatContextDto?): ToolExecutionResult {
        val origin = args["origin"]?.jsonPrimitive?.contentOrNull 
            ?: context?.userOriginAirport
            ?: return ToolExecutionResult(
                data = mapOf("error" to "I need to know where you're flying from. Please share your location or tell me your departure city."),
                uiType = null
            )
        val travelType = args["travel_type"]?.jsonPrimitive?.contentOrNull
        val maxResults = args["max_results"]?.jsonPrimitive?.intOrNull ?: 6
        
        log.info("Getting popular destinations from $origin, type: $travelType")
        
        // Popular destinations with metadata
        data class DestinationInfo(
            val code: String,
            val name: String,
            val country: String,
            val type: List<String>, // leisure, business, family, adventure
            val description: String,
            val imageHint: String
        )
        
        val popularDestinations = listOf(
            DestinationInfo("DXB", "Dubai", "UAE", listOf("leisure", "business", "family"), "Modern luxury and entertainment", "dubai-skyline"),
            DestinationInfo("JED", "Jeddah", "Saudi Arabia", listOf("leisure", "business"), "Coastal city with rich history", "jeddah-corniche"),
            DestinationInfo("CAI", "Cairo", "Egypt", listOf("leisure", "family", "adventure"), "Ancient pyramids and history", "cairo-pyramids"),
            DestinationInfo("IST", "Istanbul", "Turkey", listOf("leisure", "family"), "Where East meets West", "istanbul-mosque"),
            DestinationInfo("BAH", "Bahrain", "Bahrain", listOf("leisure", "business"), "Island getaway nearby", "bahrain-skyline"),
            DestinationInfo("AMM", "Amman", "Jordan", listOf("leisure", "adventure"), "Gateway to Petra", "amman-citadel"),
            DestinationInfo("MLE", "Maldives", "Maldives", listOf("leisure"), "Paradise island escape", "maldives-beach"),
            DestinationInfo("BKK", "Bangkok", "Thailand", listOf("leisure", "family", "adventure"), "Vibrant culture and cuisine", "bangkok-temple"),
            DestinationInfo("MCT", "Muscat", "Oman", listOf("leisure", "adventure"), "Scenic coastal beauty", "muscat-mosque"),
            DestinationInfo("KWI", "Kuwait City", "Kuwait", listOf("business"), "Modern Arabian city", "kuwait-towers")
        )
        
        // Filter by travel type if specified
        val filtered = if (travelType != null) {
            popularDestinations.filter { travelType.lowercase() in it.type }
        } else {
            popularDestinations
        }
        
        // Get routes from origin to check availability
        val routeMap = navitaireClient.getRouteMap()
        val availableDestinations = routeMap.getDestinationsFor(AirportCode(origin.uppercase()))
            .map { it.value }
            .toSet()
        
        // Filter to only available destinations and get prices
        val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date
        val searchDate = today.plus(3, DateTimeUnit.DAY) // Search for 3 days out
        
        val availableFiltered = filtered
            .filter { it.code in availableDestinations }
            .take(maxResults)
        
        val suggestions = buildList {
            for (dest in availableFiltered) {
                // Get price for this destination
                val price = try {
                    val request = FlightSearchRequest(
                        origin = AirportCode(origin.uppercase()),
                        destination = AirportCode(dest.code),
                        departureDate = searchDate,
                        passengers = PassengerCounts(adults = 1, children = 0, infants = 0)
                    )
                    val result = navitaireClient.searchFlights(request)
                    result.flights.flatMap { it.fareFamilies }.minOfOrNull { it.price.amountAsDouble }
                } catch (e: Exception) {
                    null
                }
                
                // Get image URL from metadata
                val imageUrl = cityMetadata[dest.code]?.third ?: ""
                
                add(mapOf(
                    "destinationCode" to dest.code,
                    "destinationName" to dest.name,
                    "country" to dest.country,
                    "imageUrl" to imageUrl,
                    "lowestPrice" to price,
                    "currency" to "SAR",
                    "description" to dest.description,
                    "travelTypes" to dest.type,
                    "reason" to dest.description
                ))
            }
        }
        
        return ToolExecutionResult(
            data = mapOf(
                "suggestionType" to "popular",
                "originCode" to origin.uppercase(),
                "travelType" to (travelType ?: "all"),
                "suggestions" to suggestions,
                "count" to suggestions.size
            ),
            uiType = ChatUiType.DESTINATION_SUGGESTIONS
        )
    }

    /**
     * Parse flexible date strings like "tomorrow", "next Friday", or ISO dates.
     */
    private fun parseFlexibleDate(dateStr: String): LocalDate {
        val today = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Riyadh")).date
        
        return when (dateStr.lowercase().trim()) {
            "today" -> today
            "tomorrow" -> today.plus(1, DateTimeUnit.DAY)
            "day after tomorrow" -> today.plus(2, DateTimeUnit.DAY)
            else -> {
                // Try parsing as ISO date
                try {
                    LocalDate.parse(dateStr)
                } catch (e: Exception) {
                    // Try parsing relative day names
                    val dayOfWeek = when {
                        dateStr.contains("monday", ignoreCase = true) -> DayOfWeek.MONDAY
                        dateStr.contains("tuesday", ignoreCase = true) -> DayOfWeek.TUESDAY
                        dateStr.contains("wednesday", ignoreCase = true) -> DayOfWeek.WEDNESDAY
                        dateStr.contains("thursday", ignoreCase = true) -> DayOfWeek.THURSDAY
                        dateStr.contains("friday", ignoreCase = true) -> DayOfWeek.FRIDAY
                        dateStr.contains("saturday", ignoreCase = true) -> DayOfWeek.SATURDAY
                        dateStr.contains("sunday", ignoreCase = true) -> DayOfWeek.SUNDAY
                        else -> null
                    }
                    
                    if (dayOfWeek != null) {
                        var date = today
                        val isNext = dateStr.contains("next", ignoreCase = true)
                        if (isNext) {
                            date = date.plus(7, DateTimeUnit.DAY)
                        }
                        while (date.dayOfWeek != dayOfWeek) {
                            date = date.plus(1, DateTimeUnit.DAY)
                        }
                        date
                    } else {
                        // Default to tomorrow
                        today.plus(1, DateTimeUnit.DAY)
                    }
                }
            }
        }
    }
}
