package com.fairair.app.ui.screens.manage

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.app.api.*
import com.fairair.contract.dto.BookingModificationsDto
import com.fairair.contract.dto.CancelBookingRequestDto
import com.fairair.contract.dto.CancelBookingResponseDto
import com.fairair.contract.dto.ManageBookingResponseDto
import com.fairair.contract.dto.ModifyBookingRequestDto
import com.fairair.contract.dto.ModifyBookingResponseDto
import com.fairair.contract.dto.PassengerUpdateDto
import com.fairair.contract.dto.RetrieveBookingRequestDto
import com.fairair.app.util.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Manage Booking screen.
 * Handles booking retrieval, modification, and cancellation.
 */
class ManageBookingScreenModel(
    private val apiClient: FairairApiClient
) : ScreenModel {

    private val _uiState = MutableStateFlow(ManageBookingUiState())
    val uiState: StateFlow<ManageBookingUiState> = _uiState.asStateFlow()

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
     * Retrieves booking details.
     */
    fun retrieveBooking() {
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

            when (val result = apiClient.retrieveBooking(state.pnr, state.lastName)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = ManageStep.VIEW_BOOKING,
                            booking = result.data
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
     * Starts the modification flow.
     */
    fun startModification(type: ModificationType) {
        _uiState.update {
            it.copy(
                step = ManageStep.MODIFY,
                modificationType = type
            )
        }
    }

    /**
     * Updates the new flight number for change.
     */
    fun updateNewFlightNumber(flightNumber: String) {
        _uiState.update {
            it.copy(pendingModification = it.pendingModification.copy(newFlightNumber = flightNumber))
        }
    }

    /**
     * Updates the new date for change.
     */
    fun updateNewDate(date: String) {
        _uiState.update {
            it.copy(pendingModification = it.pendingModification.copy(newDepartureDate = date))
        }
    }

    /**
     * Updates passenger name for correction.
     */
    fun updatePassengerName(passengerId: String, firstName: String?, lastName: String?) {
        _uiState.update { state ->
            val currentUpdates = state.pendingModification.passengerUpdates.toMutableList()
            val existingIndex = currentUpdates.indexOfFirst { it.passengerId == passengerId }
            val update = PassengerUpdateDto(passengerId, firstName, lastName)
            
            if (existingIndex >= 0) {
                currentUpdates[existingIndex] = update
            } else {
                currentUpdates.add(update)
            }
            
            state.copy(
                pendingModification = state.pendingModification.copy(
                    passengerUpdates = currentUpdates
                )
            )
        }
    }

    /**
     * Submits the modification request.
     */
    fun submitModification() {
        val state = _uiState.value

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val request = ModifyBookingRequestDto(
                pnr = state.pnr,
                lastName = state.lastName,
                modifications = state.pendingModification
            )

            when (val result = apiClient.modifyBooking(state.pnr, request)) {
                is ApiResult.Success -> {
                    if (result.data.requiresPayment) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                step = ManageStep.PAYMENT_REQUIRED,
                                modificationResult = result.data
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                step = ManageStep.MODIFICATION_COMPLETE,
                                modificationResult = result.data
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
     * Starts the cancellation flow.
     */
    fun startCancellation() {
        _uiState.update { it.copy(step = ManageStep.CONFIRM_CANCEL, showCancelDialog = true) }
    }

    /**
     * Updates cancellation reason.
     */
    fun updateCancelReason(reason: String) {
        _uiState.update { it.copy(cancelReason = reason) }
    }

    /**
     * Confirms and processes the cancellation.
     */
    fun confirmCancellation() {
        val state = _uiState.value

        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, showCancelDialog = false) }

            val request = CancelBookingRequestDto(
                pnr = state.pnr,
                lastName = state.lastName,
                reason = state.cancelReason.takeIf { it.isNotBlank() }
            )

            when (val result = apiClient.cancelBooking(state.pnr, request)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = ManageStep.CANCELLATION_COMPLETE,
                            cancellationResult = result.data
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
     * Dismisses the cancel dialog.
     */
    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false, step = ManageStep.VIEW_BOOKING) }
    }

    /**
     * Goes back to the previous step.
     */
    fun goBack() {
        _uiState.update { state ->
            when (state.step) {
                ManageStep.MODIFY, ManageStep.CONFIRM_CANCEL -> 
                    state.copy(step = ManageStep.VIEW_BOOKING, showCancelDialog = false)
                ManageStep.VIEW_BOOKING -> 
                    ManageBookingUiState(pnr = state.pnr, lastName = state.lastName)
                else -> 
                    ManageBookingUiState()
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Resets to start fresh.
     */
    fun reset() {
        _uiState.update { ManageBookingUiState() }
    }
}

/**
 * Steps in the manage booking flow.
 */
enum class ManageStep {
    ENTER_DETAILS,
    VIEW_BOOKING,
    MODIFY,
    PAYMENT_REQUIRED,
    MODIFICATION_COMPLETE,
    CONFIRM_CANCEL,
    CANCELLATION_COMPLETE
}

/**
 * Types of modifications available.
 */
enum class ModificationType {
    CHANGE_FLIGHT,
    CHANGE_DATE,
    NAME_CORRECTION,
    ADD_ANCILLARY
}

/**
 * UI state for the Manage Booking screen.
 */
data class ManageBookingUiState(
    // Form inputs
    val pnr: String = "",
    val lastName: String = "",
    
    // Loading state
    val isLoading: Boolean = false,
    
    // Flow step
    val step: ManageStep = ManageStep.ENTER_DETAILS,
    
    // Booking details
    val booking: ManageBookingResponseDto? = null,
    
    // Modification state
    val modificationType: ModificationType? = null,
    val pendingModification: BookingModificationsDto = BookingModificationsDto(),
    val modificationResult: ModifyBookingResponseDto? = null,
    
    // Cancellation state
    val showCancelDialog: Boolean = false,
    val cancelReason: String = "",
    val cancellationResult: CancelBookingResponseDto? = null,
    
    // Error message
    val error: String? = null
) {
    val canRetrieve: Boolean
        get() = pnr.length == 6 && lastName.isNotBlank()
    
    val canModify: Boolean
        get() = booking?.allowedActions?.contains("MODIFY") == true
    
    val canCancel: Boolean
        get() = booking?.allowedActions?.contains("CANCEL") == true
    
    val canCheckIn: Boolean
        get() = booking?.allowedActions?.contains("CHECKIN") == true
}
