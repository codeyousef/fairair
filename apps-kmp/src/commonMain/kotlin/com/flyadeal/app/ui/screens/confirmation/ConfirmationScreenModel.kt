package com.flyadeal.app.ui.screens.confirmation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.flyadeal.app.api.BookingConfirmationDto
import com.flyadeal.app.state.BookingFlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Confirmation screen.
 * Displays booking confirmation details.
 */
class ConfirmationScreenModel(
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(ConfirmationUiState())
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    init {
        loadConfirmation()
    }

    /**
     * Loads confirmation data from booking state.
     */
    private fun loadConfirmation() {
        screenModelScope.launch {
            val confirmation = bookingFlowState.bookingConfirmation
            val criteria = bookingFlowState.searchCriteria
            val selectedFlight = bookingFlowState.selectedFlight
            val passengers = bookingFlowState.passengerInfo

            if (confirmation == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Booking confirmation not available"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    confirmation = confirmation,
                    originCode = criteria?.origin?.code ?: "",
                    originCity = criteria?.origin?.city ?: "",
                    destinationCode = criteria?.destination?.code ?: "",
                    destinationCity = criteria?.destination?.city ?: "",
                    departureDate = criteria?.departureDate ?: "",
                    departureTime = selectedFlight?.flight?.departureTime ?: "",
                    arrivalTime = selectedFlight?.flight?.arrivalTime ?: "",
                    flightNumber = selectedFlight?.flight?.flightNumber ?: "",
                    passengerCount = passengers.size,
                    primaryPassengerName = passengers.firstOrNull()?.let { "${it.firstName} ${it.lastName}" } ?: ""
                )
            }
        }
    }

    /**
     * Clears booking state and starts new booking.
     */
    fun startNewBooking(onNavigate: () -> Unit) {
        bookingFlowState.reset()
        onNavigate()
    }
}

/**
 * UI state for the Confirmation screen.
 */
data class ConfirmationUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val confirmation: BookingConfirmationDto? = null,
    val originCode: String = "",
    val originCity: String = "",
    val destinationCode: String = "",
    val destinationCity: String = "",
    val departureDate: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val flightNumber: String = "",
    val passengerCount: Int = 0,
    val primaryPassengerName: String = ""
) {
    val pnr: String
        get() = confirmation?.pnr ?: ""

    val totalPrice: String
        get() = confirmation?.totalPrice ?: "0"

    val currency: String
        get() = confirmation?.currency ?: "SAR"

    val bookingStatus: String
        get() = confirmation?.status ?: "PENDING"
}
