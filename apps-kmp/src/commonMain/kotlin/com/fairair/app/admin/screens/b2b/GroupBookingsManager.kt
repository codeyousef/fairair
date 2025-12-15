package com.fairair.app.admin.screens.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.admin.api.*
import com.fairair.app.admin.state.AdminState
import com.fairair.app.ui.theme.FairairColors
import kotlinx.coroutines.launch

/**
 * Group Bookings management screen for B2B portal.
 * Manage group booking requests, quotes, and approvals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupBookingsManager(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val groupBookings by adminState.groupBookings.collectAsState()
    val isLoading by adminState.groupBookingsLoading.collectAsState()
    val currentAdmin by adminState.currentAdmin.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Pending
    var selectedBooking by remember { mutableStateOf<GroupBookingRequestDto?>(null) }
    var quoteDialog by remember { mutableStateOf<GroupBookingRequestDto?>(null) }

    LaunchedEffect(Unit) {
        loadGroupBookings(adminState, adminApiClient)
    }

    val pendingBookings = groupBookings.filter { it.status == "PENDING" }
    val displayedBookings = if (selectedTab == 0) groupBookings else pendingBookings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Group Bookings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = FairairColors.Gray900
        )
        Text(
            text = "Manage group booking requests from agencies and direct customers.",
            fontSize = 16.sp,
            color = FairairColors.Gray600,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = FairairColors.White,
            contentColor = FairairColors.Purple
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("All Requests (${groupBookings.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pending")
                        if (pendingBookings.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = FairairColors.Warning,
                                contentColor = FairairColors.White
                            ) {
                                Text(pendingBookings.size.toString())
                            }
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FairairColors.White)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FairairColors.Purple)
                }
            } else if (displayedBookings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = FairairColors.Gray400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 1) "No pending requests" else "No group booking requests",
                            fontSize = 16.sp,
                            color = FairairColors.Gray500
                        )
                    }
                }
            } else {
                LazyColumn {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FairairColors.Gray100)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Request #", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Contact", Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Route", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Date", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Pax", Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Status", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Actions", Modifier.width(120.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                        }
                    }

                    items(displayedBookings) { booking ->
                        GroupBookingRow(
                            booking = booking,
                            onView = { selectedBooking = booking },
                            onQuote = { quoteDialog = booking },
                            onAssign = {
                                scope.launch {
                                    currentAdmin?.id?.let { adminId ->
                                        adminApiClient.assignGroupBooking(booking.id, adminId)
                                        loadGroupBookings(adminState, adminApiClient)
                                    }
                                }
                            }
                        )
                        HorizontalDivider(color = FairairColors.Gray200)
                    }
                }
            }
        }
    }

    // View Details Dialog
    selectedBooking?.let { booking ->
        GroupBookingDetailsDialog(
            booking = booking,
            onDismiss = { selectedBooking = null }
        )
    }

    // Quote Dialog
    quoteDialog?.let { booking ->
        QuoteDialog(
            booking = booking,
            adminId = currentAdmin?.id ?: "",
            onDismiss = { quoteDialog = null },
            onSubmit = { request ->
                scope.launch {
                    adminApiClient.submitGroupBookingQuote(booking.id, request)
                    loadGroupBookings(adminState, adminApiClient)
                    quoteDialog = null
                }
            }
        )
    }
}

@Composable
private fun GroupBookingRow(
    booking: GroupBookingRequestDto,
    onView: () -> Unit,
    onQuote: () -> Unit,
    onAssign: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = booking.requestNumber,
            modifier = Modifier.weight(0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FairairColors.Purple
        )

        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = booking.contactName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FairairColors.Gray900
            )
            booking.companyName?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = FairairColors.Gray600
                )
            }
        }

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = FairairColors.Gray500
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${booking.origin} → ${booking.destination}",
                fontSize = 14.sp,
                color = FairairColors.Gray700
            )
        }

        Text(
            text = booking.departureDate.take(10),
            modifier = Modifier.weight(0.8f),
            fontSize = 14.sp,
            color = FairairColors.Gray700
        )

        Row(
            modifier = Modifier.weight(0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = FairairColors.Gray500
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${booking.passengerCount}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FairairColors.Gray900
            )
        }

        Box(modifier = Modifier.weight(0.8f)) {
            val (statusColor, statusBg) = when (booking.status) {
                "PENDING" -> FairairColors.Warning to FairairColors.Warning.copy(alpha = 0.1f)
                "QUOTED" -> FairairColors.Info to FairairColors.Info.copy(alpha = 0.1f)
                "ACCEPTED" -> FairairColors.Success to FairairColors.Success.copy(alpha = 0.1f)
                "COMPLETED" -> FairairColors.Success to FairairColors.Success.copy(alpha = 0.1f)
                "REJECTED", "CANCELLED" -> FairairColors.Gray600 to FairairColors.Gray200
                else -> FairairColors.Gray600 to FairairColors.Gray200
            }
            Text(
                text = booking.statusDisplayName,
                fontSize = 12.sp,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Row(modifier = Modifier.width(120.dp)) {
            IconButton(onClick = onView) {
                Icon(Icons.Default.Info, "View", tint = FairairColors.Gray600)
            }

            if (booking.status == "PENDING") {
                if (booking.assignedTo == null) {
                    IconButton(onClick = onAssign) {
                        Icon(Icons.Default.Person, "Assign to me", tint = FairairColors.Info)
                    }
                } else {
                    IconButton(onClick = onQuote) {
                        Icon(Icons.Default.Email, "Send Quote", tint = FairairColors.Success)
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupBookingDetailsDialog(
    booking: GroupBookingRequestDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 500.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.List, null, tint = FairairColors.Purple)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Request ${booking.requestNumber}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Status
                val (statusColor, statusBg) = when (booking.status) {
                    "PENDING" -> FairairColors.Warning to FairairColors.Warning.copy(alpha = 0.1f)
                    "QUOTED" -> FairairColors.Info to FairairColors.Info.copy(alpha = 0.1f)
                    "ACCEPTED" -> FairairColors.Success to FairairColors.Success.copy(alpha = 0.1f)
                    else -> FairairColors.Gray600 to FairairColors.Gray200
                }
                Text(
                    text = booking.statusDisplayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Info
                Text("Contact Information", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Name", booking.contactName)
                DetailRow("Email", booking.contactEmail)
                DetailRow("Phone", booking.contactPhone)
                booking.companyName?.let { DetailRow("Company", it) }

                Spacer(modifier = Modifier.height(16.dp))

                // Flight Details
                Text("Flight Details", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Route", "${booking.origin} → ${booking.destination}")
                DetailRow("Type", if (booking.tripType == "ROUND_TRIP") "Round Trip" else "One Way")
                DetailRow("Departure", booking.departureDate.take(10))
                booking.returnDate?.let { DetailRow("Return", it.take(10)) }
                DetailRow("Passengers", booking.passengerCount.toString())
                booking.fareClassPreference?.let { DetailRow("Class Preference", it) }

                booking.specialRequirements?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Special Requirements", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, fontSize = 14.sp, color = FairairColors.Gray700)
                }

                if (booking.quotedAmount != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Quote", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Amount", "${booking.quotedCurrency ?: "SAR"} ${booking.quotedAmount}")
                    booking.quoteValidUntil?.let { DetailRow("Valid Until", it.take(10)) }
                }

                booking.bookingPnr?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Booking Reference", fontWeight = FontWeight.SemiBold, color = FairairColors.Gray900)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = FairairColors.Purple)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuoteDialog(
    booking: GroupBookingRequestDto,
    adminId: String,
    onDismiss: () -> Unit,
    onSubmit: (SubmitQuoteRequest) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("SAR") }
    var validDays by remember { mutableStateOf("7") }
    var currencyExpanded by remember { mutableStateOf(false) }

    val currencies = listOf("SAR", "USD", "EUR", "GBP")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Submit Quote", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Submit a quote for request ${booking.requestNumber}")
                Text("${booking.passengerCount} passengers · ${booking.origin} → ${booking.destination}", 
                    fontSize = 13.sp, color = FairairColors.Gray600)

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Total Amount") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it },
                        modifier = Modifier.width(100.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            currencies.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr) },
                                    onClick = {
                                        currency = curr
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = validDays,
                    onValueChange = { validDays = it },
                    label = { Text("Quote Valid For (days)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(SubmitQuoteRequest(
                        quotedAmount = amount,
                        quotedCurrency = currency,
                        quotedBy = adminId,
                        validDays = validDays.toIntOrNull() ?: 7
                    ))
                },
                enabled = amount.isNotBlank() && amount.toDoubleOrNull() != null,
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Success)
            ) {
                Text("Submit Quote")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(120.dp),
            fontSize = 13.sp,
            color = FairairColors.Gray600
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = FairairColors.Gray900
        )
    }
}

private suspend fun loadGroupBookings(adminState: AdminState, adminApiClient: AdminApiClient) {
    adminState.setGroupBookingsLoading(true)

    when (val result = adminApiClient.getAllGroupBookings()) {
        is AdminApiResult.Success -> adminState.setGroupBookings(result.data)
        is AdminApiResult.Error -> adminState.setError(result.message)
    }

    adminState.setGroupBookingsLoading(false)
}
