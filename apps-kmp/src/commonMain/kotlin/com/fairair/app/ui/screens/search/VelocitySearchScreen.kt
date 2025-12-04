package com.fairair.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.fairair.app.api.StationDto
import com.fairair.app.localization.AppStrings
import com.fairair.app.ui.components.velocity.*
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme
import com.fairair.app.ui.theme.VelocityThemeWithBackground
import kotlinx.datetime.LocalDate

/**
 * Velocity-styled search screen with natural language sentence builder.
 *
 * Features:
 * - Deep purple gradient background
 * - Natural language search sentence with tappable fields
 * - Bottom sheets for airport, date, and passenger selection
 * - Circular launch button with glow effect
 * - RTL support for Arabic
 * - Low-fare calendar with prices on date picker
 * - Round-trip support with return date
 *
 * @param state Current search state
 * @param strings Localized strings
 * @param isRtl Whether to use RTL layout
 * @param onOriginSelect Callback when origin is selected
 * @param onDestinationSelect Callback when destination is selected
 * @param onDateSelect Callback when departure date is selected
 * @param onReturnDateSelect Callback when return date is selected
 * @param onPassengerSelect Callback when passenger count is selected
 * @param onTripTypeChange Callback when trip type is changed
 * @param onFieldActivate Callback when a field is tapped
 * @param onSearch Callback when search is initiated
 * @param onNavigateToSettings Callback to navigate to settings
 * @param onNavigateToSavedBookings Callback to navigate to saved bookings
 * @param onMonthChange Callback when month changes in date picker (for fetching low fares)
 * @param onReturnMonthChange Callback when month changes in return date picker
 */
@Composable
fun VelocitySearchScreen(
    state: VelocitySearchState,
    strings: AppStrings,
    isRtl: Boolean,
    onBack: (() -> Unit)? = null,
    onOriginSelect: (StationDto) -> Unit,
    onDestinationSelect: (StationDto) -> Unit,
    onDateSelect: (LocalDate) -> Unit,
    onReturnDateSelect: ((LocalDate) -> Unit)? = null,
    onPassengerSelect: (PassengerCounts) -> Unit,
    onTripTypeChange: ((TripType) -> Unit)? = null,
    onFieldActivate: (SearchField?) -> Unit,
    onSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSavedBookings: () -> Unit,
    onMonthChange: ((year: Int, month: Int) -> Unit)? = null,
    onReturnMonthChange: ((year: Int, month: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    VelocityThemeWithBackground(
        isRtl = isRtl,
        destinationTheme = state.destinationBackground
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Main content
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                VelocitySearchHeader(
                    onBack = onBack,
                    onSettingsClick = onNavigateToSettings,
                    onSavedBookingsClick = onNavigateToSavedBookings
                )

                Spacer(modifier = Modifier.weight(0.1f))

                // Hero title
                Text(
                    text = strings.appName,
                    style = VelocityTheme.typography.heroTitle,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Sentence builder
                if (isRtl) {
                    SentenceBuilderArabic(
                        originValue = state.selectedOrigin?.city,
                        destinationValue = state.selectedDestination?.city,
                        dateValue = if (state.departureDate != null) state.formattedDateArabic else null,
                        returnDateValue = if (state.returnDate != null) state.formattedReturnDateArabic else null,
                        passengerValue = state.passengerLabelArabic,
                        tripType = state.tripType,
                        activeField = state.activeField,
                        onFieldClick = onFieldActivate,
                        onTripTypeChange = onTripTypeChange,
                        strings = strings
                    )
                } else {
                    SentenceBuilder(
                        originValue = state.selectedOrigin?.city,
                        destinationValue = state.selectedDestination?.city,
                        dateValue = if (state.departureDate != null) state.formattedDate else null,
                        returnDateValue = if (state.returnDate != null) state.formattedReturnDate else null,
                        passengerValue = state.passengerLabel,
                        tripType = state.tripType,
                        activeField = state.activeField,
                        onFieldClick = onFieldActivate,
                        onTripTypeChange = onTripTypeChange,
                        strings = strings
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Launch button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LaunchButton(
                        enabled = state.isSearchEnabled,
                        loading = state.isSearching,
                        onClick = onSearch
                    )
                }
            }

            // Error display
            if (state.error != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = VelocityColors.Error.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = state.error,
                        style = VelocityTheme.typography.body.copy(
                            color = VelocityColors.TextMain
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        // Selection sheets
        AirportSelectionBottomSheet(
            isVisible = state.activeField == SearchField.ORIGIN,
            title = strings.selectOrigin,
            stations = state.availableOrigins,
            selectedCode = state.selectedOrigin?.code,
            onSelect = { station ->
                onOriginSelect(station)
                onFieldActivate(null)
            },
            onDismiss = { onFieldActivate(null) }
        )

        AirportSelectionBottomSheet(
            isVisible = state.activeField == SearchField.DESTINATION,
            title = strings.selectDestination,
            stations = state.availableDestinations.ifEmpty { state.availableOrigins },
            selectedCode = state.selectedDestination?.code,
            onSelect = { station ->
                onDestinationSelect(station)
                onFieldActivate(null)
            },
            onDismiss = { onFieldActivate(null) }
        )

        DateSelectionBottomSheet(
            isVisible = state.activeField == SearchField.DATE,
            title = strings.selectDate,
            selectedDate = state.departureDate,
            lowFares = state.lowFares,
            isLoadingPrices = state.loadingLowFares,
            onSelect = { date ->
                onDateSelect(date)
                onFieldActivate(null)
            },
            onDismiss = { onFieldActivate(null) },
            onMonthChange = onMonthChange
        )

        // Return date selection sheet (for round-trip)
        if (state.tripType == TripType.ROUND_TRIP && onReturnDateSelect != null) {
            DateSelectionBottomSheet(
                isVisible = state.activeField == SearchField.RETURN_DATE,
                title = strings.velocitySelectReturnDate,
                selectedDate = state.returnDate,
                minDate = state.departureDate,
                lowFares = state.returnLowFares,
                isLoadingPrices = state.loadingReturnLowFares,
                onSelect = { date ->
                    onReturnDateSelect(date)
                    onFieldActivate(null)
                },
                onDismiss = { onFieldActivate(null) },
                onMonthChange = onReturnMonthChange
            )
        }

        PassengerSelectionBottomSheet(
            isVisible = state.activeField == SearchField.PASSENGERS,
            title = strings.passengers,
            currentAdults = state.adultsCount,
            currentChildren = state.childrenCount,
            currentInfants = state.infantsCount,
            strings = strings,
            onSelect = { counts ->
                onPassengerSelect(counts)
                onFieldActivate(null)
            },
            onDismiss = { onFieldActivate(null) }
        )
    }
}

@Composable
private fun VelocitySearchHeader(
    onBack: (() -> Unit)?,
    onSettingsClick: () -> Unit,
    onSavedBookingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button or Saved bookings
        Row {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = VelocityColors.TextMuted
                    )
                }
            }
            IconButton(onClick = onSavedBookingsClick) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Saved Bookings",
                    tint = VelocityColors.TextMuted
                )
            }
        }

        // Settings button
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = VelocityColors.TextMuted
            )
        }
    }
}

/**
 * Loading state for the Velocity search screen.
 */
@Composable
fun VelocitySearchScreenLoading(
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    // Simplified loading screen without custom fonts to avoid Wasm rendering issues
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VelocityColors.BackgroundDeep),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = VelocityColors.Accent,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Error state for the Velocity search screen.
 */
@Composable
fun VelocitySearchScreenError(
    message: String,
    onRetry: () -> Unit,
    isRtl: Boolean,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    VelocityThemeWithBackground(isRtl = isRtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
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

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent,
                        contentColor = VelocityColors.BackgroundDeep
                    )
                ) {
                    Text(
                        text = strings.retry,
                        style = VelocityTheme.typography.button
                    )
                }
            }
        }
    }
}
