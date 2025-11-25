package com.flyadeal.app.ui.screens.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.flyadeal.app.navigation.AppScreen
import com.flyadeal.app.ui.components.*
import com.flyadeal.app.ui.screens.confirmation.ConfirmationScreen

/**
 * Payment screen for entering card details and completing booking.
 */
class PaymentScreen : Screen, AppScreen.Payment {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<PaymentScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Payment",
                            style = MaterialTheme.typography.titleMedium
                        )
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
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total to pay",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "SAR ${uiState.totalPrice}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        PrimaryButton(
                            text = if (uiState.isProcessing) "Processing..." else "Pay Now",
                            onClick = {
                                screenModel.processPayment {
                                    navigator.replaceAll(ConfirmationScreen())
                                }
                            },
                            enabled = uiState.isFormValid && !uiState.isProcessing,
                            loading = uiState.isProcessing
                        )
                    }
                }
            }
        ) { paddingValues ->
            PaymentContent(
                uiState = uiState,
                onCardNumberChange = screenModel::updateCardNumber,
                onCardholderNameChange = screenModel::updateCardholderName,
                onExpiryDateChange = screenModel::updateExpiryDate,
                onCvvChange = screenModel::updateCvv,
                formatCardNumber = screenModel::formatCardNumber,
                formatExpiryDate = screenModel::formatExpiryDate,
                detectCardType = screenModel::detectCardType,
                onClearError = screenModel::clearError,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun PaymentContent(
    uiState: PaymentUiState,
    onCardNumberChange: (String) -> Unit,
    onCardholderNameChange: (String) -> Unit,
    onExpiryDateChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    formatCardNumber: (String) -> String,
    formatExpiryDate: (String) -> String,
    detectCardType: (String) -> CardType,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            LoadingIndicator(
                message = "Loading payment...",
                modifier = modifier
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order Summary
                item {
                    OrderSummaryCard(
                        flightPrice = uiState.flightPrice,
                        ancillariesPrice = uiState.ancillariesPrice,
                        totalPrice = uiState.totalPrice,
                        passengerCount = uiState.passengerCount
                    )
                }

                // Payment Methods Header
                item {
                    SectionHeader(title = "Payment Method")
                }

                // Card Icons
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PaymentMethodBadge(type = CardType.VISA, isActive = detectCardType(uiState.cardNumber) == CardType.VISA)
                        PaymentMethodBadge(type = CardType.MASTERCARD, isActive = detectCardType(uiState.cardNumber) == CardType.MASTERCARD)
                        PaymentMethodBadge(type = CardType.AMEX, isActive = detectCardType(uiState.cardNumber) == CardType.AMEX)
                    }
                }

                // Card Number
                item {
                    OutlinedTextField(
                        value = formatCardNumber(uiState.cardNumber),
                        onValueChange = { value ->
                            onCardNumberChange(value.filter { it.isDigit() })
                        },
                        label = { Text("Card Number") },
                        placeholder = { Text("1234 5678 9012 3456") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        isError = uiState.cardNumberError != null,
                        supportingText = uiState.cardNumberError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Cardholder Name
                item {
                    OutlinedTextField(
                        value = uiState.cardholderName,
                        onValueChange = onCardholderNameChange,
                        label = { Text("Cardholder Name") },
                        placeholder = { Text("John Doe") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        isError = uiState.cardholderNameError != null,
                        supportingText = uiState.cardholderNameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Expiry and CVV
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = formatExpiryDate(uiState.expiryDate),
                            onValueChange = { value ->
                                onExpiryDateChange(value.filter { it.isDigit() })
                            },
                            label = { Text("Expiry (MM/YY)") },
                            placeholder = { Text("12/25") },
                            isError = uiState.expiryDateError != null,
                            supportingText = uiState.expiryDateError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = uiState.cvv,
                            onValueChange = onCvvChange,
                            label = { Text("CVV") },
                            placeholder = { Text("123") },
                            isError = uiState.cvvError != null,
                            supportingText = uiState.cvvError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Security Notice
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Secure Payment",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Your payment information is encrypted and secure",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryCard(
    flightPrice: String,
    ancillariesPrice: String,
    totalPrice: String,
    passengerCount: Int
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Flight ($passengerCount passenger${if (passengerCount > 1) "s" else ""})",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "SAR $flightPrice",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (ancillariesPrice != "0") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Extras",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "SAR $ancillariesPrice",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SAR $totalPrice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodBadge(
    type: CardType,
    isActive: Boolean
) {
    val label = when (type) {
        CardType.VISA -> "VISA"
        CardType.MASTERCARD -> "MC"
        CardType.AMEX -> "AMEX"
        CardType.UNKNOWN -> "Card"
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isActive)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
