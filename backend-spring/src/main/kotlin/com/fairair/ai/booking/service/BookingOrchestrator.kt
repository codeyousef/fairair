package com.fairair.ai.booking.service

import com.fairair.ai.booking.agent.BookingAgentResult
import com.fairair.ai.booking.agent.FlightBookingAgent
import com.fairair.ai.booking.exception.EntityExtractionException
import com.fairair.ai.booking.exception.RouteValidationException
import com.fairair.contract.dto.*
import com.fairair.contract.model.*
import com.fairair.koog.KoogException
import com.fairair.service.BookingService
import com.fairair.service.FlightService
import com.fairair.service.ProfileService
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BookingOrchestrator(
    private val flightBookingAgent: FlightBookingAgent,
    private val profileService: ProfileService,
    private val bookingService: BookingService,
    private val flightService: FlightService,
    private val referenceDataService: ReferenceDataService
) {
    private val logger = LoggerFactory.getLogger(BookingOrchestrator::class.java)
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    suspend fun handleUserRequest(userInput: String, context: ChatContextDto? = null): BookingAgentResult {
        val normalized = userInput.trim().lowercase()
        
        // Heuristic: Handle negative response
        if (normalized.matches(Regex("^(no|nope|nah|cancel|nevermind|never mind).*"))) {
            return BookingAgentResult(
                response = "Okay, let me know if you need anything else.",
                pendingContext = PendingBookingContext() // Clear pending context
            )
        }

        // =========================================================================
        // BOOKING FLOW STATE MACHINE
        // =========================================================================
        
        // Check current booking flow step from context
        val currentStep = context?.let { detectBookingStep(it) } ?: BookingFlowStep.NONE
        logger.info("Current booking step: $currentStep, input: '$normalized'")
        
        return when (currentStep) {
            BookingFlowStep.FLIGHT_SELECTED -> handleFlightSelectedStep(userInput, context!!)
            BookingFlowStep.AWAITING_PASSENGERS -> handleAwaitingPassengersStep(userInput, context!!)
            BookingFlowStep.READY_TO_CONFIRM -> handleReadyToConfirmStep(userInput, context!!)
            else -> handleInitialState(userInput, context)
        }
    }
    
    /**
     * Detect the current booking flow step based on context.
     */
    private fun detectBookingStep(context: ChatContextDto): BookingFlowStep {
        val selectedFlight = context.lastFlightNumber
        val searchId = context.lastSearchId
        
        // If we have metadata indicating the step, use it
        context.metadata["bookingStep"]?.let { step ->
            return try {
                BookingFlowStep.valueOf(step)
            } catch (e: Exception) {
                BookingFlowStep.NONE
            }
        }
        
        // Otherwise infer from context
        return when {
            selectedFlight != null && searchId != null -> BookingFlowStep.FLIGHT_SELECTED
            else -> BookingFlowStep.NONE
        }
    }
    
    /**
     * Handle initial state - flight search or selection.
     */
    private suspend fun handleInitialState(userInput: String, context: ChatContextDto?): BookingAgentResult {
        val normalized = userInput.trim().lowercase()
        
        // Check for flight selection pattern
        val flightMatch = Regex("(?:take|select|choose|want|book)\\s+(?:flight\\s+)?(f\\d+)", RegexOption.IGNORE_CASE).find(userInput)
            ?: Regex("\\b(f\\d{4})\\b", RegexOption.IGNORE_CASE).find(userInput)
            
        if (flightMatch != null) {
            val flightNum = flightMatch.groupValues.last().uppercase()
            return handleFlightSelection(flightNum, context)
        }
        
        // Check for booking confirmation without proper context
        if (isBookingConfirmation(userInput) && context?.lastFlightNumber != null) {
            return handleFlightSelectedStep(userInput, context)
        }
        
        // Check for destination recommendation requests
        if (isRecommendationRequest(normalized)) {
            return handleRecommendationRequest(normalized, context)
        }
        
        // Default: Process as flight search
        return try {
            flightBookingAgent.process(userInput, context)
        } catch (e: EntityExtractionException) {
            logger.info("Entity extraction needs more info: ${e.message}")
            BookingAgentResult(
                response = e.message ?: "Could you please tell me more about your travel plans?",
                pendingContext = e.pendingContext
            )
        } catch (e: RouteValidationException) {
            logger.info("Route validation failed: ${e.message}")
            BookingAgentResult(response = "Sorry, we don't fly that route. We serve destinations in Saudi Arabia, UAE, and Egypt. Where would you like to go?")
        } catch (e: KoogException) {
            logger.warn("Koog execution failed (handled): ${e.message}")
            BookingAgentResult(
                response = "I'm sorry, I couldn't understand your request clearly. Could you please specify where you're flying from, where you're going, and when?"
            )
        } catch (e: Exception) {
            logger.error("Unexpected error in BookingOrchestrator", e)
            BookingAgentResult(
                response = "I apologize, but I am currently experiencing technical difficulties. Please try again later."
            )
        }
    }
    
    /**
     * Detect if the user is asking for a destination recommendation.
     * This catches queries where the user doesn't have a specific destination in mind.
     */
    private fun isRecommendationRequest(input: String): Boolean {
        // Direct recommendation keywords
        val recommendationPatterns = listOf(
            "recommend",
            "suggest",
            "where should i",
            "where can i",
            "closest",
            "nearest",
            "nice weather",
            "warm weather",
            "good weather",
            "best destination",
            "best place",
            "where to go",
            "any destination",
            "somewhere",
            "anywhere",
            "don't know where",
            "not sure where",
            "help me choose",
            "help me decide",
            "cheapest destination",
            "cheapest place",
            "budget destination",
            "affordable",
            "cheap flight",
            "cheapest flight"
        )
        
        if (recommendationPatterns.any { input.contains(it) }) {
            return true
        }
        
        // Pattern: "fly to [superlative] destination/place" without a specific city
        // e.g., "fly to cheapest destination", "go to warmest place"
        val superlativePattern = Regex("(fly|go|travel)\\s+to\\s+(the\\s+)?(cheapest|warmest|closest|nearest|best|nicest)")
        if (superlativePattern.containsMatchIn(input)) {
            return true
        }
        
        // Pattern: wanting to fly but asking about options/destinations
        if ((input.contains("want to fly") || input.contains("want to go") || input.contains("want to travel")) &&
            (input.contains("cheap") || input.contains("where") || input.contains("option") || 
             input.contains("destination") || input.contains("place"))) {
            return true
        }
        
        return false
    }
    
    /**
     * Handle destination recommendation requests.
     */
    private suspend fun handleRecommendationRequest(input: String, context: ChatContextDto?): BookingAgentResult {
        logger.info("Handling recommendation request: $input")
        
        // Determine the origin - from context or user's location
        val origin = context?.pendingOrigin 
            ?: context?.userOriginAirport 
            ?: return BookingAgentResult(
                response = "I'd love to help you find a great destination! Where will you be flying from?",
                pendingContext = PendingBookingContext()
            )
        
        // Get available destinations from this origin
        val destinations = referenceDataService.getDestinationsFrom(origin)
        
        if (destinations.isEmpty()) {
            return BookingAgentResult(
                response = "I couldn't find any flights from $origin. Would you like to try a different departure city?",
                pendingContext = PendingBookingContext()
            )
        }
        
        val isWeatherQuery = input.contains("weather") || input.contains("warm") || input.contains("nice")
        val isCheapestQuery = input.contains("cheap") || input.contains("budget") || input.contains("affordable")
        
        // Build destination suggestions with city names, images, weather, and prices
        var suggestionDtos = destinations.map { code ->
            val cityName = referenceDataService.getCityName(code).replaceFirstChar { it.uppercase() }
            val weather = getWeatherForDestination(code)
            val basePrice = getBasePriceForRoute(origin, code)
            DestinationSuggestionDto(
                destinationCode = code,
                destinationName = cityName,
                country = getCountryForCode(code),
                weather = weather,
                lowestPrice = basePrice,
                imageUrl = getCityImageUrl(code),
                reason = getReasonForDestination(code, input)
            )
        }
        
        // If user asked for nice/warm weather, filter to warm destinations (18-28°C)
        if (isWeatherQuery) {
            val warmDestinations = suggestionDtos.filter { dto ->
                dto.weather?.let { it.temperature in 18..28 } ?: false
            }
            if (warmDestinations.isNotEmpty()) {
                suggestionDtos = warmDestinations
            }
        }
        
        // If user asked for cheapest, sort by price
        if (isCheapestQuery) {
            suggestionDtos = suggestionDtos.sortedBy { it.lowestPrice ?: Double.MAX_VALUE }
        }
        
        // Generate a friendly response with suggestions
        val destList = suggestionDtos.joinToString(", ") { it.destinationName }
        val suggestions = suggestionDtos.map { "Fly to ${it.destinationName}" }
        
        val responseText = when {
            isCheapestQuery -> {
                val cheapest = suggestionDtos.firstOrNull()
                if (cheapest != null) {
                    "Looking for budget-friendly options from $origin! The cheapest destination is ${cheapest.destinationName} starting from SAR ${cheapest.lowestPrice?.toInt() ?: "N/A"}.\n\nHere are all available destinations sorted by price. Pick one and tell me when you'd like to travel!"
                } else {
                    "Here are destinations from $origin sorted by price. Pick one and tell me when you'd like to travel!"
                }
            }
            isWeatherQuery -> {
                "I found some destinations with lovely warm weather! From $origin, you can fly to: $destList.\n\nWhich destination sounds good to you? Just pick one and tell me when you'd like to travel!"
            }
            else -> {
                "Great! From $origin, you can fly to: $destList.\n\nWhich destination sounds good to you? Just pick one and tell me when you'd like to travel!"
            }
        }
        
        return BookingAgentResult(
            response = responseText,
            uiType = ChatUiType.DESTINATION_SUGGESTIONS,
            uiData = json.encodeToString(DestinationSuggestionsPayloadDto(
                suggestions = suggestionDtos,
                suggestionType = detectSuggestionType(input),
                originCode = origin
            )),
            pendingContext = PendingBookingContext(
                origin = origin
            ),
            suggestions = suggestions.take(4)
        )
    }
    
    /**
     * Get simulated weather for a destination (December weather approximations).
     */
    private fun getWeatherForDestination(code: String): WeatherInfoDto {
        // December weather approximations for each destination
        return when (code) {
            "DXB" -> WeatherInfoDto(24, "sunny", "Pleasant sunny weather, perfect for sightseeing")
            "AUH" -> WeatherInfoDto(23, "sunny", "Warm and sunny, ideal for outdoor activities")
            "CAI" -> WeatherInfoDto(18, "partly_cloudy", "Mild and comfortable, great for exploring")
            "SSH" -> WeatherInfoDto(22, "sunny", "Warm beach weather, perfect for diving")
            "HRG" -> WeatherInfoDto(21, "sunny", "Beautiful Red Sea weather")
            "LXR" -> WeatherInfoDto(20, "sunny", "Warm days, cool nights - perfect for temples")
            "RUH" -> WeatherInfoDto(16, "partly_cloudy", "Cool and pleasant winter weather")
            "JED" -> WeatherInfoDto(26, "sunny", "Warm coastal weather year-round")
            "DMM" -> WeatherInfoDto(18, "partly_cloudy", "Mild winter weather")
            "MED" -> WeatherInfoDto(22, "sunny", "Pleasant weather for pilgrimage")
            "GIZ" -> WeatherInfoDto(28, "sunny", "Warm coastal weather")
            else -> WeatherInfoDto(22, "sunny", "Pleasant weather")
        }
    }
    
    /**
     * Get a representative image URL for each destination city.
     */
    private fun getCityImageUrl(code: String): String {
        // Using Unsplash for high-quality city images
        return when (code) {
            "DXB" -> "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=400&h=300&fit=crop" // Dubai skyline
            "AUH" -> "https://images.unsplash.com/photo-1583095180430-1c56dc8f3f73?w=400&h=300&fit=crop" // Abu Dhabi mosque
            "CAI" -> "https://images.unsplash.com/photo-1572252009286-268acec5ca0a?w=400&h=300&fit=crop" // Cairo pyramids
            "SSH" -> "https://images.unsplash.com/photo-1539768942893-daf53e448371?w=400&h=300&fit=crop" // Sharm beach
            "HRG" -> "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=400&h=300&fit=crop" // Red Sea diving
            "LXR" -> "https://images.unsplash.com/photo-1568322445389-f64ac2515020?w=400&h=300&fit=crop" // Luxor temple
            "RUH" -> "https://images.unsplash.com/photo-1586724237569-f3d0c1dee8c6?w=400&h=300&fit=crop" // Riyadh skyline
            "JED" -> "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=400&h=300&fit=crop" // Jeddah waterfront
            "DMM" -> "https://images.unsplash.com/photo-1578895101408-1a36b834405b?w=400&h=300&fit=crop" // Dammam
            "MED" -> "https://images.unsplash.com/photo-1591604129939-f1efa4d9f7fa?w=400&h=300&fit=crop" // Medina
            "GIZ" -> "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=400&h=300&fit=crop" // Jizan coast
            else -> "https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=400&h=300&fit=crop" // Generic travel
        }
    }
    
    private fun getCountryForCode(code: String): String {
        return when (code) {
            "JED", "RUH", "DMM", "MED", "GIZ" -> "Saudi Arabia"
            "DXB", "AUH", "SHJ" -> "UAE"
            "CAI", "SSH", "HRG", "LXR" -> "Egypt"
            else -> ""
        }
    }
    
    /**
     * Get base price estimate for a route (for recommendation display).
     */
    private fun getBasePriceForRoute(origin: String, destination: String): Double {
        val route = setOf(origin, destination)
        return when {
            route == setOf("JED", "RUH") -> 350.0
            route == setOf("JED", "DMM") -> 450.0
            route == setOf("RUH", "DMM") -> 280.0
            route == setOf("RUH", "DXB") || route == setOf("JED", "DXB") -> 650.0
            route == setOf("DMM", "DXB") -> 550.0
            route == setOf("JED", "CAI") || route == setOf("RUH", "CAI") -> 850.0
            route.contains("SSH") || route.contains("HRG") -> 750.0
            route.contains("LXR") -> 900.0
            route.contains("AUH") -> 600.0
            route.contains("MED") -> 400.0
            route.contains("GIZ") -> 380.0
            else -> 500.0
        }
    }
    
    private fun getReasonForDestination(code: String, userInput: String): String {
        // Provide contextual reasons based on user's request
        val isWeatherQuery = userInput.contains("weather") || userInput.contains("sunny") || userInput.contains("warm")
        val isQuickTrip = userInput.contains("closest") || userInput.contains("nearest") || userInput.contains("quick")
        val isCheapQuery = userInput.contains("cheap") || userInput.contains("budget") || userInput.contains("affordable")
        
        return when {
            isCheapQuery -> when (code) {
                "DMM" -> "Great value domestic flight"
                "RUH" -> "Budget-friendly capital trip"
                "JED" -> "Affordable coastal getaway"
                "MED" -> "Economical pilgrimage"
                else -> "Good value destination"
            }
            isWeatherQuery -> when (code) {
                "SSH" -> "Perfect beach weather for diving"
                "HRG" -> "Warm Red Sea paradise"
                "DXB" -> "Sunny skies, world-class attractions"
                "JED" -> "Warm coastal escape"
                "CAI" -> "Mild weather, ancient wonders"
                else -> "Pleasant weather awaits"
            }
            isQuickTrip -> when (code) {
                "RUH" -> "Quick domestic flight"
                "JED" -> "Short hop to the coast"
                "DMM" -> "Eastern Province getaway"
                "DXB" -> "Just a short flight away"
                else -> "Easy to reach"
            }
            else -> when (code) {
                "DXB" -> "Shopping, beaches, adventure"
                "CAI" -> "Ancient pyramids, rich history"
                "MED" -> "Sacred city, spiritual journey"
                "SSH" -> "Diving paradise"
                else -> "Popular destination"
            }
        }
    }
    
    private fun detectSuggestionType(input: String): String {
        return when {
            input.contains("weather") || input.contains("sunny") || input.contains("warm") || input.contains("nice") -> "weather"
            input.contains("cheap") || input.contains("budget") || input.contains("affordable") -> "cheapest"
            input.contains("closest") || input.contains("nearest") -> "quick_getaway"
            input.contains("popular") || input.contains("best") -> "popular"
            else -> "popular"
        }
    }
    
    /**
     * Handle flight selection - check auth and fetch saved travelers.
     */
    private suspend fun handleFlightSelection(flightNum: String, context: ChatContextDto?): BookingAgentResult {
        logger.info("Flight selection detected: $flightNum")
        
        val isSignedIn = context?.userId != null
        
        if (!isSignedIn) {
            return BookingAgentResult(
                response = "Flight $flightNum is a great choice! To complete your booking, please sign in or create an account. This lets us save your booking and send you confirmation details.",
                uiType = ChatUiType.SIGN_IN_REQUIRED,
                pendingContext = PendingBookingContext(
                    origin = context?.pendingOrigin,
                    destination = context?.pendingDestination,
                    date = context?.pendingDate,
                    passengers = context?.pendingPassengers,
                    searchId = context?.lastSearchId,
                    selectedFlight = flightNum,
                    bookingStep = BookingFlowStep.FLIGHT_SELECTED
                )
            )
        }
        
        // User is signed in - fetch saved travelers
        val userId = context.userId!!
        val travelers = profileService.getTravelers(userId)
        
        return if (travelers.isNotEmpty()) {
            // Has saved travelers - ask which ones to book for
            val travelerNames = travelers.mapIndexed { index, t -> 
                "${index + 1}. ${t.firstName} ${t.lastName}" + if (t.isMainTraveler) " (You)" else ""
            }.joinToString("\n")
            
            BookingAgentResult(
                response = "Great choice! Flight $flightNum is selected.\n\nI found your saved travelers:\n$travelerNames\n\nWho will be traveling? You can say \"book for myself\" or specify names like \"book for Ahmed and Sara\".",
                uiType = ChatUiType.PASSENGER_SELECT,
                uiData = json.encodeToString(PassengerSelectPayload(
                    travelers = travelers.map { 
                        TravelerOptionDto(
                            id = it.id,
                            name = "${it.firstName} ${it.lastName}",
                            isMainTraveler = it.isMainTraveler
                        )
                    },
                    flightNumber = flightNum
                )),
                pendingContext = PendingBookingContext(
                    origin = context.pendingOrigin,
                    destination = context.pendingDestination,
                    date = context.pendingDate,
                    passengers = context.pendingPassengers,
                    searchId = context.lastSearchId,
                    selectedFlight = flightNum,
                    bookingStep = BookingFlowStep.FLIGHT_SELECTED
                ),
                suggestions = listOf("Book for myself", "Book for all travelers", "Enter new passenger")
            )
        } else {
            // No saved travelers - ask for passenger details
            BookingAgentResult(
                response = "Great choice! Flight $flightNum is selected.\n\nYou don't have any saved travelers yet. Please provide the passenger details:\n\n• Full name (as on ID)\n• Date of birth\n• Nationality\n• ID/Passport number\n\nFor example: \"John Smith, born 15 March 1990, Saudi, passport A12345678\"",
                uiType = ChatUiType.PASSENGER_FORM,
                pendingContext = PendingBookingContext(
                    origin = context.pendingOrigin,
                    destination = context.pendingDestination,
                    date = context.pendingDate,
                    passengers = 1,
                    searchId = context.lastSearchId,
                    selectedFlight = flightNum,
                    bookingStep = BookingFlowStep.AWAITING_PASSENGERS
                ),
                suggestions = listOf("Save this traveler for future bookings")
            )
        }
    }
    
    /**
     * Handle the FLIGHT_SELECTED step - user is selecting passengers.
     */
    private suspend fun handleFlightSelectedStep(userInput: String, context: ChatContextDto): BookingAgentResult {
        val userId = context.userId
        val selectedFlight = context.lastFlightNumber
        
        if (userId == null) {
            return BookingAgentResult(
                response = "Please sign in to continue with your booking.",
                uiType = ChatUiType.SIGN_IN_REQUIRED,
                pendingContext = PendingBookingContext(
                    selectedFlight = selectedFlight,
                    bookingStep = BookingFlowStep.FLIGHT_SELECTED
                )
            )
        }
        
        if (selectedFlight == null) {
            return BookingAgentResult(
                response = "I don't see a flight selected. Please search for flights first."
            )
        }
        
        val normalized = userInput.trim().lowercase()
        val travelers = profileService.getTravelers(userId)
        
        // Check if user wants to book for themselves
        if (normalized.contains("myself") || normalized.contains("just me") ||
            (isBookingConfirmation(userInput) && travelers.any { it.isMainTraveler })) {
            
            val mainTraveler = travelers.find { it.isMainTraveler }
            if (mainTraveler != null) {
                return createBookingForTravelers(listOf(mainTraveler), selectedFlight, context)
            } else if (travelers.isNotEmpty()) {
                // Use first traveler if no main traveler set
                return createBookingForTravelers(listOf(travelers.first()), selectedFlight, context)
            } else {
                return BookingAgentResult(
                    response = "You don't have any saved travelers. Please provide your details:\n\n• Full name (as on ID)\n• Date of birth (DD/MM/YYYY)\n• Nationality\n• ID/Passport number",
                    pendingContext = PendingBookingContext(
                        selectedFlight = selectedFlight,
                        searchId = context.lastSearchId,
                        bookingStep = BookingFlowStep.AWAITING_PASSENGERS
                    )
                )
            }
        }
        
        // Check if user wants to book for all travelers
        if (normalized.contains("all") || normalized.contains("everyone") || normalized.contains("all travelers")) {
            if (travelers.isNotEmpty()) {
                return createBookingForTravelers(travelers, selectedFlight, context)
            }
        }
        
        // Check if user wants to enter new passenger
        if (normalized.contains("new") || normalized.contains("enter") || normalized.contains("add")) {
            return BookingAgentResult(
                response = "Please provide the passenger details:\n\n• Full name (as on ID)\n• Date of birth (DD/MM/YYYY)\n• Nationality\n• ID/Passport number\n\nFor example: \"Ahmed Al-Rashid, 15/03/1990, Saudi, A12345678\"",
                pendingContext = PendingBookingContext(
                    selectedFlight = selectedFlight,
                    searchId = context.lastSearchId,
                    bookingStep = BookingFlowStep.AWAITING_PASSENGERS
                )
            )
        }
        
        // Try to match specific traveler names
        val matchedTravelers = travelers.filter { traveler ->
            val fullName = "${traveler.firstName} ${traveler.lastName}".lowercase()
            normalized.contains(traveler.firstName.lowercase()) || 
            normalized.contains(traveler.lastName.lowercase()) ||
            normalized.contains(fullName)
        }
        
        if (matchedTravelers.isNotEmpty()) {
            return createBookingForTravelers(matchedTravelers, selectedFlight, context)
        }
        
        // Couldn't understand - ask again
        val travelerNames = travelers.mapIndexed { index, t -> 
            "${index + 1}. ${t.firstName} ${t.lastName}" + if (t.isMainTraveler) " (You)" else ""
        }.joinToString("\n")
        
        return BookingAgentResult(
            response = "I couldn't identify which passengers you'd like to book for. Your saved travelers are:\n$travelerNames\n\nPlease specify who will be traveling, or say \"book for myself\".",
            pendingContext = PendingBookingContext(
                selectedFlight = selectedFlight,
                searchId = context.lastSearchId,
                bookingStep = BookingFlowStep.FLIGHT_SELECTED
            ),
            suggestions = listOf("Book for myself", "Book for all travelers")
        )
    }
    
    /**
     * Handle AWAITING_PASSENGERS step - user is entering passenger details.
     */
    private suspend fun handleAwaitingPassengersStep(userInput: String, context: ChatContextDto): BookingAgentResult {
        val selectedFlight = context.lastFlightNumber ?: return BookingAgentResult(
            response = "I don't see a flight selected. Please search for flights first."
        )
        
        // Try to parse passenger details from input
        val passengerData = parsePassengerFromInput(userInput)
        
        if (passengerData == null) {
            return BookingAgentResult(
                response = "I couldn't understand the passenger details. Please provide:\n\n• Full name (as on ID)\n• Date of birth (DD/MM/YYYY)\n• Nationality\n• ID/Passport number\n\nFor example: \"Ahmed Al-Rashid, 15/03/1990, Saudi, A12345678\"",
                pendingContext = PendingBookingContext(
                    selectedFlight = selectedFlight,
                    searchId = context.lastSearchId,
                    bookingStep = BookingFlowStep.AWAITING_PASSENGERS
                )
            )
        }
        
        // Create booking with parsed passenger data
        return createBookingWithPassengerData(passengerData, selectedFlight, context)
    }
    
    /**
     * Handle READY_TO_CONFIRM step - final confirmation.
     */
    private suspend fun handleReadyToConfirmStep(userInput: String, context: ChatContextDto): BookingAgentResult {
        if (isBookingConfirmation(userInput)) {
            return BookingAgentResult(
                response = "Your booking has already been processed. Check 'My Bookings' to see your reservations.",
                suggestions = listOf("Show my bookings", "Search new flight")
            )
        }
        
        return BookingAgentResult(
            response = "Would you like to confirm this booking? Say 'yes' to proceed or 'no' to cancel.",
            pendingContext = PendingBookingContext(
                selectedFlight = context.lastFlightNumber,
                searchId = context.lastSearchId,
                bookingStep = BookingFlowStep.READY_TO_CONFIRM
            )
        )
    }
    
    /**
     * Create a booking for the specified saved travelers.
     */
    private suspend fun createBookingForTravelers(
        travelers: List<SavedTravelerDto>,
        flightNumber: String,
        context: ChatContextDto
    ): BookingAgentResult {
        val userId = context.userId!!
        val searchId = context.lastSearchId
        val userEmail = context.userEmail ?: travelers.firstOrNull()?.email ?: "customer@fairair.com"
        
        logger.info("Creating booking for ${travelers.size} travelers on flight $flightNumber")
        
        // Convert saved travelers to Passenger objects
        val passengers = travelers.map { traveler ->
            val dob = LocalDate.parse(traveler.dateOfBirth)
            val age = java.time.LocalDate.now().year - dob.year
            
            val passengerType = when {
                age < 2 -> PassengerType.INFANT
                age < 12 -> PassengerType.CHILD
                else -> PassengerType.ADULT
            }
            
            val title = when {
                passengerType != PassengerType.ADULT -> 
                    if (traveler.gender == Gender.MALE) Title.MSTR else Title.MISS
                traveler.gender == Gender.MALE -> Title.MR
                else -> Title.MS
            }
            
            val documentNumber = traveler.documents.firstOrNull()?.number ?: "UNKNOWN"
            
            Passenger(
                type = passengerType,
                title = title,
                firstName = traveler.firstName,
                lastName = traveler.lastName,
                nationality = traveler.nationality,
                dateOfBirth = dob,
                documentId = documentNumber
            )
        }
        
        return executeBooking(passengers, flightNumber, searchId, userEmail, userId)
    }
    
    /**
     * Create a booking with manually entered passenger data.
     */
    private suspend fun createBookingWithPassengerData(
        passengerData: ParsedPassenger,
        flightNumber: String,
        context: ChatContextDto
    ): BookingAgentResult {
        val userId = context.userId
        val searchId = context.lastSearchId
        val userEmail = context.userEmail ?: "customer@fairair.com"
        
        logger.info("Creating booking with manually entered passenger: ${passengerData.firstName} ${passengerData.lastName}")
        
        val age = java.time.LocalDate.now().year - passengerData.dateOfBirth.year
        val passengerType = when {
            age < 2 -> PassengerType.INFANT
            age < 12 -> PassengerType.CHILD
            else -> PassengerType.ADULT
        }
        
        val title = Title.MR // Default
        
        val passenger = Passenger(
            type = passengerType,
            title = title,
            firstName = passengerData.firstName,
            lastName = passengerData.lastName,
            nationality = passengerData.nationality,
            dateOfBirth = passengerData.dateOfBirth,
            documentId = passengerData.documentNumber
        )
        
        return executeBooking(listOf(passenger), flightNumber, searchId, userEmail, userId)
    }
    
    /**
     * Execute the actual booking creation.
     */
    private suspend fun executeBooking(
        passengers: List<Passenger>,
        flightNumber: String,
        searchId: String?,
        contactEmail: String,
        userId: String?
    ): BookingAgentResult {
        val effectiveSearchId = searchId ?: "chat-${System.currentTimeMillis()}"
        
        try {
            // Get the fare price from the cached flight search
            val totalAmount = try {
                val cachedSearch = flightService.getCachedSearch(effectiveSearchId)
                val flight = cachedSearch?.flights?.find { it.flightNumber == flightNumber }
                val fare = flight?.lowestFare()
                if (fare != null) {
                    // Multiply by number of passengers
                    Money.of(fare.price.amountAsDouble * passengers.size, fare.price.currency)
                } else {
                    // Fallback to a reasonable default price
                    Money.sar(250.0 * passengers.size)
                }
            } catch (e: Exception) {
                logger.warn("Could not get fare price, using default: ${e.message}")
                Money.sar(250.0 * passengers.size)
            }
            
            val bookingRequest = BookingRequest(
                searchId = effectiveSearchId,
                flightNumber = flightNumber,
                fareFamily = FareFamilyCode.FLY,
                passengers = passengers,
                ancillaries = emptyList(),
                contactEmail = contactEmail,
                payment = PaymentDetails(
                    cardholderName = passengers.firstOrNull()?.fullName ?: "Card Holder",
                    cardNumberLast4 = "0000",
                    totalAmount = totalAmount
                )
            )
            
            val confirmation = bookingService.createBooking(bookingRequest, userId)
            
            val passengerNames = passengers.joinToString(", ") { "${it.firstName} ${it.lastName}" }
            
            logger.info("Booking created successfully: PNR=${confirmation.pnr.value}")
            
            return BookingAgentResult(
                response = "✅ Booking confirmed!\n\n" +
                    "**Booking Reference:** ${confirmation.pnr.value}\n" +
                    "**Flight:** $flightNumber\n" +
                    "**Passengers:** $passengerNames\n\n" +
                    "A confirmation email has been sent to $contactEmail. You can view this booking in 'My Bookings'.",
                uiType = ChatUiType.BOOKING_CONFIRMED,
                uiData = json.encodeToString(BookingConfirmationPayload(
                    pnr = confirmation.pnr.value,
                    flightNumber = flightNumber,
                    passengerCount = passengers.size,
                    passengerNames = passengers.map { "${it.firstName} ${it.lastName}" }
                )),
                pendingContext = PendingBookingContext(
                    bookingStep = BookingFlowStep.BOOKING_COMPLETE
                ),
                suggestions = listOf("Show my bookings", "Search another flight", "Add extras to booking")
            )
        } catch (e: Exception) {
            logger.error("Failed to create booking", e)
            return BookingAgentResult(
                response = "I'm sorry, there was an error creating your booking: ${e.message}\n\nPlease try again or contact support.",
                pendingContext = PendingBookingContext(
                    selectedFlight = flightNumber,
                    searchId = searchId,
                    bookingStep = BookingFlowStep.FLIGHT_SELECTED
                )
            )
        }
    }
    
    /**
     * Parse passenger details from natural language input.
     */
    private fun parsePassengerFromInput(input: String): ParsedPassenger? {
        try {
            // Extract name
            val nameMatch = Regex("^([A-Za-z]+)\\s+([A-Za-z\\-]+)").find(input)
            val firstName = nameMatch?.groupValues?.get(1) ?: return null
            val lastName = nameMatch?.groupValues?.get(2) ?: return null
            
            // Extract date of birth
            val dateMatch = Regex("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{4})").find(input)
                ?: Regex("(\\d{4})[/\\-](\\d{1,2})[/\\-](\\d{1,2})").find(input)
            
            val dob = if (dateMatch != null) {
                val groups = dateMatch.groupValues
                if (groups[1].length == 4) {
                    LocalDate(groups[1].toInt(), groups[2].toInt(), groups[3].toInt())
                } else {
                    LocalDate(groups[3].toInt(), groups[2].toInt(), groups[1].toInt())
                }
            } else {
                return null
            }
            
            // Extract nationality
            val nationalityMap = mapOf(
                "saudi" to "SA", "egyptian" to "EG", "emirati" to "AE", 
                "jordanian" to "JO", "kuwaiti" to "KW", "bahraini" to "BH",
                "omani" to "OM", "qatari" to "QA", "american" to "US",
                "british" to "GB", "indian" to "IN", "pakistani" to "PK"
            )
            val nationalityMatch = Regex("(${nationalityMap.keys.joinToString("|")})", RegexOption.IGNORE_CASE).find(input)
            val nationality = nationalityMatch?.value?.lowercase()?.let { nationalityMap[it] } ?: "SA"
            
            // Extract document number
            val docMatch = Regex("[A-Z]?\\d{6,12}|[A-Z]{1,2}\\d{6,9}").find(input.uppercase())
            val documentNumber = docMatch?.value ?: return null
            
            return ParsedPassenger(firstName, lastName, dob, nationality, documentNumber)
        } catch (e: Exception) {
            logger.warn("Failed to parse passenger from input: $input", e)
            return null
        }
    }
    
    private fun isBookingConfirmation(input: String): Boolean {
        val normalized = input.trim().lowercase().replace(Regex("[^a-z\\s]"), "")
        return normalized in setOf("yes", "ok", "confirm", "book", "book it", "sure", "please", "proceed", "continue") || 
               normalized.startsWith("yes") || 
               normalized.contains("book it") ||
               normalized.contains("confirm") ||
               normalized.contains("proceed")
    }
}

private data class ParsedPassenger(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val nationality: String,
    val documentNumber: String
)

@Serializable
private data class PassengerSelectPayload(
    val travelers: List<TravelerOptionDto>,
    val flightNumber: String
)

@Serializable
private data class TravelerOptionDto(
    val id: String,
    val name: String,
    val isMainTraveler: Boolean
)

@Serializable
private data class BookingConfirmationPayload(
    val pnr: String,
    val flightNumber: String,
    val passengerCount: Int,
    val passengerNames: List<String>
)
