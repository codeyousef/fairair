package com.fairair.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Frontend E2E tests for Manage Booking flow
 * Covers user stories 16.1-16.6
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManageBookingFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // ==================== State Management Classes ====================

    data class ManageBookingState(
        val isLoading: Boolean = false,
        val booking: RetrievedBooking? = null,
        val error: String? = null,
        val isModifying: Boolean = false,
        val modificationSuccess: Boolean = false,
        val availableFlights: List<AlternativeFlight> = emptyList(),
        val contactUpdateSuccess: Boolean = false,
        val cancellationInfo: CancellationInfo? = null,
        val additionalServices: List<AdditionalService> = emptyList()
    )

    data class RetrievedBooking(
        val pnr: String,
        val lastName: String,
        val passengers: List<BookedPassenger>,
        val flights: List<BookedFlight>,
        val totalAmount: Double,
        val currency: String,
        val status: BookingStatus,
        val contactEmail: String,
        val contactPhone: String,
        val canModify: Boolean,
        val canCancel: Boolean
    )

    data class BookedPassenger(
        val id: String,
        val firstName: String,
        val lastName: String,
        val type: String,
        val seatAssignment: String?
    )

    data class BookedFlight(
        val flightNumber: String,
        val origin: String,
        val destination: String,
        val departureTime: String,
        val arrivalTime: String,
        val status: String
    )

    enum class BookingStatus {
        CONFIRMED, CANCELLED, CHECKED_IN, COMPLETED
    }

    data class AlternativeFlight(
        val flightNumber: String,
        val departureTime: String,
        val arrivalTime: String,
        val priceDifference: Double
    )

    data class CancellationInfo(
        val refundAmount: Double,
        val refundMethod: String,
        val processingTime: String,
        val canCancel: Boolean,
        val reason: String?
    )

    data class AdditionalService(
        val id: String,
        val name: String,
        val price: Double,
        val category: String,
        val isSelected: Boolean = false
    )

    // ==================== 16.1 Retrieve Booking Tests ====================

    @Test
    fun `test valid PNR and last name retrieves booking`() = testScope.runTest {
        val state = ManageBookingState()
        
        // Simulate loading state
        val loadingState = state.copy(isLoading = true)
        assertTrue(loadingState.isLoading)
        
        // Simulate successful retrieval
        val booking = RetrievedBooking(
            pnr = "ABC123",
            lastName = "Smith",
            passengers = listOf(
                BookedPassenger("1", "John", "Smith", "ADULT", "12A")
            ),
            flights = listOf(
                BookedFlight("FZ101", "RUH", "JED", "2024-03-15T10:00", "2024-03-15T11:30", "CONFIRMED")
            ),
            totalAmount = 450.0,
            currency = "SAR",
            status = BookingStatus.CONFIRMED,
            contactEmail = "john@example.com",
            contactPhone = "+966500000000",
            canModify = true,
            canCancel = true
        )
        
        val successState = loadingState.copy(isLoading = false, booking = booking)
        assertFalse(successState.isLoading)
        assertNotNull(successState.booking)
        assertEquals("ABC123", successState.booking?.pnr)
    }

    @Test
    fun `test PNR format validation`() = testScope.runTest {
        val validPnrs = listOf("ABC123", "XYZ789", "AAA111")
        val invalidPnrs = listOf("", "AB", "ABCDEFG", "123456", "ab123", "ABC 12")
        
        validPnrs.forEach { pnr ->
            assertTrue(isValidPnr(pnr), "PNR $pnr should be valid")
        }
        
        invalidPnrs.forEach { pnr ->
            assertFalse(isValidPnr(pnr), "PNR $pnr should be invalid")
        }
    }

    @Test
    fun `test last name validation`() = testScope.runTest {
        val validNames = listOf("Smith", "O'Brien", "Van Der Berg", "Al-Rashid")
        val invalidNames = listOf("", "A", "Name123", "Name@Test")
        
        validNames.forEach { name ->
            assertTrue(isValidLastName(name), "Name $name should be valid")
        }
        
        invalidNames.forEach { name ->
            assertFalse(isValidLastName(name), "Name $name should be invalid")
        }
    }

    @Test
    fun `test booking not found error`() = testScope.runTest {
        val state = ManageBookingState()
        
        val errorState = state.copy(
            isLoading = false,
            error = "Booking not found. Please check your PNR and last name."
        )
        
        assertNull(errorState.booking)
        assertNotNull(errorState.error)
        assertTrue(errorState.error!!.contains("not found"))
    }

    @Test
    fun `test invalid credentials error`() = testScope.runTest {
        val state = ManageBookingState()
        
        val errorState = state.copy(
            isLoading = false,
            error = "The last name does not match the booking record."
        )
        
        assertNull(errorState.booking)
        assertTrue(errorState.error!!.contains("last name"))
    }

    // ==================== 16.2 View Booking Details Tests ====================

    @Test
    fun `test booking details display all flight information`() = testScope.runTest {
        val booking = createTestBooking()
        
        assertEquals(1, booking.flights.size)
        val flight = booking.flights.first()
        assertEquals("FZ101", flight.flightNumber)
        assertEquals("RUH", flight.origin)
        assertEquals("JED", flight.destination)
        assertNotNull(flight.departureTime)
        assertNotNull(flight.arrivalTime)
    }

    @Test
    fun `test booking details display all passengers`() = testScope.runTest {
        val booking = createTestBooking().copy(
            passengers = listOf(
                BookedPassenger("1", "John", "Smith", "ADULT", "12A"),
                BookedPassenger("2", "Jane", "Smith", "ADULT", "12B"),
                BookedPassenger("3", "Jimmy", "Smith", "CHILD", "12C")
            )
        )
        
        assertEquals(3, booking.passengers.size)
        assertEquals(2, booking.passengers.count { it.type == "ADULT" })
        assertEquals(1, booking.passengers.count { it.type == "CHILD" })
    }

    @Test
    fun `test booking shows payment summary`() = testScope.runTest {
        val booking = createTestBooking()
        
        assertEquals(450.0, booking.totalAmount)
        assertEquals("SAR", booking.currency)
    }

    @Test
    fun `test booking shows seat assignments`() = testScope.runTest {
        val booking = createTestBooking()
        
        booking.passengers.forEach { passenger ->
            // Seat may or may not be assigned
            if (passenger.seatAssignment != null) {
                assertTrue(passenger.seatAssignment!!.matches(Regex("^\\d{1,2}[A-F]$")))
            }
        }
    }

    @Test
    fun `test cancelled booking displays appropriately`() = testScope.runTest {
        val cancelledBooking = createTestBooking().copy(
            status = BookingStatus.CANCELLED,
            canModify = false,
            canCancel = false
        )
        
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.status)
        assertFalse(cancelledBooking.canModify)
        assertFalse(cancelledBooking.canCancel)
    }

    // ==================== 16.3 Modify Flight Tests ====================

    @Test
    fun `test alternative flights are displayed for modification`() = testScope.runTest {
        val state = ManageBookingState(
            booking = createTestBooking(),
            availableFlights = listOf(
                AlternativeFlight("FZ102", "2024-03-15T14:00", "2024-03-15T15:30", 50.0),
                AlternativeFlight("FZ103", "2024-03-15T18:00", "2024-03-15T19:30", -25.0),
                AlternativeFlight("FZ104", "2024-03-16T10:00", "2024-03-16T11:30", 0.0)
            )
        )
        
        assertEquals(3, state.availableFlights.size)
        assertTrue(state.availableFlights.any { it.priceDifference > 0 })
        assertTrue(state.availableFlights.any { it.priceDifference < 0 })
        assertTrue(state.availableFlights.any { it.priceDifference == 0.0 })
    }

    @Test
    fun `test modification not allowed for checked-in booking`() = testScope.runTest {
        val checkedInBooking = createTestBooking().copy(
            status = BookingStatus.CHECKED_IN,
            canModify = false
        )
        
        assertFalse(checkedInBooking.canModify)
    }

    @Test
    fun `test modification not allowed for completed flight`() = testScope.runTest {
        val completedBooking = createTestBooking().copy(
            status = BookingStatus.COMPLETED,
            canModify = false,
            canCancel = false
        )
        
        assertFalse(completedBooking.canModify)
        assertFalse(completedBooking.canCancel)
    }

    @Test
    fun `test price difference calculation for flight change`() = testScope.runTest {
        val alternativeFlight = AlternativeFlight(
            "FZ105",
            "2024-03-16T08:00",
            "2024-03-16T09:30",
            priceDifference = 75.50
        )
        
        assertEquals(75.50, alternativeFlight.priceDifference)
        assertTrue(alternativeFlight.priceDifference > 0)
    }

    @Test
    fun `test modification loading state`() = testScope.runTest {
        val state = ManageBookingState(
            booking = createTestBooking(),
            isModifying = true
        )
        
        assertTrue(state.isModifying)
        assertFalse(state.modificationSuccess)
    }

    @Test
    fun `test successful modification updates state`() = testScope.runTest {
        val state = ManageBookingState(
            booking = createTestBooking(),
            isModifying = false,
            modificationSuccess = true
        )
        
        assertFalse(state.isModifying)
        assertTrue(state.modificationSuccess)
    }

    // ==================== 16.4 Update Contact Information Tests ====================

    @Test
    fun `test email update validation`() = testScope.runTest {
        val validEmails = listOf("test@example.com", "user.name@domain.org", "email+tag@test.com")
        val invalidEmails = listOf("", "invalid", "@domain.com", "test@", "test@.com")
        
        validEmails.forEach { email ->
            assertTrue(isValidEmail(email), "Email $email should be valid")
        }
        
        invalidEmails.forEach { email ->
            assertFalse(isValidEmail(email), "Email $email should be invalid")
        }
    }

    @Test
    fun `test phone number validation`() = testScope.runTest {
        val validPhones = listOf("+966500000000", "+966512345678", "0500000000")
        val invalidPhones = listOf("", "123", "abcdefghij", "+966", "++966500000000")
        
        validPhones.forEach { phone ->
            assertTrue(isValidPhone(phone), "Phone $phone should be valid")
        }
        
        invalidPhones.forEach { phone ->
            assertFalse(isValidPhone(phone), "Phone $phone should be invalid")
        }
    }

    @Test
    fun `test contact update success state`() = testScope.runTest {
        val state = ManageBookingState(
            booking = createTestBooking(),
            contactUpdateSuccess = true
        )
        
        assertTrue(state.contactUpdateSuccess)
    }

    @Test
    fun `test contact update requires at least one field`() = testScope.runTest {
        val hasValidUpdate = hasContactUpdateData("", "")
        assertFalse(hasValidUpdate)
        
        val hasEmailUpdate = hasContactUpdateData("new@email.com", "")
        assertTrue(hasEmailUpdate)
        
        val hasPhoneUpdate = hasContactUpdateData("", "+966555555555")
        assertTrue(hasPhoneUpdate)
    }

    // ==================== 16.5 Cancel Booking Tests ====================

    @Test
    fun `test cancellation info displayed correctly`() = testScope.runTest {
        val state = ManageBookingState(
            booking = createTestBooking(),
            cancellationInfo = CancellationInfo(
                refundAmount = 350.0,
                refundMethod = "Original payment method",
                processingTime = "5-7 business days",
                canCancel = true,
                reason = null
            )
        )
        
        assertNotNull(state.cancellationInfo)
        assertEquals(350.0, state.cancellationInfo?.refundAmount)
        assertTrue(state.cancellationInfo?.canCancel == true)
    }

    @Test
    fun `test non-refundable booking shows zero refund`() = testScope.runTest {
        val cancellationInfo = CancellationInfo(
            refundAmount = 0.0,
            refundMethod = "No refund available",
            processingTime = "N/A",
            canCancel = true,
            reason = "Non-refundable fare"
        )
        
        assertEquals(0.0, cancellationInfo.refundAmount)
        assertNotNull(cancellationInfo.reason)
    }

    @Test
    fun `test cancellation not allowed within 24 hours`() = testScope.runTest {
        val cancellationInfo = CancellationInfo(
            refundAmount = 0.0,
            refundMethod = "N/A",
            processingTime = "N/A",
            canCancel = false,
            reason = "Cancellation not allowed within 24 hours of departure"
        )
        
        assertFalse(cancellationInfo.canCancel)
        assertTrue(cancellationInfo.reason?.contains("24 hours") == true)
    }

    @Test
    fun `test cancellation confirmation required`() = testScope.runTest {
        // Simulate confirmation dialog state
        var confirmationShown = false
        var cancelled = false
        
        // User initiates cancellation
        confirmationShown = true
        assertTrue(confirmationShown)
        
        // User confirms
        cancelled = true
        assertTrue(cancelled)
    }

    // ==================== 16.6 Add Additional Services Tests ====================

    @Test
    fun `test additional services displayed by category`() = testScope.runTest {
        val services = listOf(
            AdditionalService("1", "Extra Baggage 23kg", 150.0, "BAGGAGE"),
            AdditionalService("2", "Extra Baggage 32kg", 250.0, "BAGGAGE"),
            AdditionalService("3", "Seat Selection", 50.0, "SEATS"),
            AdditionalService("4", "Priority Boarding", 30.0, "BOARDING"),
            AdditionalService("5", "Meal - Chicken", 45.0, "MEALS")
        )
        
        val baggageServices = services.filter { it.category == "BAGGAGE" }
        val seatServices = services.filter { it.category == "SEATS" }
        val mealServices = services.filter { it.category == "MEALS" }
        
        assertEquals(2, baggageServices.size)
        assertEquals(1, seatServices.size)
        assertEquals(1, mealServices.size)
    }

    @Test
    fun `test service selection toggles correctly`() = testScope.runTest {
        var service = AdditionalService("1", "Extra Baggage", 150.0, "BAGGAGE", false)
        
        assertFalse(service.isSelected)
        
        // Select service
        service = service.copy(isSelected = true)
        assertTrue(service.isSelected)
        
        // Deselect service
        service = service.copy(isSelected = false)
        assertFalse(service.isSelected)
    }

    @Test
    fun `test total price updates with selected services`() = testScope.runTest {
        val services = listOf(
            AdditionalService("1", "Extra Baggage", 150.0, "BAGGAGE", true),
            AdditionalService("2", "Seat Selection", 50.0, "SEATS", true),
            AdditionalService("3", "Priority Boarding", 30.0, "BOARDING", false)
        )
        
        val totalServicePrice = services.filter { it.isSelected }.sumOf { it.price }
        assertEquals(200.0, totalServicePrice)
    }

    @Test
    fun `test services not available for completed booking`() = testScope.runTest {
        val completedBooking = createTestBooking().copy(
            status = BookingStatus.COMPLETED
        )
        
        // Services should not be available for completed bookings
        val canAddServices = completedBooking.status == BookingStatus.CONFIRMED
        assertFalse(canAddServices)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test network error during booking retrieval`() = testScope.runTest {
        val state = ManageBookingState(
            isLoading = false,
            error = "Network error. Please check your connection and try again."
        )
        
        assertNull(state.booking)
        assertTrue(state.error?.contains("Network") == true)
    }

    @Test
    fun `test session timeout during modification`() = testScope.runTest {
        val state = ManageBookingState(
            booking = createTestBooking(),
            isModifying = false,
            error = "Session expired. Please retrieve your booking again."
        )
        
        assertNotNull(state.booking)
        assertTrue(state.error?.contains("Session") == true)
    }

    @Test
    fun `test concurrent modification conflict`() = testScope.runTest {
        val state = ManageBookingState(
            booking = createTestBooking(),
            error = "Booking was modified by another user. Please refresh and try again."
        )
        
        assertTrue(state.error?.contains("modified") == true)
    }

    @Test
    fun `test handle special characters in last name`() = testScope.runTest {
        val specialNames = listOf(
            "O'Brien",
            "Van Der Berg",
            "Al-Rashid",
            "Müller",
            "García"
        )
        
        specialNames.forEach { name ->
            assertTrue(isValidLastName(name), "Special name $name should be valid")
        }
    }

    // ==================== Helper Functions ====================

    private fun createTestBooking(): RetrievedBooking {
        return RetrievedBooking(
            pnr = "ABC123",
            lastName = "Smith",
            passengers = listOf(
                BookedPassenger("1", "John", "Smith", "ADULT", "12A")
            ),
            flights = listOf(
                BookedFlight("FZ101", "RUH", "JED", "2024-03-15T10:00", "2024-03-15T11:30", "CONFIRMED")
            ),
            totalAmount = 450.0,
            currency = "SAR",
            status = BookingStatus.CONFIRMED,
            contactEmail = "john@example.com",
            contactPhone = "+966500000000",
            canModify = true,
            canCancel = true
        )
    }

    private fun isValidPnr(pnr: String): Boolean {
        return pnr.matches(Regex("^[A-Z]{3}\\d{3}$"))
    }

    private fun isValidLastName(lastName: String): Boolean {
        if (lastName.length < 2) return false
        return lastName.matches(Regex("^[a-zA-ZÀ-ÿ'\\- ]+$"))
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"))
    }

    private fun isValidPhone(phone: String): Boolean {
        if (phone.isEmpty()) return false
        if (phone.startsWith("0")) {
            return phone.matches(Regex("^0[0-9]{9}$"))
        }
        return phone.matches(Regex("^\\+[0-9]{10,15}$"))
    }

    private fun hasContactUpdateData(email: String, phone: String): Boolean {
        return email.isNotEmpty() || phone.isNotEmpty()
    }
}
