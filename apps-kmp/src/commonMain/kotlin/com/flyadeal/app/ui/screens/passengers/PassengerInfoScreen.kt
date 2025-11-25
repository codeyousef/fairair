package com.flyadeal.app.ui.screens.passengers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.flyadeal.app.navigation.AppScreen
import com.flyadeal.app.ui.components.*
import com.flyadeal.app.ui.screens.ancillaries.AncillariesScreen

/**
 * Passenger information entry screen.
 */
class PassengerInfoScreen : Screen, AppScreen.PassengerInfo {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<PassengerInfoScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Passenger Information",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (uiState.passengers.isNotEmpty()) {
                                Text(
                                    text = "Passenger ${uiState.currentPassengerIndex + 1} of ${uiState.passengers.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { paddingValues ->
            PassengerInfoContent(
                uiState = uiState,
                onFieldChange = screenModel::updatePassengerField,
                onNextPassenger = screenModel::nextPassenger,
                onPreviousPassenger = screenModel::previousPassenger,
                onGoToPassenger = screenModel::goToPassenger,
                onContinue = {
                    screenModel.validateAndProceed {
                        navigator.push(AncillariesScreen())
                    }
                },
                onClearError = screenModel::clearError,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun PassengerInfoContent(
    uiState: PassengerInfoUiState,
    onFieldChange: (String, PassengerField, String) -> Unit,
    onNextPassenger: () -> Unit,
    onPreviousPassenger: () -> Unit,
    onGoToPassenger: (Int) -> Unit,
    onContinue: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPassenger = uiState.currentPassenger

    when {
        uiState.isLoading -> {
            LoadingIndicator(
                message = "Loading...",
                modifier = modifier
            )
        }
        currentPassenger == null -> {
            ErrorDisplay(
                message = uiState.error ?: "No passenger data available",
                modifier = modifier
            )
        }
        else -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { uiState.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Passenger tabs
                PassengerTabs(
                    passengers = uiState.passengers,
                    currentIndex = uiState.currentPassengerIndex,
                    onSelectPassenger = onGoToPassenger
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Passenger type header
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (currentPassenger.type) {
                                    PassengerType.ADULT -> Icons.Default.Person
                                    PassengerType.CHILD -> Icons.Default.Face
                                    PassengerType.INFANT -> Icons.Default.Favorite
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentPassenger.label,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = currentPassenger.type.ageRange,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Personal details
                    item {
                        SectionHeader(title = "Personal Details")
                    }

                    // Title dropdown
                    item {
                        TitleDropdown(
                            selectedTitle = currentPassenger.title,
                            passengerType = currentPassenger.type,
                            onTitleSelected = { onFieldChange(currentPassenger.id, PassengerField.TITLE, it) }
                        )
                    }

                    // Name fields
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = currentPassenger.firstName,
                                onValueChange = { onFieldChange(currentPassenger.id, PassengerField.FIRST_NAME, it) },
                                label = { Text("First Name *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currentPassenger.lastName,
                                onValueChange = { onFieldChange(currentPassenger.id, PassengerField.LAST_NAME, it) },
                                label = { Text("Last Name *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Date of birth
                    item {
                        OutlinedTextField(
                            value = currentPassenger.dateOfBirth,
                            onValueChange = { onFieldChange(currentPassenger.id, PassengerField.DATE_OF_BIRTH, it) },
                            label = { Text("Date of Birth * (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("1990-01-15") }
                        )
                    }

                    // Nationality
                    item {
                        OutlinedTextField(
                            value = currentPassenger.nationality,
                            onValueChange = { onFieldChange(currentPassenger.id, PassengerField.NATIONALITY, it) },
                            label = { Text("Nationality (ISO Code)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("SA") }
                        )
                    }

                    // Travel document section
                    item {
                        SectionHeader(title = "Travel Document")
                    }

                    // Document type
                    item {
                        DocumentTypeDropdown(
                            selectedType = currentPassenger.documentType,
                            onTypeSelected = { onFieldChange(currentPassenger.id, PassengerField.DOCUMENT_TYPE, it) }
                        )
                    }

                    // Document number and expiry
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = currentPassenger.documentNumber,
                                onValueChange = { onFieldChange(currentPassenger.id, PassengerField.DOCUMENT_NUMBER, it) },
                                label = { Text("Document Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currentPassenger.documentExpiry,
                                onValueChange = { onFieldChange(currentPassenger.id, PassengerField.DOCUMENT_EXPIRY, it) },
                                label = { Text("Expiry Date") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("YYYY-MM-DD") }
                            )
                        }
                    }

                    // Contact info only for first adult
                    if (currentPassenger.type == PassengerType.ADULT && currentPassenger.id == "adult_0") {
                        item {
                            SectionHeader(title = "Contact Information")
                        }

                        item {
                            OutlinedTextField(
                                value = currentPassenger.email,
                                onValueChange = { onFieldChange(currentPassenger.id, PassengerField.EMAIL, it) },
                                label = { Text("Email Address *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = currentPassenger.phone,
                                onValueChange = { onFieldChange(currentPassenger.id, PassengerField.PHONE, it) },
                                label = { Text("Phone Number *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                placeholder = { Text("+966 5XX XXX XXXX") }
                            )
                        }
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

                // Bottom navigation
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!uiState.isFirstPassenger) {
                            SecondaryButton(
                                text = "Previous",
                                onClick = onPreviousPassenger,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (uiState.isLastPassenger) {
                            PrimaryButton(
                                text = "Continue",
                                onClick = onContinue,
                                modifier = Modifier.weight(if (uiState.isFirstPassenger) 1f else 1f)
                            )
                        } else {
                            PrimaryButton(
                                text = "Next Passenger",
                                onClick = onNextPassenger,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassengerTabs(
    passengers: List<PassengerFormData>,
    currentIndex: Int,
    onSelectPassenger: (Int) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = currentIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 16.dp
    ) {
        passengers.forEachIndexed { index, passenger ->
            Tab(
                selected = index == currentIndex,
                onClick = { onSelectPassenger(index) },
                text = {
                    Text(
                        text = passenger.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                icon = {
                    Icon(
                        imageVector = when (passenger.type) {
                            PassengerType.ADULT -> Icons.Default.Person
                            PassengerType.CHILD -> Icons.Default.Face
                            PassengerType.INFANT -> Icons.Default.Favorite
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleDropdown(
    selectedTitle: String,
    passengerType: PassengerType,
    onTitleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val titles = when (passengerType) {
        PassengerType.ADULT -> listOf("Mr", "Mrs", "Ms", "Dr")
        PassengerType.CHILD -> listOf("Master", "Miss")
        PassengerType.INFANT -> listOf("Infant")
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedTitle,
            onValueChange = {},
            readOnly = true,
            label = { Text("Title *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            titles.forEach { title ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onTitleSelected(title)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTypeDropdown(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val documentTypes = listOf(
        "PASSPORT" to "Passport",
        "NATIONAL_ID" to "National ID",
        "IQAMA" to "Iqama (Resident ID)"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = documentTypes.find { it.first == selectedType }?.second ?: selectedType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Document Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            documentTypes.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onTypeSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}
