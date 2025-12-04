package com.fairair.app.ui.screens.results

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.localization.LocalLocalization
import com.fairair.app.localization.LocalStrings
import com.fairair.app.navigation.AppScreen
import com.fairair.app.ui.screens.passengers.PassengerInfoScreen
import com.fairair.app.ui.screens.search.SearchScreenModel
import com.fairair.app.ui.theme.VelocityThemeWithBackground

/**
 * Velocity-styled results screen that displays the glassmorphic results overlay.
 *
 * Shows flight search results in a slide-up overlay with glassmorphic cards.
 * Supports expandable flight cards with fare selection.
 */
class VelocityResultsScreen : Screen, AppScreen.Results {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<ResultsScreenModel>()
        val searchScreenModel = getScreenModel<SearchScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val velocityState by searchScreenModel.velocityState.collectAsState()
        val localization = LocalLocalization.current
        val strings = LocalStrings.current

        // Convert FlightDto to VelocityFlightCard
        val velocityFlights = remember(uiState.flights) {
            uiState.flights.toVelocityFlightCards()
        }

        // Build VelocityResultsState
        val resultsState = VelocityResultsState(
            isVisible = true,
            isLoading = uiState.isLoading,
            flights = velocityFlights,
            expandedFlightId = uiState.selectedFlightId,
            selectedFare = null, // Will be set when fare is selected
            error = uiState.error
        )

        VelocityThemeWithBackground(
            isRtl = localization.isRtl,
            destinationTheme = velocityState.destinationBackground
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                // Results overlay aligned to bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    VelocityResultsOverlay(
                        state = resultsState,
                        originCode = uiState.originCode,
                        destinationCode = uiState.destinationCode,
                        formattedDate = uiState.departureDate,
                        onFlightClick = { velocityFlight ->
                            // Find original FlightDto
                            val flight = uiState.flights.find { it.flightNumber == velocityFlight.id }
                            if (flight != null && flight.fares.isNotEmpty()) {
                                // Toggle expansion or select first fare
                                val defaultFare = flight.fares.first()
                                screenModel.selectFlight(flight, defaultFare.fareFamilyCode.ifEmpty { defaultFare.fareFamily }, defaultFare.totalPrice)
                            }
                        },
                        onFareSelect = { fareFamily ->
                            // Find the selected flight and update with selected fare
                            val selectedFlight = uiState.flights.find { it.flightNumber == uiState.selectedFlightId }
                            if (selectedFlight != null) {
                                val fare = selectedFlight.fares.find { it.fareFamily == fareFamily.displayName }
                                if (fare != null) {
                                    screenModel.selectFlight(selectedFlight, fare.fareFamilyCode.ifEmpty { fare.fareFamily }, fare.totalPrice)
                                    // Navigate to next screen
                                    screenModel.confirmSelection {
                                        navigator.push(PassengerInfoScreen())
                                    }
                                }
                            }
                        },
                        onClose = { navigator.pop() },
                        onRetry = { screenModel.retry() },
                        strings = strings
                    )
                }
            }
        }
    }
}
