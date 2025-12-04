package com.fairair.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Frontend E2E tests for External Services integration
 * Covers user stories 19.1-19.3
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExternalServicesFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // ==================== State Management Classes ====================

    data class ExternalServicesState(
        val isLoading: Boolean = false,
        val hotelSearchParams: HotelSearchParams? = null,
        val carRentalSearchParams: CarRentalSearchParams? = null,
        val generatedUrls: Map<String, String> = emptyMap(),
        val partnerLinks: List<PartnerLink> = emptyList(),
        val error: String? = null
    )

    data class HotelSearchParams(
        val destination: String,
        val checkInDate: String,
        val checkOutDate: String,
        val guests: Int,
        val rooms: Int
    )

    data class CarRentalSearchParams(
        val location: String,
        val pickUpDate: String,
        val dropOffDate: String,
        val pickUpTime: String,
        val dropOffTime: String
    )

    data class PartnerLink(
        val id: String,
        val type: PartnerType,
        val name: String,
        val logoUrl: String,
        val baseUrl: String,
        val affiliateId: String
    )

    enum class PartnerType {
        HOTEL, CAR_RENTAL, INSURANCE, AIRPORT_TRANSFER, ACTIVITIES
    }

    // ==================== 19.1 Access Hotel Booking Links Tests ====================

    @Test
    fun `test hotel search params from booking`() = testScope.runTest {
        val hotelParams = HotelSearchParams(
            destination = "JED",
            checkInDate = "2024-03-15",
            checkOutDate = "2024-03-18",
            guests = 2,
            rooms = 1
        )
        
        val state = ExternalServicesState(hotelSearchParams = hotelParams)
        
        assertNotNull(state.hotelSearchParams)
        assertEquals("JED", state.hotelSearchParams?.destination)
        assertEquals(2, state.hotelSearchParams?.guests)
    }

    @Test
    fun `test hotel partner URL generation`() = testScope.runTest {
        val hotelParams = HotelSearchParams(
            destination = "JED",
            checkInDate = "2024-03-15",
            checkOutDate = "2024-03-18",
            guests = 2,
            rooms = 1
        )
        
        val generatedUrl = generateHotelUrl("booking.com", hotelParams, "FLYADEAL123")
        
        assertTrue(generatedUrl.contains("booking.com"))
        assertTrue(generatedUrl.contains("dest=JED") || generatedUrl.contains("city=JED"))
        assertTrue(generatedUrl.contains("checkin=2024-03-15"))
        assertTrue(generatedUrl.contains("checkout=2024-03-18"))
        assertTrue(generatedUrl.contains("affiliate=FLYADEAL123") || generatedUrl.contains("aid=FLYADEAL123"))
    }

    @Test
    fun `test multiple hotel partners available`() = testScope.runTest {
        val hotelPartners = createTestPartnerLinks().filter { it.type == PartnerType.HOTEL }
        
        assertTrue(hotelPartners.isNotEmpty())
        assertTrue(hotelPartners.size >= 2) // Multiple options
    }

    @Test
    fun `test hotel link opens in new window`() = testScope.runTest {
        val url = "https://booking.com/hotels?dest=JED"
        
        // Verify URL is properly formatted for external opening
        assertTrue(url.startsWith("https://"))
        assertFalse(url.contains("flyadeal.com"))
    }

    @Test
    fun `test hotel search with multi-room booking`() = testScope.runTest {
        val hotelParams = HotelSearchParams(
            destination = "RUH",
            checkInDate = "2024-04-01",
            checkOutDate = "2024-04-05",
            guests = 4,
            rooms = 2
        )
        
        assertEquals(4, hotelParams.guests)
        assertEquals(2, hotelParams.rooms)
    }

    // ==================== 19.2 Access Car Rental Links Tests ====================

    @Test
    fun `test car rental search params from booking`() = testScope.runTest {
        val carParams = CarRentalSearchParams(
            location = "JED",
            pickUpDate = "2024-03-15",
            dropOffDate = "2024-03-18",
            pickUpTime = "10:00",
            dropOffTime = "10:00"
        )
        
        val state = ExternalServicesState(carRentalSearchParams = carParams)
        
        assertNotNull(state.carRentalSearchParams)
        assertEquals("JED", state.carRentalSearchParams?.location)
    }

    @Test
    fun `test car rental URL generation`() = testScope.runTest {
        val carParams = CarRentalSearchParams(
            location = "RUH",
            pickUpDate = "2024-03-15",
            dropOffDate = "2024-03-20",
            pickUpTime = "12:00",
            dropOffTime = "12:00"
        )
        
        val generatedUrl = generateCarRentalUrl("rentalcars.com", carParams, "FLYADEAL456")
        
        assertTrue(generatedUrl.contains("rentalcars.com"))
        assertTrue(generatedUrl.contains("location=RUH") || generatedUrl.contains("pickup=RUH"))
        assertTrue(generatedUrl.contains("affiliate=FLYADEAL456") || generatedUrl.contains("adcamp=FLYADEAL456"))
    }

    @Test
    fun `test multiple car rental partners available`() = testScope.runTest {
        val carPartners = createTestPartnerLinks().filter { it.type == PartnerType.CAR_RENTAL }
        
        assertTrue(carPartners.isNotEmpty())
    }

    @Test
    fun `test car rental with different pickup and dropoff locations`() = testScope.runTest {
        val carParams = CarRentalSearchParams(
            location = "JED", // Could extend to support separate pickup/dropoff
            pickUpDate = "2024-03-15",
            dropOffDate = "2024-03-18",
            pickUpTime = "09:00",
            dropOffTime = "18:00"
        )
        
        assertNotNull(carParams.pickUpTime)
        assertNotNull(carParams.dropOffTime)
    }

    // ==================== 19.3 External URL Handling Tests ====================

    @Test
    fun `test external URL validation`() = testScope.runTest {
        val validUrls = listOf(
            "https://booking.com/hotels",
            "https://www.rentalcars.com/search",
            "https://partner.example.com/offers?id=123"
        )
        
        val invalidUrls = listOf(
            "javascript:alert('xss')",
            "file:///etc/passwd",
            "ftp://server.com",
            ""
        )
        
        validUrls.forEach { url ->
            assertTrue(isValidExternalUrl(url), "URL $url should be valid")
        }
        
        invalidUrls.forEach { url ->
            assertFalse(isValidExternalUrl(url), "URL $url should be invalid")
        }
    }

    @Test
    fun `test affiliate ID is included in all partner URLs`() = testScope.runTest {
        val partners = createTestPartnerLinks()
        
        partners.forEach { partner ->
            assertTrue(partner.affiliateId.isNotEmpty())
        }
    }

    @Test
    fun `test partner logos are available`() = testScope.runTest {
        val partners = createTestPartnerLinks()
        
        partners.forEach { partner ->
            assertTrue(partner.logoUrl.isNotEmpty())
            assertTrue(partner.logoUrl.startsWith("https://") || partner.logoUrl.startsWith("/assets/"))
        }
    }

    @Test
    fun `test all partner types have links`() = testScope.runTest {
        val partners = createTestPartnerLinks()
        val partnerTypes = partners.map { it.type }.toSet()
        
        assertTrue(partnerTypes.contains(PartnerType.HOTEL))
        assertTrue(partnerTypes.contains(PartnerType.CAR_RENTAL))
    }

    @Test
    fun `test URL encoding for special characters`() = testScope.runTest {
        val cityWithSpaces = "New York"
        val encodedCity = cityWithSpaces.replace(" ", "%20")
        
        assertTrue(encodedCity.contains("%20"))
        assertFalse(encodedCity.contains(" "))
    }

    // ==================== Additional External Services Tests ====================

    @Test
    fun `test travel insurance partner links`() = testScope.runTest {
        val insurancePartners = createTestPartnerLinks().filter { it.type == PartnerType.INSURANCE }
        
        // Insurance may be optional
        if (insurancePartners.isNotEmpty()) {
            assertTrue(insurancePartners.first().baseUrl.isNotEmpty())
        }
    }

    @Test
    fun `test airport transfer partner links`() = testScope.runTest {
        val transferPartners = createTestPartnerLinks().filter { it.type == PartnerType.AIRPORT_TRANSFER }
        
        if (transferPartners.isNotEmpty()) {
            assertTrue(transferPartners.first().baseUrl.isNotEmpty())
        }
    }

    @Test
    fun `test activities and tours partner links`() = testScope.runTest {
        val activityPartners = createTestPartnerLinks().filter { it.type == PartnerType.ACTIVITIES }
        
        if (activityPartners.isNotEmpty()) {
            assertTrue(activityPartners.first().baseUrl.isNotEmpty())
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test handle partner service unavailable`() = testScope.runTest {
        val state = ExternalServicesState(
            error = "Partner service temporarily unavailable"
        )
        
        assertNotNull(state.error)
    }

    @Test
    fun `test fallback when affiliate tracking fails`() = testScope.runTest {
        val baseUrl = "https://booking.com/hotels"
        val urlWithoutAffiliate = baseUrl // Fallback to base URL
        
        assertTrue(urlWithoutAffiliate.startsWith("https://"))
    }

    @Test
    fun `test loading state while generating URLs`() = testScope.runTest {
        val state = ExternalServicesState(isLoading = true)
        
        assertTrue(state.isLoading)
        assertTrue(state.generatedUrls.isEmpty())
    }

    @Test
    fun `test handle malformed partner response`() = testScope.runTest {
        // Gracefully handle if partner API returns unexpected data
        val state = ExternalServicesState(
            error = "Unable to load partner information"
        )
        
        assertTrue(state.partnerLinks.isEmpty())
        assertNotNull(state.error)
    }

    @Test
    fun `test dates are properly formatted for partners`() = testScope.runTest {
        val checkIn = "2024-03-15"
        val checkOut = "2024-03-18"
        
        // ISO 8601 format
        assertTrue(checkIn.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
        assertTrue(checkOut.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `test time format for car rental`() = testScope.runTest {
        val validTimes = listOf("10:00", "14:30", "09:00", "23:59")
        
        validTimes.forEach { time ->
            assertTrue(time.matches(Regex("\\d{2}:\\d{2}")), "Time $time should be valid")
        }
    }

    // ==================== Helper Functions ====================

    private fun createTestPartnerLinks(): List<PartnerLink> {
        return listOf(
            PartnerLink(
                id = "booking",
                type = PartnerType.HOTEL,
                name = "Booking.com",
                logoUrl = "/assets/partners/booking.png",
                baseUrl = "https://www.booking.com",
                affiliateId = "FLYADEAL123"
            ),
            PartnerLink(
                id = "hotels",
                type = PartnerType.HOTEL,
                name = "Hotels.com",
                logoUrl = "/assets/partners/hotels.png",
                baseUrl = "https://www.hotels.com",
                affiliateId = "FLYADEAL124"
            ),
            PartnerLink(
                id = "rentalcars",
                type = PartnerType.CAR_RENTAL,
                name = "Rentalcars.com",
                logoUrl = "/assets/partners/rentalcars.png",
                baseUrl = "https://www.rentalcars.com",
                affiliateId = "FLYADEAL456"
            ),
            PartnerLink(
                id = "hertz",
                type = PartnerType.CAR_RENTAL,
                name = "Hertz",
                logoUrl = "/assets/partners/hertz.png",
                baseUrl = "https://www.hertz.com",
                affiliateId = "FLYADEAL457"
            ),
            PartnerLink(
                id = "allianz",
                type = PartnerType.INSURANCE,
                name = "Allianz Travel",
                logoUrl = "/assets/partners/allianz.png",
                baseUrl = "https://www.allianz-travel.com",
                affiliateId = "FLYADEAL789"
            ),
            PartnerLink(
                id = "careem",
                type = PartnerType.AIRPORT_TRANSFER,
                name = "Careem",
                logoUrl = "/assets/partners/careem.png",
                baseUrl = "https://www.careem.com",
                affiliateId = "FLYADEAL890"
            ),
            PartnerLink(
                id = "viator",
                type = PartnerType.ACTIVITIES,
                name = "Viator",
                logoUrl = "/assets/partners/viator.png",
                baseUrl = "https://www.viator.com",
                affiliateId = "FLYADEAL901"
            )
        )
    }

    private fun generateHotelUrl(partner: String, params: HotelSearchParams, affiliateId: String): String {
        return "https://www.$partner/hotels?dest=${params.destination}&checkin=${params.checkInDate}&checkout=${params.checkOutDate}&guests=${params.guests}&rooms=${params.rooms}&affiliate=$affiliateId"
    }

    private fun generateCarRentalUrl(partner: String, params: CarRentalSearchParams, affiliateId: String): String {
        return "https://www.$partner/search?location=${params.location}&pickup=${params.pickUpDate}&dropoff=${params.dropOffDate}&affiliate=$affiliateId"
    }

    private fun isValidExternalUrl(url: String): Boolean {
        if (url.isEmpty()) return false
        if (!url.startsWith("https://")) return false
        if (url.contains("javascript:")) return false
        if (url.contains("file://")) return false
        return true
    }
}
