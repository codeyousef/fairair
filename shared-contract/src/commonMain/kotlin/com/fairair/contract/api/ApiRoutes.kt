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
         * Cache: 5 minutes
         */
        const val FLIGHTS = BASE
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
     * Health check endpoint.
     */
    object Health {
        /**
         * GET: Health check endpoint.
         * Response: {"status": "UP"}
         */
        const val CHECK = "/health"
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
