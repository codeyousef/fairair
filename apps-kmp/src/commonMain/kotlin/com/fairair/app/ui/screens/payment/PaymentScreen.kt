package com.fairair.app.ui.screens.payment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.navigation.AppScreen
import com.fairair.app.ui.screens.confirmation.ConfirmationScreen
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Payment screen with Velocity design system.
 */
class PaymentScreen : Screen, AppScreen.Payment {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<PaymentScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        VelocityTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                VelocityColors.GradientStart,
                                VelocityColors.GradientEnd
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    // Header
                    VelocityPaymentHeader(onBack = { navigator.pop() })

                    // Content
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = VelocityColors.Accent)
                                }
                            }
                            else -> {
                                PaymentContent(
                                    uiState = uiState,
                                    onCardNumberChange = screenModel::updateCardNumber,
                                    onCardholderNameChange = screenModel::updateCardholderName,
                                    onExpiryDateChange = screenModel::updateExpiryDate,
                                    onCvvChange = screenModel::updateCvv,
                                    formatCardNumber = screenModel::formatCardNumber,
                                    formatExpiryDate = screenModel::formatExpiryDate,
                                    detectCardType = screenModel::detectCardType,
                                    onClearError = screenModel::clearError
                                )
                            }
                        }
                    }

                    // Bottom bar
                    VelocityPaymentBottomBar(
                        totalPrice = uiState.totalPrice,
                        isProcessing = uiState.isProcessing,
                        isFormValid = uiState.isFormValid,
                        onPayNow = {
                            screenModel.processPayment {
                                navigator.replaceAll(ConfirmationScreen())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VelocityPaymentHeader(onBack: () -> Unit) {
    val typography = VelocityTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VelocityColors.TextMain
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Payment",
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
            Text(
                text = "Secure checkout",
                style = typography.duration,
                color = VelocityColors.TextMuted
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
    onClearError: () -> Unit
) {
    val typography = VelocityTheme.typography

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Order Summary Card
        item {
            VelocityGlassCard {
                Text(
                    text = "Order Summary",
                    style = typography.body,
                    color = VelocityColors.TextMuted,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                SummaryRow("Flight (${uiState.passengerCount} pax)", "SAR ${uiState.flightPrice}")

                if (uiState.ancillariesPrice != "0") {
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow("Extras", "SAR ${uiState.ancillariesPrice}")
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = VelocityColors.GlassBorder.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = typography.body,
                        color = VelocityColors.TextMain
                    )
                    Text(
                        text = "SAR ${uiState.totalPrice}",
                        style = typography.timeBig,
                        color = VelocityColors.Accent
                    )
                }
            }
        }

        // Payment Method Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(VelocityColors.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = VelocityColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Payment Method",
                        style = typography.body,
                        color = VelocityColors.TextMain
                    )
                    Text(
                        text = "256-bit SSL encrypted",
                        style = typography.labelSmall,
                        color = VelocityColors.TextMuted
                    )
                }
            }
        }

        // Card Type Badges
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardTypeBadge("VISA", detectCardType(uiState.cardNumber) == CardType.VISA)
                CardTypeBadge("MC", detectCardType(uiState.cardNumber) == CardType.MASTERCARD)
                CardTypeBadge("AMEX", detectCardType(uiState.cardNumber) == CardType.AMEX)
            }
        }

        // Card Form
        item {
            VelocityGlassCard {
                // Card Number
                VelocityPaymentField(
                    value = formatCardNumber(uiState.cardNumber),
                    onValueChange = { onCardNumberChange(it.filter { c -> c.isDigit() }) },
                    label = "Card Number",
                    placeholder = "1234 5678 9012 3456",
                    keyboardType = KeyboardType.Number,
                    error = uiState.cardNumberError,
                    leadingIcon = Icons.Default.Lock
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cardholder Name
                VelocityPaymentField(
                    value = uiState.cardholderName,
                    onValueChange = onCardholderNameChange,
                    label = "Cardholder Name",
                    placeholder = "JOHN DOE",
                    error = uiState.cardholderNameError,
                    leadingIcon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Expiry and CVV Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        VelocityPaymentField(
                            value = formatExpiryDate(uiState.expiryDate),
                            onValueChange = { onExpiryDateChange(it.filter { c -> c.isDigit() }) },
                            label = "Expiry",
                            placeholder = "MM/YY",
                            keyboardType = KeyboardType.Number,
                            error = uiState.expiryDateError
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        VelocityPaymentField(
                            value = uiState.cvv,
                            onValueChange = onCvvChange,
                            label = "CVV",
                            placeholder = "123",
                            keyboardType = KeyboardType.Number,
                            isPassword = true,
                            error = uiState.cvvError
                        )
                    }
                }
            }
        }

        // Security Notice
        item {
            VelocityGlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = VelocityColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Secure Payment",
                            style = typography.button,
                            color = VelocityColors.TextMain
                        )
                        Text(
                            text = "Your payment information is encrypted and secure",
                            style = typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                }
            }
        }

        // Error message
        if (uiState.error != null) {
            item {
                Surface(
                    color = VelocityColors.Error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
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
                            style = typography.body,
                            color = VelocityColors.Error,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearError) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = VelocityColors.Error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    val typography = VelocityTheme.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = typography.labelSmall, color = VelocityColors.TextMuted)
        Text(text = value, style = typography.labelSmall, color = VelocityColors.TextMuted)
    }
}

@Composable
private fun CardTypeBadge(label: String, isActive: Boolean) {
    val typography = VelocityTheme.typography
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) VelocityColors.Accent else Color.Transparent,
        border = BorderStroke(1.dp, if (isActive) VelocityColors.Accent else VelocityColors.GlassBorder)
    ) {
        Text(
            text = label,
            style = typography.labelSmall,
            color = if (isActive) VelocityColors.BackgroundDeep else VelocityColors.TextMuted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun VelocityGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VelocityColors.GlassBg,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun VelocityPaymentField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    error: String? = null,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val typography = VelocityTheme.typography

    Column {
        Text(
            text = label,
            style = typography.labelSmall,
            color = if (error != null) VelocityColors.Error else VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(
                    1.dp,
                    if (error != null) VelocityColors.Error else VelocityColors.GlassBorder.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = VelocityColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = typography.body.copy(color = VelocityColors.TextMain),
                cursorBrush = SolidColor(VelocityColors.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = typography.body,
                                color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
        if (error != null) {
            Text(
                text = error,
                style = typography.labelSmall,
                color = VelocityColors.Error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun VelocityPaymentBottomBar(
    totalPrice: String,
    isProcessing: Boolean,
    isFormValid: Boolean,
    onPayNow: () -> Unit
) {
    val typography = VelocityTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        VelocityColors.BackgroundDeep.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total to pay",
                    style = typography.body,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = "SAR $totalPrice",
                    style = typography.timeBig,
                    color = VelocityColors.Accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onPayNow,
                enabled = isFormValid && !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                color = if (isFormValid && !isProcessing) VelocityColors.Accent else VelocityColors.GlassBorder
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            color = VelocityColors.BackgroundDeep,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Pay Now",
                            style = typography.button,
                            color = if (isFormValid) VelocityColors.BackgroundDeep else VelocityColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}
