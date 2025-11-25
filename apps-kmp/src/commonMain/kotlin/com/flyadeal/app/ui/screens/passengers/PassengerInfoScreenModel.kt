package com.flyadeal.app.ui.screens.passengers

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.flyadeal.app.state.BookingFlowState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Passenger Info screen.
 * Handles passenger data entry and validation.
 */
class PassengerInfoScreenModel(
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(PassengerInfoUiState())
    val uiState: StateFlow<PassengerInfoUiState> = _uiState.asStateFlow()

    init {
        initializePassengers()
    }

    /**
     * Initializes passenger forms based on search criteria.
     */
    private fun initializePassengers() {
        screenModelScope.launch {
            val criteria = bookingFlowState.searchCriteria
            if (criteria == null) {
                _uiState.update { it.copy(error = "Search criteria not available") }
                return@launch
            }

            val passengers = mutableListOf<PassengerFormData>()

            // Add adult passengers
            repeat(criteria.passengers.adults) { index ->
                passengers.add(
                    PassengerFormData(
                        id = "adult_$index",
                        type = PassengerType.ADULT,
                        label = "Adult ${index + 1}"
                    )
                )
            }

            // Add child passengers
            repeat(criteria.passengers.children) { index ->
                passengers.add(
                    PassengerFormData(
                        id = "child_$index",
                        type = PassengerType.CHILD,
                        label = "Child ${index + 1}"
                    )
                )
            }

            // Add infant passengers
            repeat(criteria.passengers.infants) { index ->
                passengers.add(
                    PassengerFormData(
                        id = "infant_$index",
                        type = PassengerType.INFANT,
                        label = "Infant ${index + 1}"
                    )
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    passengers = passengers,
                    currentPassengerIndex = 0
                )
            }
        }
    }

    /**
     * Updates a passenger field value.
     */
    fun updatePassengerField(passengerId: String, field: PassengerField, value: String) {
        _uiState.update { state ->
            val updatedPassengers = state.passengers.map { passenger ->
                if (passenger.id == passengerId) {
                    when (field) {
                        PassengerField.TITLE -> passenger.copy(title = value)
                        PassengerField.FIRST_NAME -> passenger.copy(firstName = value)
                        PassengerField.LAST_NAME -> passenger.copy(lastName = value)
                        PassengerField.DATE_OF_BIRTH -> passenger.copy(dateOfBirth = value)
                        PassengerField.NATIONALITY -> passenger.copy(nationality = value)
                        PassengerField.DOCUMENT_TYPE -> passenger.copy(documentType = value)
                        PassengerField.DOCUMENT_NUMBER -> passenger.copy(documentNumber = value)
                        PassengerField.DOCUMENT_EXPIRY -> passenger.copy(documentExpiry = value)
                        PassengerField.EMAIL -> passenger.copy(email = value)
                        PassengerField.PHONE -> passenger.copy(phone = value)
                    }
                } else {
                    passenger
                }
            }
            state.copy(passengers = updatedPassengers, error = null)
        }
    }

    /**
     * Moves to the next passenger form.
     */
    fun nextPassenger() {
        _uiState.update { state ->
            val currentIndex = state.currentPassengerIndex
            if (currentIndex < state.passengers.size - 1) {
                state.copy(currentPassengerIndex = currentIndex + 1)
            } else {
                state
            }
        }
    }

    /**
     * Moves to the previous passenger form.
     */
    fun previousPassenger() {
        _uiState.update { state ->
            val currentIndex = state.currentPassengerIndex
            if (currentIndex > 0) {
                state.copy(currentPassengerIndex = currentIndex - 1)
            } else {
                state
            }
        }
    }

    /**
     * Navigates to a specific passenger by index.
     */
    fun goToPassenger(index: Int) {
        _uiState.update { state ->
            if (index in state.passengers.indices) {
                state.copy(currentPassengerIndex = index)
            } else {
                state
            }
        }
    }

    /**
     * Validates all passengers and proceeds if valid.
     */
    fun validateAndProceed(onValid: () -> Unit) {
        val state = _uiState.value
        val validationErrors = mutableListOf<String>()

        state.passengers.forEachIndexed { index, passenger ->
            val errors = validatePassenger(passenger)
            if (errors.isNotEmpty()) {
                validationErrors.add("${passenger.label}: ${errors.joinToString(", ")}")
            }
        }

        if (validationErrors.isNotEmpty()) {
            _uiState.update {
                it.copy(error = validationErrors.first())
            }
            // Navigate to first passenger with error
            val firstErrorIndex = state.passengers.indexOfFirst { validatePassenger(it).isNotEmpty() }
            if (firstErrorIndex >= 0) {
                goToPassenger(firstErrorIndex)
            }
            return
        }

        // Store passenger info in booking flow state
        bookingFlowState.setPassengerInfo(
            state.passengers.map { form ->
                com.flyadeal.app.state.PassengerInfo(
                    id = form.id,
                    type = form.type.name,
                    title = form.title,
                    firstName = form.firstName,
                    lastName = form.lastName,
                    dateOfBirth = form.dateOfBirth,
                    nationality = form.nationality,
                    documentType = form.documentType,
                    documentNumber = form.documentNumber,
                    documentExpiry = form.documentExpiry,
                    email = form.email,
                    phone = form.phone
                )
            }
        )

        onValid()
    }

    /**
     * Validates a single passenger form.
     */
    private fun validatePassenger(passenger: PassengerFormData): List<String> {
        val errors = mutableListOf<String>()

        if (passenger.title.isBlank()) {
            errors.add("Title is required")
        }
        if (passenger.firstName.isBlank()) {
            errors.add("First name is required")
        }
        if (passenger.lastName.isBlank()) {
            errors.add("Last name is required")
        }
        if (passenger.dateOfBirth.isBlank()) {
            errors.add("Date of birth is required")
        }

        // Only validate contact info for first adult passenger
        if (passenger.type == PassengerType.ADULT && passenger.id == "adult_0") {
            if (passenger.email.isBlank()) {
                errors.add("Email is required")
            } else if (!isValidEmail(passenger.email)) {
                errors.add("Invalid email format")
            }
            if (passenger.phone.isBlank()) {
                errors.add("Phone number is required")
            }
        }

        return errors
    }

    /**
     * Basic email validation.
     */
    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Passenger Info screen.
 */
data class PassengerInfoUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val passengers: List<PassengerFormData> = emptyList(),
    val currentPassengerIndex: Int = 0
) {
    val currentPassenger: PassengerFormData?
        get() = passengers.getOrNull(currentPassengerIndex)

    val isFirstPassenger: Boolean
        get() = currentPassengerIndex == 0

    val isLastPassenger: Boolean
        get() = currentPassengerIndex == passengers.size - 1

    val progress: Float
        get() = if (passengers.isEmpty()) 0f else (currentPassengerIndex + 1).toFloat() / passengers.size
}

/**
 * Form data for a single passenger.
 */
data class PassengerFormData(
    val id: String,
    val type: PassengerType,
    val label: String,
    val title: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val dateOfBirth: String = "",
    val nationality: String = "SA",
    val documentType: String = "PASSPORT",
    val documentNumber: String = "",
    val documentExpiry: String = "",
    val email: String = "",
    val phone: String = ""
)

/**
 * Passenger types with age ranges.
 */
enum class PassengerType(val ageRange: String) {
    ADULT("12+ years"),
    CHILD("2-11 years"),
    INFANT("Under 2 years")
}

/**
 * Fields in the passenger form.
 */
enum class PassengerField {
    TITLE,
    FIRST_NAME,
    LAST_NAME,
    DATE_OF_BIRTH,
    NATIONALITY,
    DOCUMENT_TYPE,
    DOCUMENT_NUMBER,
    DOCUMENT_EXPIRY,
    EMAIL,
    PHONE
}
