package com.fairair.app.ui.screens.checkin

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.app.api.*
import com.fairair.app.util.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Check-In screen.
 * Handles PNR lookup, passenger selection, and check-in processing.
 */
class CheckInScreenModel(
    private val apiClient: FairairApiClient
) : ScreenModel {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    /**
     * Updates the PNR input field.
     */
    fun updatePnr(pnr: String) {
        _uiState.update { it.copy(pnr = pnr.uppercase().take(6), error = null) }
    }

    /**
     * Updates the last name input field.
     */
    fun updateLastName(lastName: String) {
        _uiState.update { it.copy(lastName = lastName, error = null) }
    }

    /**
     * Initiates check-in lookup with PNR and last name.
     */
    fun initiateCheckIn() {
        val state = _uiState.value
        
        if (state.pnr.length != 6) {
            _uiState.update { it.copy(error = "Please enter a valid 6-character booking reference") }
            return
        }
        
        if (state.lastName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter the passenger's last name") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = apiClient.lookupForCheckIn(state.pnr, state.lastName)) {
                is ApiResult.Success -> {
                    val data = result.data
                    if (data.isEligibleForCheckIn) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                step = CheckInStep.SELECT_PASSENGERS,
                                lookupResult = data,
                                selectedPassengerIds = data.passengers
                                    .filter { p -> !p.isCheckedIn }
                                    .map { p -> p.passengerId }
                                    .toSet()
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = data.eligibilityMessage ?: "Check-in is not available for this booking"
                            )
                        }
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.toDisplayMessage())
                    }
                }
            }
        }
    }

    /**
     * Toggles passenger selection for check-in.
     */
    fun togglePassenger(passengerId: String) {
        _uiState.update { state ->
            val newSelection = if (passengerId in state.selectedPassengerIds) {
                state.selectedPassengerIds - passengerId
            } else {
                state.selectedPassengerIds + passengerId
            }
            state.copy(selectedPassengerIds = newSelection)
        }
    }

    /**
     * Updates seat preference for a passenger.
     */
    fun updateSeatPreference(passengerId: String, preference: SeatPreference) {
        _uiState.update { state ->
            state.copy(
                seatPreferences = state.seatPreferences + (passengerId to preference)
            )
        }
    }

    /**
     * Completes the check-in process.
     */
    fun completeCheckIn() {
        val state = _uiState.value
        
        if (state.selectedPassengerIds.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one passenger") }
            return
        }

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val request = CheckInProcessRequestDto(
                pnr = state.pnr,
                passengerIds = state.selectedPassengerIds.toList(),
                seatPreferences = state.seatPreferences.mapValues { (_, pref) ->
                    SeatPreferenceDto(
                        preferWindow = pref == SeatPreference.WINDOW,
                        preferAisle = pref == SeatPreference.AISLE,
                        preferFront = pref == SeatPreference.FRONT
                    )
                }
            )

            when (val result = apiClient.processCheckIn(request)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = CheckInStep.COMPLETE,
                            checkInResult = result.data
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.toDisplayMessage())
                    }
                }
            }
        }
    }

    /**
     * Fetches boarding pass for a specific passenger.
     */
    fun getBoardingPass(passengerId: String) {
        val state = _uiState.value
        
        // Find the passenger index
        val passengerIndex = state.lookupResult?.passengers
            ?.indexOfFirst { it.passengerId == passengerId } ?: 0

        screenModelScope.launch {
            _uiState.update { it.copy(isLoadingBoardingPass = true) }

            when (val result = apiClient.getBoardingPass(state.pnr, passengerIndex)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingBoardingPass = false,
                            currentBoardingPass = result.data
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingBoardingPass = false,
                            error = result.toDisplayMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Closes the boarding pass view.
     */
    fun closeBoardingPass() {
        _uiState.update { it.copy(currentBoardingPass = null) }
    }

    /**
     * Goes back to the form step.
     */
    fun goBackToForm() {
        _uiState.update {
            CheckInUiState(
                pnr = it.pnr,
                lastName = it.lastName
            )
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Resets to start a new check-in.
     */
    fun reset() {
        _uiState.update { CheckInUiState() }
    }
}

/**
 * Steps in the check-in flow.
 */
enum class CheckInStep {
    ENTER_DETAILS,
    SELECT_PASSENGERS,
    COMPLETE,
    ERROR
}

/**
 * Seat preference options.
 */
enum class SeatPreference {
    NONE,
    WINDOW,
    AISLE,
    FRONT
}

/**
 * UI state for the Check-In screen.
 */
data class CheckInUiState(
    // Form inputs
    val pnr: String = "",
    val lastName: String = "",
    
    // Loading states
    val isLoading: Boolean = false,
    val isLoadingBoardingPass: Boolean = false,
    
    // Flow step
    val step: CheckInStep = CheckInStep.ENTER_DETAILS,
    
    // Lookup result
    val lookupResult: CheckInLookupResponseDto? = null,
    
    // Selected passengers for check-in
    val selectedPassengerIds: Set<String> = emptySet(),
    
    // Seat preferences per passenger
    val seatPreferences: Map<String, SeatPreference> = emptyMap(),
    
    // Check-in result
    val checkInResult: CheckInResultDto? = null,
    
    // Current boarding pass being viewed
    val currentBoardingPass: BoardingPassDto? = null,
    
    // Error message
    val error: String? = null
) {
    val canProceed: Boolean
        get() = pnr.length == 6 && lastName.isNotBlank()
    
    val eligiblePassengers: List<CheckInPassengerDto>
        get() = lookupResult?.passengers?.filter { !it.isCheckedIn } ?: emptyList()
    
    val alreadyCheckedInPassengers: List<CheckInPassengerDto>
        get() = lookupResult?.passengers?.filter { it.isCheckedIn } ?: emptyList()
}
