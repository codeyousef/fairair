package com.fairair.app.ui.screens.saved

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.contract.dto.BookingConfirmationDto
import com.fairair.app.api.ApiResult
import com.fairair.app.api.FairairApiClient
import com.fairair.app.persistence.LocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Saved Bookings screen.
 * Fetches bookings from the backend API when authenticated,
 * and uses local storage for offline access.
 */
class SavedBookingsScreenModel(
    private val localStorage: LocalStorage,
    private val apiClient: FairairApiClient
) : ScreenModel {

    private val _uiState = MutableStateFlow(SavedBookingsUiState())
    val uiState: StateFlow<SavedBookingsUiState> = _uiState.asStateFlow()

    init {
        loadSavedBookings()
        observeSavedBookings()
    }

    /**
     * Initial load of saved bookings.
     * Tries to fetch from backend API first, falls back to local cache.
     */
    private fun loadSavedBookings() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val authToken = localStorage.getAuthToken()
            
            if (authToken != null) {
                // Try to fetch from backend API
                when (val result = apiClient.getMyBookings(authToken)) {
                    is ApiResult.Success -> {
                        val bookings = result.data
                        // Cache bookings locally for offline access
                        bookings.forEach { booking ->
                            localStorage.saveBooking(booking)
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                bookings = bookings,
                                isAuthenticated = true
                            )
                        }
                        return@launch
                    }
                    is ApiResult.Error -> {
                        // Auth token might be expired, fall back to local storage
                        println("Failed to fetch bookings from API: ${result.message}")
                    }
                }
            }
            
            // Fall back to local storage (offline mode or not authenticated)
            try {
                val bookings = localStorage.getSavedBookingsList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookings = bookings,
                        isAuthenticated = authToken != null
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
     * Refreshes bookings from the backend API.
     */
    fun refresh() {
        loadSavedBookings()
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
    val selectedBooking: BookingConfirmationDto? = null,
    val isAuthenticated: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && bookings.isEmpty()

    val hasBookings: Boolean
        get() = bookings.isNotEmpty()
}
