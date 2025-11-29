package com.flyadeal.app.viewmodel

import com.flyadeal.app.api.*
import com.flyadeal.app.state.BookingFlowState
import com.flyadeal.app.state.SearchCriteria
import com.flyadeal.app.ui.components.velocity.DestinationTheme
import com.flyadeal.app.ui.screens.search.SearchField
import com.flyadeal.app.ui.screens.search.VelocitySearchState
import com.flyadeal.app.util.toDisplayMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for the Search screen that works without Voyager dependency.
 * This is used for Wasm target where Voyager Navigator causes rendering issues.
 *
 * Unlike SearchScreenModel which extends Voyager's ScreenModel, this class
 * takes a CoroutineScope parameter that can be provided by the composable
 * using rememberCoroutineScope().
 */
class WasmSearchViewModel(
    private val apiClient: FlyadealApiClient,
    private val bookingFlowState: BookingFlowState,
    private val scope: CoroutineScope
) {
    private val _velocityState = MutableStateFlow(VelocitySearchState())
    val velocityState: StateFlow<VelocitySearchState> = _velocityState.asStateFlow()

    init {
        loadInitialData()
    }

    /**
     * Loads stations and routes on init.
     */
    private fun loadInitialData() {
        scope.launch {
            _velocityState.update { it.copy(isLoading = true, error = null) }

            val stationsResult = apiClient.getStations()
            val routesResult = apiClient.getRoutes()

            when {
                stationsResult is ApiResult.Success && routesResult is ApiResult.Success -> {
                    _velocityState.update {
                        it.copy(
                            isLoading = false,
                            availableOrigins = stationsResult.data,
                            routeMap = routesResult.data.routes
                        )
                    }
                }
                stationsResult is ApiResult.Error -> {
                    _velocityState.update {
                        it.copy(isLoading = false, error = stationsResult.toDisplayMessage())
                    }
                }
                routesResult is ApiResult.Error -> {
                    _velocityState.update {
                        it.copy(isLoading = false, error = (routesResult as ApiResult.Error).toDisplayMessage())
                    }
                }
            }
        }
    }

    /**
     * Sets the active field for the Velocity sentence builder UI.
     * Pass null to dismiss any open selection sheet.
     */
    fun setActiveField(field: SearchField?) {
        _velocityState.update { it.copy(activeField = field) }
    }

    /**
     * Selects an origin airport (Velocity UI).
     */
    fun selectVelocityOrigin(station: StationDto) {
        _velocityState.update { state ->
            val validDestinationCodes = state.routeMap[station.code] ?: emptyList()
            val availableDestinations = state.availableOrigins.filter { it.code in validDestinationCodes }

            // If current destination is not in the new valid destinations list, clear it
            val currentDestination = state.selectedDestination
            val newDestination = if (currentDestination != null &&
                validDestinationCodes.contains(currentDestination.code)) {
                currentDestination
            } else {
                null
            }

            state.copy(
                selectedOrigin = station,
                selectedDestination = newDestination,
                availableDestinations = availableDestinations,
                // Clear destination background if destination was cleared
                destinationBackground = if (newDestination != null) state.destinationBackground else null
            )
        }
    }

    /**
     * Selects a destination airport (Velocity UI).
     */
    fun selectVelocityDestination(station: StationDto) {
        _velocityState.update { state ->
            state.copy(
                selectedDestination = station,
                destinationBackground = DestinationTheme.forDestination(station.code)
            )
        }
    }

    /**
     * Selects a departure date (Velocity UI).
     */
    fun selectVelocityDate(date: LocalDate) {
        _velocityState.update { it.copy(departureDate = date) }
    }

    /**
     * Updates passenger count (Velocity UI).
     * For now, Velocity UI only supports adult passengers.
     */
    fun setVelocityPassengerCount(count: Int) {
        _velocityState.update { it.copy(passengerCount = count.coerceIn(1, 9)) }
    }

    /**
     * Initiates flight search from Velocity UI.
     */
    fun searchFromVelocity(onSearchComplete: () -> Unit) {
        val state = _velocityState.value
        val origin = state.selectedOrigin ?: return
        val destination = state.selectedDestination ?: return
        val date = state.departureDate ?: return

        scope.launch {
            _velocityState.update { it.copy(isSearching = true, error = null) }

            val request = FlightSearchRequestDto(
                origin = origin.code,
                destination = destination.code,
                departureDate = date.toString(),
                passengers = PassengerCountsDto(
                    adults = state.passengerCount,
                    children = 0,
                    infants = 0
                )
            )

            when (val result = apiClient.searchFlights(request)) {
                is ApiResult.Success -> {
                    bookingFlowState.setSearchCriteria(
                        SearchCriteria(
                            origin = origin,
                            destination = destination,
                            departureDate = date.toString(),
                            passengers = request.passengers
                        )
                    )
                    bookingFlowState.setSearchResult(result.data)
                    _velocityState.update { it.copy(isSearching = false) }
                    onSearchComplete()
                }
                is ApiResult.Error -> {
                    val errorMessage = result.toDisplayMessage()
                    _velocityState.update {
                        it.copy(isSearching = false, error = errorMessage)
                    }
                }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _velocityState.update { it.copy(error = null) }
    }

    /**
     * Retries loading initial data.
     */
    fun retry() {
        _velocityState.update { it.copy(isLoading = true, error = null) }
        loadInitialData()
    }
}
