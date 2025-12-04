package com.fairair.service

import com.fairair.contract.model.*
import com.fairair.controller.BookingNotFoundException
import com.fairair.controller.CancellationNotAllowedException
import com.fairair.controller.ModificationNotAllowedException
import com.fairair.repository.BookingRepository
import com.fairair.repository.CheckInRepository
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service for manage booking operations.
 * Handles booking retrieval, modification, and cancellation.
 * 
 * Production Notes:
 * - Replace mock implementations with Navitaire API calls
 * - Implement proper fare rules validation
 * - Add payment processing for change fees
 * - Store modification history in database
 */
@Service
class ManageBookingService(
    private val bookingRepository: BookingRepository,
    private val checkInRepository: CheckInRepository,
    private val flightService: FlightService
) {
    private val log = LoggerFactory.getLogger(ManageBookingService::class.java)
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Retrieves a booking for management.
     */
    suspend fun retrieveBooking(pnr: String, lastName: String): ManagedBooking {
        log.info("Retrieving booking for PNR=$pnr")

        val normalizedPnr = pnr.uppercase().trim()
        val normalizedLastName = lastName.uppercase().trim()

        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        // Verify last name
        if (!booking.passengersJson.uppercase().contains(normalizedLastName)) {
            throw BookingNotFoundException(normalizedPnr)
        }

        return convertToManagedBooking(booking)
    }

    /**
     * Updates passenger details.
     */
    suspend fun updatePassengers(pnr: String, updates: List<PassengerUpdate>): ManagedBooking {
        log.info("Updating passengers for PNR=$pnr, updates=${updates.size}")

        val normalizedPnr = pnr.uppercase().trim()
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        // Check if modifications are allowed
        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val now = Clock.System.now()
        
        if (now >= departureTime) {
            throw ModificationNotAllowedException("Cannot modify booking after departure")
        }

        // In production: validate each update and apply to Navitaire
        // For mock: just return the current booking
        log.info("Passenger updates applied (mock)")

        return convertToManagedBooking(booking)
    }

    /**
     * Gets a quote for changing the flight.
     */
    suspend fun getFlightChangeQuote(
        pnr: String,
        newDate: LocalDate,
        preferredFlightNumber: String?
    ): FlightChangeQuote {
        log.info("Getting flight change quote for PNR=$pnr, newDate=$newDate")

        val normalizedPnr = pnr.uppercase().trim()
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        val fareFamily = FareFamilyCode.valueOf(booking.fareFamily)
        val inclusions = FareInclusions.forFareFamily(fareFamily)

        // Check if changes are allowed
        if (!inclusions.changePolicy.allowed) {
            throw ModificationNotAllowedException("Flight changes not allowed for ${fareFamily.displayName} fare")
        }

        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val now = Clock.System.now()
        
        if (now >= departureTime) {
            throw ModificationNotAllowedException("Cannot change booking after departure")
        }

        // Get available flights for new date
        val searchRequest = FlightSearchRequest(
            origin = AirportCode(booking.origin),
            destination = AirportCode(booking.destination),
            departureDate = newDate,
            passengers = PassengerCounts.SINGLE_ADULT
        )
        
        val searchResponse = flightService.searchFlights(searchRequest)
        
        val availableFlights = searchResponse.flights.map { flight ->
            val selectedFare = flight.fareFamilies.find { it.code == fareFamily }
            val originalPrice = Money.of(booking.totalAmount, Currency.valueOf(booking.currency))
            val newPrice = selectedFare?.price ?: Money.sar(0.0)
            
            FlightChangeOption(
                flightNumber = flight.flightNumber,
                departureTime = flight.departureTime,
                arrivalTime = flight.arrivalTime,
                priceDifference = Money.sar(newPrice.amountAsDouble - originalPrice.amountAsDouble),
                seatsAvailable = (5..50).random() // Mock availability
            )
        }

        val changeFee = inclusions.changePolicy.feeAmount
        val firstOption = availableFlights.firstOrNull()
        val priceDiff = firstOption?.priceDifference ?: Money.sar(0.0)
        
        val totalFee = changeFee?.let { 
            Money.sar(it.amountAsDouble + maxOf(0.0, priceDiff.amountAsDouble))
        } ?: Money.sar(maxOf(0.0, priceDiff.amountAsDouble))

        return FlightChangeQuote(
            originalFlight = FlightSummary(
                flightNumber = booking.flightNumber,
                origin = AirportCode(booking.origin),
                destination = AirportCode(booking.destination),
                departureTime = departureTime,
                fareFamily = fareFamily
            ),
            availableFlights = availableFlights,
            changeFee = changeFee,
            priceDifference = priceDiff,
            totalToPay = totalFee,
            expiresAt = Clock.System.now().plus(30, DateTimeUnit.MINUTE)
        )
    }

    /**
     * Cancels a booking.
     */
    suspend fun cancelBooking(
        pnr: String,
        reason: CancellationReason,
        comments: String?
    ): CancellationConfirmation {
        log.info("Cancelling booking PNR=$pnr, reason=$reason")

        val normalizedPnr = pnr.uppercase().trim()
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        val fareFamily = FareFamilyCode.valueOf(booking.fareFamily)
        val inclusions = FareInclusions.forFareFamily(fareFamily)

        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val now = Clock.System.now()
        
        if (now >= departureTime) {
            throw CancellationNotAllowedException("Cannot cancel booking after departure")
        }

        // Calculate refund based on fare rules
        val totalPaid = Money.of(booking.totalAmount, Currency.valueOf(booking.currency))
        val refundAmount = if (inclusions.cancellationPolicy.allowed) {
            val percentage = inclusions.cancellationPolicy.refundPercentage
            Money.sar(totalPaid.amountAsDouble * percentage / 100)
        } else {
            Money.sar(0.0)
        }

        // In production: process refund through payment gateway
        // Delete check-in records
        checkInRepository.deleteByPnr(normalizedPnr)
        
        // In production: mark booking as cancelled in database (don't delete)
        // For mock: we leave it as-is

        return CancellationConfirmation(
            pnr = PnrCode(normalizedPnr),
            cancelledAt = Clock.System.now(),
            refundAmount = refundAmount,
            refundMethod = "Original payment method",
            refundEstimatedDays = if (refundAmount.amountAsDouble > 0) 14 else 0,
            cancellationReference = "CX${UUID.randomUUID().toString().take(8).uppercase()}"
        )
    }

    /**
     * Adds ancillaries to an existing booking.
     */
    suspend fun addAncillaries(pnr: String, ancillaries: List<AncillaryRequest>): ManagedBooking {
        log.info("Adding ancillaries to PNR=$pnr, items=${ancillaries.size}")

        val normalizedPnr = pnr.uppercase().trim()
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw BookingNotFoundException(normalizedPnr)

        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val now = Clock.System.now()
        
        if (now >= departureTime) {
            throw ModificationNotAllowedException("Cannot add ancillaries after departure")
        }

        // In production: 
        // 1. Calculate prices for each ancillary
        // 2. Process payment
        // 3. Update booking in Navitaire
        log.info("Ancillaries added (mock)")

        return convertToManagedBooking(booking)
    }

    private suspend fun convertToManagedBooking(booking: com.fairair.entity.BookingEntity): ManagedBooking {
        val departureTime = Instant.fromEpochMilliseconds(booking.departureTime.toEpochMilli())
        val now = Clock.System.now()
        
        // Determine booking status
        val status = when {
            now >= departureTime -> BookingStatus.FLOWN
            checkInRepository.findByPnr(booking.pnr).isNotEmpty() -> BookingStatus.CHECKED_IN
            else -> BookingStatus.CONFIRMED
        }

        // Parse passengers
        val passengers = parsePassengersForManagement(booking.passengersJson, booking.pnr)
        
        // Determine allowed actions based on status and fare
        val fareFamily = FareFamilyCode.valueOf(booking.fareFamily)
        val inclusions = FareInclusions.forFareFamily(fareFamily)
        
        val allowedActions = buildList {
            if (status == BookingStatus.CONFIRMED) {
                add(BookingAction.CHECK_IN)
                add(BookingAction.SELECT_SEATS)
                add(BookingAction.ORDER_MEALS)
                add(BookingAction.ADD_ANCILLARIES)
                add(BookingAction.UPDATE_PASSENGERS)
                if (inclusions.changePolicy.allowed) add(BookingAction.CHANGE_FLIGHT)
                if (inclusions.cancellationPolicy.allowed) {
                    add(BookingAction.CANCEL_WITH_REFUND)
                } else {
                    add(BookingAction.CANCEL_NO_REFUND)
                }
            }
            if (status == BookingStatus.CHECKED_IN) {
                add(BookingAction.VIEW_BOARDING_PASS)
            }
            add(BookingAction.DOWNLOAD_TICKET)
        }

        val totalMoney = Money.of(booking.totalAmount, Currency.valueOf(booking.currency))

        return ManagedBooking(
            pnr = PnrCode(booking.pnr),
            bookingReference = booking.bookingReference,
            status = status,
            flight = ManagedFlightDetails(
                flightNumber = booking.flightNumber,
                origin = AirportCode(booking.origin),
                destination = AirportCode(booking.destination),
                originCity = getStationCity(booking.origin),
                destinationCity = getStationCity(booking.destination),
                originAirport = getStationName(booking.origin),
                destinationAirport = getStationName(booking.destination),
                departureTime = departureTime,
                arrivalTime = departureTime.plus(90, DateTimeUnit.MINUTE), // Mock duration
                durationMinutes = 90,
                fareFamily = fareFamily,
                aircraft = "A320neo",
                flightStatus = if (now >= departureTime) FlightStatus.LANDED else FlightStatus.SCHEDULED
            ),
            passengers = passengers,
            ancillaries = emptyList(), // Mock: no ancillaries stored yet
            payment = PaymentSummary(
                baseFare = totalMoney,
                taxes = Money.sar(totalMoney.amountAsDouble * 0.15),
                ancillaries = Money.sar(0.0),
                fees = Money.sar(0.0),
                total = totalMoney,
                paid = totalMoney,
                refunded = Money.sar(0.0),
                paymentMethod = "Credit Card",
                lastFourDigits = "****"
            ),
            contact = ContactDetails(
                email = "customer@email.com", // In production: store in booking
                phone = null,
                alternatePhone = null
            ),
            createdAt = Instant.fromEpochMilliseconds(booking.createdAt.toEpochMilli()),
            lastModifiedAt = Instant.fromEpochMilliseconds(booking.createdAt.toEpochMilli()),
            allowedActions = allowedActions
        )
    }

    private fun parsePassengersForManagement(passengersJson: String, pnr: String): List<ManagedPassenger> {
        // Parse passenger names from JSON
        val nameRegex = """"fullName"\s*:\s*"([^"]+)"""".toRegex()
        val names = nameRegex.findAll(passengersJson).map { it.groupValues[1] }.toList()
        
        val checkedIn = checkInRepository.findByPnr(pnr)
        
        return names.mapIndexed { index, name ->
            val parts = name.split(" ")
            val checkInRecord = checkedIn.find { it.passengerIndex == index }
            
            ManagedPassenger(
                index = index,
                type = PassengerType.ADULT,
                title = Title.MR,
                firstName = parts.firstOrNull() ?: "Unknown",
                lastName = parts.drop(1).joinToString(" ").ifEmpty { "Unknown" },
                nationality = "SA",
                dateOfBirth = LocalDate(1990, 1, 1), // Mock
                documentId = "AB12345678",
                seatNumber = checkInRecord?.seatNumber,
                isCheckedIn = checkInRecord != null,
                specialRequests = emptyList(),
                ancillaries = emptyList()
            )
        }.ifEmpty {
            listOf(
                ManagedPassenger(
                    index = 0,
                    type = PassengerType.ADULT,
                    title = Title.MR,
                    firstName = "Unknown",
                    lastName = "Passenger",
                    nationality = "SA",
                    dateOfBirth = LocalDate(1990, 1, 1),
                    documentId = "AB12345678",
                    seatNumber = null,
                    isCheckedIn = false,
                    specialRequests = emptyList(),
                    ancillaries = emptyList()
                )
            )
        }
    }

    private fun getStationCity(code: String): String = when (code) {
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

    private fun getStationName(code: String): String = when (code) {
        "JED" -> "King Abdulaziz International Airport"
        "RUH" -> "King Khalid International Airport"
        "DMM" -> "King Fahd International Airport"
        "AHB" -> "Abha International Airport"
        "GIZ" -> "King Abdullah bin Abdulaziz Airport"
        "TUU" -> "Tabuk Regional Airport"
        "DXB" -> "Dubai International Airport"
        "CAI" -> "Cairo International Airport"
        else -> code
    }
}
