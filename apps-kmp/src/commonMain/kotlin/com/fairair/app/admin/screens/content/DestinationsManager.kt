package com.fairair.app.admin.screens.content

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
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
 * Destinations management screen for admin portal.
 * Manages destination marketing content, images, and featured routes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationsManager(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val destinations by adminState.destinations.collectAsState()
    val isLoading by adminState.destinationsLoading.collectAsState()
    
    var editingDestination by remember { mutableStateOf<DestinationContentDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf<DestinationContentDto?>(null) }

    LaunchedEffect(Unit) {
        loadDestinations(adminState, adminApiClient)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Destinations",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.Gray900
                )
                Text(
                    text = "Manage destination content, images, and lowest fares for marketing.",
                    fontSize = 16.sp,
                    color = FairairColors.Gray600,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Purple)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Destination")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Destinations Grid
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = FairairColors.Purple)
            }
        } else if (destinations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = FairairColors.White)
            ) {
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
                            text = "No destinations yet",
                            fontSize = 16.sp,
                            color = FairairColors.Gray500
                        )
                        Text(
                            text = "Add destination content for marketing",
                            fontSize = 14.sp,
                            color = FairairColors.Gray400
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(destinations) { destination ->
                    DestinationCard(
                        destination = destination,
                        onEdit = { editingDestination = destination },
                        onDelete = { deleteDialog = destination },
                        onToggleFeatured = {
                            scope.launch {
                                adminApiClient.updateDestination(
                                    destination.id,
                                    UpdateDestinationRequest(
                                        titleEn = destination.titleEn,
                                        titleAr = destination.titleAr,
                                        descriptionEn = destination.descriptionEn,
                                        descriptionAr = destination.descriptionAr,
                                        imageUrl = destination.imageUrl,
                                        lowestFare = destination.lowestFare,
                                        currency = destination.currency,
                                        isFeatured = !destination.isFeatured,
                                        displayOrder = destination.displayOrder
                                    )
                                )
                                loadDestinations(adminState, adminApiClient)
                            }
                        }
                    )
                }
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        DestinationDialog(
            destination = null,
            onDismiss = { showCreateDialog = false },
            onSaveCreate = { request ->
                scope.launch {
                    adminApiClient.createDestination(request)
                    loadDestinations(adminState, adminApiClient)
                    showCreateDialog = false
                }
            },
            onSaveUpdate = null
        )
    }

    // Edit Dialog
    editingDestination?.let { destination ->
        DestinationDialog(
            destination = destination,
            onDismiss = { editingDestination = null },
            onSaveCreate = null,
            onSaveUpdate = { request ->
                scope.launch {
                    adminApiClient.updateDestination(destination.id, request)
                    loadDestinations(adminState, adminApiClient)
                    editingDestination = null
                }
            }
        )
    }

    // Delete Confirmation
    deleteDialog?.let { destination ->
        AlertDialog(
            onDismissRequest = { deleteDialog = null },
            title = { Text("Delete Destination?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${destination.titleEn}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            adminApiClient.deleteDestination(destination.id)
                            loadDestinations(adminState, adminApiClient)
                            deleteDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DestinationCard(
    destination: DestinationContentDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFeatured: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FairairColors.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder for image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FairairColors.Purple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = FairairColors.Purple
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = destination.titleEn,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FairairColors.Gray900
                    )
                    if (destination.isFeatured) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Featured",
                            modifier = Modifier.size(18.dp),
                            tint = FairairColors.Yellow
                        )
                    }
                }
                
                Text(
                    text = destination.airportCode,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FairairColors.Purple
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                destination.descriptionEn?.let {
                    Text(
                        text = it.take(100) + if (it.length > 100) "..." else "",
                        fontSize = 13.sp,
                        color = FairairColors.Gray600,
                        maxLines = 2
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Lowest Fare
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "From",
                    fontSize = 12.sp,
                    color = FairairColors.Gray500
                )
                Text(
                    text = "${destination.lowestFare ?: "-"} ${destination.currency}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.Purple
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Actions
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = FairairColors.Gray600)
                }
                IconButton(onClick = onToggleFeatured) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = if (destination.isFeatured) "Unfeature" else "Feature",
                        tint = if (destination.isFeatured) FairairColors.Yellow else FairairColors.Gray400
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = FairairColors.Error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationDialog(
    destination: DestinationContentDto?,
    onDismiss: () -> Unit,
    onSaveCreate: ((CreateDestinationRequest) -> Unit)?,
    onSaveUpdate: ((UpdateDestinationRequest) -> Unit)?
) {
    val isEdit = destination != null
    
    var airportCode by remember { mutableStateOf(destination?.airportCode ?: "") }
    var titleEn by remember { mutableStateOf(destination?.titleEn ?: "") }
    var titleAr by remember { mutableStateOf(destination?.titleAr ?: "") }
    var descriptionEn by remember { mutableStateOf(destination?.descriptionEn ?: "") }
    var descriptionAr by remember { mutableStateOf(destination?.descriptionAr ?: "") }
    var imageUrl by remember { mutableStateOf(destination?.imageUrl ?: "") }
    var lowestFare by remember { mutableStateOf(destination?.lowestFare?.toString() ?: "") }
    var currency by remember { mutableStateOf(destination?.currency ?: "SAR") }
    var isFeatured by remember { mutableStateOf(destination?.isFeatured ?: false) }
    var displayOrder by remember { mutableStateOf(destination?.displayOrder?.toString() ?: "0") }
    
    var currencyExpanded by remember { mutableStateOf(false) }
    val currencies = listOf("SAR", "USD", "EUR", "GBP", "AED")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 600.dp),
        title = {
            Text(
                text = if (isEdit) "Edit Destination" else "Add Destination",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Airport Code (only for create)
                if (!isEdit) {
                    OutlinedTextField(
                        value = airportCode,
                        onValueChange = { airportCode = it.uppercase().take(3) },
                        label = { Text("Airport Code (IATA)") },
                        placeholder = { Text("e.g., DXB") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Title English
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (English)") },
                    placeholder = { Text("e.g., Dubai") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title Arabic
                OutlinedTextField(
                    value = titleAr,
                    onValueChange = { titleAr = it },
                    label = { Text("Title (Arabic)") },
                    placeholder = { Text("e.g., دبي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description English
                OutlinedTextField(
                    value = descriptionEn,
                    onValueChange = { descriptionEn = it },
                    label = { Text("Description (English)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description Arabic
                OutlinedTextField(
                    value = descriptionAr,
                    onValueChange = { descriptionAr = it },
                    label = { Text("Description (Arabic)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Image URL
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Image URL") },
                    placeholder = { Text("https://...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Star, null)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Lowest Fare & Currency
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = lowestFare,
                        onValueChange = { lowestFare = it },
                        label = { Text("Lowest Fare") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = it },
                        modifier = Modifier.width(120.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Currency") },
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

                // Display Order
                OutlinedTextField(
                    value = displayOrder,
                    onValueChange = { displayOrder = it },
                    label = { Text("Display Order") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Featured toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isFeatured) FairairColors.Yellow else FairairColors.Gray400
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Featured Destination",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = FairairColors.Gray900
                        )
                        Text(
                            text = "Display prominently on homepage",
                            fontSize = 13.sp,
                            color = FairairColors.Gray600
                        )
                    }
                    Switch(
                        checked = isFeatured,
                        onCheckedChange = { isFeatured = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = FairairColors.Yellow,
                            checkedTrackColor = FairairColors.Yellow.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isEdit && onSaveUpdate != null) {
                        onSaveUpdate(UpdateDestinationRequest(
                            titleEn = titleEn,
                            titleAr = titleAr.ifBlank { null },
                            descriptionEn = descriptionEn.ifBlank { null },
                            descriptionAr = descriptionAr.ifBlank { null },
                            imageUrl = imageUrl.ifBlank { null },
                            lowestFare = lowestFare.toDoubleOrNull()?.toString(),
                            currency = currency,
                            isFeatured = isFeatured,
                            displayOrder = displayOrder.toIntOrNull() ?: 0
                        ))
                    } else if (onSaveCreate != null) {
                        onSaveCreate(CreateDestinationRequest(
                            airportCode = airportCode,
                            titleEn = titleEn,
                            titleAr = titleAr.ifBlank { null },
                            descriptionEn = descriptionEn.ifBlank { null },
                            descriptionAr = descriptionAr.ifBlank { null },
                            imageUrl = imageUrl.ifBlank { null },
                            lowestFare = lowestFare.toDoubleOrNull()?.toString(),
                            currency = currency,
                            isFeatured = isFeatured,
                            displayOrder = displayOrder.toIntOrNull() ?: 0
                        ))
                    }
                },
                enabled = titleEn.isNotBlank() && (isEdit || airportCode.length == 3),
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Purple)
            ) {
                Text(if (isEdit) "Save Changes" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun loadDestinations(adminState: AdminState, adminApiClient: AdminApiClient) {
    adminState.setDestinationsLoading(true)
    
    when (val result = adminApiClient.getAllDestinations()) {
        is AdminApiResult.Success -> adminState.setDestinations(result.data)
        is AdminApiResult.Error -> adminState.setError(result.message)
    }
    
    adminState.setDestinationsLoading(false)
}
