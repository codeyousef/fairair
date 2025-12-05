package com.fairair.app.ui.screens.results

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fairair.contract.dto.FlightDto
import com.fairair.app.localization.AppStrings
import com.fairair.app.ui.components.velocity.*
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Slide-up overlay displaying flight search results in glassmorphic cards.
 *
 * Features:
 * - Slides up from bottom with spring animation
 * - Glassmorphic background with blur
 * - Responsive grid layout for flight cards
 * - Header with route info and close button
 * - Loading, empty, and error states
 * - Employee mode with standby fare option
 *
 * @param state Current results state
 * @param originCode Origin airport code for display
 * @param destinationCode Destination airport code for display
 * @param formattedDate Formatted departure date for display
 * @param isEmployee Whether user is logged in as employee (shows standby option)
 * @param flights Raw flight DTOs with seat availability
 * @param onFlightClick Callback when a flight card is tapped
 * @param onFareSelect Callback when a fare is selected
 * @param onStandbySelect Callback when standby fare is selected (employee only)
 * @param onClose Callback to close the overlay
 * @param onRetry Callback to retry on error
 * @param strings Localized strings
 * @param modifier Modifier to apply to the component
 */
@Composable
fun VelocityResultsOverlay(
    state: VelocityResultsState,
    originCode: String,
    destinationCode: String,
    formattedDate: String,
    isEmployee: Boolean = false,
    flights: List<FlightDto> = emptyList(),
    onFlightClick: (VelocityFlightCard) -> Unit,
    onFareSelect: (FareFamily) -> Unit,
    onStandbySelect: (FlightDto) -> Unit = {},
    onClose: () -> Unit,
    onRetry: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag handle
                ResultsDragHandle()

                // Header
                ResultsHeader(
                    originCode = originCode,
                    destinationCode = destinationCode,
                    formattedDate = formattedDate,
                    resultCount = state.flights.size,
                    onClose = onClose
                )

                // Content
                when {
                    state.isLoading -> {
                        ResultsLoadingContent()
                    }
                    state.error != null -> {
                        ResultsErrorContent(
                            message = state.error,
                            onRetry = onRetry,
                            strings = strings
                        )
                    }
                    state.flights.isEmpty() -> {
                        ResultsEmptyContent(strings = strings)
                    }
                    else -> {
                        ResultsFlightGrid(
                            flights = state.flights,
                            flightDtos = flights,
                            expandedFlightId = state.expandedFlightId,
                            selectedFare = state.selectedFare,
                            isEmployee = isEmployee,
                            onFlightClick = onFlightClick,
                            onFareSelect = onFareSelect,
                            onStandbySelect = onStandbySelect
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grid of flight cards with responsive layout.
 */
@Composable
private fun ResultsFlightGrid(
    flights: List<VelocityFlightCard>,
    flightDtos: List<FlightDto>,
    expandedFlightId: String?,
    selectedFare: FareFamily?,
    isEmployee: Boolean,
    onFlightClick: (VelocityFlightCard) -> Unit,
    onFareSelect: (FareFamily) -> Unit,
    onStandbySelect: (FlightDto) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(
            items = flights,
            key = { it.id }
        ) { flight ->
            val flightDto = flightDtos.find { it.flightNumber == flight.id }
            VelocityFlightCardView(
                flight = flight,
                isExpanded = flight.id == expandedFlightId,
                selectedFare = if (flight.id == expandedFlightId) selectedFare else null,
                isEmployee = isEmployee,
                seatsAvailable = flightDto?.seatsAvailable ?: 0,
                seatsBooked = flightDto?.seatsBooked ?: 0,
                onClick = { onFlightClick(flight) },
                onFareSelect = onFareSelect,
                onStandbySelect = { 
                    flightDto?.let { onStandbySelect(it) }
                }
            )
        }
    }
}

/**
 * Loading state content.
 */
@Composable
private fun ResultsLoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = VelocityColors.Accent,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Finding flights...",
                style = VelocityTheme.typography.body,
                color = VelocityColors.TextMuted
            )
        }
    }
}

/**
 * Empty state content when no flights found.
 */
@Composable
private fun ResultsEmptyContent(
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = strings.noFlightsAvailable,
                style = VelocityTheme.typography.body,
                color = VelocityColors.TextMain
            )
            Text(
                text = strings.tryDifferentDate,
                style = VelocityTheme.typography.labelSmall,
                color = VelocityColors.TextMuted
            )
        }
    }
}

/**
 * Error state content with retry button.
 */
@Composable
private fun ResultsErrorContent(
    message: String,
    onRetry: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = VelocityTheme.typography.body,
                color = VelocityColors.Error
            )
            GlassPill(
                onClick = onRetry
            ) {
                Text(
                    text = strings.retry,
                    style = VelocityTheme.typography.button,
                    color = VelocityColors.Accent
                )
            }
        }
    }
}

/**
 * Skeleton loading state with placeholder cards.
 */
@Composable
fun VelocityResultsOverlaySkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(VelocityColors.BackgroundDeep.copy(alpha = 0.95f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ResultsDragHandle()

            Spacer(modifier = Modifier.height(48.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) {
                    VelocityFlightCardSkeleton()
                }
            }
        }
    }
}
