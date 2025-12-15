package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.b2b.api.*
import com.fairair.app.b2b.state.B2BState

/**
 * B2B Booking Flow component.
 * Handles passenger information collection and booking confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2BBookingFlow(
    b2bState: B2BState,
    accentColor: Color,
    passengerCount: Int,
    adults: Int,
    children: Int,
    infants: Int,
    onBack: () -> Unit
) {
    val selectedOutboundFlight by b2bState.selectedOutboundFlight.collectAsState()
    val selectedOutboundFare by b2bState.selectedOutboundFare.collectAsState()
    val selectedReturnFlight by b2bState.selectedReturnFlight.collectAsState()
    val selectedReturnFare by b2bState.selectedReturnFare.collectAsState()
    val isBooking by b2bState.isBooking.collectAsState()
    val bookingConfirmation by b2bState.bookingConfirmation.collectAsState()
    val bookingError by b2bState.bookingError.collectAsState()

    var currentStep by remember { mutableStateOf(0) } // 0: passengers, 1: contact, 2: review, 3: confirmation
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var clientReference by remember { mutableStateOf("") }

    // Initialize passenger forms
    var passengerForms by remember { 
        mutableStateOf(
            buildList {
                repeat(adults) { add(createEmptyPassenger("ADULT")) }
                repeat(children) { add(createEmptyPassenger("CHILD")) }
                repeat(infants) { add(createEmptyPassenger("INFANT")) }
            }
        )
    }

    // Show confirmation if booking successful
    bookingConfirmation?.let { confirmation ->
        BookingConfirmationView(
            confirmation = confirmation,
            accentColor = accentColor,
            onDone = onBack
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header with back button and progress
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            onBack()
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Progress indicator
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Passengers", "Contact", "Review").forEachIndexed { index, label ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index <= currentStep) accentColor
                                        else Color.White.copy(alpha = 0.2f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = if (index <= currentStep) accentColor else Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Flight summary
        item {
            FlightSummaryCard(
                outboundFlight = selectedOutboundFlight,
                outboundFare = selectedOutboundFare,
                returnFlight = selectedReturnFlight,
                returnFare = selectedReturnFare,
                accentColor = accentColor
            )
        }

        // Error message
        bookingError?.let { error ->
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
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { b2bState.clearBookingError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFFEF4444))
                        }
                    }
                }
            }
        }

        when (currentStep) {
            0 -> {
                // Passenger information step
                item {
                    Text(
                        text = "Passenger Information",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                itemsIndexed(passengerForms) { index, passenger ->
                    PassengerFormCard(
                        index = index,
                        passenger = passenger,
                        onUpdate = { updated ->
                            passengerForms = passengerForms.toMutableList().apply {
                                this[index] = updated
                            }
                        },
                        accentColor = accentColor
                    )
                }

                item {
                    Button(
                        onClick = { currentStep = 1 },
                        enabled = passengerForms.all { it.isValid() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue to Contact Info", fontWeight = FontWeight.SemiBold)
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            1 -> {
                // Contact information step
                item {
                    Text(
                        text = "Contact Information",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = contactEmail,
                                onValueChange = { contactEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = accentColor)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = textFieldColors(accentColor),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = { Text("Phone Number") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = accentColor)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                colors = textFieldColors(accentColor),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = clientReference,
                                onValueChange = { clientReference = it },
                                label = { Text("Client Reference (Optional)") },
                                placeholder = { Text("Your internal booking reference") },
                                singleLine = true,
                                colors = textFieldColors(accentColor),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = { currentStep = 2 },
                        enabled = contactEmail.isNotBlank() && contactPhone.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Review Booking", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            2 -> {
                // Review step
                item {
                    Text(
                        text = "Review & Confirm",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Passengers summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Passengers",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            passengerForms.forEachIndexed { index, passenger ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. ${passenger.title} ${passenger.firstName} ${passenger.lastName}",
                                        color = Color.White
                                    )
                                    Text(
                                        text = passenger.type,
                                        fontSize = 12.sp,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Contact summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.05f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Contact",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Text(text = "Email: $contactEmail", color = Color.White)
                            Text(text = "Phone: $contactPhone", color = Color.White)
                            if (clientReference.isNotBlank()) {
                                Text(text = "Reference: $clientReference", color = Color.White)
                            }
                        }
                    }
                }

                // Price summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = accentColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Price Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            val outboundPrice = selectedOutboundFare?.price ?: 0L
                            val returnPrice = selectedReturnFare?.price ?: 0L
                            val currency = selectedOutboundFare?.currency ?: "SAR"
                            val totalPerPerson = outboundPrice + returnPrice
                            val totalPrice = totalPerPerson * passengerForms.size

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Outbound (${selectedOutboundFare?.fareFamily})", color = Color.White)
                                Text(outboundPrice.formatPrice(currency), color = Color.White)
                            }

                            selectedReturnFare?.let {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Return (${it.fareFamily})", color = Color.White)
                                    Text(returnPrice.formatPrice(currency), color = Color.White)
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${passengerForms.size} passenger(s) x ${totalPerPerson.formatPrice(currency)}",
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Total",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    totalPrice.formatPrice(currency),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                    }
                }

                // Payment method notice
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF22C55E).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF22C55E)
                            )
                            Text(
                                "This booking will be charged to your agency credit account",
                                color = Color(0xFF22C55E),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Confirm button
                item {
                    Button(
                        onClick = {
                            b2bState.updatePassengers(passengerForms)
                            b2bState.createBooking(
                                contactEmail = contactEmail,
                                contactPhone = contactPhone,
                                clientReference = clientReference.ifBlank { null }
                            )
                        },
                        enabled = !isBooking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isBooking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun FlightSummaryCard(
    outboundFlight: FlightOptionDto?,
    outboundFare: FareDto?,
    returnFlight: FlightOptionDto?,
    returnFare: FareDto?,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Selected Flights",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            outboundFlight?.let { flight ->
                FlightSegmentRow(
                    label = "Outbound",
                    flight = flight,
                    fare = outboundFare,
                    accentColor = accentColor
                )
            }

            returnFlight?.let { flight ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )
                FlightSegmentRow(
                    label = "Return",
                    flight = flight,
                    fare = returnFare,
                    accentColor = accentColor
                )
            }
        }
    }
}

@Composable
private fun FlightSegmentRow(
    label: String,
    flight: FlightOptionDto,
    fare: FareDto?,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = "${flight.origin} → ${flight.destination}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "${flight.flightNumber} • ${flight.departureTime.substringAfter("T").take(5)}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            fare?.let {
                Text(
                    text = it.fareFamily.replace("_", " "),
                    fontSize = 12.sp,
                    color = accentColor
                )
                Text(
                    text = it.price.formatPrice(it.currency),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun PassengerFormCard(
    index: Int,
    passenger: PassengerDto,
    onUpdate: (PassengerDto) -> Unit,
    accentColor: Color
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = "Passenger ${index + 1}",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = passenger.type,
                            fontSize = 12.sp,
                            color = accentColor
                        )
                    }
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = Color.White
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Title selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val titles = when (passenger.type) {
                        "CHILD" -> listOf("MSTR", "MISS")
                        "INFANT" -> listOf("INF")
                        else -> listOf("MR", "MRS", "MS")
                    }
                    titles.forEach { title ->
                        FilterChip(
                            selected = passenger.title == title,
                            onClick = { onUpdate(passenger.copy(title = title)) },
                            label = { Text(title) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = passenger.firstName,
                        onValueChange = { onUpdate(passenger.copy(firstName = it)) },
                        label = { Text("First Name") },
                        singleLine = true,
                        colors = textFieldColors(accentColor),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passenger.lastName,
                        onValueChange = { onUpdate(passenger.copy(lastName = it)) },
                        label = { Text("Last Name") },
                        singleLine = true,
                        colors = textFieldColors(accentColor),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = passenger.dateOfBirth,
                        onValueChange = { onUpdate(passenger.copy(dateOfBirth = it)) },
                        label = { Text("Date of Birth") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors(accentColor),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passenger.nationality,
                        onValueChange = { onUpdate(passenger.copy(nationality = it.take(2).uppercase())) },
                        label = { Text("Nationality") },
                        placeholder = { Text("SA") },
                        singleLine = true,
                        colors = textFieldColors(accentColor),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = passenger.documentNumber,
                        onValueChange = { onUpdate(passenger.copy(documentNumber = it)) },
                        label = { Text("Passport/ID Number") },
                        singleLine = true,
                        colors = textFieldColors(accentColor),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passenger.documentExpiry,
                        onValueChange = { onUpdate(passenger.copy(documentExpiry = it)) },
                        label = { Text("Document Expiry") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors(accentColor),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingConfirmationView(
    confirmation: BookingConfirmation,
    accentColor: Color,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFF22C55E).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Booking Confirmed!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your booking has been successfully created",
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PNR Card
        Card(
            modifier = Modifier.widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = accentColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Booking Reference",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    text = confirmation.pnr,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 4.sp
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = Color.White.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Status", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(confirmation.status, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(
                            confirmation.totalAmount.formatPrice(confirmation.currency),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (confirmation.agencyCommission > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Commission Earned", fontSize = 12.sp, color = Color(0xFF22C55E))
                        Text(
                            confirmation.agencyCommission.formatPrice(confirmation.currency),
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Passengers summary
        Text(
            text = "${confirmation.passengers.size} Passenger(s)",
            fontSize = 16.sp,
            color = Color.White
        )
        confirmation.passengers.forEach { pax ->
            Text(
                text = pax.name,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Done button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Done", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun textFieldColors(accentColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accentColor,
    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
    focusedLabelColor = accentColor,
    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = accentColor
)

private fun createEmptyPassenger(type: String): PassengerDto {
    return PassengerDto(
        type = type,
        title = when (type) {
            "CHILD" -> "MSTR"
            "INFANT" -> "INF"
            else -> "MR"
        },
        firstName = "",
        lastName = "",
        dateOfBirth = "",
        nationality = "",
        documentType = "PASSPORT",
        documentNumber = "",
        documentExpiry = "",
        documentCountry = ""
    )
}

private fun PassengerDto.isValid(): Boolean {
    return firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            dateOfBirth.isNotBlank() &&
            nationality.length == 2 &&
            documentNumber.isNotBlank() &&
            documentExpiry.isNotBlank()
}
