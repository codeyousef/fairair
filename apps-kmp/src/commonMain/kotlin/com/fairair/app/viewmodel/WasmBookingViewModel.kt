package com.fairair.app.viewmodel

import com.fairair.app.api.*
import com.fairair.app.state.BookingFlowState
import com.fairair.app.state.PassengerInfo
import com.fairair.app.state.SelectedAncillaries
import com.fairair.app.state.SelectedFlight
import com.fairair.app.ui.screens.results.FareFamily
import com.fairair.app.ui.screens.results.VelocityFlightCard
import com.fairair.app.ui.screens.results.VelocityResultsState
import com.fairair.app.ui.screens.results.toVelocityFlightCards
import com.fairair.app.util.toDisplayMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Unified ViewModel for the entire Wasm booking flow.
 * Manages state across all screens: Search -> Results -> Passengers -> Payment -> Confirmation.
 *
 * This replaces the need for multiple Voyager ScreenModels by combining all booking
 * flow state into a single composable-safe ViewModel.
 */
class WasmBookingViewModel(
    private val apiClient: FairairApiClient,
    private val bookingFlowState: BookingFlowState,
    private val scope: CoroutineScope
) {
    // Results state
    private val _resultsState = MutableStateFlow(WasmResultsUiState())
    val resultsState: StateFlow<WasmResultsUiState> = _resultsState.asStateFlow()

    // Seat selection state
    private val _seatSelectionState = MutableStateFlow(WasmSeatSelectionUiState())
    val seatSelectionState: StateFlow<WasmSeatSelectionUiState> = _seatSelectionState.asStateFlow()

    // Passenger form state
    private val _passengerState = MutableStateFlow(WasmPassengerUiState())
    val passengerState: StateFlow<WasmPassengerUiState> = _passengerState.asStateFlow()

    // Payment state
    private val _paymentState = MutableStateFlow(WasmPaymentUiState())
    val paymentState: StateFlow<WasmPaymentUiState> = _paymentState.asStateFlow()

    // Confirmation state
    private val _confirmationState = MutableStateFlow(WasmConfirmationUiState())
    val confirmationState: StateFlow<WasmConfirmationUiState> = _confirmationState.asStateFlow()

    /**
     * Initialize seat selection state for Airbus A320 configuration.
     */
    fun initializeSeatSelection() {
        val searchCriteria = bookingFlowState.searchCriteria ?: return
        val selectedFlight = bookingFlowState.selectedFlight ?: return
        val passengers = searchCriteria.passengers
        val passengerCount = passengers.adults + passengers.children // Infants don't need seats
        
        // Generate A320 seat map (3-3 configuration, rows 1-30)
        // Rows 1-4 are exit rows with extra legroom (premium)
        // Rows 12-13 are exit rows
        val seatMap = generateA320SeatMap()
        
        _seatSelectionState.update {
            WasmSeatSelectionUiState(
                passengerCount = passengerCount,
                selectedSeats = emptyList(),
                seatMap = seatMap,
                flightNumber = selectedFlight.flight.flightNumber,
                originCode = selectedFlight.flight.origin,
                destinationCode = selectedFlight.flight.destination,
                isLoading = false
            )
        }
    }
    
    /**
     * Generate Airbus A320 seat map with typical configuration.
     * 3-3 layout (A-B-C aisle D-E-F), 30 rows
     */
    private fun generateA320SeatMap(): List<SeatRow> {
        val rows = mutableListOf<SeatRow>()
        val occupiedSeats = setOf(
            "1A", "1F", "3B", "5C", "7D", "8A", "10E", "12C", "15A", "15B",
            "18D", "20F", "22A", "24C", "25E", "27B", "28D", "29A", "30F"
        ) // Some random occupied seats
        
        for (rowNum in 1..30) {
            val isExitRow = rowNum in listOf(1, 12, 13)
            val isPremium = rowNum <= 4
            val seats = listOf("A", "B", "C", "D", "E", "F").map { col ->
                val seatId = "$rowNum$col"
                val isOccupied = seatId in occupiedSeats
                val price = when {
                    isPremium -> 75
                    isExitRow -> 50
                    rowNum <= 10 -> 25
                    else -> 0 // Free seats at the back
                }
                Seat(
                    id = seatId,
                    row = rowNum,
                    column = col,
                    isAvailable = !isOccupied,
                    isExitRow = isExitRow,
                    isPremium = isPremium,
                    price = price
                )
            }
            rows.add(SeatRow(rowNum, seats, isExitRow))
        }
        return rows
    }
    
    /**
     * Toggle seat selection for a passenger.
     */
    fun toggleSeatSelection(seat: Seat) {
        if (!seat.isAvailable) return
        
        _seatSelectionState.update { state ->
            val currentSelected = state.selectedSeats.toMutableList()
            
            if (seat.id in currentSelected) {
                // Deselect
                currentSelected.remove(seat.id)
            } else if (currentSelected.size < state.passengerCount) {
                // Select if we haven't reached the passenger limit
                currentSelected.add(seat.id)
            } else {
                // Replace the first selected seat if at limit
                currentSelected.removeAt(0)
                currentSelected.add(seat.id)
            }
            
            // Calculate total seat price
            val totalSeatPrice = state.seatMap.flatMap { it.seats }
                .filter { it.id in currentSelected }
                .sumOf { it.price }
            
            state.copy(
                selectedSeats = currentSelected,
                totalSeatPrice = totalSeatPrice
            )
        }
    }
    
    /**
     * Skip seat selection (keep random assignment).
     */
    fun skipSeatSelection() {
        _seatSelectionState.update {
            it.copy(selectedSeats = emptyList(), totalSeatPrice = 0)
        }
    }

    /**
     * Initialize results state from booking flow state after search completes.
     */
    fun initializeResults() {
        val searchResult = bookingFlowState.searchResult ?: return
        val searchCriteria = bookingFlowState.searchCriteria ?: return

        val velocityFlights = searchResult.flights.toVelocityFlightCards()

        _resultsState.update {
            it.copy(
                isLoading = false,
                flights = searchResult.flights,
                velocityFlights = velocityFlights,
                originCode = searchCriteria.origin.code,
                destinationCode = searchCriteria.destination.code,
                departureDate = searchCriteria.departureDate,
                error = null
            )
        }
    }

    /**
     * Select a flight and optionally expand it to show fare options.
     */
    fun selectFlight(flight: FlightDto, fareFamily: String? = null) {
        _resultsState.update { state ->
            val newExpandedId = if (state.expandedFlightId == flight.flightNumber) null else flight.flightNumber
            state.copy(
                expandedFlightId = newExpandedId,
                selectedFlightNumber = if (fareFamily != null) flight.flightNumber else state.selectedFlightNumber,
                selectedFareFamily = fareFamily ?: state.selectedFareFamily
            )
        }

        if (fareFamily != null) {
            val fare = flight.fares.find { it.fareFamily == fareFamily }
            if (fare != null) {
                bookingFlowState.setSelectedFlight(
                    SelectedFlight(
                        flight = flight,
                        fareFamily = fareFamily,
                        totalPrice = fare.totalPrice
                    )
                )
            }
        }
    }

    /**
     * Select a fare family for the currently expanded flight.
     */
    fun selectFare(fareFamily: FareFamily): Boolean {
        val state = _resultsState.value
        val flight = state.flights.find { it.flightNumber == state.expandedFlightId } ?: return false
        val fare = flight.fares.find { it.fareFamily == fareFamily.displayName } ?: return false

        bookingFlowState.setSelectedFlight(
            SelectedFlight(
                flight = flight,
                fareFamily = fare.fareFamilyCode.ifEmpty { fare.fareFamily },
                totalPrice = fare.totalPrice
            )
        )

        _resultsState.update {
            it.copy(
                selectedFlightNumber = flight.flightNumber,
                selectedFareFamily = fare.fareFamilyCode.ifEmpty { fare.fareFamily }
            )
        }

        return true
    }

    /**
     * Select standby fare for employees - fixed SAR 100 price.
     */
    fun selectStandbyFare(flight: FlightDto): Boolean {
        bookingFlowState.setSelectedFlight(
            SelectedFlight(
                flight = flight,
                fareFamily = "STANDBY",
                totalPrice = "SAR 100"
            )
        )

        _resultsState.update {
            it.copy(
                selectedFlightNumber = flight.flightNumber,
                selectedFareFamily = "STANDBY"
            )
        }

        return true
    }

    /**
     * Initialize passenger forms based on search criteria.
     */
    fun initializePassengerForms() {
        val criteria = bookingFlowState.searchCriteria ?: return
        val passengers = mutableListOf<WasmPassengerForm>()

        // Add adult passengers
        repeat(criteria.passengers.adults) { index ->
            passengers.add(
                WasmPassengerForm(
                    id = "adult_$index",
                    type = "ADULT",
                    label = "Adult ${index + 1}"
                )
            )
        }

        // Add child passengers
        repeat(criteria.passengers.children) { index ->
            passengers.add(
                WasmPassengerForm(
                    id = "child_$index",
                    type = "CHILD",
                    label = "Child ${index + 1}"
                )
            )
        }

        // Add infant passengers
        repeat(criteria.passengers.infants) { index ->
            passengers.add(
                WasmPassengerForm(
                    id = "infant_$index",
                    type = "INFANT",
                    label = "Infant ${index + 1}"
                )
            )
        }

        _passengerState.update {
            it.copy(
                passengers = passengers,
                currentIndex = 0,
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * Update a field in the current passenger's form with input validation.
     */
    fun updatePassengerField(field: PassengerFormField, value: String) {
        _passengerState.update { state ->
            val updatedPassengers = state.passengers.toMutableList()
            val currentPassenger = updatedPassengers.getOrNull(state.currentIndex) ?: return@update state

            val updatedPassenger = when (field) {
                PassengerFormField.TITLE -> currentPassenger.copy(title = value)
                PassengerFormField.FIRST_NAME -> {
                    // Only letters, spaces, hyphens, apostrophes - convert to uppercase
                    val filtered = value.filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' }.uppercase().take(50)
                    currentPassenger.copy(firstName = filtered)
                }
                PassengerFormField.LAST_NAME -> {
                    // Only letters, spaces, hyphens, apostrophes - convert to uppercase
                    val filtered = value.filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' }.uppercase().take(50)
                    currentPassenger.copy(lastName = filtered)
                }
                PassengerFormField.DATE_OF_BIRTH -> currentPassenger.copy(dateOfBirth = value)
                PassengerFormField.NATIONALITY -> {
                    // Only letters, max 2 characters (country code)
                    val filtered = value.filter { it.isLetter() }.uppercase().take(2)
                    currentPassenger.copy(nationality = filtered)
                }
                PassengerFormField.DOCUMENT_TYPE -> currentPassenger.copy(documentType = value)
                PassengerFormField.DOCUMENT_NUMBER -> currentPassenger.copy(documentNumber = formatDocumentNumber(value, currentPassenger.documentType))
                PassengerFormField.DOCUMENT_EXPIRY -> currentPassenger.copy(documentExpiry = value)
                PassengerFormField.EMAIL -> {
                    // Convert to lowercase, allow valid email characters
                    val filtered = value.filter { it.isLetterOrDigit() || it in "@._+-" }.lowercase().take(100)
                    currentPassenger.copy(email = filtered)
                }
                PassengerFormField.PHONE -> currentPassenger.copy(phone = formatPhoneNumber(value))
            }

            updatedPassengers[state.currentIndex] = updatedPassenger
            state.copy(passengers = updatedPassengers, error = null)
        }
    }
    
    /**
     * Formats document number based on document type.
     */
    private fun formatDocumentNumber(input: String, documentType: String): String {
        return when (documentType) {
            "PASSPORT" -> input.uppercase().filter { it.isLetterOrDigit() }.take(20)
            "NATIONAL_ID" -> input.filter { it.isDigit() }.take(10) // Saudi National ID is 10 digits
            "IQAMA" -> input.filter { it.isDigit() }.take(10) // Iqama is 10 digits
            else -> input.uppercase().filter { it.isLetterOrDigit() }.take(20)
        }
    }
    
    /**
     * Formats phone number with digits only, allowing + at start.
     */
    private fun formatPhoneNumber(input: String): String {
        val hasPlus = input.startsWith("+")
        val digits = input.filter { it.isDigit() }.take(15)
        return if (hasPlus) "+$digits" else digits
    }

    /**
     * Navigate to the next passenger form.
     */
    fun nextPassenger() {
        _passengerState.update { state ->
            if (state.currentIndex < state.passengers.size - 1) {
                state.copy(currentIndex = state.currentIndex + 1, error = null)
            } else {
                state
            }
        }
    }

    /**
     * Navigate to the previous passenger form.
     */
    fun previousPassenger() {
        _passengerState.update { state ->
            if (state.currentIndex > 0) {
                state.copy(currentIndex = state.currentIndex - 1, error = null)
            } else {
                state
            }
        }
    }

    /**
     * Validate all passenger forms and return true if valid.
     */
    fun validatePassengers(): Boolean {
        val state = _passengerState.value

        for ((index, passenger) in state.passengers.withIndex()) {
            if (passenger.firstName.isBlank()) {
                _passengerState.update {
                    it.copy(currentIndex = index, error = "First name is required for ${passenger.label}")
                }
                return false
            }
            if (passenger.lastName.isBlank()) {
                _passengerState.update {
                    it.copy(currentIndex = index, error = "Last name is required for ${passenger.label}")
                }
                return false
            }
            if (passenger.dateOfBirth.isBlank()) {
                _passengerState.update {
                    it.copy(currentIndex = index, error = "Date of birth is required for ${passenger.label}")
                }
                return false
            }
            // Validate date format (YYYY-MM-DD)
            if (!isValidDateFormat(passenger.dateOfBirth)) {
                _passengerState.update {
                    it.copy(currentIndex = index, error = "Invalid date format for ${passenger.label}. Use YYYY-MM-DD")
                }
                return false
            }
            // Validate document number if provided
            if (passenger.documentNumber.isNotBlank()) {
                val docError = validateDocumentNumber(passenger.documentNumber, passenger.documentType)
                if (docError != null) {
                    _passengerState.update {
                        it.copy(currentIndex = index, error = "$docError for ${passenger.label}")
                    }
                    return false
                }
            }
            // Validate document expiry if provided
            if (passenger.documentExpiry.isNotBlank() && !isValidDateFormat(passenger.documentExpiry)) {
                _passengerState.update {
                    it.copy(currentIndex = index, error = "Invalid expiry date format for ${passenger.label}. Use YYYY-MM-DD")
                }
                return false
            }
            // Require email and phone for first adult
            if (passenger.id == "adult_0") {
                if (passenger.email.isBlank()) {
                    _passengerState.update {
                        it.copy(currentIndex = index, error = "Email is required for primary contact")
                    }
                    return false
                }
                // Basic email validation
                if (!passenger.email.contains("@") || !passenger.email.contains(".")) {
                    _passengerState.update {
                        it.copy(currentIndex = index, error = "Invalid email format for primary contact")
                    }
                    return false
                }
                if (passenger.phone.isBlank()) {
                    _passengerState.update {
                        it.copy(currentIndex = index, error = "Phone number is required for primary contact")
                    }
                    return false
                }
                // Basic phone validation (at least 7 digits)
                val phoneDigits = passenger.phone.filter { it.isDigit() }
                if (phoneDigits.length < 7) {
                    _passengerState.update {
                        it.copy(currentIndex = index, error = "Phone number must have at least 7 digits")
                    }
                    return false
                }
            }
        }

        // Save validated passenger info
        val passengerInfoList = state.passengers.map { passenger ->
            PassengerInfo(
                id = passenger.id,
                type = passenger.type,
                title = passenger.title,
                firstName = passenger.firstName,
                lastName = passenger.lastName,
                dateOfBirth = passenger.dateOfBirth,
                nationality = passenger.nationality,
                documentType = passenger.documentType,
                documentNumber = passenger.documentNumber,
                documentExpiry = passenger.documentExpiry,
                email = passenger.email,
                phone = passenger.phone
            )
        }
        bookingFlowState.setPassengerInfo(passengerInfoList)

        return true
    }
    
    /**
     * Validates date format is YYYY-MM-DD and the date is valid.
     */
    private fun isValidDateFormat(dateStr: String): Boolean {
        if (dateStr.length != 10) return false
        if (dateStr[4] != '-' || dateStr[7] != '-') return false
        
        val parts = dateStr.split("-")
        if (parts.size != 3) return false
        
        val year = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        val day = parts[2].toIntOrNull() ?: return false
        
        // Basic range validation
        if (year < 1900 || year > 2100) return false
        if (month < 1 || month > 12) return false
        if (day < 1 || day > 31) return false
        
        // Month-specific day validation
        val maxDays = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> return false
        }
        
        return day <= maxDays
    }
    
    /**
     * Validates document number based on document type.
     */
    private fun validateDocumentNumber(docNumber: String, documentType: String): String? {
        return when (documentType) {
            "NATIONAL_ID" -> {
                if (docNumber.length != 10) "National ID must be 10 digits"
                else if (!docNumber.all { it.isDigit() }) "National ID must contain only digits"
                else null
            }
            "IQAMA" -> {
                if (docNumber.length != 10) "Iqama must be 10 digits"
                else if (!docNumber.all { it.isDigit() }) "Iqama must contain only digits"
                else null
            }
            "PASSPORT" -> {
                if (docNumber.length < 5) "Passport number is too short"
                else if (docNumber.length > 20) "Passport number is too long"
                else null
            }
            else -> null
        }
    }

    /**
     * Initialize payment state with booking summary.
     */
    fun initializePayment() {
        val selectedFlight = bookingFlowState.selectedFlight ?: return
        val criteria = bookingFlowState.searchCriteria ?: return
        val passengers = bookingFlowState.passengerInfo

        val totalPassengers = criteria.passengers.adults + criteria.passengers.children + criteria.passengers.infants
        val priceValue = selectedFlight.totalPrice.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
        val totalPrice = priceValue * totalPassengers
        val formattedTotalPrice = formatPrice(totalPrice)

        _paymentState.update {
            it.copy(
                flightNumber = selectedFlight.flight.flightNumber,
                fareFamily = selectedFlight.fareFamily,
                passengerCount = totalPassengers,
                pricePerPerson = selectedFlight.totalPrice,
                totalPrice = formattedTotalPrice,
                currency = "SAR",
                primaryPassengerName = passengers.firstOrNull()?.let { p -> "${p.firstName} ${p.lastName}" } ?: "",
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * Update payment form fields with input validation.
     */
    fun updatePaymentField(field: PaymentFormField, value: String) {
        _paymentState.update { state ->
            when (field) {
                PaymentFormField.CARDHOLDER_NAME -> {
                    // Only allow letters, spaces, hyphens, apostrophes
                    val filtered = value.filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' }.take(50)
                    state.copy(cardholderName = filtered, error = null)
                }
                PaymentFormField.CARD_NUMBER -> {
                    // Only allow digits, max 16
                    val filtered = value.filter { it.isDigit() }.take(16)
                    state.copy(cardNumber = filtered, error = null)
                }
                PaymentFormField.EXPIRY -> {
                    // Handled by VelocityCardExpiryTextField, but filter anyway
                    val filtered = value.filter { it.isDigit() || it == '/' }.take(5)
                    state.copy(expiryDate = filtered, error = null)
                }
                PaymentFormField.CVV -> {
                    // Only allow digits, max 4 (for Amex)
                    val filtered = value.filter { it.isDigit() }.take(4)
                    state.copy(cvv = filtered, error = null)
                }
            }
        }
    }

    /**
     * Process payment and create booking.
     * @param authToken Optional JWT token for authenticated users
     */
    fun processPayment(authToken: String?, onSuccess: () -> Unit) {
        val state = _paymentState.value
        val selectedFlight = bookingFlowState.selectedFlight ?: return
        val passengers = bookingFlowState.passengerInfo
        val primaryPassenger = passengers.firstOrNull() ?: return

        // Validate payment form
        if (state.cardholderName.isBlank()) {
            _paymentState.update { it.copy(error = "Cardholder name is required") }
            return
        }
        if (state.cardNumber.length < 16) {
            _paymentState.update { it.copy(error = "Valid card number is required") }
            return
        }
        if (state.expiryDate.isBlank()) {
            _paymentState.update { it.copy(error = "Expiry date is required") }
            return
        }
        if (state.cvv.length < 3) {
            _paymentState.update { it.copy(error = "Valid CVV is required") }
            return
        }

        _paymentState.update { it.copy(isProcessing = true, error = null) }

        scope.launch {
            val request = BookingRequestDto(
                searchId = bookingFlowState.searchResult?.searchId ?: "",
                flightNumber = selectedFlight.flight.flightNumber,
                fareFamily = selectedFlight.fareFamily,
                passengers = passengers.map { passenger ->
                    PassengerDto(
                        type = passenger.type,
                        title = passenger.title,
                        firstName = passenger.firstName,
                        lastName = passenger.lastName,
                        dateOfBirth = passenger.dateOfBirth,
                        nationality = passenger.nationality,
                        documentId = passenger.documentNumber
                    )
                },
                ancillaries = emptyList(),
                contactEmail = primaryPassenger.email,
                contactPhone = primaryPassenger.phone,
                payment = PaymentDto(
                    cardholderName = state.cardholderName,
                    cardNumberLast4 = state.cardNumber.takeLast(4),
                    totalAmountMinor = ((state.totalPrice.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                    currency = state.currency
                )
            )

            // Use authenticated endpoint if token is present
            when (val result = apiClient.createBookingAuthenticated(request, authToken)) {
                is ApiResult.Success -> {
                    bookingFlowState.setBookingConfirmation(result.data)
                    _paymentState.update { it.copy(isProcessing = false) }
                    onSuccess()
                }
                is ApiResult.Error -> {
                    _paymentState.update {
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
     * Initialize confirmation state from booking confirmation.
     */
    fun initializeConfirmation() {
        val confirmation = bookingFlowState.bookingConfirmation ?: return
        val selectedFlight = bookingFlowState.selectedFlight ?: return
        val criteria = bookingFlowState.searchCriteria ?: return
        val passengers = bookingFlowState.passengerInfo

        _confirmationState.update {
            WasmConfirmationUiState(
                pnr = confirmation.pnr,
                bookingStatus = confirmation.status,
                flightNumber = selectedFlight.flight.flightNumber,
                originCode = criteria.origin.code,
                originCity = criteria.origin.city,
                destinationCode = criteria.destination.code,
                destinationCity = criteria.destination.city,
                departureDate = formatDisplayDate(selectedFlight.flight.departureTime),
                departureTime = formatDisplayTime(selectedFlight.flight.departureTime),
                arrivalTime = formatDisplayTime(selectedFlight.flight.arrivalTime),
                passengerCount = passengers.size,
                primaryPassengerName = passengers.firstOrNull()?.let { "${it.firstName} ${it.lastName}" } ?: "",
                totalPrice = selectedFlight.totalPrice.filter { it.isDigit() || it == '.' },
                currency = confirmation.currency,
                isLoading = false,
                error = null
            )
        }
    }

    /**
     * Reset all state for a new booking.
     */
    fun resetForNewBooking() {
        bookingFlowState.reset()
        _resultsState.value = WasmResultsUiState()
        _passengerState.value = WasmPassengerUiState()
        _paymentState.value = WasmPaymentUiState()
        _confirmationState.value = WasmConfirmationUiState()
    }

    /**
     * Clear any error in the current screen.
     */
    fun clearResultsError() {
        _resultsState.update { it.copy(error = null) }
    }

    fun clearPassengerError() {
        _passengerState.update { it.copy(error = null) }
    }

    fun clearPaymentError() {
        _paymentState.update { it.copy(error = null) }
    }

    /**
     * Format a price value with two decimal places.
     * Uses manual string manipulation since String.format is not available in KMP common.
     */
    private fun formatPrice(value: Double): String {
        val intPart = value.toLong()
        val decimalPart = ((value - intPart) * 100).toLong().let { if (it < 0) -it else it }
        return "$intPart.${decimalPart.toString().padStart(2, '0')}"
    }
    
    /**
     * Formats an ISO datetime string to display time (HH:mm).
     * Expects format like 2025-12-05T03:00:00Z or 2025-12-05T03:00:00
     */
    private fun formatDisplayTime(isoDateTime: String): String {
        return try {
            if (isoDateTime.contains("T")) {
                val timePart = isoDateTime.substringAfter("T")
                timePart.split(":").take(2).joinToString(":")
            } else if (isoDateTime.matches(Regex("\\d{2}:\\d{2}.*"))) {
                isoDateTime.take(5)
            } else {
                isoDateTime
            }
        } catch (e: Exception) {
            isoDateTime
        }
    }
    
    /**
     * Formats an ISO datetime string to display date (e.g., "Fri, Dec 5").
     * Expects format like 2025-12-05T03:00:00Z
     */
    private fun formatDisplayDate(isoDateTime: String): String {
        return try {
            val datePart = if (isoDateTime.contains("T")) {
                isoDateTime.substringBefore("T")
            } else {
                isoDateTime
            }
            
            val parts = datePart.split("-")
            if (parts.size == 3) {
                val year = parts[0].toIntOrNull() ?: return datePart
                val month = parts[1].toIntOrNull() ?: return datePart
                val day = parts[2].toIntOrNull() ?: return datePart
                
                val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                
                // Simple day of week calculation (Zeller's formula simplified)
                val dayOfWeek = calculateDayOfWeek(year, month, day)
                
                "${dayNames[dayOfWeek]}, ${monthNames.getOrElse(month) { "" }} $day"
            } else {
                datePart
            }
        } catch (e: Exception) {
            isoDateTime
        }
    }
    
    /**
     * Calculates day of week (0 = Monday, 6 = Sunday) using Zeller's congruence.
     */
    private fun calculateDayOfWeek(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        if (m < 3) {
            m += 12
            y -= 1
        }
        val k = y % 100
        val j = y / 100
        val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j) % 7
        // Convert from Zeller (0=Sat) to our format (0=Mon)
        return ((h + 5) % 7)
    }
}

/**
 * UI state for results screen.
 */
data class WasmResultsUiState(
    val isLoading: Boolean = true,
    val flights: List<FlightDto> = emptyList(),
    val velocityFlights: List<VelocityFlightCard> = emptyList(),
    val originCode: String = "",
    val destinationCode: String = "",
    val departureDate: String = "",
    val expandedFlightId: String? = null,
    val selectedFlightNumber: String? = null,
    val selectedFareFamily: String? = null,
    val error: String? = null
)

/**
 * UI state for passenger forms.
 */
data class WasmPassengerUiState(
    val passengers: List<WasmPassengerForm> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val currentPassenger: WasmPassengerForm? get() = passengers.getOrNull(currentIndex)
    val isFirstPassenger: Boolean get() = currentIndex == 0
    val isLastPassenger: Boolean get() = currentIndex == passengers.size - 1
    val progress: Float get() = if (passengers.isEmpty()) 0f else (currentIndex + 1).toFloat() / passengers.size
}

/**
 * Form data for a single passenger.
 */
data class WasmPassengerForm(
    val id: String,
    val type: String,
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
 * Passenger form field identifiers.
 */
enum class PassengerFormField {
    TITLE, FIRST_NAME, LAST_NAME, DATE_OF_BIRTH, NATIONALITY,
    DOCUMENT_TYPE, DOCUMENT_NUMBER, DOCUMENT_EXPIRY, EMAIL, PHONE
}

/**
 * UI state for payment screen.
 */
data class WasmPaymentUiState(
    val flightNumber: String = "",
    val fareFamily: String = "",
    val passengerCount: Int = 0,
    val pricePerPerson: String = "",
    val totalPrice: String = "",
    val currency: String = "SAR",
    val primaryPassengerName: String = "",
    val cardholderName: String = "",
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cvv: String = "",
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null
)

/**
 * Payment form field identifiers.
 */
enum class PaymentFormField {
    CARDHOLDER_NAME, CARD_NUMBER, EXPIRY, CVV
}

/**
 * UI state for confirmation screen.
 */
data class WasmConfirmationUiState(
    val pnr: String = "",
    val bookingStatus: String = "",
    val flightNumber: String = "",
    val originCode: String = "",
    val originCity: String = "",
    val destinationCode: String = "",
    val destinationCity: String = "",
    val departureDate: String = "",
    val departureTime: String = "",
    val arrivalTime: String = "",
    val passengerCount: Int = 0,
    val primaryPassengerName: String = "",
    val totalPrice: String = "",
    val currency: String = "SAR",
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * UI state for seat selection screen.
 */
data class WasmSeatSelectionUiState(
    val passengerCount: Int = 1,
    val selectedSeats: List<String> = emptyList(),
    val seatMap: List<SeatRow> = emptyList(),
    val flightNumber: String = "",
    val originCode: String = "",
    val destinationCode: String = "",
    val totalSeatPrice: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Represents a row of seats in the aircraft.
 */
data class SeatRow(
    val rowNumber: Int,
    val seats: List<Seat>,
    val isExitRow: Boolean = false
)

/**
 * Represents an individual seat.
 */
data class Seat(
    val id: String,
    val row: Int,
    val column: String,
    val isAvailable: Boolean = true,
    val isExitRow: Boolean = false,
    val isPremium: Boolean = false,
    val price: Int = 0 // Price in SAR, 0 = free
)
