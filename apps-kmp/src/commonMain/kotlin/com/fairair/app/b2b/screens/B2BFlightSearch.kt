package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.b2b.api.*
import com.fairair.app.b2b.state.B2BState

/**
 * B2B Flight Search component.
 * Provides flight search functionality with fare selection and booking flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2BFlightSearch(
    b2bState: B2BState,
    accentColor: Color
) {
    val stations by b2bState.stations.collectAsState()
    val routes by b2bState.routes.collectAsState()
    val isSearching by b2bState.isSearching.collectAsState()
    val searchResults by b2bState.searchResults.collectAsState()
    val searchError by b2bState.searchError.collectAsState()
    val selectedOutboundFlight by b2bState.selectedOutboundFlight.collectAsState()
    val selectedOutboundFare by b2bState.selectedOutboundFare.collectAsState()
    val isLoadingRoutes by b2bState.isLoadingRoutes.collectAsState()

    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var departureDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var adults by remember { mutableStateOf(1) }
    var children by remember { mutableStateOf(0) }
    var infants by remember { mutableStateOf(0) }
    var isRoundTrip by remember { mutableStateOf(false) }
    var showBookingFlow by remember { mutableStateOf(false) }

    // Load routes on first render
    LaunchedEffect(Unit) {
        if (stations.isEmpty()) {
            b2bState.loadRoutes()
        }
    }

    if (showBookingFlow && selectedOutboundFlight != null && selectedOutboundFare != null) {
        // Booking flow
        B2BBookingFlow(
            b2bState = b2bState,
            accentColor = accentColor,
            passengerCount = adults + children + infants,
            adults = adults,
            children = children,
            infants = infants,
            onBack = {
                showBookingFlow = false
                b2bState.clearFlightSelection()
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Search form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Search Flights",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Trip type selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = !isRoundTrip,
                                onClick = { isRoundTrip = false },
                                label = { Text("One Way") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = isRoundTrip,
                                onClick = { isRoundTrip = true },
                                label = { Text("Round Trip") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        // Origin and Destination
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StationSelector(
                                label = "From",
                                value = origin,
                                stations = stations,
                                onSelect = { 
                                    origin = it
                                    // Clear destination if not valid for new origin
                                    if (destination.isNotEmpty() && !routes[it]?.contains(destination).orFalse()) {
                                        destination = ""
                                    }
                                },
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                            StationSelector(
                                label = "To",
                                value = destination,
                                stations = if (origin.isNotEmpty()) {
                                    b2bState.getDestinationsForOrigin(origin)
                                } else {
                                    stations
                                },
                                onSelect = { destination = it },
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DateField(
                                label = "Departure Date",
                                value = departureDate,
                                onValueChange = { departureDate = it },
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                            if (isRoundTrip) {
                                DateField(
                                    label = "Return Date",
                                    value = returnDate,
                                    onValueChange = { returnDate = it },
                                    accentColor = accentColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Passengers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PassengerCounter(
                                label = "Adults",
                                value = adults,
                                onValueChange = { adults = it.coerceIn(1, 9) },
                                min = 1,
                                max = 9,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                            PassengerCounter(
                                label = "Children (2-11)",
                                value = children,
                                onValueChange = { children = it.coerceIn(0, 8) },
                                min = 0,
                                max = 8,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                            PassengerCounter(
                                label = "Infants (0-2)",
                                value = infants,
                                onValueChange = { infants = it.coerceIn(0, adults) },
                                min = 0,
                                max = adults,
                                accentColor = accentColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Search button
                        Button(
                            onClick = {
                                b2bState.searchFlights(
                                    FlightSearchRequest(
                                        origin = origin,
                                        destination = destination,
                                        departureDate = departureDate,
                                        returnDate = if (isRoundTrip) returnDate else null,
                                        adults = adults,
                                        children = children,
                                        infants = infants
                                    )
                                )
                            },
                            enabled = origin.isNotBlank() &&
                                    destination.isNotBlank() &&
                                    departureDate.isNotBlank() &&
                                    (!isRoundTrip || returnDate.isNotBlank()) &&
                                    !isSearching,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search Flights", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Error message
            searchError?.let { error ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                            Text(error, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            // Search results
            searchResults?.let { results ->
                item {
                    Text(
                        text = "Outbound Flights (${results.outboundFlights.size} found)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (results.outboundFlights.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.3f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No flights found for this route",
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Try different dates or destinations",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(results.outboundFlights) { flight ->
                        FlightResultCard(
                            flight = flight,
                            currency = results.currency,
                            accentColor = accentColor,
                            isSelected = selectedOutboundFlight?.flightId == flight.flightId,
                            onSelectFare = { fare ->
                                b2bState.selectOutboundFlight(flight, fare)
                                showBookingFlow = true
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationSelector(
    label: String,
    value: String,
    stations: List<StationDto>,
    onSelect: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedStation = stations.find { it.code == value }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedStation?.let { "${it.code} - ${it.city}" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedLabelColor = accentColor,
                unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            stations.forEach { station ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${station.code} - ${station.name}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${station.city}, ${station.country}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    onClick = {
                        onSelect(station.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { 
            // Simple date input formatting (YYYY-MM-DD)
            val filtered = it.filter { c -> c.isDigit() || c == '-' }
            if (filtered.length <= 10) {
                onValueChange(filtered)
            }
        },
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD", color = Color.White.copy(alpha = 0.4f)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
            focusedLabelColor = accentColor,
            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
private fun PassengerCounter(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int,
    max: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = if (value > min) 0.1f else 0.05f))
            ) {
                Text("-", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(
                text = "$value",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.width(32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = if (value < max) 0.1f else 0.05f))
            ) {
                Text("+", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FlightResultCard(
    flight: FlightOptionDto,
    currency: String,
    accentColor: Color,
    isSelected: Boolean,
    onSelectFare: (FareDto) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Flight header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Times and route
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = flight.departureTime.substringAfter("T").take(5),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = flight.origin,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = flight.durationMinutes.formatDuration(),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        HorizontalDivider(
                            modifier = Modifier.width(60.dp),
                            color = accentColor
                        )
                        Text(
                            text = flight.flightNumber,
                            fontSize = 10.sp,
                            color = accentColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = flight.arrivalTime.substringAfter("T").take(5),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = flight.destination,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Price and seats
                Column(horizontalAlignment = Alignment.End) {
                    val lowestFare = flight.fares.minByOrNull { it.price }
                    Text(
                        text = "From ${lowestFare?.price?.formatPrice(currency) ?: "N/A"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Text(
                        text = "${flight.availableSeats} seats left",
                        fontSize = 12.sp,
                        color = if (flight.availableSeats < 10) Color(0xFFEF4444) else Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Expanded fare options
            if (expanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )

                Text(
                    text = "Select Fare",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    flight.fares.forEach { fare ->
                        FareOptionCard(
                            fare = fare,
                            accentColor = accentColor,
                            onSelect = { onSelectFare(fare) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FareOptionCard(
    fare: FareDto,
    accentColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fareColor = when (fare.fareFamily) {
        "FLY" -> Color(0xFF6B7280)
        "FLY_PLUS" -> accentColor
        "FLY_MAX" -> Color(0xFF8B5CF6)
        else -> accentColor
    }

    Card(
        modifier = modifier.clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = fareColor.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = fare.fareFamily.replace("_", " "),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = fareColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = fare.price.formatPrice(fare.currency),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fare benefits
            FareBenefit(icon = Icons.Default.Star, text = fare.cabinBaggage)
            FareBenefit(icon = Icons.Default.Star, text = fare.checkedBaggage)
            FareBenefit(icon = Icons.Default.Star, text = fare.seatSelection)
            if (fare.mealIncluded) {
                FareBenefit(icon = Icons.Default.Check, text = "Meal included")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = fareColor),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("Select", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FareBenefit(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// Helper extension
private fun Boolean?.orFalse(): Boolean = this ?: false
