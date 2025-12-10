package com.fairair.contract.api

/**
 * Centralized definition of all API routes.
 * This object serves as the single source of truth for endpoint paths,
 * used by both the backend (controllers) and frontend (API client).
 */
object ApiRoutes {
    /**
     * API version prefix. All routes are prefixed with this.
     */
    const val API_VERSION = "v1"

    /**
     * Base path for all API endpoints.
     */
    const val BASE_PATH = "/api/$API_VERSION"

    /**
     * Authentication endpoints.
     */
    object Auth {
        /**
         * Base path for auth endpoints.
         */
        const val BASE = "$BASE_PATH/auth"

        /**
         * POST: Login with email and password.
         * Request: LoginRequestDto
         * Response: LoginResponseDto
         */
        const val LOGIN = "$BASE/login"

        /**
         * POST: Register a new user account.
         * Request: RegisterRequestDto
         * Response: RegisterResponseDto
         */
        const val REGISTER = "$BASE/register"

        /**
         * POST: Request password reset email.
         * Request: ForgotPasswordRequestDto
         * Response: ForgotPasswordResponseDto
         */
        const val FORGOT_PASSWORD = "$BASE/forgot-password"

        /**
         * POST: Reset password with token.
         * Request: ResetPasswordRequestDto
         * Response: ResetPasswordResponseDto
         */
        const val RESET_PASSWORD = "$BASE/reset-password"

        /**
         * POST: Refresh access token.
         * Request: RefreshTokenRequestDto
         * Response: LoginResponseDto
         */
        const val REFRESH = "$BASE/refresh"

        /**
         * POST: Logout current user.
         * Response: LogoutResponseDto
         */
        const val LOGOUT = "$BASE/logout"
    }

    /**
     * Configuration endpoints for static/cached data.
     */
    object Config {
        /**
         * Base path for configuration endpoints.
         */
        const val BASE = "$BASE_PATH/config"

        /**
         * GET: Retrieve the route map (origin-destination pairs).
         * Response: RouteMap
         * Cache: 24 hours
         */
        const val ROUTES = "$BASE/routes"

        /**
         * GET: Retrieve all stations (airports).
         * Response: List<Station>
         * Cache: 24 hours
         */
        const val STATIONS = "$BASE/stations"
        
        /**
         * GET: Retrieve valid destinations for a given origin.
         * Path param: {origin} - Origin airport code (e.g., "JED")
         * Response: List<Station>
         */
        const val DESTINATIONS = "$BASE/destinations"
        
        /**
         * Constructs the URL for retrieving destinations for a specific origin.
         * @param origin The origin airport code
         * @return The full API path
         */
        fun destinationsFor(origin: String): String = "$DESTINATIONS/$origin"
    }

    /**
     * Flight search endpoints.
     */
    object Search {
        /**
         * Base path for search endpoints.
         */
        const val BASE = "$BASE_PATH/search"

        /**
         * POST: Search for available flights.
         * Request: FlightSearchRequest
         * Response: FlightResponse
         * Cache: 5 minutes (shared across users for same route)
         */
        const val FLIGHTS = BASE
        
        /**
         * GET: Get lowest fare prices for a date range.
         * Query params: origin, destination, startDate, endDate, adults, children, infants
         * Response: LowFaresResponse with price per date
         * Cache: 5 minutes per date
         */
        const val LOW_FARES = "$BASE/low-fares"
        
        /**
         * Constructs the URL for fetching low fares.
         * @param origin Origin airport code
         * @param destination Destination airport code
         * @param startDate Start date (ISO format)
         * @param endDate End date (ISO format)
         * @param adults Number of adults (default 1)
         * @param children Number of children (default 0)
         * @param infants Number of infants (default 0)
         * @return The full API path with query parameters
         */
        fun lowFaresUrl(
            origin: String,
            destination: String,
            startDate: String,
            endDate: String,
            adults: Int = 1,
            children: Int = 0,
            infants: Int = 0
        ): String = "$LOW_FARES?origin=$origin&destination=$destination&startDate=$startDate&endDate=$endDate&adults=$adults&children=$children&infants=$infants"
    }

    /**
     * Booking endpoints.
     */
    object Booking {
        /**
         * Base path for booking endpoints.
         */
        const val BASE = "$BASE_PATH/booking"

        /**
         * POST: Create a new booking.
         * Request: BookingRequest
         * Response: BookingConfirmation
         */
        const val CREATE = BASE

        /**
         * GET: Retrieve booking by PNR.
         * Path param: {pnr}
         * Response: BookingConfirmation
         */
        const val BY_PNR = "$BASE/{pnr}"

        /**
         * Constructs the URL for retrieving a specific booking.
         * @param pnr The PNR code
         * @return The full API path
         */
        fun byPnr(pnr: String): String = "$BASE/$pnr"

        /**
         * GET: Retrieve all bookings for the authenticated user.
         * Response: List<BookingConfirmationDto>
         * Requires: Authorization header with Bearer token
         */
        const val USER_BOOKINGS = "$BASE/user/me"
    }

    /**
     * Ancillary services endpoints.
     */
    object Ancillaries {
        /**
         * Base path for ancillary endpoints.
         */
        const val BASE = "$BASE_PATH/ancillaries"

        /**
         * GET: Retrieve available ancillaries and pricing.
         * Query params: fareFamily, route
         * Response: List<AncillaryOption>
         */
        const val AVAILABLE = BASE
    }

    /**
     * Check-in endpoints.
     */
    object CheckIn {
        /**
         * Base path for check-in endpoints.
         */
        const val BASE = "$BASE_PATH/checkin"

        /**
         * POST: Initiate check-in with PNR and last name.
         * Request: CheckInRequestDto
         * Response: CheckInResponseDto
         */
        const val INITIATE = BASE

        /**
         * POST: Complete check-in for specific passengers.
         * Path param: {pnr}
         * Request: CompleteCheckInRequestDto
         * Response: BoardingPassDto
         */
        const val COMPLETE = "$BASE/{pnr}/complete"

        /**
         * Constructs URL for completing check-in.
         */
        fun completeFor(pnr: String): String = "$BASE/$pnr/complete"

        /**
         * GET: Get boarding pass for a checked-in passenger.
         * Path param: {pnr}, {passengerIndex}
         * Response: BoardingPassDto
         */
        const val BOARDING_PASS = "$BASE/{pnr}/boarding-pass/{passengerIndex}"

        /**
         * Constructs URL for retrieving boarding pass.
         */
        fun boardingPassFor(pnr: String, passengerIndex: Int): String =
            "$BASE/$pnr/boarding-pass/$passengerIndex"
    }

    /**
     * Manage booking endpoints.
     */
    object ManageBooking {
        /**
         * Base path for manage booking endpoints.
         */
        const val BASE = "$BASE_PATH/manage"

        /**
         * POST: Retrieve booking with PNR and last name.
         * Request: RetrieveBookingRequestDto
         * Response: ManagedBookingDto
         */
        const val RETRIEVE = BASE

        /**
         * PUT: Update passenger details.
         * Path param: {pnr}
         * Request: UpdatePassengersRequestDto
         * Response: ManagedBookingDto
         */
        const val UPDATE_PASSENGERS = "$BASE/{pnr}/passengers"

        /**
         * Constructs URL for updating passengers.
         */
        fun updatePassengersFor(pnr: String): String = "$BASE/$pnr/passengers"

        /**
         * POST: Change flight (date/time).
         * Path param: {pnr}
         * Request: ChangeFlightRequestDto
         * Response: FlightChangeQuoteDto (with fee if applicable)
         */
        const val CHANGE_FLIGHT = "$BASE/{pnr}/change"

        /**
         * Constructs URL for changing flight.
         */
        fun changeFlightFor(pnr: String): String = "$BASE/$pnr/change"

        /**
         * POST: Cancel booking.
         * Path param: {pnr}
         * Request: CancelBookingRequestDto
         * Response: CancellationConfirmationDto
         */
        const val CANCEL = "$BASE/{pnr}/cancel"

        /**
         * Constructs URL for cancellation.
         */
        fun cancelFor(pnr: String): String = "$BASE/$pnr/cancel"

        /**
         * POST: Add ancillaries to existing booking.
         * Path param: {pnr}
         * Request: AddAncillariesRequestDto
         * Response: ManagedBookingDto
         */
        const val ADD_ANCILLARIES = "$BASE/{pnr}/ancillaries"

        /**
         * Constructs URL for adding ancillaries.
         */
        fun addAncillariesFor(pnr: String): String = "$BASE/$pnr/ancillaries"
    }

    /**
     * Membership subscription endpoints.
     */
    object Membership {
        /**
         * Base path for membership endpoints.
         */
        const val BASE = "$BASE_PATH/membership"

        /**
         * GET: Get available membership plans.
         * Response: List<MembershipPlanDto>
         */
        const val PLANS = "$BASE/plans"

        /**
         * POST: Subscribe to a membership plan.
         * Request: SubscribeRequestDto
         * Response: SubscriptionDto
         */
        const val SUBSCRIBE = "$BASE/subscribe"

        /**
         * GET: Get current user's subscription status.
         * Response: SubscriptionDto or 404 if not subscribed
         */
        const val STATUS = "$BASE/status"

        /**
         * GET: Get membership usage stats for current billing period.
         * Response: MembershipUsageDto
         */
        const val USAGE = "$BASE/usage"

        /**
         * POST: Cancel subscription.
         * Response: CancellationDto
         */
        const val CANCEL = "$BASE/cancel"

        /**
         * POST: Book a flight using membership credits.
         * Request: MembershipBookingRequestDto
         * Response: BookingConfirmationDto
         */
        const val BOOK = "$BASE/book"
    }

    /**
     * Seat selection endpoints.
     */
    object Seats {
        /**
         * Base path for seat endpoints.
         */
        const val BASE = "$BASE_PATH/seats"

        /**
         * GET: Get seat map for a flight.
         * Path param: {flightNumber}
         * Query param: date
         * Response: SeatMapDto
         */
        const val MAP = "$BASE/{flightNumber}"

        /**
         * Constructs URL for seat map.
         */
        fun mapFor(flightNumber: String, date: String): String =
            "$BASE/$flightNumber?date=$date"

        /**
         * POST: Reserve seats (temporary hold during booking).
         * Request: SeatReservationRequestDto
         * Response: SeatReservationDto
         */
        const val RESERVE = "$BASE/reserve"

        /**
         * POST: Assign seats to existing booking.
         * Request: SeatAssignmentRequestDto
         * Response: SeatAssignmentDto
         */
        const val ASSIGN = "$BASE/assign"
    }

    /**
     * Meals pre-order endpoints.
     */
    object Meals {
        /**
         * Base path for meals endpoints.
         */
        const val BASE = "$BASE_PATH/meals"

        /**
         * GET: Get available meals for a route.
         * Query params: origin, destination, flightDuration
         * Response: List<MealOptionDto>
         */
        const val AVAILABLE = BASE

        /**
         * Constructs URL for available meals.
         */
        fun availableFor(origin: String, destination: String): String =
            "$BASE?origin=$origin&destination=$destination"

        /**
         * POST: Add meal to booking.
         * Request: AddMealRequestDto
         * Response: MealConfirmationDto
         */
        const val ADD = "$BASE/add"
    }

    /**
     * Content endpoints (Help Center, Destinations, etc.).
     */
    object Content {
        /**
         * Base path for content endpoints.
         */
        const val BASE = "$BASE_PATH/content"

        /**
         * GET: Get help center categories.
         * Response: List<HelpCategoryDto>
         */
        const val HELP_CATEGORIES = "$BASE/help/categories"

        /**
         * GET: Get articles in a category.
         * Path param: {categoryId}
         * Response: List<HelpArticleDto>
         */
        const val HELP_ARTICLES = "$BASE/help/categories/{categoryId}/articles"

        /**
         * Constructs URL for articles.
         */
        fun articlesFor(categoryId: String): String =
            "$BASE/help/categories/$categoryId/articles"

        /**
         * GET: Get single article.
         * Path param: {articleId}
         * Response: HelpArticleDto
         */
        const val HELP_ARTICLE = "$BASE/help/articles/{articleId}"

        /**
         * Constructs URL for single article.
         */
        fun article(articleId: String): String = "$BASE/help/articles/$articleId"

        /**
         * GET: Get destination details.
         * Path param: {code}
         * Response: DestinationDetailDto
         */
        const val DESTINATION = "$BASE/destinations/{code}"

        /**
         * Constructs URL for destination.
         */
        fun destination(code: String): String = "$BASE/destinations/$code"

        /**
         * GET: Get all destinations.
         * Response: List<DestinationSummaryDto>
         */
        const val DESTINATIONS = "$BASE/destinations"

        /**
         * POST: Subscribe to newsletter.
         * Request: NewsletterSubscribeRequestDto
         * Response: NewsletterSubscribeResponseDto
         */
        const val NEWSLETTER_SUBSCRIBE = "$BASE/newsletter/subscribe"

        /**
         * POST: Submit contact form.
         * Request: ContactFormRequestDto
         * Response: ContactFormResponseDto
         */
        const val CONTACT = "$BASE/contact"
    }

    /**
     * Health check endpoint.
     */
    object Health {
        /**
         * GET: Health check endpoint.
         * Response: {"status": "UP"}
         */
        const val CHECK = "/health"
    }

    /**
     * AI Chat endpoints for the Pilot assistant.
     */
    object Chat {
        /**
         * Base path for chat endpoints.
         */
        const val BASE = "$BASE_PATH/chat"

        /**
         * POST: Send a message to the AI assistant.
         * Request: ChatMessageRequestDto
         * Response: ChatResponseDto
         */
        const val MESSAGE = "$BASE/message"

        /**
         * GET: Retrieve chat session history.
         * Path param: {sessionId}
         * Response: ChatSessionDto
         */
        const val SESSION = "$BASE/sessions/{sessionId}"

        /**
         * Constructs URL for session retrieval.
         */
        fun session(sessionId: String): String = "$BASE/sessions/$sessionId"

        /**
         * DELETE: Clear a chat session.
         * Path param: {sessionId}
         * Response: 204 No Content
         */
        const val DELETE_SESSION = "$BASE/sessions/{sessionId}"

        /**
         * POST: Create a new chat session.
         * Response: ChatSessionDto with new sessionId
         */
        const val CREATE_SESSION = "$BASE/sessions"

        /**
         * WebSocket endpoint for streaming chat.
         * Query param: sessionId
         */
        const val STREAM = "$BASE/stream"
    }

    /**
     * User Profile endpoints - saved travelers and payment methods.
     */
    object Profile {
        /**
         * Base path for profile endpoints.
         */
        const val BASE = "$BASE_PATH/profile"

        /**
         * GET: Get current user's complete profile.
         * Response: UserProfileDto
         */
        const val GET_PROFILE = BASE

        // ============ Saved Travelers ============

        /**
         * GET: Get all saved travelers.
         * Response: List<SavedTravelerDto>
         */
        const val TRAVELERS = "$BASE/travelers"

        /**
         * POST: Add a new saved traveler.
         * Request: SaveTravelerRequest
         * Response: SavedTravelerDto
         */
        const val ADD_TRAVELER = TRAVELERS

        /**
         * GET: Get a specific saved traveler.
         * Path param: {travelerId}
         * Response: SavedTravelerDto
         */
        const val TRAVELER = "$TRAVELERS/{travelerId}"

        /**
         * Constructs URL for a specific traveler.
         */
        fun traveler(travelerId: String): String = "$TRAVELERS/$travelerId"

        /**
         * PUT: Update a saved traveler.
         * Path param: {travelerId}
         * Request: SaveTravelerRequest
         * Response: SavedTravelerDto
         */
        const val UPDATE_TRAVELER = TRAVELER

        /**
         * DELETE: Remove a saved traveler.
         * Path param: {travelerId}
         * Response: 204 No Content
         */
        const val DELETE_TRAVELER = TRAVELER

        // ============ Travel Documents ============

        /**
         * GET: Get all documents for a traveler.
         * Path param: {travelerId}
         * Response: List<TravelDocumentDto>
         */
        const val TRAVELER_DOCUMENTS = "$TRAVELERS/{travelerId}/documents"

        /**
         * Constructs URL for traveler documents.
         */
        fun travelerDocuments(travelerId: String): String = "$TRAVELERS/$travelerId/documents"

        /**
         * POST: Add a document to a traveler.
         * Path param: {travelerId}
         * Request: AddDocumentRequest
         * Response: TravelDocumentDto
         */
        const val ADD_DOCUMENT = TRAVELER_DOCUMENTS

        /**
         * DELETE: Remove a document from a traveler.
         * Path params: {travelerId}, {documentId}
         * Response: 204 No Content
         */
        const val DELETE_DOCUMENT = "$TRAVELERS/{travelerId}/documents/{documentId}"

        /**
         * Constructs URL for a specific document.
         */
        fun document(travelerId: String, documentId: String): String = 
            "$TRAVELERS/$travelerId/documents/$documentId"

        // ============ Saved Payment Methods ============

        /**
         * GET: Get all saved payment methods.
         * Response: List<SavedPaymentMethodDto>
         */
        const val PAYMENT_METHODS = "$BASE/payment-methods"

        /**
         * POST: Add a new payment method.
         * Request: SavePaymentMethodRequest
         * Response: SavedPaymentMethodDto
         */
        const val ADD_PAYMENT_METHOD = PAYMENT_METHODS

        /**
         * DELETE: Remove a payment method.
         * Path param: {paymentMethodId}
         * Response: 204 No Content
         */
        const val DELETE_PAYMENT_METHOD = "$PAYMENT_METHODS/{paymentMethodId}"

        /**
         * Constructs URL for a specific payment method.
         */
        fun paymentMethod(paymentMethodId: String): String = "$PAYMENT_METHODS/$paymentMethodId"

        /**
         * PUT: Set a payment method as default.
         * Path param: {paymentMethodId}
         * Response: SavedPaymentMethodDto
         */
        const val SET_DEFAULT_PAYMENT = "$PAYMENT_METHODS/{paymentMethodId}/default"

        /**
         * Constructs URL for setting default payment.
         */
        fun setDefaultPayment(paymentMethodId: String): String = 
            "$PAYMENT_METHODS/$paymentMethodId/default"
    }
}

/**
 * HTTP method constants for API documentation.
 */
object HttpMethods {
    const val GET = "GET"
    const val POST = "POST"
    const val PUT = "PUT"
    const val DELETE = "DELETE"
}

/**
 * Common HTTP header names used in the API.
 */
object HttpHeaders {
    const val CONTENT_TYPE = "Content-Type"
    const val ACCEPT = "Accept"
    const val ACCEPT_LANGUAGE = "Accept-Language"
    const val AUTHORIZATION = "Authorization"
    const val CACHE_CONTROL = "Cache-Control"
}

/**
 * Content type values for API requests/responses.
 */
object ContentTypes {
    const val JSON = "application/json"
    const val JSON_UTF8 = "application/json; charset=utf-8"
}

/**
 * Supported language codes for the Accept-Language header.
 */
object LanguageCodes {
    const val ENGLISH = "en"
    const val ARABIC = "ar"
}
