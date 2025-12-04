package com.fairair.app.viewmodel

import com.fairair.app.api.*
import com.fairair.app.state.BookingFlowState
import com.fairair.app.state.SearchCriteria
import com.fairair.app.ui.components.velocity.DestinationTheme
import com.fairair.app.ui.components.velocity.PassengerCounts
import com.fairair.app.ui.screens.search.SearchField
import com.fairair.app.ui.screens.search.TripType
import com.fairair.app.ui.screens.search.VelocitySearchState
import com.fairair.app.util.toDisplayMessage
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
    private val apiClient: FairairApiClient,
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
     * Sets the trip type (one-way, round-trip, multi-city).
     */
    fun setTripType(tripType: TripType) {
        _velocityState.update { state ->
            // Clear return date when switching to one-way
            if (tripType == TripType.ONE_WAY) {
                state.copy(
                    tripType = tripType,
                    returnDate = null,
                    returnLowFares = emptyMap()
                )
            } else {
                state.copy(tripType = tripType)
            }
        }
    }

    /**
     * Selects an origin airport (Velocity UI).
     * Fetches valid destinations from the backend.
     */
    fun selectVelocityOrigin(station: StationDto) {
        // Immediately update origin selection with loading state
        _velocityState.update { state ->
            state.copy(
                selectedOrigin = station,
                selectedDestination = null, // Clear destination while loading
                availableDestinations = emptyList(), // Clear destinations while fetching
                destinationBackground = null,
                loadingDestinations = true
            )
        }

        // Fetch valid destinations from backend
        scope.launch {
            when (val result = apiClient.getDestinationsForOrigin(station.code)) {
                is ApiResult.Success -> {
                    _velocityState.update { state ->
                        state.copy(
                            availableDestinations = result.data,
                            loadingDestinations = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    // On error, fall back to filtering from available origins using local route map
                    _velocityState.update { state ->
                        val validDestinationCodes = state.routeMap[station.code] ?: emptyList()
                        val fallbackDestinations = state.availableOrigins.filter { it.code in validDestinationCodes }
                        state.copy(
                            availableDestinations = fallbackDestinations,
                            loadingDestinations = false
                        )
                    }
                }
            }
        }
    }

    /**
     * Selects a destination airport (Velocity UI).
     * Also triggers fetching low fares if both origin and destination are selected.
     */
    fun selectVelocityDestination(station: StationDto) {
        _velocityState.update { state ->
            state.copy(
                selectedDestination = station,
                destinationBackground = DestinationTheme.forDestination(station.code),
                // Clear existing low fares when destination changes
                lowFares = emptyMap(),
                returnLowFares = emptyMap()
            )
        }
    }

    /**
     * Selects a departure date (Velocity UI).
     * If the return date is before the new departure date, clears it.
     */
    fun selectVelocityDate(date: LocalDate) {
        _velocityState.update { state ->
            // Clear return date if it's before the new departure date
            val newReturnDate = if (state.returnDate != null && state.returnDate < date) {
                null
            } else {
                state.returnDate
            }
            state.copy(
                departureDate = date,
                returnDate = newReturnDate
            )
        }
    }

    /**
     * Selects a return date (Velocity UI) for round-trip flights.
     */
    fun selectVelocityReturnDate(date: LocalDate) {
        _velocityState.update { it.copy(returnDate = date) }
    }
    
    /**
     * Fetches low fare prices for a given month.
     * Called when the date picker is opened or when the user navigates to a different month.
     * 
     * @param year The year to fetch prices for
     * @param month The month number (1-12) to fetch prices for
     */
    fun fetchLowFaresForMonth(year: Int, month: Int) {
        val state = _velocityState.value
        val origin = state.selectedOrigin ?: return
        val destination = state.selectedDestination ?: return
        
        // Calculate start and end dates for the month
        val startDate = LocalDate(year, month, 1)
        val daysInMonth = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 30
        }
        val endDate = LocalDate(year, month, daysInMonth)
        
        // Don't fetch if we already have data for this range
        val hasDataForMonth = state.lowFares.keys.any { 
            it.year == year && it.monthNumber == month 
        }
        if (hasDataForMonth && !state.lowFares.isEmpty()) {
            return
        }
        
        _velocityState.update { it.copy(loadingLowFares = true) }
        
        scope.launch {
            val result = apiClient.getLowFares(
                origin = origin.code,
                destination = destination.code,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                adults = state.adultsCount,
                children = state.childrenCount,
                infants = state.infantsCount
            )
            
            when (result) {
                is ApiResult.Success -> {
                    _velocityState.update { currentState ->
                        val newLowFares = currentState.lowFares.toMutableMap()
                        result.data.dates.forEach { dto ->
                            try {
                                val date = LocalDate.parse(dto.date)
                                newLowFares[date] = dto
                            } catch (e: Exception) {
                                // Skip invalid dates
                            }
                        }
                        currentState.copy(
                            lowFares = newLowFares,
                            loadingLowFares = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _velocityState.update { it.copy(loadingLowFares = false) }
                }
            }
        }
    }

    /**
     * Fetches low fare prices for the return flight for a given month.
     * Uses reversed origin/destination.
     * 
     * @param year The year to fetch prices for
     * @param month The month number (1-12) to fetch prices for
     */
    fun fetchReturnLowFaresForMonth(year: Int, month: Int) {
        val state = _velocityState.value
        val origin = state.selectedOrigin ?: return
        val destination = state.selectedDestination ?: return
        
        // Calculate start and end dates for the month
        val startDate = LocalDate(year, month, 1)
        val daysInMonth = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 30
        }
        val endDate = LocalDate(year, month, daysInMonth)
        
        // Don't fetch if we already have data for this range
        val hasDataForMonth = state.returnLowFares.keys.any { 
            it.year == year && it.monthNumber == month 
        }
        if (hasDataForMonth && !state.returnLowFares.isEmpty()) {
            return
        }
        
        _velocityState.update { it.copy(loadingReturnLowFares = true) }
        
        scope.launch {
            // Note: reversed origin/destination for return flight
            val result = apiClient.getLowFares(
                origin = destination.code,
                destination = origin.code,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                adults = state.adultsCount,
                children = state.childrenCount,
                infants = state.infantsCount
            )
            
            when (result) {
                is ApiResult.Success -> {
                    _velocityState.update { currentState ->
                        val newLowFares = currentState.returnLowFares.toMutableMap()
                        result.data.dates.forEach { dto ->
                            try {
                                val date = LocalDate.parse(dto.date)
                                newLowFares[date] = dto
                            } catch (e: Exception) {
                                // Skip invalid dates
                            }
                        }
                        currentState.copy(
                            returnLowFares = newLowFares,
                            loadingReturnLowFares = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _velocityState.update { it.copy(loadingReturnLowFares = false) }
                }
            }
        }
    }

    /**
     * Updates passenger counts (Velocity UI).
     * Supports adults, children, and infants.
     */
    fun setVelocityPassengerCount(counts: PassengerCounts) {
        _velocityState.update {
            it.copy(
                adultsCount = counts.adults.coerceIn(1, 9),
                childrenCount = counts.children.coerceIn(0, 8),
                infantsCount = counts.infants.coerceIn(0, counts.adults.coerceAtMost(4))
            )
        }
    }

    /**
     * Initiates flight search from Velocity UI.
     * For round-trip, searches outbound flight first.
     */
    fun searchFromVelocity(onSearchComplete: () -> Unit) {
        val state = _velocityState.value
        val origin = state.selectedOrigin ?: return
        val destination = state.selectedDestination ?: return
        val date = state.departureDate ?: return

        // For round-trip, require return date
        if (state.tripType == TripType.ROUND_TRIP && state.returnDate == null) {
            return
        }

        scope.launch {
            _velocityState.update { it.copy(isSearching = true, error = null) }

            val request = FlightSearchRequestDto(
                origin = origin.code,
                destination = destination.code,
                departureDate = date.toString(),
                returnDate = if (state.tripType == TripType.ROUND_TRIP) state.returnDate?.toString() else null,
                passengers = PassengerCountsDto(
                    adults = state.adultsCount,
                    children = state.childrenCount,
                    infants = state.infantsCount
                )
            )

            when (val result = apiClient.searchFlights(request)) {
                is ApiResult.Success -> {
                    bookingFlowState.setSearchCriteria(
                        SearchCriteria(
                            origin = origin,
                            destination = destination,
                            departureDate = date.toString(),
                            returnDate = state.returnDate?.toString(),
                            passengers = request.passengers,
                            tripType = state.tripType
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

    /**
     * Preselects an origin by airport code.
     * If the code matches an available origin, it will be selected.
     */
    fun preselectOrigin(code: String) {
        val state = _velocityState.value
        val station = state.availableOrigins.find { it.code == code }
        if (station != null) {
            selectVelocityOrigin(station)
        }
    }

    /**
     * Preselects a destination by airport code.
     * Clears any previously selected origin.
     * If the code matches an available station, it will be selected as destination.
     */
    fun preselectDestination(code: String) {
        // Clear the origin first
        _velocityState.update { state ->
            state.copy(
                selectedOrigin = null,
                selectedDestination = null,
                availableDestinations = emptyList(),
                destinationBackground = null
            )
        }
        // Then set the destination
        val state = _velocityState.value
        val station = state.availableOrigins.find { it.code == code }
        if (station != null) {
            selectVelocityDestination(station)
        }
    }

    /**
     * Preselects both origin and destination by airport codes.
     */
    fun preselectRoute(originCode: String, destinationCode: String) {
        // First select origin, which will populate available destinations
        preselectOrigin(originCode)
        // Then select destination from the now-available destinations
        val state = _velocityState.value
        val destinationStation = state.availableDestinations.find { it.code == destinationCode }
            ?: state.availableOrigins.find { it.code == destinationCode }
        if (destinationStation != null) {
            selectVelocityDestination(destinationStation)
        }
    }
}
