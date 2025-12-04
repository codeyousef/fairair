package com.fairair.service

import com.fairair.contract.model.*
import com.fairair.controller.BookingNotFoundException
import com.fairair.controller.CheckInNotAllowedException
import com.fairair.repository.BookingRepository
import com.fairair.repository.CheckInRepository
import kotlinx.datetime.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.random.Random

/**
 * Service for check-in operations.
 * Handles eligibility verification, check-in completion, and boarding pass generation.
 * 
 * Production Notes:
 * - Replace mock implementations with Navitaire DCS API calls
 * - Implement proper seat assignment logic with inventory management
 * - Add QR code generation for boarding passes
 */
@Service
class CheckInService(
    private val bookingRepository: BookingRepository,
    private val checkInRepository: CheckInRepository
) {
    private val log = LoggerFactory.getLogger(CheckInService::class.java)

    companion object {
        // Check-in opens 48 hours before departure
        const val CHECK_IN_OPENS_HOURS = 48L
        // Check-in closes 3 hours before departure (for international) or 1 hour (domestic)
        const val CHECK_IN_CLOSES_HOURS_DOMESTIC = 1L
        const val CHECK_IN_CLOSES_HOURS_INTERNATIONAL = 3L
    }

    /**
     * Retrieves check-in eligibility for a booking.
     * Validates PNR + last name and returns passenger eligibility status.
     */
    suspend fun getCheckInEligibility(pnr: String, lastName: String): CheckInEligibility {
        log.info("Getting check-in eligibility for PNR=$pnr")

        val normalizedPnr = pnr.uppercase().trim()
        val normalizedLastName = lastName.uppercase().trim()

        // Find booking
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        // Verify last name matches at least one passenger
        val passengersJson = booking.passengersJson
        // In production: properly deserialize and check last names
        if (!passengersJson.uppercase().contains(normalizedLastName)) {
            throw BookingNotFoundException(normalizedPnr)
        }

        val now = Clock.System.now()
        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        
        val checkInOpenTime = departureTime.minus(CHECK_IN_OPENS_HOURS, DateTimeUnit.HOUR)
        val isInternational = isInternationalFlight(booking.origin, booking.destination)
        val closeHours = if (isInternational) CHECK_IN_CLOSES_HOURS_INTERNATIONAL else CHECK_IN_CLOSES_HOURS_DOMESTIC
        val checkInCloseTime = departureTime.minus(closeHours, DateTimeUnit.HOUR)
        
        val isCheckInOpen = now >= checkInOpenTime && now <= checkInCloseTime

        // Get or create check-in status for each passenger
        val checkedInPassengers = checkInRepository.findByPnr(normalizedPnr)
        val checkedInIndices = checkedInPassengers.map { it.passengerIndex }.toSet()

        // Parse passengers (mock implementation)
        val passengers = parsePassengers(passengersJson).mapIndexed { index, name ->
            val isInfant = name.lowercase().contains("infant") || index >= 3 // mock logic
            val isCheckedIn = index in checkedInIndices
            
            val (eligible, reason) = when {
                isCheckedIn -> true to null
                now < checkInOpenTime -> false to CheckInIneligibilityReason.TOO_EARLY
                now > checkInCloseTime -> false to CheckInIneligibilityReason.TOO_LATE
                isInfant -> false to CheckInIneligibilityReason.INFANT_PASSENGER
                else -> true to null
            }

            val existingCheckIn = checkedInPassengers.find { it.passengerIndex == index }
            
            PassengerCheckInStatus(
                passengerIndex = index,
                fullName = name,
                type = if (isInfant) PassengerType.INFANT else PassengerType.ADULT,
                isEligible = eligible,
                isCheckedIn = isCheckedIn,
                seatNumber = existingCheckIn?.seatNumber,
                boardingGroup = existingCheckIn?.boardingGroup,
                ineligibilityReason = reason
            )
        }

        return CheckInEligibility(
            pnr = PnrCode(normalizedPnr),
            flight = FlightSummary(
                flightNumber = booking.flightNumber,
                origin = AirportCode(booking.origin),
                destination = AirportCode(booking.destination),
                departureTime = departureTime,
                fareFamily = FareFamilyCode.valueOf(booking.fareFamily)
            ),
            passengers = passengers,
            checkInOpenTime = checkInOpenTime,
            checkInCloseTime = checkInCloseTime,
            isCheckInOpen = isCheckInOpen
        )
    }

    /**
     * Completes check-in for selected passengers.
     * Assigns seats and generates boarding passes.
     */
    suspend fun completeCheckIn(
        pnr: String,
        passengerIndices: List<Int>,
        seatPreferences: List<SeatPreference>?
    ): CheckInCompletion {
        log.info("Completing check-in for PNR=$pnr, passengers=$passengerIndices")

        val normalizedPnr = pnr.uppercase().trim()
        
        // Verify eligibility
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        val now = Clock.System.now()
        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val departureDate = departureTime.toLocalDateTime(TimeZone.of("Asia/Riyadh")).date
        
        // Check if check-in is open
        val checkInOpenTime = departureTime.minus(CHECK_IN_OPENS_HOURS, DateTimeUnit.HOUR)
        val isInternational = isInternationalFlight(booking.origin, booking.destination)
        val closeHours = if (isInternational) CHECK_IN_CLOSES_HOURS_INTERNATIONAL else CHECK_IN_CLOSES_HOURS_DOMESTIC
        val checkInCloseTime = departureTime.minus(closeHours, DateTimeUnit.HOUR)
        
        if (now < checkInOpenTime) {
            throw CheckInNotAllowedException("Check-in opens ${CHECK_IN_OPENS_HOURS} hours before departure")
        }
        if (now > checkInCloseTime) {
            throw CheckInNotAllowedException("Check-in has closed for this flight")
        }

        val passengers = parsePassengers(booking.passengersJson)
        val boardingPasses = mutableListOf<BoardingPass>()

        for (passengerIndex in passengerIndices) {
            if (passengerIndex < 0 || passengerIndex >= passengers.size) {
                throw IllegalArgumentException("Invalid passenger index: $passengerIndex")
            }

            // Check if already checked in
            val existing = checkInRepository.findByPnrAndPassengerIndex(normalizedPnr, passengerIndex)
            if (existing != null) {
                log.info("Passenger $passengerIndex already checked in, returning existing boarding pass")
                boardingPasses.add(createBoardingPass(existing, booking, passengers[passengerIndex], departureDate))
                continue
            }

            // Assign seat
            val seatPreference = seatPreferences?.find { it.passengerIndex == passengerIndex }
            val seatNumber = assignSeat(seatPreference)
            val boardingGroup = determineBoardingGroup(seatNumber)
            val sequenceNumber = generateSequenceNumber()

            // Save check-in record
            val checkInEntity = checkInRepository.save(
                pnr = normalizedPnr,
                passengerIndex = passengerIndex,
                seatNumber = seatNumber,
                boardingGroup = boardingGroup,
                sequenceNumber = sequenceNumber,
                checkedInAt = now
            )

            boardingPasses.add(createBoardingPass(checkInEntity, booking, passengers[passengerIndex], departureDate))
        }

        return CheckInCompletion(
            pnr = PnrCode(normalizedPnr),
            boardingPasses = boardingPasses,
            checkedInCount = boardingPasses.size,
            message = "Check-in complete for ${boardingPasses.size} passenger(s)"
        )
    }

    /**
     * Retrieves boarding pass for a checked-in passenger.
     */
    suspend fun getBoardingPass(pnr: String, passengerIndex: Int): BoardingPass? {
        log.info("Getting boarding pass for PNR=$pnr, passenger=$passengerIndex")

        val normalizedPnr = pnr.uppercase().trim()
        
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        val checkIn = checkInRepository.findByPnrAndPassengerIndex(normalizedPnr, passengerIndex)
            ?: return null

        val passengers = parsePassengers(booking.passengersJson)
        if (passengerIndex >= passengers.size) {
            return null
        }

        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val departureDate = departureTime.toLocalDateTime(TimeZone.of("Asia/Riyadh")).date

        return createBoardingPass(checkIn, booking, passengers[passengerIndex], departureDate)
    }

    private fun createBoardingPass(
        checkIn: CheckInRecord,
        booking: com.fairair.entity.BookingEntity,
        passengerName: String,
        departureDate: LocalDate
    ): BoardingPass {
        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val departureDateTime = departureTime.toLocalDateTime(TimeZone.of("Asia/Riyadh"))
        val boardingTime = departureTime.minus(45, DateTimeUnit.MINUTE)
            .toLocalDateTime(TimeZone.of("Asia/Riyadh"))

        val originCity = getStationCity(booking.origin)
        val destinationCity = getStationCity(booking.destination)

        return BoardingPass(
            pnr = PnrCode(checkIn.pnr),
            passengerName = passengerName,
            passengerType = PassengerType.ADULT,
            flightNumber = booking.flightNumber,
            origin = AirportCode(booking.origin),
            destination = AirportCode(booking.destination),
            originCity = originCity,
            destinationCity = destinationCity,
            departureDate = departureDate,
            departureTime = "${departureDateTime.hour.toString().padStart(2, '0')}:${departureDateTime.minute.toString().padStart(2, '0')}",
            boardingTime = "${boardingTime.hour.toString().padStart(2, '0')}:${boardingTime.minute.toString().padStart(2, '0')}",
            gate = "A${Random.nextInt(1, 20)}",
            seatNumber = checkIn.seatNumber,
            boardingGroup = checkIn.boardingGroup,
            sequenceNumber = checkIn.sequenceNumber,
            fareFamily = FareFamilyCode.valueOf(booking.fareFamily),
            cabinClass = "Economy",
            barcodeData = generateBarcodeData(checkIn.pnr, checkIn.passengerIndex, booking.flightNumber),
            issuedAt = checkIn.checkedInAt
        )
    }

    private fun parsePassengers(passengersJson: String): List<String> {
        // Mock: extract names from JSON
        // In production: proper JSON parsing with kotlinx.serialization
        val regex = """"fullName"\s*:\s*"([^"]+)"""".toRegex()
        return regex.findAll(passengersJson).map { it.groupValues[1] }.toList()
            .ifEmpty { listOf("Passenger 1") }
    }

    private fun isInternationalFlight(origin: String, destination: String): Boolean {
        val saudiAirports = setOf("JED", "RUH", "DMM", "AHB", "GIZ", "TUU")
        return !(origin in saudiAirports && destination in saudiAirports)
    }

    private fun assignSeat(preference: SeatPreference?): String {
        // Mock seat assignment
        // In production: check seat map availability from Navitaire
        val row = Random.nextInt(10, 35)
        val column = when (preference?.preferenceType) {
            SeatPreferenceType.WINDOW -> listOf("A", "F").random()
            SeatPreferenceType.AISLE -> listOf("C", "D").random()
            SeatPreferenceType.MIDDLE -> listOf("B", "E").random()
            else -> listOf("A", "B", "C", "D", "E", "F").random()
        }
        return "$row$column"
    }

    private fun determineBoardingGroup(seatNumber: String): String {
        val row = seatNumber.filter { it.isDigit() }.toIntOrNull() ?: 20
        return when {
            row <= 10 -> "A"
            row <= 20 -> "B"
            row <= 30 -> "C"
            else -> "D"
        }
    }

    private fun generateSequenceNumber(): Int = Random.nextInt(1, 200)

    private fun generateBarcodeData(pnr: String, passengerIndex: Int, flightNumber: String): String {
        // Mock BCBP (Bar Coded Boarding Pass) data
        // In production: generate proper IATA BCBP format
        return "M1${pnr}${passengerIndex}${flightNumber}${UUID.randomUUID().toString().take(8).uppercase()}"
    }

    private fun getStationCity(code: String): String {
        return when (code) {
            "JED" -> "Jeddah"
            "RUH" -> "Riyadh"
            "DMM" -> "Dammam"
            "AHB" -> "Abha"
            "GIZ" -> "Jazan"
            "TUU" -> "Tabuk"
            "DXB" -> "Dubai"
            "CAI" -> "Cairo"
            else -> code
        }
    }
}

/**
 * Check-in record for database storage.
 */
data class CheckInRecord(
    val pnr: String,
    val passengerIndex: Int,
    val seatNumber: String,
    val boardingGroup: String,
    val sequenceNumber: Int,
    val checkedInAt: Instant
)
