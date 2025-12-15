package com.fairair.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.localization.strings
import com.fairair.app.navigation.AppScreen
import com.fairair.contract.dto.*

/**
 * Profile screen for managing saved travelers and payment methods.
 */
class ProfileScreen : Screen, AppScreen.Profile {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ProfileScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val strings = strings()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Profile",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        when (uiState.selectedTab) {
                            ProfileTab.TRAVELERS -> screenModel.showAddTravelerDialog()
                            ProfileTab.PAYMENT_METHODS -> screenModel.showAddPaymentDialog()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Tab Row
                TabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = uiState.selectedTab == ProfileTab.TRAVELERS,
                        onClick = { screenModel.selectTab(ProfileTab.TRAVELERS) },
                        text = { Text("Travelers") },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )
                    Tab(
                        selected = uiState.selectedTab == ProfileTab.PAYMENT_METHODS,
                        onClick = { screenModel.selectTab(ProfileTab.PAYMENT_METHODS) },
                        text = { Text("Payment") },
                        icon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
                    )
                }

                // Content
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.error != null -> {
                        ErrorContent(
                            message = uiState.error!!,
                            onRetry = { screenModel.loadProfile() }
                        )
                    }
                    else -> {
                        when (uiState.selectedTab) {
                            ProfileTab.TRAVELERS -> TravelersContent(
                                travelers = uiState.travelers,
                                onEdit = { screenModel.showEditTravelerDialog(it) },
                                onDelete = { screenModel.deleteTraveler(it.id) },
                                onAddDocument = { screenModel.showAddDocumentDialog(it) },
                                onDeleteDocument = { travelerId, documentId ->
                                    screenModel.deleteDocument(travelerId, documentId)
                                }
                            )
                            ProfileTab.PAYMENT_METHODS -> PaymentMethodsContent(
                                paymentMethods = uiState.paymentMethods,
                                onDelete = { screenModel.deletePaymentMethod(it.id) }
                            )
                        }
                    }
                }
            }
        }

        // Dialogs
        if (uiState.showAddTravelerDialog) {
            AddTravelerDialog(
                editingTraveler = uiState.editingTraveler,
                isSaving = uiState.isSaving,
                error = uiState.saveError,
                onDismiss = { screenModel.dismissTravelerDialog() },
                onSave = { firstName, lastName, dob, gender, nationality, email, phone, isMainTraveler ->
                    screenModel.saveTraveler(firstName, lastName, dob, gender, nationality, email, phone, isMainTraveler)
                }
            )
        }

        if (uiState.showAddDocumentDialog && uiState.selectedTravelerForDocument != null) {
            AddDocumentDialog(
                traveler = uiState.selectedTravelerForDocument!!,
                isSaving = uiState.isSaving,
                error = uiState.saveError,
                onDismiss = { screenModel.dismissDocumentDialog() },
                onSave = { type, number, country, expiry, isDefault ->
                    screenModel.saveDocument(type, number, country, expiry, isDefault)
                }
            )
        }

        if (uiState.showAddPaymentDialog) {
            AddPaymentMethodDialog(
                isSaving = uiState.isSaving,
                error = uiState.saveError,
                onDismiss = { screenModel.dismissPaymentDialog() },
                onSave = { cardToken, type, nickname, isDefault ->
                    screenModel.savePaymentMethod(cardToken, type, nickname, isDefault)
                }
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun TravelersContent(
    travelers: List<SavedTravelerDto>,
    onEdit: (SavedTravelerDto) -> Unit,
    onDelete: (SavedTravelerDto) -> Unit,
    onAddDocument: (SavedTravelerDto) -> Unit,
    onDeleteDocument: (String, String) -> Unit
) {
    if (travelers.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Person,
            title = "No saved travelers",
            subtitle = "Add travelers to speed up booking"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(travelers, key = { it.id }) { traveler ->
                TravelerCard(
                    traveler = traveler,
                    onEdit = { onEdit(traveler) },
                    onDelete = { onDelete(traveler) },
                    onAddDocument = { onAddDocument(traveler) },
                    onDeleteDocument = { docId -> onDeleteDocument(traveler.id, docId) }
                )
            }
        }
    }
}

@Composable
private fun TravelerCard(
    traveler: SavedTravelerDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddDocument: () -> Unit,
    onDeleteDocument: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expandedDocuments by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${traveler.firstName} ${traveler.lastName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (traveler.isMainTraveler) {
                            Text(
                                text = "Main Traveler",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Traveler details
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailItem(label = "Date of Birth", value = traveler.dateOfBirth)
                    DetailItem(label = "Gender", value = traveler.gender.name)
                }
                Column(modifier = Modifier.weight(1f)) {
                    DetailItem(label = "Nationality", value = traveler.nationality)
                    traveler.email?.let { DetailItem(label = "Email", value = it) }
                }
            }

            // Documents section
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDocuments = !expandedDocuments }
                    .pointerHoverIcon(PointerIcon.Hand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Documents (${traveler.documents.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    IconButton(
                        onClick = onAddDocument,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add document",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = if (expandedDocuments) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }

            if (expandedDocuments) {
                Spacer(modifier = Modifier.height(8.dp))
                if (traveler.documents.isEmpty()) {
                    Text(
                        text = "No documents added",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    traveler.documents.forEach { doc ->
                        DocumentItem(
                            document = doc,
                            onDelete = { onDeleteDocument(doc.id) }
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Traveler") },
            text = { Text("Are you sure you want to delete ${traveler.firstName} ${traveler.lastName}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun DocumentItem(
    document: TravelDocumentDto,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.AccountBox,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "${document.type.name}: ${document.number}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${document.issuingCountry} • Expires: ${document.expiryDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete this document?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PaymentMethodsContent(
    paymentMethods: List<SavedPaymentMethodDto>,
    onDelete: (SavedPaymentMethodDto) -> Unit
) {
    if (paymentMethods.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Star,
            title = "No saved payment methods",
            subtitle = "Add a payment method for faster checkout"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(paymentMethods, key = { it.id }) { method ->
                PaymentMethodCard(
                    method = method,
                    onDelete = { onDelete(method) }
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: SavedPaymentMethodDto,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = method.cardBrand?.name ?: method.type.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (method.isDefault) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = "•••• ${method.lastFourDigits}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (method.expiryMonth != null && method.expiryYear != null) {
                        Text(
                            text = "Expires ${method.expiryMonth}/${method.expiryYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    method.nickname?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Payment Method") },
            text = { Text("Are you sure you want to delete this payment method?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ==================== Dialogs ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTravelerDialog(
    editingTraveler: SavedTravelerDto?,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Gender, String, String?, String?, Boolean) -> Unit
) {
    var firstName by remember { mutableStateOf(editingTraveler?.firstName ?: "") }
    var lastName by remember { mutableStateOf(editingTraveler?.lastName ?: "") }
    var dateOfBirth by remember { mutableStateOf(editingTraveler?.dateOfBirth ?: "") }
    var gender by remember { mutableStateOf(editingTraveler?.gender ?: Gender.MALE) }
    var nationality by remember { mutableStateOf(editingTraveler?.nationality ?: "") }
    var email by remember { mutableStateOf(editingTraveler?.email ?: "") }
    var phone by remember { mutableStateOf(editingTraveler?.phone ?: "") }
    var isMainTraveler by remember { mutableStateOf(editingTraveler?.isMainTraveler ?: false) }
    var genderExpanded by remember { mutableStateOf(false) }

    val isValid = firstName.isNotBlank() && lastName.isNotBlank() && 
                  dateOfBirth.isNotBlank() && nationality.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (editingTraveler != null) "Edit Traveler" else "Add Traveler") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("1990-01-15") }
                )
                
                // Gender dropdown
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = it }
                ) {
                    OutlinedTextField(
                        value = gender.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gender *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false }
                    ) {
                        Gender.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    gender = option
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = nationality,
                    onValueChange = { nationality = it.uppercase().take(2) },
                    label = { Text("Nationality (2-letter code) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("SA") }
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isMainTraveler,
                        onCheckedChange = { isMainTraveler = it }
                    )
                    Text("This is me (main traveler)")
                }

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        firstName, lastName, dateOfBirth, gender, nationality,
                        email.takeIf { it.isNotBlank() },
                        phone.takeIf { it.isNotBlank() },
                        isMainTraveler
                    )
                },
                enabled = isValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDocumentDialog(
    traveler: SavedTravelerDto,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (DocumentType, String, String, String, Boolean) -> Unit
) {
    var documentType by remember { mutableStateOf(DocumentType.PASSPORT) }
    var documentNumber by remember { mutableStateOf("") }
    var issuingCountry by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    val isValid = documentNumber.isNotBlank() && issuingCountry.isNotBlank() && expiryDate.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Add Document for ${traveler.firstName}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Document type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = documentType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Document Type *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DocumentType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    documentType = option
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = documentNumber,
                    onValueChange = { documentNumber = it },
                    label = { Text("Document Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = issuingCountry,
                    onValueChange = { issuingCountry = it.uppercase().take(2) },
                    label = { Text("Issuing Country (2-letter code) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("SA") }
                )
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (YYYY-MM-DD) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("2030-01-15") }
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text("Set as primary document")
                }

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(documentType, documentNumber, issuingCountry, expiryDate, isDefault) },
                enabled = isValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPaymentMethodDialog(
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String, PaymentType, String?, Boolean) -> Unit
) {
    var cardToken by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf(PaymentType.CREDIT_CARD) }
    var nickname by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    // In a real app, cardToken would come from a payment provider like Stripe
    val isValid = cardToken.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Add Payment Method") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "In production, this would use a secure payment form from your payment provider (e.g., Stripe Elements).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Payment type dropdown
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = paymentType.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        PaymentType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name.replace("_", " ")) },
                                onClick = {
                                    paymentType = option
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = cardToken,
                    onValueChange = { cardToken = it },
                    label = { Text("Card Token (demo) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("tok_visa_1234") }
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Personal Card") }
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text("Set as default payment method")
                }

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        cardToken,
                        paymentType,
                        nickname.takeIf { it.isNotBlank() },
                        isDefault
                    )
                },
                enabled = isValid && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}
