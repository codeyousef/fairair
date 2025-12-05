package com.fairair.app.ui.screens.saved

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.contract.dto.BookingConfirmationDto
import com.fairair.app.persistence.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Saved Bookings screen.
 * Manages locally saved bookings for offline access.
 */
class SavedBookingsScreenModel(
    private val localStorage: LocalStorage
) : ScreenModel {

    private val _uiState = MutableStateFlow(SavedBookingsUiState())
    val uiState: StateFlow<SavedBookingsUiState> = _uiState.asStateFlow()

    init {
        loadSavedBookings()
        observeSavedBookings()
    }

    /**
     * Initial load of saved bookings.
     */
    private fun loadSavedBookings() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val bookings = localStorage.getSavedBookingsList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookings = bookings
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load saved bookings: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Observes saved bookings for real-time updates.
     */
    private fun observeSavedBookings() {
        screenModelScope.launch {
            localStorage.getSavedBookingsFlow().collect { bookings ->
                _uiState.update { it.copy(bookings = bookings) }
            }
        }
    }

    /**
     * Deletes a booking by PNR.
     */
    fun deleteBooking(pnr: String) {
        screenModelScope.launch {
            try {
                localStorage.deleteBooking(pnr)
                // Update will come through the flow observer
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete booking: ${e.message}") }
            }
        }
    }

    /**
     * Clears all saved bookings.
     */
    fun clearAllBookings() {
        screenModelScope.launch {
            try {
                localStorage.clearAllBookings()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to clear bookings: ${e.message}") }
            }
        }
    }

    /**
     * Clears error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Selects a booking for viewing details.
     */
    fun selectBooking(booking: BookingConfirmationDto) {
        _uiState.update { it.copy(selectedBooking = booking) }
    }

    /**
     * Clears selected booking.
     */
    fun clearSelectedBooking() {
        _uiState.update { it.copy(selectedBooking = null) }
    }
}

/**
 * UI state for the Saved Bookings screen.
 */
data class SavedBookingsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val bookings: List<BookingConfirmationDto> = emptyList(),
    val selectedBooking: BookingConfirmationDto? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && bookings.isEmpty()

    val hasBookings: Boolean
        get() = bookings.isNotEmpty()
}
