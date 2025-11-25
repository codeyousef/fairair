package com.flyadeal.app.ui.screens.payment

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.flyadeal.app.api.BookingRequestDto
import com.flyadeal.app.api.FlyadealApiClient
import com.flyadeal.app.api.ApiResult
import com.flyadeal.app.api.PassengerDto
import com.flyadeal.app.api.PaymentDto
import com.flyadeal.app.state.BookingFlowState
import com.flyadeal.app.util.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Payment screen.
 * Handles payment form validation and booking submission.
 */
class PaymentScreenModel(
    private val apiClient: FlyadealApiClient,
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        loadPaymentInfo()
    }

    /**
     * Loads payment summary from booking state.
     */
    private fun loadPaymentInfo() {
        screenModelScope.launch {
            val selectedFlight = bookingFlowState.selectedFlight
            val ancillaries = bookingFlowState.selectedAncillaries
            val passengerInfo = bookingFlowState.passengerInfo

            if (selectedFlight == null || passengerInfo.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Booking information not available"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    flightPrice = selectedFlight.totalPrice,
                    ancillariesPrice = ancillaries?.ancillariesTotal ?: "0",
                    totalPrice = ancillaries?.grandTotal ?: selectedFlight.totalPrice,
                    passengerCount = passengerInfo.size
                )
            }
        }
    }

    /**
     * Updates the card number field.
     */
    fun updateCardNumber(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(16)
        _uiState.update {
            it.copy(
                cardNumber = digitsOnly,
                cardNumberError = null
            )
        }
    }

    /**
     * Updates the cardholder name field.
     */
    fun updateCardholderName(value: String) {
        _uiState.update {
            it.copy(
                cardholderName = value,
                cardholderNameError = null
            )
        }
    }

    /**
     * Updates the expiry date field.
     */
    fun updateExpiryDate(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(4)
        _uiState.update {
            it.copy(
                expiryDate = digitsOnly,
                expiryDateError = null
            )
        }
    }

    /**
     * Updates the CVV field.
     */
    fun updateCvv(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(4)
        _uiState.update {
            it.copy(
                cvv = digitsOnly,
                cvvError = null
            )
        }
    }

    /**
     * Formats card number for display (with spaces).
     */
    fun formatCardNumber(number: String): String {
        return number.chunked(4).joinToString(" ")
    }

    /**
     * Formats expiry date for display (MM/YY).
     */
    fun formatExpiryDate(date: String): String {
        return if (date.length >= 2) {
            "${date.take(2)}/${date.drop(2)}"
        } else {
            date
        }
    }

    /**
     * Detects card type from number.
     */
    fun detectCardType(number: String): CardType {
        return when {
            number.startsWith("4") -> CardType.VISA
            number.startsWith("5") || number.startsWith("2") -> CardType.MASTERCARD
            number.startsWith("3") -> CardType.AMEX
            else -> CardType.UNKNOWN
        }
    }

    /**
     * Validates and processes the payment.
     */
    fun processPayment(onSuccess: () -> Unit) {
        val state = _uiState.value
        var hasError = false

        // Validate card number using Luhn algorithm
        if (state.cardNumber.length < 13) {
            _uiState.update { it.copy(cardNumberError = "Card number is too short") }
            hasError = true
        } else if (!isValidLuhn(state.cardNumber)) {
            _uiState.update { it.copy(cardNumberError = "Invalid card number") }
            hasError = true
        }

        // Validate cardholder name
        if (state.cardholderName.isBlank()) {
            _uiState.update { it.copy(cardholderNameError = "Cardholder name is required") }
            hasError = true
        }

        // Validate expiry date
        if (state.expiryDate.length != 4) {
            _uiState.update { it.copy(expiryDateError = "Invalid expiry date") }
            hasError = true
        } else {
            val month = state.expiryDate.take(2).toIntOrNull() ?: 0
            if (month < 1 || month > 12) {
                _uiState.update { it.copy(expiryDateError = "Invalid month") }
                hasError = true
            }
        }

        // Validate CVV
        if (state.cvv.length < 3) {
            _uiState.update { it.copy(cvvError = "CVV is required") }
            hasError = true
        }

        if (hasError) return

        // Process payment
        screenModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            val criteria = bookingFlowState.searchCriteria
            val selectedFlight = bookingFlowState.selectedFlight
            val passengers = bookingFlowState.passengerInfo
            val ancillaries = bookingFlowState.selectedAncillaries

            if (criteria == null || selectedFlight == null || passengers.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Booking information not available"
                    )
                }
                return@launch
            }

            val bookingRequest = BookingRequestDto(
                flightNumber = selectedFlight.flight.flightNumber,
                fareFamily = selectedFlight.fareFamily,
                passengers = passengers.map { p ->
                    PassengerDto(
                        type = p.type,
                        title = p.title,
                        firstName = p.firstName,
                        lastName = p.lastName,
                        dateOfBirth = p.dateOfBirth,
                        nationality = p.nationality,
                        documentType = p.documentType,
                        documentNumber = p.documentNumber,
                        documentExpiry = p.documentExpiry
                    )
                },
                contactEmail = passengers.firstOrNull()?.email ?: "",
                contactPhone = passengers.firstOrNull()?.phone ?: "",
                payment = PaymentDto(
                    cardholderName = state.cardholderName,
                    cardNumberLast4 = state.cardNumber.takeLast(4),
                    totalAmountMinor = (state.totalPrice.toDoubleOrNull() ?: 0.0).toLong() * 100,
                    currency = "SAR"
                )
            )

            when (val result = apiClient.createBooking(bookingRequest)) {
                is ApiResult.Success -> {
                    bookingFlowState.setBookingConfirmation(result.data)
                    _uiState.update { it.copy(isProcessing = false) }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = result.toDisplayMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Validates card number using Luhn algorithm.
     * This is the industry-standard checksum validation for credit cards.
     */
    private fun isValidLuhn(cardNumber: String): Boolean {
        if (cardNumber.isEmpty()) return false

        var sum = 0
        var isSecondDigit = false

        for (i in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[i].digitToIntOrNull() ?: return false

            if (isSecondDigit) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
            isSecondDigit = !isSecondDigit
        }

        return sum % 10 == 0
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Payment screen.
 */
data class PaymentUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val flightPrice: String = "0",
    val ancillariesPrice: String = "0",
    val totalPrice: String = "0",
    val passengerCount: Int = 0,
    val cardNumber: String = "",
    val cardNumberError: String? = null,
    val cardholderName: String = "",
    val cardholderNameError: String? = null,
    val expiryDate: String = "",
    val expiryDateError: String? = null,
    val cvv: String = "",
    val cvvError: String? = null
) {
    val isFormValid: Boolean
        get() = cardNumber.length >= 13 &&
                cardholderName.isNotBlank() &&
                expiryDate.length == 4 &&
                cvv.length >= 3 &&
                cardNumberError == null &&
                cardholderNameError == null &&
                expiryDateError == null &&
                cvvError == null
}

/**
 * Card types for display.
 */
enum class CardType {
    VISA,
    MASTERCARD,
    AMEX,
    UNKNOWN
}
