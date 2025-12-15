package com.fairair.app.admin.screens.b2b

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.admin.api.*
import com.fairair.app.admin.state.AdminState
import com.fairair.app.ui.theme.FairairColors
import kotlinx.coroutines.launch

/**
 * Charter Requests management screen for B2B portal.
 * Manage charter flight requests from agencies and corporate clients.
 */
@Composable
fun CharterRequestsManager(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val charterRequests by adminState.charterRequests.collectAsState()
    val isLoading by adminState.charterRequestsLoading.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var selectedRequest by remember { mutableStateOf<CharterRequestDto?>(null) }

    LaunchedEffect(Unit) {
        loadCharterRequests(adminState, adminApiClient)
    }

    val pendingRequests = charterRequests.filter { it.status == "PENDING" }
    val displayedRequests = if (selectedTab == 0) charterRequests else pendingRequests

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Charter Requests",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = FairairColors.Gray900
        )
        Text(
            text = "Manage charter flight requests for Hajj/Umrah, sports teams, corporate events, and more.",
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
                text = { Text("All Requests (${charterRequests.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pending")
                        if (pendingRequests.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Badge(
                                containerColor = FairairColors.Warning,
                                contentColor = FairairColors.White
                            ) {
                                Text(pendingRequests.size.toString())
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
            } else if (displayedRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = FairairColors.Gray400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedTab == 1) "No pending charter requests" else "No charter requests",
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
                            Text("Type", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Contact", Modifier.weight(1.2f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Route", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Date", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Pax", Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Status", Modifier.weight(0.8f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("", Modifier.width(60.dp))
                        }
                    }

                    items(displayedRequests) { request ->
                        CharterRequestRow(
                            request = request,
                            onClick = { selectedRequest = request }
                        )
                        HorizontalDivider(color = FairairColors.Gray200)
                    }
                }
            }
        }
    }

    // View Details Dialog
    selectedRequest?.let { request ->
        CharterRequestDetailsDialog(
            request = request,
            onDismiss = { selectedRequest = null }
        )
    }
}

@Composable
private fun CharterRequestRow(
    request: CharterRequestDto,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = request.requestNumber,
            modifier = Modifier.weight(0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FairairColors.Purple
        )

        Box(modifier = Modifier.weight(0.8f)) {
            val typeIcon = when (request.charterType) {
                "HAJJ_UMRAH" -> Icons.Default.Home
                "SPORTS_TEAM" -> Icons.Default.Star
                "CORPORATE" -> Icons.Default.Person
                "GOVERNMENT" -> Icons.Default.Home
                "MILITARY" -> Icons.Default.Lock
                else -> Icons.AutoMirrored.Filled.Send
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = FairairColors.Purple
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = request.charterTypeDisplayName,
                    fontSize = 13.sp,
                    color = FairairColors.Gray700
                )
            }
        }

        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = request.contactName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FairairColors.Gray900
            )
            request.companyName?.let {
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
                text = "${request.origin} → ${request.destination}",
                fontSize = 14.sp,
                color = FairairColors.Gray700
            )
        }

        Text(
            text = request.departureDate.take(10),
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
                text = "${request.passengerCount}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = FairairColors.Gray900
            )
        }

        Box(modifier = Modifier.weight(0.8f)) {
            val (statusColor, statusBg) = when (request.status) {
                "PENDING" -> FairairColors.Warning to FairairColors.Warning.copy(alpha = 0.1f)
                "QUOTED" -> FairairColors.Info to FairairColors.Info.copy(alpha = 0.1f)
                "ACCEPTED" -> FairairColors.Success to FairairColors.Success.copy(alpha = 0.1f)
                "COMPLETED" -> FairairColors.Success to FairairColors.Success.copy(alpha = 0.1f)
                "REJECTED", "CANCELLED" -> FairairColors.Gray600 to FairairColors.Gray200
                else -> FairairColors.Gray600 to FairairColors.Gray200
            }
            Text(
                text = request.status.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 12.sp,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Box(modifier = Modifier.width(60.dp)) {
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Info, "View", tint = FairairColors.Gray600)
            }
        }
    }
}

@Composable
private fun CharterRequestDetailsDialog(
    request: CharterRequestDto,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 550.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Send, null, tint = FairairColors.Purple)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Charter Request ${request.requestNumber}", fontWeight = FontWeight.Bold)
                    Text(
                        text = request.charterTypeDisplayName,
                        fontSize = 14.sp,
                        color = FairairColors.Gray600
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Status
                val (statusColor, statusBg) = when (request.status) {
                    "PENDING" -> FairairColors.Warning to FairairColors.Warning.copy(alpha = 0.1f)
                    "QUOTED" -> FairairColors.Info to FairairColors.Info.copy(alpha = 0.1f)
                    "ACCEPTED" -> FairairColors.Success to FairairColors.Success.copy(alpha = 0.1f)
                    else -> FairairColors.Gray600 to FairairColors.Gray200
                }
                Text(
                    text = request.status.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Contact Info
                SectionHeader("Contact Information")
                DetailRow("Name", request.contactName)
                DetailRow("Email", request.contactEmail)
                DetailRow("Phone", request.contactPhone)
                request.companyName?.let { DetailRow("Company", it) }

                Spacer(modifier = Modifier.height(20.dp))

                // Flight Details
                SectionHeader("Flight Details")
                DetailRow("Route", "${request.origin} → ${request.destination}")
                DetailRow("Departure", request.departureDate.take(10))
                request.returnDate?.let { DetailRow("Return", it.take(10)) }
                DetailRow("Passengers", request.passengerCount.toString())

                request.aircraftPreference?.let {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("Aircraft Preference")
                    Text(it, fontSize = 14.sp, color = FairairColors.Gray700)
                }

                request.cateringRequirements?.let {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("Catering Requirements")
                    Text(it, fontSize = 14.sp, color = FairairColors.Gray700)
                }

                request.specialRequirements?.let {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("Special Requirements")
                    Text(it, fontSize = 14.sp, color = FairairColors.Gray700)
                }

                if (request.quotedAmount != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("Quote")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FairairColors.Success.copy(alpha = 0.1f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Quoted Amount", fontSize = 12.sp, color = FairairColors.Gray600)
                            Text(
                                text = "${request.quotedCurrency ?: "SAR"} ${request.quotedAmount}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = FairairColors.Success
                            )
                        }
                        request.quoteValidUntil?.let {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Valid Until", fontSize = 12.sp, color = FairairColors.Gray600)
                                Text(it.take(10), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = FairairColors.Gray900)
                            }
                        }
                    }
                }

                request.notes?.let {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionHeader("Internal Notes")
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = FairairColors.Gray700,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FairairColors.Gray100)
                            .padding(12.dp)
                    )
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
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = FairairColors.Gray900,
        modifier = Modifier.padding(bottom = 8.dp)
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

private suspend fun loadCharterRequests(adminState: AdminState, adminApiClient: AdminApiClient) {
    adminState.setCharterRequestsLoading(true)

    when (val result = adminApiClient.getAllCharterRequests()) {
        is AdminApiResult.Success -> adminState.setCharterRequests(result.data)
        is AdminApiResult.Error -> adminState.setError(result.message)
    }

    adminState.setCharterRequestsLoading(false)
}
