package com.fairair

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.http.MediaType
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.springframework.boot.test.mock.mockito.MockBean
import com.fairair.ai.GenAiProvider
import com.fairair.ai.AiChatResponse
import com.fairair.ai.booking.executor.LocalModelExecutor
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking

/**
 * Base class for E2E tests providing common test infrastructure.
 * 
 * Tests cover all user stories from docs/private/user-stories.md including:
 * - Flight Search & Discovery (1.1-1.6)
 * - Flight Results & Fare Selection (2.1-2.5)
 * - Passenger Information (3.1-3.4)
 * - Ancillary Services (4.1-4.4)
 * - Payment (5.1-5.5)
 * - Booking Confirmation (6.1-6.3)
 * - Saved Bookings & Offline (7.1-7.3)
 * - Guest Checkout & Auth (12.1-12.3)
 * - Error Handling (11.1-11.4)
 * - App Initialization (13.1-13.3)
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [FairairApplication::class]
)
@ActiveProfiles("test")
abstract class E2ETestBase {

    @MockBean
    protected lateinit var genAiProvider: GenAiProvider

    @MockBean
    protected lateinit var localModelExecutor: LocalModelExecutor

    @BeforeEach
    fun setupMocks() {
        runBlocking {
            val defaultJson = """
                {
                    "origin": "JED", 
                    "destination": "RUH", 
                    "date": "2025-12-25", 
                    "passengers": 1
                }
            """.trimIndent()
            
            // Stub generic responses to prevent NPEs
            whenever(genAiProvider.chat(any(), any(), anyOrNull())).thenReturn(
                AiChatResponse(text = defaultJson)
            )
            whenever(localModelExecutor.generate(any())).thenReturn(defaultJson)
        }
    }

    @LocalServerPort
    protected var port: Int = 0

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    protected val webClient: WebTestClient by lazy {
        WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:$port")
            .responseTimeout(Duration.ofSeconds(30))
            .build()
    }

    // Test data constants
    protected companion object {
        // Valid airports from the mock route network
        const val ORIGIN_JED = "JED"
        const val ORIGIN_RUH = "RUH"
        const val ORIGIN_DMM = "DMM"
        const val DESTINATION_RUH = "RUH"
        const val DESTINATION_JED = "JED"
        const val DESTINATION_DXB = "DXB"
        const val INVALID_ORIGIN = "XXX"
        const val INVALID_DESTINATION = "YYY"

        // Demo user credentials (from data.sql)
        const val DEMO_USER_EMAIL = "jane@test.com"
        const val DEMO_USER_PASSWORD = "password"
        const val DEMO_EMPLOYEE_EMAIL = "employee@fairair.com"
        const val DEMO_ADMIN_EMAIL = "admin@test.com"
        const val INVALID_EMAIL = "invalid-email"
        const val INVALID_PASSWORD = "wrong-password"

        // Test passenger data
        val VALID_ADULT_PASSENGER = mapOf(
            "type" to "ADULT",
            "title" to "MR",
            "firstName" to "John",
            "lastName" to "Doe",
            "nationality" to "SA",
            "dateOfBirth" to "1990-01-15",
            "documentId" to "ABC12345678"
        )

        val VALID_CHILD_PASSENGER = mapOf(
            "type" to "CHILD",
            "title" to "MSTR",
            "firstName" to "Tommy",
            "lastName" to "Doe",
            "nationality" to "SA",
            "dateOfBirth" to "2018-06-20",
            "documentId" to "DEF87654321"
        )

        val VALID_INFANT_PASSENGER = mapOf(
            "type" to "INFANT",
            "title" to "MSTR",
            "firstName" to "Baby",
            "lastName" to "Doe",
            "nationality" to "SA",
            "dateOfBirth" to "2024-01-10",
            "documentId" to "GHI11111111"
        )

        // Valid payment data
        val VALID_PAYMENT = mapOf(
            "cardholderName" to "John Doe",
            "cardNumberLast4" to "4242",
            "totalAmountMinor" to 35000L,
            "currency" to "SAR"
        )

        // Get a future date for flight search
        fun getFutureDateString(daysFromNow: Int = 7): String {
            return LocalDate.now().plusDays(daysFromNow.toLong())
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    // Helper methods for common operations
    protected fun parseResponseAsTree(body: String) = objectMapper.readTree(body)

    protected fun createSearchRequest(
        origin: String = ORIGIN_JED,
        destination: String = DESTINATION_RUH,
        departureDate: String = getFutureDateString(),
        adults: Int = 1,
        children: Int = 0,
        infants: Int = 0
    ): Map<String, Any> {
        return mapOf(
            "origin" to origin,
            "destination" to destination,
            "departureDate" to departureDate,
            "passengers" to mapOf(
                "adults" to adults,
                "children" to children,
                "infants" to infants
            )
        )
    }

    protected fun createBookingRequest(
        searchId: String,
        flightNumber: String,
        fareFamily: String = "FLY",
        passengers: List<Map<String, Any>> = listOf(VALID_ADULT_PASSENGER),
        ancillaries: List<Map<String, Any>> = emptyList(),
        contactEmail: String = "test@example.com",
        payment: Map<String, Any> = VALID_PAYMENT
    ): Map<String, Any> {
        return mapOf(
            "searchId" to searchId,
            "flightNumber" to flightNumber,
            "fareFamily" to fareFamily,
            "passengers" to passengers,
            "ancillaries" to ancillaries,
            "contactEmail" to contactEmail,
            "payment" to payment
        )
    }

    protected fun createLoginRequest(
        email: String = DEMO_USER_EMAIL,
        password: String = DEMO_USER_PASSWORD
    ): Map<String, String> {
        return mapOf(
            "email" to email,
            "password" to password
        )
    }

    /**
     * Performs a flight search and returns the response.
     */
    protected fun searchFlights(
        origin: String = ORIGIN_JED,
        destination: String = DESTINATION_RUH,
        adults: Int = 1,
        children: Int = 0,
        infants: Int = 0
    ): WebTestClient.ResponseSpec {
        val request = createSearchRequest(origin, destination, getFutureDateString(), adults, children, infants)
        return webClient.post()
            .uri("/api/v1/search")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(request))
            .exchange()
    }

    /**
     * Performs login and returns the access token.
     */
    protected fun login(
        email: String = DEMO_USER_EMAIL,
        password: String = DEMO_USER_PASSWORD
    ): String? {
        val response = webClient.post()
            .uri("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(createLoginRequest(email, password)))
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody

        return response?.let {
            val parsed = objectMapper.readTree(it)
            parsed.get("accessToken")?.asText()
        }
    }

    /**
     * Creates a complete booking with valid data and returns the PNR.
     */
    protected fun createValidBooking(): Pair<String, String> {
        // First, search for flights
        val searchResponse = searchFlights()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        val searchParsed = objectMapper.readTree(searchResponse)
        val searchId = searchParsed.get("searchId").asText()
        val flightNumber = searchParsed.get("flights").get(0).get("flightNumber").asText()

        // Create booking
        val bookingRequest = createBookingRequest(searchId, flightNumber)
        val bookingResponse = webClient.post()
            .uri("/api/v1/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(bookingRequest))
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        val bookingParsed = objectMapper.readTree(bookingResponse)
        val pnr = bookingParsed.get("pnr").asText()
        
        return Pair(pnr, searchId)
    }
}
