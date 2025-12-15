package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.b2b.api.*
import com.fairair.app.b2b.state.B2BState

/**
 * B2B Bookings Manager component.
 * Displays and manages all agency bookings with filtering and details view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2BBookingsManager(
    b2bState: B2BState,
    accentColor: Color
) {
    val bookings by b2bState.agencyBookings.collectAsState()
    val isLoading by b2bState.isLoadingBookings.collectAsState()
    val selectedBooking by b2bState.selectedBooking.collectAsState()
    val currentPage by b2bState.bookingsPage.collectAsState()
    val totalPages by b2bState.totalBookingsPages.collectAsState()

    var statusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf<String?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    // Load bookings on first render
    LaunchedEffect(Unit) {
        b2bState.loadAgencyBookings()
    }

    // Reload when filter changes
    LaunchedEffect(statusFilter) {
        b2bState.loadAgencyBookings(status = statusFilter)
    }

    // Cancel dialog
    showCancelDialog?.let { pnr ->
        AlertDialog(
            onDismissRequest = { showCancelDialog = null },
            title = { Text("Cancel Booking") },
            text = {
                Column {
                    Text("Are you sure you want to cancel booking $pnr?")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Cancellation Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        b2bState.cancelBooking(pnr, cancelReason) {
                            showCancelDialog = null
                            cancelReason = ""
                        }
                    },
                    enabled = cancelReason.isNotBlank()
                ) {
                    Text("Cancel Booking", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = null }) {
                    Text("Keep Booking")
                }
            }
        )
    }

    // Show booking details if selected
    selectedBooking?.let { booking ->
        BookingDetailsView(
            booking = booking,
            accentColor = accentColor,
            onBack = { b2bState.clearSelectedBooking() },
            onCancel = { showCancelDialog = booking.pnr }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with filters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Bookings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Refresh button
                IconButton(
                    onClick = { b2bState.loadAgencyBookings(status = statusFilter) }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = accentColor)
                }
            }
        }

        // Search and filter bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by PNR or passenger name...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = accentColor)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Status filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = statusFilter == "CONFIRMED",
                        onClick = { statusFilter = if (statusFilter == "CONFIRMED") null else "CONFIRMED" },
                        label = { Text("Confirmed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22C55E),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = statusFilter == "PENDING",
                        onClick = { statusFilter = if (statusFilter == "PENDING") null else "PENDING" },
                        label = { Text("Pending") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF59E0B),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = statusFilter == "CANCELLED",
                        onClick = { statusFilter = if (statusFilter == "CANCELLED") null else "CANCELLED" },
                        label = { Text("Cancelled") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Loading indicator
        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accentColor)
                }
            }
        }

        // Bookings list
        val filteredBookings = bookings.filter { booking ->
            searchQuery.isBlank() ||
                    booking.pnr.contains(searchQuery, ignoreCase = true) ||
                    booking.passengerNames.contains(searchQuery, ignoreCase = true)
        }

        if (filteredBookings.isEmpty() && !isLoading) {
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
                                text = if (searchQuery.isNotBlank()) "No bookings match your search"
                                else "No bookings found",
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredBookings) { booking ->
                BookingCard(
                    booking = booking,
                    accentColor = accentColor,
                    onClick = { b2bState.loadBookingDetails(booking.pnr) },
                    onCancel = { showCancelDialog = booking.pnr }
                )
            }
        }

        // Pagination
        if (totalPages > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentPage > 0) {
                                b2bState.loadAgencyBookings(page = currentPage - 1, status = statusFilter)
                            }
                        },
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = Color.White)
                    }

                    Text(
                        text = "Page ${currentPage + 1} of $totalPages",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(
                        onClick = {
                            if (currentPage < totalPages - 1) {
                                b2bState.loadAgencyBookings(page = currentPage + 1, status = statusFilter)
                            }
                        },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Next", tint = Color.White)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun BookingCard(
    booking: BookingSummaryDto,
    accentColor: Color,
    onClick: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (booking.status) {
        "CONFIRMED", "TICKETED" -> Color(0xFF22C55E)
        "PENDING" -> Color(0xFFF59E0B)
        "CANCELLED" -> Color(0xFFEF4444)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (booking.status) {
                                "CONFIRMED", "TICKETED" -> Icons.Default.Check
                                "CANCELLED" -> Icons.Default.Close
                                else -> Icons.Default.Star
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = booking.pnr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = booking.status.toDisplayStatus(),
                            fontSize = 12.sp,
                            color = statusColor
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = booking.totalAmount.formatPrice(booking.currency),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = booking.createdAt.take(10),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.White.copy(alpha = 0.1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = booking.route,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                    Text(
                        text = booking.departureDate,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = booking.passengerNames,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    booking.clientReference?.let {
                        Text(
                            text = "Ref: $it",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                // Action buttons
                if (booking.status == "CONFIRMED" || booking.status == "PENDING") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onCancel,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFEF4444)
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onClick,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("View Details", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDetailsView(
    booking: BookingDetails,
    accentColor: Color,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (booking.status) {
        "CONFIRMED", "TICKETED" -> Color(0xFF22C55E)
        "PENDING" -> Color(0xFFF59E0B)
        "CANCELLED" -> Color(0xFFEF4444)
        else -> Color.Gray
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Booking ${booking.pnr}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = booking.status.toDisplayStatus(),
                            fontSize = 14.sp,
                            color = statusColor
                        )
                    }
                }

                if (booking.status == "CONFIRMED" || booking.status == "PENDING") {
                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel Booking")
                    }
                }
            }
        }

        // Flight segments
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
                        text = "Flight Details",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    booking.flights.forEach { segment ->
                        FlightSegmentCard(segment = segment, accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Passengers
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
                        text = "Passengers (${booking.passengers.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    booking.passengers.forEachIndexed { index, passenger ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${passenger.title} ${passenger.firstName} ${passenger.lastName}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${passenger.type} • ${passenger.nationality}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "DOB: ${passenger.dateOfBirth}",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                passenger.ticketNumber?.let {
                                    Text(
                                        text = "Ticket: $it",
                                        fontSize = 12.sp,
                                        color = accentColor
                                    )
                                }
                                if (passenger.seatAssignments.isNotEmpty()) {
                                    Text(
                                        text = "Seats: ${passenger.seatAssignments.joinToString { it.seatNumber }}",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ancillaries
        if (booking.ancillaries.isNotEmpty()) {
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
                            text = "Add-ons",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        booking.ancillaries.forEach { ancillary ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ancillary.description,
                                    color = Color.White
                                )
                                Text(
                                    text = ancillary.price.formatPrice(ancillary.currency),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Payment summary
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
                        text = "Payment Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val payment = booking.payment

                    PaymentRow("Base Fare", payment.basefare, payment.currency)
                    PaymentRow("Taxes & Fees", payment.taxes + payment.fees, payment.currency)
                    if (payment.ancillaries > 0) {
                        PaymentRow("Add-ons", payment.ancillaries, payment.currency)
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
                            text = "Total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = payment.totalAmount.formatPrice(payment.currency),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    if (payment.agencyCommission > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Your Commission",
                                color = Color(0xFF22C55E)
                            )
                            Text(
                                text = payment.agencyCommission.formatPrice(payment.currency),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF22C55E)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Payment Method",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = payment.method,
                            color = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Payment Status",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = payment.paymentStatus,
                            color = if (payment.paymentStatus == "PAID") Color(0xFF22C55E) else Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // Contact info
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
                        text = "Contact Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column {
                            Text("Email", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(booking.contactEmail, color = Color.White)
                        }
                        Column {
                            Text("Phone", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                            Text(booking.contactPhone, color = Color.White)
                        }
                    }

                    booking.clientReference?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Client Reference", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Text(it, color = Color.White)
                    }
                }
            }
        }

        // Booking info
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
                        text = "Booking Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Created", color = Color.White.copy(alpha = 0.6f))
                        Text(booking.createdAt, color = Color.White)
                    }

                    booking.ticketedAt?.let {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ticketed", color = Color.White.copy(alpha = 0.6f))
                            Text(it, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Booked By", color = Color.White.copy(alpha = 0.6f))
                        Text(booking.agentEmail, color = Color.White)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun FlightSegmentCard(
    segment: FlightSegmentDetailDto,
    accentColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = segment.departureTime.substringAfter("T").take(5),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = segment.origin,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = segment.originName,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = segment.flightNumber,
                fontSize = 12.sp,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = segment.durationMinutes.formatDuration(),
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            HorizontalDivider(
                modifier = Modifier.width(60.dp),
                color = accentColor
            )
            Text(
                text = segment.fareFamily,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = segment.arrivalTime.substringAfter("T").take(5),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = segment.destination,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = segment.destinationName,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun PaymentRow(label: String, amount: Long, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f))
        Text(amount.formatPrice(currency), color = Color.White)
    }
}
