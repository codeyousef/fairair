package com.fairair.app.b2b.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.b2b.api.*
import com.fairair.app.b2b.state.B2BState

/**
 * B2B Group Bookings Screen.
 * Allows agencies to request group bookings (10+ passengers) and manage quotes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun B2BGroupBookingsScreen(
    b2bState: B2BState,
    accentColor: Color
) {
    val groupRequests by b2bState.groupBookingRequests.collectAsState()
    val isLoading by b2bState.isLoadingGroupRequests.collectAsState()
    val stations by b2bState.stations.collectAsState()
    val error by b2bState.groupRequestError.collectAsState()

    var showNewRequestDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<GroupBookingRequestDto?>(null) }

    // Load data on first render
    LaunchedEffect(Unit) {
        b2bState.loadGroupBookingRequests()
        if (stations.isEmpty()) {
            b2bState.loadRoutes()
        }
    }

    // New request dialog
    if (showNewRequestDialog) {
        GroupBookingRequestDialog(
            b2bState = b2bState,
            stations = stations,
            accentColor = accentColor,
            onDismiss = { showNewRequestDialog = false },
            onSubmit = { request ->
                b2bState.submitGroupBookingRequest(request) {
                    showNewRequestDialog = false
                }
            }
        )
    }

    // Request details dialog
    selectedRequest?.let { request ->
        GroupRequestDetailsDialog(
            request = request,
            accentColor = accentColor,
            onDismiss = { selectedRequest = null },
            onAcceptQuote = {
                b2bState.acceptGroupQuote(request.id) {
                    selectedRequest = null
                }
            },
            onRejectQuote = {
                b2bState.rejectGroupQuote(request.id) {
                    selectedRequest = null
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Group Booking Requests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Request quotes for 10+ passengers",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = { showNewRequestDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Request")
                }
            }
        }

        // Error message
        error?.let {
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
                        Text(it, color = Color(0xFFEF4444))
                    }
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

        // Stats cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = "Pending",
                    value = "${groupRequests.count { it.status == "PENDING" }}",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Quoted",
                    value = "${groupRequests.count { it.status == "QUOTED" }}",
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Accepted",
                    value = "${groupRequests.count { it.status == "ACCEPTED" }}",
                    color = Color(0xFF22C55E),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Total",
                    value = "${groupRequests.size}",
                    color = accentColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Requests list
        if (groupRequests.isEmpty() && !isLoading) {
            item {
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "No group booking requests",
                    subtitle = "Request a quote for your next group trip",
                    accentColor = accentColor,
                    actionLabel = "New Request",
                    onAction = { showNewRequestDialog = true }
                )
            }
        } else {
            items(groupRequests.sortedByDescending { it.createdAt }) { request ->
                GroupRequestCard(
                    request = request,
                    accentColor = accentColor,
                    onClick = { selectedRequest = request }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupBookingRequestDialog(
    b2bState: B2BState,
    stations: List<StationDto>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSubmit: (GroupBookingRequest) -> Unit
) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var departureDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var passengerCount by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("LEISURE") }
    var notes by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var flexibleDates by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New Group Booking Request")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    Text("Flight Details", fontWeight = FontWeight.SemiBold, color = accentColor)
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StationDropdown(
                            label = "From",
                            value = origin,
                            stations = stations,
                            onSelect = { origin = it },
                            modifier = Modifier.weight(1f)
                        )
                        StationDropdown(
                            label = "To",
                            value = destination,
                            stations = stations.filter { it.code != origin },
                            onSelect = { destination = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = departureDate,
                            onValueChange = { departureDate = it },
                            label = { Text("Departure Date") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = returnDate,
                            onValueChange = { returnDate = it },
                            label = { Text("Return (Optional)") },
                            placeholder = { Text("YYYY-MM-DD") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = flexibleDates,
                            onCheckedChange = { flexibleDates = it }
                        )
                        Text("Flexible dates (+/- 3 days)")
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = passengerCount,
                            onValueChange = { passengerCount = it.filter { c -> c.isDigit() } },
                            label = { Text("Passengers (10+)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        PurposeDropdown(
                            value = purpose,
                            onSelect = { purpose = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Additional Notes (Optional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Contact Information", fontWeight = FontWeight.SemiBold, color = accentColor)
                }

                item {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = { contactEmail = it },
                            label = { Text("Email") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            val count = passengerCount.toIntOrNull() ?: 0
            Button(
                onClick = {
                    onSubmit(
                        GroupBookingRequest(
                            origin = origin,
                            destination = destination,
                            departureDate = departureDate,
                            returnDate = returnDate.ifBlank { null },
                            passengerCount = count,
                            purpose = purpose,
                            notes = notes.ifBlank { null },
                            contactName = contactName,
                            contactEmail = contactEmail,
                            contactPhone = contactPhone,
                            flexibleDates = flexibleDates
                        )
                    )
                },
                enabled = origin.isNotBlank() &&
                        destination.isNotBlank() &&
                        departureDate.isNotBlank() &&
                        count >= 10 &&
                        contactName.isNotBlank() &&
                        contactEmail.isNotBlank() &&
                        contactPhone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("Submit Request")
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
private fun GroupRequestDetailsDialog(
    request: GroupBookingRequestDto,
    accentColor: Color,
    onDismiss: () -> Unit,
    onAcceptQuote: () -> Unit,
    onRejectQuote: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Request Details")
                val statusColor = request.status.statusColor()
                Text(
                    text = request.status.toDisplayStatus(),
                    fontSize = 12.sp,
                    color = Color(statusColor),
                    modifier = Modifier
                        .background(Color(statusColor).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Route
                Text("${request.originName} → ${request.destinationName}", fontWeight = FontWeight.Bold)
                Text("${request.departureDate}${request.returnDate?.let { " - $it" } ?: " (One-way)"}")

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("Passengers", fontSize = 12.sp, color = Color.Gray)
                        Text("${request.passengerCount}")
                    }
                    Column {
                        Text("Purpose", fontSize = 12.sp, color = Color.Gray)
                        Text(request.purpose)
                    }
                }

                request.notes?.let {
                    Text("Notes", fontSize = 12.sp, color = Color.Gray)
                    Text(it)
                }

                HorizontalDivider()

                // Contact
                Text("Contact", fontWeight = FontWeight.SemiBold, color = accentColor)
                Text(request.contactName)
                Text(request.contactEmail, fontSize = 12.sp, color = Color.Gray)
                Text(request.contactPhone, fontSize = 12.sp, color = Color.Gray)

                // Quote section
                if (request.status == "QUOTED") {
                    HorizontalDivider()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF59E0B).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Quote Received", fontWeight = FontWeight.SemiBold, color = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.height(8.dp))
                            request.quotedPrice?.let {
                                Text(
                                    it.formatPrice(request.quoteCurrency ?: "SAR"),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            request.quoteValidUntil?.let {
                                Text("Valid until: $it", fontSize = 12.sp, color = Color.Gray)
                            }
                            request.quoteNotes?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (request.status == "QUOTED") {
                Button(
                    onClick = onAcceptQuote,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                ) {
                    Text("Accept Quote")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (request.status == "QUOTED") {
                OutlinedButton(
                    onClick = onRejectQuote,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Reject Quote")
                }
            }
        }
    )
}

@Composable
private fun GroupRequestCard(
    request: GroupBookingRequestDto,
    accentColor: Color,
    onClick: () -> Unit
) {
    val statusColor = Color(request.status.statusColor())

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
                Column {
                    Text(
                        text = "${request.originName} → ${request.destinationName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${request.passengerCount} passengers • ${request.purpose}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = request.status.toDisplayStatus(),
                        fontSize = 12.sp,
                        color = statusColor,
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    if (request.status == "QUOTED") {
                        request.quotedPrice?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it.formatPrice(request.quoteCurrency ?: "SAR"),
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = request.departureDate + (request.returnDate?.let { " - $it" } ?: ""),
                    fontSize = 12.sp,
                    color = accentColor
                )

                Text(
                    text = "Created: ${request.createdAt.take(10)}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            if (request.status == "QUOTED") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quote expires: ${request.quoteValidUntil ?: "N/A"}",
                        fontSize = 10.sp,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StationDropdown(
    label: String,
    value: String,
    stations: List<StationDto>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = stations.find { it.code == value }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.code} - ${it.city}" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            stations.forEach { station ->
                DropdownMenuItem(
                    text = { Text("${station.code} - ${station.city}") },
                    onClick = {
                        onSelect(station.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurposeDropdown(
    value: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val purposes = listOf("LEISURE", "CORPORATE", "SPORTS", "RELIGIOUS", "OTHER")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Purpose") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            purposes.forEach { purpose ->
                DropdownMenuItem(
                    text = { Text(purpose.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        onSelect(purpose)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
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
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    actionLabel: String,
    onAction: () -> Unit
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
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text(actionLabel)
            }
        }
    }
}
