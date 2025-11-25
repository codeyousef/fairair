package com.flyadeal.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.flyadeal.app.api.StationDto
import com.flyadeal.app.navigation.AppScreen
import com.flyadeal.app.ui.components.*
import com.flyadeal.app.ui.screens.results.ResultsScreen
import com.flyadeal.app.ui.screens.saved.SavedBookingsScreen
import com.flyadeal.app.ui.screens.settings.SettingsScreen
import com.flyadeal.app.ui.theme.FlyadealColors

/**
 * Search screen - entry point of the booking flow.
 * Allows users to select origin, destination, date, and passengers.
 */
class SearchScreen : Screen, AppScreen.Search {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<SearchScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        SearchScreenContent(
            uiState = uiState,
            onSelectOrigin = screenModel::selectOrigin,
            onSelectDestination = screenModel::selectDestination,
            onSwapAirports = screenModel::swapAirports,
            onDateChange = screenModel::setDepartureDate,
            onIncrementAdults = screenModel::incrementAdults,
            onDecrementAdults = screenModel::decrementAdults,
            onIncrementChildren = screenModel::incrementChildren,
            onDecrementChildren = screenModel::decrementChildren,
            onIncrementInfants = screenModel::incrementInfants,
            onDecrementInfants = screenModel::decrementInfants,
            onSearch = {
                screenModel.search {
                    navigator.push(ResultsScreen())
                }
            },
            onRetry = screenModel::retry,
            onClearError = screenModel::clearError,
            onNavigateToSettings = { navigator.push(SettingsScreen()) },
            onNavigateToSavedBookings = { navigator.push(SavedBookingsScreen()) }
        )
    }
}

@Composable
private fun SearchScreenContent(
    uiState: SearchUiState,
    onSelectOrigin: (StationDto) -> Unit,
    onSelectDestination: (StationDto) -> Unit,
    onSwapAirports: () -> Unit,
    onDateChange: (String) -> Unit,
    onIncrementAdults: () -> Unit,
    onDecrementAdults: () -> Unit,
    onIncrementChildren: () -> Unit,
    onDecrementChildren: () -> Unit,
    onIncrementInfants: () -> Unit,
    onDecrementInfants: () -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSavedBookings: () -> Unit
) {
    var showOriginPicker by remember { mutableStateOf(false) }
    var showDestinationPicker by remember { mutableStateOf(false) }
    var showPassengerPicker by remember { mutableStateOf(false) }

    when {
        uiState.isLoading -> {
            LoadingIndicator(message = "Loading airports...")
        }
        uiState.error != null && uiState.stations.isEmpty() -> {
            ErrorDisplay(
                message = uiState.error,
                onRetry = onRetry
            )
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header
                SearchHeader(
                    onSettingsClick = onNavigateToSettings,
                    onSavedBookingsClick = onNavigateToSavedBookings
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Airport Selection
                    item {
                        AirportSelectionCard(
                            origin = uiState.selectedOrigin,
                            destination = uiState.selectedDestination,
                            onOriginClick = { showOriginPicker = true },
                            onDestinationClick = { showDestinationPicker = true },
                            onSwapClick = onSwapAirports
                        )
                    }

                    // Date Selection
                    item {
                        DateSelectionCard(
                            selectedDate = uiState.departureDate,
                            onDateChange = onDateChange
                        )
                    }

                    // Passenger Selection
                    item {
                        PassengerSelectionCard(
                            adults = uiState.adults,
                            children = uiState.children,
                            infants = uiState.infants,
                            onClick = { showPassengerPicker = true }
                        )
                    }

                    // Error message
                    if (uiState.error != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = onClearError) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Search Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    PrimaryButton(
                        text = "Search Flights",
                        onClick = onSearch,
                        enabled = uiState.canSearch,
                        loading = uiState.isSearching,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Airport Pickers
    if (showOriginPicker) {
        AirportPickerDialog(
            title = "Select Origin",
            stations = uiState.stations,
            selectedCode = uiState.selectedOrigin?.code,
            onSelect = {
                onSelectOrigin(it)
                showOriginPicker = false
            },
            onDismiss = { showOriginPicker = false }
        )
    }

    if (showDestinationPicker) {
        AirportPickerDialog(
            title = "Select Destination",
            stations = uiState.availableDestinations.ifEmpty { uiState.stations },
            selectedCode = uiState.selectedDestination?.code,
            onSelect = {
                onSelectDestination(it)
                showDestinationPicker = false
            },
            onDismiss = { showDestinationPicker = false }
        )
    }

    // Passenger Picker
    if (showPassengerPicker) {
        PassengerPickerDialog(
            adults = uiState.adults,
            children = uiState.children,
            infants = uiState.infants,
            onIncrementAdults = onIncrementAdults,
            onDecrementAdults = onDecrementAdults,
            onIncrementChildren = onIncrementChildren,
            onDecrementChildren = onDecrementChildren,
            onIncrementInfants = onIncrementInfants,
            onDecrementInfants = onDecrementInfants,
            onDismiss = { showPassengerPicker = false }
        )
    }
}

@Composable
private fun SearchHeader(
    onSettingsClick: () -> Unit,
    onSavedBookingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Top row with icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Saved bookings button
                IconButton(onClick = onSavedBookingsClick) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Saved Bookings",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // App title
                Text(
                    text = "flyadeal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                // Settings button
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Subtitle
            Text(
                text = "Book your flight",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AirportSelectionCard(
    origin: StationDto?,
    destination: StationDto?,
    onOriginClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onSwapClick: () -> Unit
) {
    InfoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Origin
            AirportSelector(
                label = "From",
                station = origin,
                onClick = onOriginClick,
                modifier = Modifier.weight(1f)
            )

            // Swap Button
            IconButton(
                onClick = onSwapClick,
                enabled = origin != null && destination != null
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Swap airports",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Destination
            AirportSelector(
                label = "To",
                station = destination,
                onClick = onDestinationClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AirportSelector(
    label: String,
    station: StationDto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (station != null) {
            Text(
                text = station.code,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = station.city,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Select",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DateSelectionCard(
    selectedDate: String,
    onDateChange: (String) -> Unit
) {
    InfoCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* Show date picker */ },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Departure Date",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedDate.ifEmpty { "Select date" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedDate.isNotEmpty())
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PassengerSelectionCard(
    adults: Int,
    children: Int,
    infants: Int,
    onClick: () -> Unit
) {
    val total = adults + children + infants
    val description = buildString {
        append("$adults Adult${if (adults > 1) "s" else ""}")
        if (children > 0) append(", $children Child${if (children > 1) "ren" else ""}")
        if (infants > 0) append(", $infants Infant${if (infants > 1) "s" else ""}")
    }

    InfoCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Passengers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AirportPickerDialog(
    title: String,
    stations: List<StationDto>,
    selectedCode: String?,
    onSelect: (StationDto) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(stations) { station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(station) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${station.code} - ${station.city}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (station.code == selectedCode) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PassengerPickerDialog(
    adults: Int,
    children: Int,
    infants: Int,
    onIncrementAdults: () -> Unit,
    onDecrementAdults: () -> Unit,
    onIncrementChildren: () -> Unit,
    onDecrementChildren: () -> Unit,
    onIncrementInfants: () -> Unit,
    onDecrementInfants: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Passengers") },
        text = {
            Column {
                PassengerCounter(
                    label = "Adults",
                    description = "12+ years",
                    count = adults,
                    onIncrement = onIncrementAdults,
                    onDecrement = onDecrementAdults,
                    minValue = 1
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PassengerCounter(
                    label = "Children",
                    description = "2-11 years",
                    count = children,
                    onIncrement = onIncrementChildren,
                    onDecrement = onDecrementChildren,
                    minValue = 0
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                PassengerCounter(
                    label = "Infants",
                    description = "Under 2 years",
                    count = infants,
                    onIncrement = onIncrementInfants,
                    onDecrement = onDecrementInfants,
                    minValue = 0
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun PassengerCounter(
    label: String,
    description: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    minValue: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onDecrement,
            enabled = count > minValue
        ) {
            Icon(Icons.Default.KeyboardArrowDown, "Decrease")
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onIncrement) {
            Icon(Icons.Default.Add, "Increase")
        }
    }
}

