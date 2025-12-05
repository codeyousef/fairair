package com.fairair.app.ui.screens.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.app.api.*
import com.fairair.contract.dto.FlightSearchRequestDto
import com.fairair.contract.dto.LowFareDateDto
import com.fairair.contract.dto.PassengerCountsDto
import com.fairair.contract.dto.StationDto
import com.fairair.app.state.BookingFlowState
import com.fairair.app.util.toDisplayMessage
import com.fairair.app.state.SearchCriteria
import com.fairair.app.ui.components.velocity.DestinationTheme
import com.fairair.app.ui.components.velocity.PassengerCounts
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
 * ScreenModel for the Search screen.
 * Handles loading stations, routes, and initiating flight search.
 */
class SearchScreenModel(
    private val apiClient: FairairApiClient,
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _velocityState = MutableStateFlow(VelocitySearchState())
    val velocityState: StateFlow<VelocitySearchState> = _velocityState.asStateFlow()

    init {
        loadInitialData()
    }

    /**
     * Loads stations and routes on init.
     */
    private fun loadInitialData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val stationsResult = apiClient.getStations()
            val routesResult = apiClient.getRoutes()

            when {
                stationsResult is ApiResult.Success && routesResult is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            stations = stationsResult.data,
                            routes = routesResult.data.routes,
                            departureDate = getDefaultDate()
                        )
                    }
                    // Also update Velocity state
                    _velocityState.update {
                        it.copy(
                            isLoading = false,
                            availableOrigins = stationsResult.data,
                            routeMap = routesResult.data.routes
                        )
                    }
                }
                stationsResult is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = stationsResult.toDisplayMessage())
                    }
                    _velocityState.update {
                        it.copy(isLoading = false, error = stationsResult.toDisplayMessage())
                    }
                }
                routesResult is ApiResult.Error -> {
                    val errorMessage = (routesResult as ApiResult.Error).toDisplayMessage()
                    _uiState.update {
                        it.copy(isLoading = false, error = errorMessage)
                    }
                    _velocityState.update {
                        it.copy(isLoading = false, error = errorMessage)
                    }
                }
            }
        }
    }

    /**
     * Selects an origin airport.
     */
    fun selectOrigin(station: StationDto) {
        _uiState.update { state ->
            val validDestinations = state.routes[station.code] ?: emptyList()
            val currentDestination = state.selectedDestination
            val newDestination = if (currentDestination != null &&
                validDestinations.contains(currentDestination.code)) {
                currentDestination
            } else {
                null
            }

            state.copy(
                selectedOrigin = station,
                selectedDestination = newDestination,
                availableDestinations = state.stations.filter { it.code in validDestinations }
            )
        }
    }

    /**
     * Selects a destination airport.
     */
    fun selectDestination(station: StationDto) {
        _uiState.update { it.copy(selectedDestination = station) }
    }

    /**
     * Sets the departure date.
     */
    fun setDepartureDate(date: String) {
        _uiState.update { it.copy(departureDate = date) }
    }

    /**
     * Updates passenger counts.
     */
    fun updatePassengers(adults: Int, children: Int, infants: Int) {
        _uiState.update {
            it.copy(
                adults = adults.coerceIn(1, 9),
                children = children.coerceIn(0, 8),
                infants = infants.coerceIn(0, adults)
            )
        }
    }

    /**
     * Increments adult count.
     */
    fun incrementAdults() {
        _uiState.update { state ->
            val newAdults = (state.adults + 1).coerceAtMost(9 - state.children)
            state.copy(adults = newAdults)
        }
    }

    /**
     * Decrements adult count.
     */
    fun decrementAdults() {
        _uiState.update { state ->
            val newAdults = (state.adults - 1).coerceAtLeast(1).coerceAtLeast(state.infants)
            state.copy(adults = newAdults)
        }
    }

    /**
     * Increments child count.
     */
    fun incrementChildren() {
        _uiState.update { state ->
            val maxChildren = (9 - state.adults - state.infants).coerceIn(0, 8)
            val newChildren = (state.children + 1).coerceAtMost(maxChildren)
            state.copy(children = newChildren)
        }
    }

    /**
     * Decrements child count.
     */
    fun decrementChildren() {
        _uiState.update { state ->
            state.copy(children = (state.children - 1).coerceAtLeast(0))
        }
    }

    /**
     * Increments infant count.
     */
    fun incrementInfants() {
        _uiState.update { state ->
            val maxInfants = state.adults.coerceAtMost(9 - state.adults - state.children)
            val newInfants = (state.infants + 1).coerceAtMost(maxInfants)
            state.copy(infants = newInfants)
        }
    }

    /**
     * Decrements infant count.
     */
    fun decrementInfants() {
        _uiState.update { state ->
            state.copy(infants = (state.infants - 1).coerceAtLeast(0))
        }
    }

    /**
     * Swaps origin and destination if valid.
     */
    fun swapAirports() {
        _uiState.update { state ->
            val origin = state.selectedOrigin
            val destination = state.selectedDestination

            if (origin != null && destination != null) {
                val reverseValid = state.routes[destination.code]?.contains(origin.code) == true
                if (reverseValid) {
                    val newAvailableDestinations = state.stations.filter {
                        it.code in (state.routes[destination.code] ?: emptyList())
                    }
                    state.copy(
                        selectedOrigin = destination,
                        selectedDestination = origin,
                        availableDestinations = newAvailableDestinations
                    )
                } else {
                    state.copy(error = "Reverse route not available")
                }
            } else {
                state
            }
        }
    }

    /**
     * Initiates flight search.
     */
    fun search(onSearchComplete: () -> Unit) {
        val state = _uiState.value
        val origin = state.selectedOrigin ?: return
        val destination = state.selectedDestination ?: return

        screenModelScope.launch {
            _uiState.update { it.copy(isSearching = true, error = null) }

            val request = FlightSearchRequestDto(
                origin = origin.code,
                destination = destination.code,
                departureDate = state.departureDate,
                passengers = PassengerCountsDto(
                    adults = state.adults,
                    children = state.children,
                    infants = state.infants
                )
            )

            when (val result = apiClient.searchFlights(request)) {
                is ApiResult.Success -> {
                    bookingFlowState.setSearchCriteria(
                        SearchCriteria(
                            origin = origin,
                            destination = destination,
                            departureDate = state.departureDate,
                            passengers = request.passengers
                        )
                    )
                    bookingFlowState.setSearchResult(result.data)
                    _uiState.update { it.copy(isSearching = false) }
                    onSearchComplete()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isSearching = false, error = result.toDisplayMessage())
                    }
                }
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
     * Returns tomorrow's date as default.
     */
    private fun getDefaultDate(): String {
        val now = Clock.System.now()
        val tomorrow = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        return tomorrow.toString()
    }

    // ============================================================================
    // Velocity UI Methods
    // ============================================================================

    /**
     * Sets the active field for the Velocity sentence builder UI.
     * Pass null to dismiss any open selection sheet.
     */
    fun setActiveField(field: SearchField?) {
        _velocityState.update { it.copy(activeField = field) }
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
        screenModelScope.launch {
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

        // Also update legacy state for compatibility
        selectOrigin(station)
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
        // Also update legacy state for compatibility
        selectDestination(station)
    }

    /**
     * Selects a departure date (Velocity UI).
     */
    fun selectVelocityDate(date: LocalDate) {
        _velocityState.update { it.copy(departureDate = date) }
        // Also update legacy state for compatibility
        setDepartureDate(date.toString())
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
        // Also update legacy state for compatibility
        updatePassengers(counts.adults, counts.children, counts.infants)
    }

    /**
     * Initiates flight search from Velocity UI.
     */
    fun searchFromVelocity(onSearchComplete: () -> Unit) {
        val state = _velocityState.value
        val origin = state.selectedOrigin ?: return
        val destination = state.selectedDestination ?: return
        val date = state.departureDate ?: return

        screenModelScope.launch {
            _velocityState.update { it.copy(isSearching = true, error = null) }
            _uiState.update { it.copy(isSearching = true, error = null) }

            val request = FlightSearchRequestDto(
                origin = origin.code,
                destination = destination.code,
                departureDate = date.toString(),
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
                            passengers = request.passengers
                        )
                    )
                    bookingFlowState.setSearchResult(result.data)
                    _velocityState.update { it.copy(isSearching = false) }
                    _uiState.update { it.copy(isSearching = false) }
                    onSearchComplete()
                }
                is ApiResult.Error -> {
                    val errorMessage = result.toDisplayMessage()
                    _velocityState.update {
                        it.copy(isSearching = false, error = errorMessage)
                    }
                    _uiState.update {
                        it.copy(isSearching = false, error = errorMessage)
                    }
                }
            }
        }
    }
}

/**
 * UI state for the Search screen.
 */
data class SearchUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val error: String? = null,
    val stations: List<StationDto> = emptyList(),
    val routes: Map<String, List<String>> = emptyMap(),
    val selectedOrigin: StationDto? = null,
    val selectedDestination: StationDto? = null,
    val availableDestinations: List<StationDto> = emptyList(),
    val departureDate: String = "",
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0
) {
    val totalPassengers: Int get() = adults + children + infants

    val canSearch: Boolean
        get() = selectedOrigin != null &&
                selectedDestination != null &&
                departureDate.isNotBlank() &&
                totalPassengers in 1..9
}
