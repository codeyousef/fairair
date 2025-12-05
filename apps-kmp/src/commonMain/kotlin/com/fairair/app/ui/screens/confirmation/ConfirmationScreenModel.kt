package com.fairair.app.ui.screens.confirmation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.contract.dto.BookingConfirmationDto
import com.fairair.app.persistence.LocalStorage
import com.fairair.app.state.BookingFlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Confirmation screen.
 * Displays booking confirmation details and allows saving to local storage.
 */
class ConfirmationScreenModel(
    private val bookingFlowState: BookingFlowState,
    private val localStorage: LocalStorage
) : ScreenModel {

    private val _uiState = MutableStateFlow(ConfirmationUiState())
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    init {
        loadConfirmation()
    }

    /**
     * Loads confirmation data from booking state and auto-saves to local storage.
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

            // Auto-save booking to local storage
            try {
                localStorage.saveBooking(confirmation)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                // Continue even if save fails - user can retry manually
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
     * Manually saves the booking to local storage (for retry).
     */
    fun saveBooking() {
        screenModelScope.launch {
            val confirmation = _uiState.value.confirmation ?: return@launch
            try {
                localStorage.saveBooking(confirmation)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save booking: ${e.message}") }
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
    val isSaved: Boolean = false,
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
