package com.flyadeal.app.ui.screens.results

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.flyadeal.app.api.FlightDto
import com.flyadeal.app.api.FlightSearchResponseDto
import com.flyadeal.app.state.BookingFlowState
import com.flyadeal.app.state.SelectedFlight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Results screen.
 * Displays available flights and handles flight selection.
 */
class ResultsScreenModel(
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadSearchResults()
    }

    /**
     * Loads search results from the booking flow state.
     */
    private fun loadSearchResults() {
        screenModelScope.launch {
            val searchResult = bookingFlowState.searchResult
            val criteria = bookingFlowState.searchCriteria

            if (searchResult == null || criteria == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No search results available. Please search again."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    searchResponse = searchResult,
                    originCode = criteria.origin.code,
                    originCity = criteria.origin.city,
                    destinationCode = criteria.destination.code,
                    destinationCity = criteria.destination.city,
                    departureDate = criteria.departureDate,
                    passengerCount = criteria.passengers.adults +
                                    criteria.passengers.children +
                                    criteria.passengers.infants
                )
            }
        }
    }

    /**
     * Selects a flight and fare combination.
     */
    fun selectFlight(flight: FlightDto, fareFamily: String, farePrice: String) {
        val state = _uiState.value

        _uiState.update {
            it.copy(
                selectedFlightId = flight.flightNumber,
                selectedFareFamily = fareFamily
            )
        }

        bookingFlowState.setSelectedFlight(
            SelectedFlight(
                flight = flight,
                fareFamily = fareFamily,
                totalPrice = farePrice
            )
        )
    }

    /**
     * Confirms selection and proceeds to passenger info.
     */
    fun confirmSelection(onConfirmed: () -> Unit) {
        val state = _uiState.value
        if (state.selectedFlightId != null && state.selectedFareFamily != null) {
            onConfirmed()
        } else {
            _uiState.update { it.copy(error = "Please select a flight to continue") }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Results screen.
 */
data class ResultsUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchResponse: FlightSearchResponseDto? = null,
    val originCode: String = "",
    val originCity: String = "",
    val destinationCode: String = "",
    val destinationCity: String = "",
    val departureDate: String = "",
    val passengerCount: Int = 0,
    val selectedFlightId: String? = null,
    val selectedFareFamily: String? = null
) {
    val flights: List<FlightDto>
        get() = searchResponse?.flights ?: emptyList()

    val hasSelection: Boolean
        get() = selectedFlightId != null && selectedFareFamily != null
}
