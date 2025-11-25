package com.flyadeal.app.state

import com.flyadeal.app.api.*

/**
 * Centralized state management for the booking flow.
 * Holds all data across the multi-step booking process.
 * Uses simple properties for cross-screen state sharing.
 */
class BookingFlowState {

    /** Search criteria from search screen */
    var searchCriteria: SearchCriteria? = null
        private set

    /** Flight search response from API */
    var searchResult: FlightSearchResponseDto? = null
        private set

    /** Selected flight with fare */
    var selectedFlight: SelectedFlight? = null
        private set

    /** Passenger information from passenger info screen */
    var passengerInfo: List<PassengerInfo> = emptyList()
        private set

    /** Selected ancillaries from ancillaries screen */
    var selectedAncillaries: SelectedAncillaries? = null
        private set

    /** Booking confirmation from API after successful payment */
    var bookingConfirmation: BookingConfirmationDto? = null
        private set

    /**
     * Sets search criteria from search screen.
     */
    fun setSearchCriteria(criteria: SearchCriteria) {
        this.searchCriteria = criteria
    }

    /**
     * Sets search result from API.
     */
    fun setSearchResult(result: FlightSearchResponseDto) {
        this.searchResult = result
    }

    /**
     * Sets the selected flight and fare.
     */
    fun setSelectedFlight(flight: SelectedFlight) {
        this.selectedFlight = flight
    }

    /**
     * Sets passenger information from passenger info screen.
     */
    fun setPassengerInfo(passengers: List<PassengerInfo>) {
        this.passengerInfo = passengers
    }

    /**
     * Sets selected ancillaries from ancillaries screen.
     */
    fun setSelectedAncillaries(ancillaries: SelectedAncillaries) {
        this.selectedAncillaries = ancillaries
    }

    /**
     * Sets booking confirmation after successful payment.
     */
    fun setBookingConfirmation(confirmation: BookingConfirmationDto) {
        this.bookingConfirmation = confirmation
    }

    /**
     * Resets all state for a new booking.
     */
    fun reset() {
        searchCriteria = null
        searchResult = null
        selectedFlight = null
        passengerInfo = emptyList()
        selectedAncillaries = null
        bookingConfirmation = null
    }
}

/**
 * Search criteria data class.
 */
data class SearchCriteria(
    val origin: StationDto,
    val destination: StationDto,
    val departureDate: String,
    val passengers: PassengerCountsDto
)

/**
 * Selected flight with fare information.
 */
data class SelectedFlight(
    val flight: FlightDto,
    val fareFamily: String,
    val totalPrice: String
)

/**
 * Passenger information collected from form.
 */
data class PassengerInfo(
    val id: String,
    val type: String,
    val title: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String,
    val nationality: String,
    val documentType: String,
    val documentNumber: String,
    val documentExpiry: String,
    val email: String,
    val phone: String
)

/**
 * Selected ancillaries data.
 */
data class SelectedAncillaries(
    val baggageSelections: List<BaggageInfo>,
    val mealSelections: List<MealInfo>,
    val priorityBoarding: Boolean,
    val ancillariesTotal: String,
    val grandTotal: String
)

/**
 * Baggage selection for a passenger.
 */
data class BaggageInfo(
    val passengerId: String,
    val weight: Int,
    val price: String
)

/**
 * Meal selection for a passenger.
 */
data class MealInfo(
    val passengerId: String,
    val mealCode: String,
    val price: String
)
