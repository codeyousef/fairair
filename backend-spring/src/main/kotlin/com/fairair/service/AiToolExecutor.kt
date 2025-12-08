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
    private val profileService: ProfileService
) {
    private val log = LoggerFactory.getLogger(AiToolExecutor::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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
                "search_flights" -> searchFlights(args)
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

    private suspend fun searchFlights(args: JsonObject): ToolExecutionResult {
        val origin = args["origin"]?.jsonPrimitive?.contentOrNull ?: "RUH"
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
