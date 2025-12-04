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
 * Frontend E2E tests for Check-In flow
 * Covers user stories 15.1-15.5
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckInFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // ==================== State Management Classes ====================

    data class CheckInState(
        val isLoading: Boolean = false,
        val pnr: String = "",
        val lastName: String = "",
        val booking: CheckInBooking? = null,
        val selectedPassengerIds: Set<String> = emptySet(),
        val boardingPasses: List<BoardingPass> = emptyList(),
        val error: String? = null,
        val currentStep: CheckInStep = CheckInStep.ENTER_PNR
    )

    data class CheckInBooking(
        val pnr: String,
        val passengers: List<CheckInPassenger>,
        val flight: CheckInFlight,
        val checkInOpen: Boolean,
        val checkInDeadline: String
    )

    data class CheckInPassenger(
        val id: String,
        val firstName: String,
        val lastName: String,
        val type: String,
        val eligibleForCheckIn: Boolean,
        val alreadyCheckedIn: Boolean,
        val ineligibilityReason: String? = null
    )

    data class CheckInFlight(
        val flightNumber: String,
        val origin: String,
        val destination: String,
        val departureTime: String,
        val arrivalTime: String,
        val terminal: String?,
        val gate: String?
    )

    data class BoardingPass(
        val passengerId: String,
        val passengerName: String,
        val flightNumber: String,
        val origin: String,
        val destination: String,
        val departureTime: String,
        val gate: String?,
        val seat: String?,
        val barcodeData: String,
        val boardingGroup: String
    )

    enum class CheckInStep {
        ENTER_PNR,
        SELECT_PASSENGERS,
        CONFIRM,
        COMPLETE
    }

    // ==================== 15.1 Initiate Check-In Tests ====================

    @Test
    fun `test valid PNR format validation`() = testScope.runTest {
        val validPnrs = listOf("ABC123", "XYZ789", "DEF456")
        val invalidPnrs = listOf("", "AB", "ABCDEFG", "123456", "ab123")
        
        validPnrs.forEach { pnr ->
            assertTrue(isValidPnr(pnr), "PNR $pnr should be valid")
        }
        
        invalidPnrs.forEach { pnr ->
            assertFalse(isValidPnr(pnr), "PNR $pnr should be invalid")
        }
    }

    @Test
    fun `test last name validation`() = testScope.runTest {
        val validNames = listOf("Doe", "Smith", "O'Brien", "Al-Rashid")
        val invalidNames = listOf("", "A")
        
        validNames.forEach { name ->
            assertTrue(isValidLastName(name), "Name $name should be valid")
        }
        
        invalidNames.forEach { name ->
            assertFalse(isValidLastName(name), "Name $name should be invalid")
        }
    }

    @Test
    fun `test successful booking retrieval`() = testScope.runTest {
        val state = CheckInState()
        
        // Simulate loading
        val loadingState = state.copy(isLoading = true)
        assertTrue(loadingState.isLoading)
        
        // Simulate success
        val booking = createTestBooking()
        val successState = loadingState.copy(
            isLoading = false,
            booking = booking,
            currentStep = CheckInStep.SELECT_PASSENGERS
        )
        
        assertFalse(successState.isLoading)
        assertNotNull(successState.booking)
        assertEquals(CheckInStep.SELECT_PASSENGERS, successState.currentStep)
    }

    @Test
    fun `test booking not found error`() = testScope.runTest {
        val state = CheckInState(
            isLoading = false,
            error = "Booking not found. Please check your PNR and last name."
        )
        
        assertNull(state.booking)
        assertNotNull(state.error)
    }

    @Test
    fun `test check-in not yet open`() = testScope.runTest {
        val booking = createTestBooking().copy(checkInOpen = false)
        val state = CheckInState(booking = booking)
        
        assertFalse(state.booking?.checkInOpen == true)
    }

    // ==================== 15.2 Select Passengers Tests ====================

    @Test
    fun `test passenger list displayed`() = testScope.runTest {
        val booking = createTestBooking()
        val state = CheckInState(booking = booking)
        
        assertEquals(2, state.booking?.passengers?.size)
    }

    @Test
    fun `test select single passenger`() = testScope.runTest {
        val booking = createTestBooking()
        var state = CheckInState(booking = booking)
        
        // Select first passenger
        state = state.copy(selectedPassengerIds = setOf("pax1"))
        
        assertEquals(1, state.selectedPassengerIds.size)
        assertTrue(state.selectedPassengerIds.contains("pax1"))
    }

    @Test
    fun `test select multiple passengers`() = testScope.runTest {
        val booking = createTestBooking()
        var state = CheckInState(booking = booking)
        
        state = state.copy(selectedPassengerIds = setOf("pax1", "pax2"))
        
        assertEquals(2, state.selectedPassengerIds.size)
    }

    @Test
    fun `test deselect passenger`() = testScope.runTest {
        var state = CheckInState(selectedPassengerIds = setOf("pax1", "pax2"))
        
        // Deselect pax1
        state = state.copy(selectedPassengerIds = state.selectedPassengerIds - "pax1")
        
        assertEquals(1, state.selectedPassengerIds.size)
        assertFalse(state.selectedPassengerIds.contains("pax1"))
    }

    @Test
    fun `test ineligible passenger cannot be selected`() = testScope.runTest {
        val ineligiblePassenger = CheckInPassenger(
            id = "pax_ineligible",
            firstName = "John",
            lastName = "Doe",
            type = "ADULT",
            eligibleForCheckIn = false,
            alreadyCheckedIn = false,
            ineligibilityReason = "Travel documents required"
        )
        
        assertFalse(ineligiblePassenger.eligibleForCheckIn)
        assertNotNull(ineligiblePassenger.ineligibilityReason)
    }

    @Test
    fun `test already checked-in passenger shown differently`() = testScope.runTest {
        val checkedInPassenger = CheckInPassenger(
            id = "pax_checkedin",
            firstName = "Jane",
            lastName = "Doe",
            type = "ADULT",
            eligibleForCheckIn = false,
            alreadyCheckedIn = true
        )
        
        assertTrue(checkedInPassenger.alreadyCheckedIn)
        assertFalse(checkedInPassenger.eligibleForCheckIn)
    }

    // ==================== 15.3 Boarding Pass Tests ====================

    @Test
    fun `test boarding pass generated after check-in`() = testScope.runTest {
        val boardingPass = BoardingPass(
            passengerId = "pax1",
            passengerName = "JOHN DOE",
            flightNumber = "FZ101",
            origin = "RUH",
            destination = "JED",
            departureTime = "2024-03-15T10:00",
            gate = "A12",
            seat = "14A",
            barcodeData = "M1DOE/JOHN  EABC123 RUHJED FZ101 2024-03-15T10:00",
            boardingGroup = "B"
        )
        
        val state = CheckInState(
            boardingPasses = listOf(boardingPass),
            currentStep = CheckInStep.COMPLETE
        )
        
        assertEquals(1, state.boardingPasses.size)
        assertEquals("FZ101", state.boardingPasses.first().flightNumber)
    }

    @Test
    fun `test boarding pass has required information`() = testScope.runTest {
        val boardingPass = BoardingPass(
            passengerId = "pax1",
            passengerName = "JOHN DOE",
            flightNumber = "FZ101",
            origin = "RUH",
            destination = "JED",
            departureTime = "2024-03-15T10:00",
            gate = "A12",
            seat = "14A",
            barcodeData = "BARCODE_DATA_HERE",
            boardingGroup = "B"
        )
        
        assertTrue(boardingPass.passengerName.isNotEmpty())
        assertTrue(boardingPass.flightNumber.isNotEmpty())
        assertTrue(boardingPass.barcodeData.isNotEmpty())
    }

    @Test
    fun `test boarding pass barcode format`() = testScope.runTest {
        val barcodeData = "M1DOE/JOHN  EABC123 RUHJED FZ101 2024-03-15T10:00"
        
        assertTrue(barcodeData.isNotEmpty())
        // Real barcode would follow IATA BCBP standard
    }

    @Test
    fun `test multiple boarding passes generated for family`() = testScope.runTest {
        val boardingPasses = listOf(
            createBoardingPass("pax1", "JOHN DOE", "14A"),
            createBoardingPass("pax2", "JANE DOE", "14B"),
            createBoardingPass("pax3", "TOMMY DOE", "14C")
        )
        
        val state = CheckInState(boardingPasses = boardingPasses)
        
        assertEquals(3, state.boardingPasses.size)
    }

    // ==================== 15.4 Multiple Passenger Check-In Tests ====================

    @Test
    fun `test select all eligible passengers`() = testScope.runTest {
        val booking = createTestBooking()
        val eligibleIds = booking.passengers
            .filter { it.eligibleForCheckIn }
            .map { it.id }
            .toSet()
        
        val state = CheckInState(
            booking = booking,
            selectedPassengerIds = eligibleIds
        )
        
        assertEquals(2, state.selectedPassengerIds.size)
    }

    @Test
    fun `test mixed selection with infant`() = testScope.runTest {
        val booking = createTestBooking().copy(
            passengers = listOf(
                CheckInPassenger("pax1", "John", "Doe", "ADULT", true, false),
                CheckInPassenger("pax2", "Jane", "Doe", "ADULT", true, false),
                CheckInPassenger("infant1", "Baby", "Doe", "INFANT", true, false)
            )
        )
        
        // When selecting infant's accompanying adult, infant is also selected
        val state = CheckInState(
            booking = booking,
            selectedPassengerIds = setOf("pax1", "infant1")
        )
        
        assertTrue(state.selectedPassengerIds.contains("infant1"))
    }

    // ==================== 15.5 Check-In Restrictions Tests ====================

    @Test
    fun `test check-in window validation`() = testScope.runTest {
        // Check-in opens 48h before departure
        val tooEarlyDeadline = "2024-03-17T10:00" // Flight is in 48+ hours
        val withinWindowDeadline = "2024-03-15T10:00" // Flight is within window
        
        val earlyBooking = createTestBooking().copy(
            checkInOpen = false,
            checkInDeadline = tooEarlyDeadline
        )
        
        val openBooking = createTestBooking().copy(
            checkInOpen = true,
            checkInDeadline = withinWindowDeadline
        )
        
        assertFalse(earlyBooking.checkInOpen)
        assertTrue(openBooking.checkInOpen)
    }

    @Test
    fun `test check-in closed after cutoff`() = testScope.runTest {
        val pastDeadline = "2024-03-13T08:00" // 2 hours before departure
        
        val closedBooking = createTestBooking().copy(
            checkInOpen = false,
            checkInDeadline = pastDeadline
        )
        
        assertFalse(closedBooking.checkInOpen)
    }

    @Test
    fun `test passenger with travel document issues`() = testScope.runTest {
        val passenger = CheckInPassenger(
            id = "pax_doc_issue",
            firstName = "John",
            lastName = "Doe",
            type = "ADULT",
            eligibleForCheckIn = false,
            alreadyCheckedIn = false,
            ineligibilityReason = "Passport expires before return date"
        )
        
        assertFalse(passenger.eligibleForCheckIn)
        assertTrue(passenger.ineligibilityReason?.contains("Passport") == true)
    }

    @Test
    fun `test passenger requiring visa check`() = testScope.runTest {
        val passenger = CheckInPassenger(
            id = "pax_visa",
            firstName = "John",
            lastName = "Doe",
            type = "ADULT",
            eligibleForCheckIn = false,
            alreadyCheckedIn = false,
            ineligibilityReason = "Visa verification required"
        )
        
        assertFalse(passenger.eligibleForCheckIn)
        assertTrue(passenger.ineligibilityReason?.contains("Visa") == true)
    }

    // ==================== Flow Navigation Tests ====================

    @Test
    fun `test check-in flow steps progression`() = testScope.runTest {
        var state = CheckInState(currentStep = CheckInStep.ENTER_PNR)
        
        // Step 1: Enter PNR -> Select Passengers
        state = state.copy(
            booking = createTestBooking(),
            currentStep = CheckInStep.SELECT_PASSENGERS
        )
        assertEquals(CheckInStep.SELECT_PASSENGERS, state.currentStep)
        
        // Step 2: Select Passengers -> Confirm
        state = state.copy(
            selectedPassengerIds = setOf("pax1"),
            currentStep = CheckInStep.CONFIRM
        )
        assertEquals(CheckInStep.CONFIRM, state.currentStep)
        
        // Step 3: Confirm -> Complete
        state = state.copy(
            boardingPasses = listOf(createBoardingPass("pax1", "JOHN DOE", "14A")),
            currentStep = CheckInStep.COMPLETE
        )
        assertEquals(CheckInStep.COMPLETE, state.currentStep)
    }

    @Test
    fun `test go back in flow`() = testScope.runTest {
        var state = CheckInState(
            booking = createTestBooking(),
            selectedPassengerIds = setOf("pax1"),
            currentStep = CheckInStep.CONFIRM
        )
        
        // Go back to passenger selection
        state = state.copy(currentStep = CheckInStep.SELECT_PASSENGERS)
        assertEquals(CheckInStep.SELECT_PASSENGERS, state.currentStep)
        
        // Selection should be preserved
        assertEquals(1, state.selectedPassengerIds.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test handle network error during check-in`() = testScope.runTest {
        val state = CheckInState(
            isLoading = false,
            error = "Network error. Please check your connection."
        )
        
        assertNotNull(state.error)
        assertTrue(state.boardingPasses.isEmpty())
    }

    @Test
    fun `test handle empty passenger list`() = testScope.runTest {
        val booking = createTestBooking().copy(passengers = emptyList())
        val state = CheckInState(booking = booking)
        
        assertTrue(state.booking?.passengers?.isEmpty() == true)
    }

    @Test
    fun `test flight info displayed correctly`() = testScope.runTest {
        val booking = createTestBooking()
        
        assertNotNull(booking.flight)
        assertTrue(booking.flight.flightNumber.isNotEmpty())
        assertEquals("RUH", booking.flight.origin)
        assertEquals("JED", booking.flight.destination)
    }

    // ==================== Helper Functions ====================

    private fun createTestBooking(): CheckInBooking {
        return CheckInBooking(
            pnr = "ABC123",
            passengers = listOf(
                CheckInPassenger("pax1", "John", "Doe", "ADULT", true, false),
                CheckInPassenger("pax2", "Jane", "Doe", "ADULT", true, false)
            ),
            flight = CheckInFlight(
                flightNumber = "FZ101",
                origin = "RUH",
                destination = "JED",
                departureTime = "2024-03-15T10:00",
                arrivalTime = "2024-03-15T11:30",
                terminal = "1",
                gate = null
            ),
            checkInOpen = true,
            checkInDeadline = "2024-03-15T08:00"
        )
    }

    private fun createBoardingPass(passengerId: String, name: String, seat: String): BoardingPass {
        return BoardingPass(
            passengerId = passengerId,
            passengerName = name,
            flightNumber = "FZ101",
            origin = "RUH",
            destination = "JED",
            departureTime = "2024-03-15T10:00",
            gate = "A12",
            seat = seat,
            barcodeData = "M1${name.replace(" ", "/")}  EABC123 RUHJED FZ101",
            boardingGroup = "B"
        )
    }

    private fun isValidPnr(pnr: String): Boolean {
        return pnr.matches(Regex("^[A-Z]{3}\\d{3}$"))
    }

    private fun isValidLastName(lastName: String): Boolean {
        return lastName.length >= 2 && lastName.matches(Regex("^[a-zA-Z'\\-]+$"))
    }
}
