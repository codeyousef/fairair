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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.b2b.api.*
import com.fairair.app.b2b.state.B2BState

/**
 * B2B Charter Requests Screen.
 * Allows agencies to request charter flights for special purposes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun B2BCharterRequestsScreen(
    b2bState: B2BState,
    accentColor: Color
) {
    val charterRequests by b2bState.charterRequests.collectAsState()
    val isLoading by b2bState.isLoadingCharterRequests.collectAsState()
    val stations by b2bState.stations.collectAsState()
    val error by b2bState.charterRequestError.collectAsState()

    var showNewRequestDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<CharterRequestDto?>(null) }

    // Load data on first render
    LaunchedEffect(Unit) {
        b2bState.loadCharterRequests()
        if (stations.isEmpty()) {
            b2bState.loadRoutes()
        }
    }

    // New request dialog
    if (showNewRequestDialog) {
        CharterRequestDialog(
            stations = stations,
            accentColor = accentColor,
            onDismiss = { showNewRequestDialog = false },
            onSubmit = { request ->
                b2bState.submitCharterRequest(request) {
                    showNewRequestDialog = false
                }
            }
        )
    }

    // Request details dialog
    selectedRequest?.let { request ->
        CharterDetailsDialog(
            request = request,
            accentColor = accentColor,
            onDismiss = { selectedRequest = null }
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
                        text = "Charter Flight Requests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Request private charter flights for special needs",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = { showNewRequestDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Charter Request")
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
                    CircularProgressIndicator(color = Color(0xFF8B5CF6))
                }
            }
        }

        // Charter types info
        item {
            CharterTypesInfo(accentColor = accentColor)
        }

        // Requests list
        if (charterRequests.isEmpty() && !isLoading) {
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
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No charter requests yet",
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Request a private charter for Hajj, sports, corporate events and more",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showNewRequestDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                        ) {
                            Text("Request Charter")
                        }
                    }
                }
            }
        } else {
            items(charterRequests.sortedByDescending { it.createdAt }) { request ->
                CharterRequestCard(
                    request = request,
                    accentColor = accentColor,
                    onClick = { selectedRequest = request }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun CharterTypesInfo(accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CharterTypeCard(
            type = "Hajj & Umrah",
            icon = "🕋",
            color = Color(0xFF22C55E),
            modifier = Modifier.weight(1f)
        )
        CharterTypeCard(
            type = "Sports Teams",
            icon = "⚽",
            color = Color(0xFF3B82F6),
            modifier = Modifier.weight(1f)
        )
        CharterTypeCard(
            type = "Corporate",
            icon = "🏢",
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        CharterTypeCard(
            type = "Government",
            icon = "🏛️",
            color = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CharterTypeCard(
    type: String,
    icon: String,
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
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = type,
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CharterRequestDialog(
    stations: List<StationDto>,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSubmit: (CharterRequest) -> Unit
) {
    var charterType by remember { mutableStateOf("HAJJ_UMRAH") }
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var departureDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var passengerCount by remember { mutableStateOf("") }
    var aircraftPreference by remember { mutableStateOf("") }
    var specialRequirements by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }

    val charterTypes = listOf(
        "HAJJ_UMRAH" to "Hajj & Umrah",
        "SPORTS" to "Sports Team",
        "CORPORATE" to "Corporate",
        "GOVERNMENT" to "Government",
        "ENTERTAINMENT" to "Entertainment",
        "OTHER" to "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("New Charter Request")
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    Text("Charter Type", fontWeight = FontWeight.SemiBold, color = Color(0xFF8B5CF6))
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        charterTypes.forEach { (type, label) ->
                            FilterChip(
                                selected = charterType == type,
                                onClick = { charterType = type },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF8B5CF6),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                item {
                    Text("Flight Details", fontWeight = FontWeight.SemiBold, color = Color(0xFF8B5CF6))
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CharterStationDropdown(
                            label = "From",
                            value = origin,
                            stations = stations,
                            onSelect = { origin = it },
                            modifier = Modifier.weight(1f)
                        )
                        CharterStationDropdown(
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = passengerCount,
                            onValueChange = { passengerCount = it.filter { c -> c.isDigit() } },
                            label = { Text("Passengers") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = aircraftPreference,
                            onValueChange = { aircraftPreference = it },
                            label = { Text("Aircraft (Optional)") },
                            placeholder = { Text("e.g., A320") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = specialRequirements,
                        onValueChange = { specialRequirements = it },
                        label = { Text("Special Requirements (Optional)") },
                        placeholder = { Text("Catering, VIP services, etc.") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Contact Information", fontWeight = FontWeight.SemiBold, color = Color(0xFF8B5CF6))
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Contact Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company (Optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                        CharterRequest(
                            charterType = charterType,
                            origin = origin,
                            destination = destination,
                            departureDate = departureDate,
                            returnDate = returnDate.ifBlank { null },
                            passengerCount = count,
                            aircraftPreference = aircraftPreference.ifBlank { null },
                            specialRequirements = specialRequirements.ifBlank { null },
                            contactName = contactName,
                            contactEmail = contactEmail,
                            contactPhone = contactPhone,
                            companyName = companyName.ifBlank { null }
                        )
                    )
                },
                enabled = origin.isNotBlank() &&
                        destination.isNotBlank() &&
                        departureDate.isNotBlank() &&
                        count > 0 &&
                        contactName.isNotBlank() &&
                        contactEmail.isNotBlank() &&
                        contactPhone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
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
private fun CharterDetailsDialog(
    request: CharterRequestDto,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Charter Request Details")
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
                // Charter type
                Text(
                    request.charterType.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6)
                )

                // Route
                Text("${request.originName} → ${request.destinationName}", fontWeight = FontWeight.Bold)
                Text("${request.departureDate}${request.returnDate?.let { " - $it" } ?: " (One-way)"}")

                HorizontalDivider()

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("Passengers", fontSize = 12.sp, color = Color.Gray)
                        Text("${request.passengerCount}")
                    }
                    request.aircraftPreference?.let {
                        Column {
                            Text("Aircraft", fontSize = 12.sp, color = Color.Gray)
                            Text(it)
                        }
                    }
                }

                request.specialRequirements?.let {
                    Text("Special Requirements", fontSize = 12.sp, color = Color.Gray)
                    Text(it)
                }

                HorizontalDivider()

                // Contact
                Text("Contact", fontWeight = FontWeight.SemiBold, color = Color(0xFF8B5CF6))
                Text(request.contactName)
                request.companyName?.let { Text(it) }
                Text(request.contactEmail, fontSize = 12.sp, color = Color.Gray)
                Text(request.contactPhone, fontSize = 12.sp, color = Color.Gray)

                // Quote section
                if (request.status == "QUOTED") {
                    HorizontalDivider()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Quote Received", fontWeight = FontWeight.SemiBold, color = Color(0xFF8B5CF6))
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
                        }
                    }
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

@Composable
private fun CharterRequestCard(
    request: CharterRequestDto,
    accentColor: Color,
    onClick: () -> Unit
) {
    val statusColor = Color(request.status.statusColor())
    val typeColor = when (request.charterType) {
        "HAJJ_UMRAH" -> Color(0xFF22C55E)
        "SPORTS" -> Color(0xFF3B82F6)
        "CORPORATE" -> Color(0xFFF59E0B)
        "GOVERNMENT" -> Color(0xFF8B5CF6)
        else -> accentColor
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
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = request.charterType.replace("_", " "),
                            fontSize = 12.sp,
                            color = typeColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .background(typeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${request.originName} → ${request.destinationName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${request.passengerCount} passengers${request.companyName?.let { " • $it" } ?: ""}",
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
                                color = Color(0xFF8B5CF6)
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
                    color = Color(0xFF8B5CF6)
                )

                Text(
                    text = "Created: ${request.createdAt.take(10)}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharterStationDropdown(
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
