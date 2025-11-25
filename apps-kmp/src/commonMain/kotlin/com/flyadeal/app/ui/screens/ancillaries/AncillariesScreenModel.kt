package com.flyadeal.app.ui.screens.ancillaries

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.flyadeal.app.state.BookingFlowState
import com.flyadeal.app.state.SelectedAncillaries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Ancillaries screen.
 * Handles selection of extras like baggage and seat selection.
 */
class AncillariesScreenModel(
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(AncillariesUiState())
    val uiState: StateFlow<AncillariesUiState> = _uiState.asStateFlow()

    init {
        loadAncillaries()
    }

    /**
     * Loads available ancillaries.
     */
    private fun loadAncillaries() {
        screenModelScope.launch {
            val passengers = bookingFlowState.passengerInfo
            val selectedFlight = bookingFlowState.selectedFlight

            if (passengers.isEmpty() || selectedFlight == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Booking information not available"
                    )
                }
                return@launch
            }

            // Initialize baggage selections for each passenger
            val baggageSelections = passengers.associate { passenger ->
                passenger.id to BaggageSelection(
                    passengerId = passenger.id,
                    passengerName = "${passenger.firstName} ${passenger.lastName}".ifBlank { passenger.id },
                    checkedBagWeight = 0
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    baggageOptions = getAvailableBaggageOptions(),
                    baggageSelections = baggageSelections,
                    mealOptions = getAvailableMealOptions(),
                    baseFare = selectedFlight.totalPrice
                )
            }
        }
    }

    /**
     * Returns available baggage weight options.
     */
    private fun getAvailableBaggageOptions(): List<BaggageOption> = listOf(
        BaggageOption(weight = 0, price = "0", label = "No checked bag"),
        BaggageOption(weight = 20, price = "75", label = "20 kg"),
        BaggageOption(weight = 25, price = "100", label = "25 kg"),
        BaggageOption(weight = 30, price = "125", label = "30 kg"),
        BaggageOption(weight = 32, price = "150", label = "32 kg (max)")
    )

    /**
     * Returns available meal options.
     */
    private fun getAvailableMealOptions(): List<MealOption> = listOf(
        MealOption(code = "NONE", name = "No meal", price = "0"),
        MealOption(code = "CHML", name = "Chicken Meal", price = "35"),
        MealOption(code = "VGML", name = "Vegetarian Meal", price = "35"),
        MealOption(code = "KSML", name = "Kosher Meal", price = "40"),
        MealOption(code = "BBML", name = "Baby Meal", price = "25")
    )

    /**
     * Selects baggage for a passenger.
     */
    fun selectBaggage(passengerId: String, weight: Int) {
        _uiState.update { state ->
            val updatedSelections = state.baggageSelections.toMutableMap()
            updatedSelections[passengerId] = updatedSelections[passengerId]?.copy(
                checkedBagWeight = weight
            ) ?: BaggageSelection(passengerId, passengerId, weight)

            state.copy(baggageSelections = updatedSelections)
        }
        updateTotalPrice()
    }

    /**
     * Selects a meal for a passenger.
     */
    fun selectMeal(passengerId: String, mealCode: String) {
        _uiState.update { state ->
            val updatedMeals = state.mealSelections.toMutableMap()
            updatedMeals[passengerId] = mealCode
            state.copy(mealSelections = updatedMeals)
        }
        updateTotalPrice()
    }

    /**
     * Toggles priority boarding.
     */
    fun togglePriorityBoarding() {
        _uiState.update { state ->
            state.copy(priorityBoarding = !state.priorityBoarding)
        }
        updateTotalPrice()
    }

    /**
     * Updates the total price based on selections.
     */
    private fun updateTotalPrice() {
        _uiState.update { state ->
            val baggageCost = state.baggageSelections.values.sumOf { selection ->
                state.baggageOptions.find { it.weight == selection.checkedBagWeight }?.price?.toIntOrNull() ?: 0
            }

            val mealCost = state.mealSelections.values.sumOf { mealCode ->
                state.mealOptions.find { it.code == mealCode }?.price?.toIntOrNull() ?: 0
            }

            val priorityCost = if (state.priorityBoarding) 35 else 0

            val baseFareAmount = state.baseFare.replace(",", "").toIntOrNull() ?: 0
            val totalCost = baseFareAmount + baggageCost + mealCost + priorityCost

            state.copy(
                ancillariesTotal = (baggageCost + mealCost + priorityCost).toString(),
                grandTotal = totalCost.toString()
            )
        }
    }

    /**
     * Confirms selections and proceeds to payment.
     */
    fun confirmAndProceed(onConfirmed: () -> Unit) {
        val state = _uiState.value

        bookingFlowState.setSelectedAncillaries(
            SelectedAncillaries(
                baggageSelections = state.baggageSelections.map { (passengerId, selection) ->
                    com.flyadeal.app.state.BaggageInfo(
                        passengerId = passengerId,
                        weight = selection.checkedBagWeight,
                        price = state.baggageOptions.find { it.weight == selection.checkedBagWeight }?.price ?: "0"
                    )
                },
                mealSelections = state.mealSelections.map { (passengerId, mealCode) ->
                    com.flyadeal.app.state.MealInfo(
                        passengerId = passengerId,
                        mealCode = mealCode,
                        price = state.mealOptions.find { it.code == mealCode }?.price ?: "0"
                    )
                },
                priorityBoarding = state.priorityBoarding,
                ancillariesTotal = state.ancillariesTotal,
                grandTotal = state.grandTotal
            )
        )

        onConfirmed()
    }

    /**
     * Clears any error.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Ancillaries screen.
 */
data class AncillariesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val baggageOptions: List<BaggageOption> = emptyList(),
    val baggageSelections: Map<String, BaggageSelection> = emptyMap(),
    val mealOptions: List<MealOption> = emptyList(),
    val mealSelections: Map<String, String> = emptyMap(),
    val priorityBoarding: Boolean = false,
    val baseFare: String = "0",
    val ancillariesTotal: String = "0",
    val grandTotal: String = "0"
)

/**
 * Baggage weight option.
 */
data class BaggageOption(
    val weight: Int,
    val price: String,
    val label: String
)

/**
 * Baggage selection for a passenger.
 */
data class BaggageSelection(
    val passengerId: String,
    val passengerName: String,
    val checkedBagWeight: Int
)

/**
 * Meal option.
 */
data class MealOption(
    val code: String,
    val name: String,
    val price: String
)
