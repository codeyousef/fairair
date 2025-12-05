package com.fairair.app.ui.screens.manage

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.api.*
import com.fairair.contract.dto.CancelBookingResponseDto
import com.fairair.contract.dto.ManageBookingResponseDto
import com.fairair.contract.dto.ModifyBookingResponseDto
import com.fairair.app.ui.components.velocity.GlassCard
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.screens.checkin.CheckInScreen
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Screen for managing existing bookings.
 * Allows users to view, modify, or cancel their bookings.
 */
class ManageBookingScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<ManageBookingScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        VelocityTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                VelocityColors.BackgroundDeep,
                                VelocityColors.BackgroundMid
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    ManageBookingHeader(
                        showBack = uiState.step != ManageStep.ENTER_DETAILS,
                        onBack = { 
                            if (uiState.step == ManageStep.ENTER_DETAILS) {
                                navigator.pop()
                            } else {
                                screenModel.goBack()
                            }
                        }
                    )

                    // Content
                    AnimatedContent(
                        targetState = uiState.step,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                        }
                    ) { step ->
                        when (step) {
                            ManageStep.ENTER_DETAILS -> {
                                RetrieveBookingForm(
                                    pnr = uiState.pnr,
                                    lastName = uiState.lastName,
                                    isLoading = uiState.isLoading,
                                    error = uiState.error,
                                    onPnrChange = screenModel::updatePnr,
                                    onLastNameChange = screenModel::updateLastName,
                                    onSubmit = screenModel::retrieveBooking
                                )
                            }
                            ManageStep.VIEW_BOOKING -> {
                                BookingDetailsView(
                                    booking = uiState.booking!!,
                                    canModify = uiState.canModify,
                                    canCancel = uiState.canCancel,
                                    canCheckIn = uiState.canCheckIn,
                                    onModify = { screenModel.startModification(it) },
                                    onCancel = screenModel::startCancellation,
                                    onCheckIn = { navigator.push(CheckInScreen()) }
                                )
                            }
                            ManageStep.MODIFY -> {
                                ModificationView(
                                    booking = uiState.booking!!,
                                    type = uiState.modificationType!!,
                                    isLoading = uiState.isLoading,
                                    error = uiState.error,
                                    onSubmit = screenModel::submitModification,
                                    onCancel = screenModel::goBack
                                )
                            }
                            ManageStep.MODIFICATION_COMPLETE -> {
                                ModificationCompleteView(
                                    result = uiState.modificationResult!!,
                                    onDone = screenModel::reset
                                )
                            }
                            ManageStep.PAYMENT_REQUIRED -> {
                                PaymentRequiredView(
                                    result = uiState.modificationResult!!,
                                    onPay = { /* Navigate to payment */ },
                                    onCancel = screenModel::goBack
                                )
                            }
                            ManageStep.CONFIRM_CANCEL, ManageStep.CANCELLATION_COMPLETE -> {
                                if (uiState.cancellationResult != null) {
                                    CancellationCompleteView(
                                        result = uiState.cancellationResult!!,
                                        onDone = screenModel::reset
                                    )
                                } else {
                                    BookingDetailsView(
                                        booking = uiState.booking!!,
                                        canModify = uiState.canModify,
                                        canCancel = uiState.canCancel,
                                        canCheckIn = uiState.canCheckIn,
                                        onModify = { screenModel.startModification(it) },
                                        onCancel = screenModel::startCancellation,
                                        onCheckIn = { navigator.push(CheckInScreen()) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Cancel confirmation dialog
                if (uiState.showCancelDialog) {
                    CancelConfirmationDialog(
                        reason = uiState.cancelReason,
                        isLoading = uiState.isLoading,
                        onReasonChange = screenModel::updateCancelReason,
                        onConfirm = screenModel::confirmCancellation,
                        onDismiss = screenModel::dismissCancelDialog
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageBookingHeader(
    showBack: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VelocityColors.TextMain
            )
        }
        
        Text(
            text = "Manage Booking",
            style = MaterialTheme.typography.headlineSmall,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun RetrieveBookingForm(
    pnr: String,
    lastName: String,
    isLoading: Boolean,
    error: String?,
    onPnrChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Icon
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = VelocityColors.Accent,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Find Your Booking",
            style = MaterialTheme.typography.headlineMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Enter your booking reference and last name",
            style = MaterialTheme.typography.bodyMedium,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )
        
        // Form
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PNR Input
                OutlinedTextField(
                    value = pnr,
                    onValueChange = onPnrChange,
                    label = { Text("Booking Reference (PNR)") },
                    placeholder = { Text("e.g., ABC123") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelocityColors.Accent,
                        unfocusedBorderColor = VelocityColors.GlassBorder,
                        focusedLabelColor = VelocityColors.Accent,
                        unfocusedLabelColor = VelocityColors.TextMuted,
                        focusedTextColor = VelocityColors.TextMain,
                        unfocusedTextColor = VelocityColors.TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Last Name Input
                OutlinedTextField(
                    value = lastName,
                    onValueChange = onLastNameChange,
                    label = { Text("Last Name") },
                    placeholder = { Text("e.g., Smith") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelocityColors.Accent,
                        unfocusedBorderColor = VelocityColors.GlassBorder,
                        focusedLabelColor = VelocityColors.Accent,
                        unfocusedLabelColor = VelocityColors.TextMuted,
                        focusedTextColor = VelocityColors.TextMain,
                        unfocusedTextColor = VelocityColors.TextMain
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Error
                AnimatedVisibility(visible = error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Submit Button
                Button(
                    onClick = onSubmit,
                    enabled = pnr.length == 6 && lastName.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Find Booking",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDetailsView(
    booking: ManageBookingResponseDto,
    canModify: Boolean,
    canCancel: Boolean,
    canCheckIn: Boolean,
    onModify: (ModificationType) -> Unit,
    onCancel: () -> Unit,
    onCheckIn: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Badge
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PNR: ${booking.pnr}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold
                )
                
                StatusBadge(status = booking.status)
            }
        }
        
        // Flight Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = VelocityColors.Accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Flight Details",
                            style = MaterialTheme.typography.titleMedium,
                            color = VelocityColors.TextMain,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Route
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = booking.flight.origin,
                                style = MaterialTheme.typography.headlineSmall,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = booking.flight.originName,
                                style = MaterialTheme.typography.bodySmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = booking.flight.flightNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = VelocityColors.Accent
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = VelocityColors.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = booking.flight.destination,
                                style = MaterialTheme.typography.headlineSmall,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = booking.flight.destinationName,
                                style = MaterialTheme.typography.bodySmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = VelocityColors.GlassBorder
                    )
                    
                    // Time and Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoItem(
                            icon = Icons.Default.DateRange,
                            label = "Date",
                            value = booking.flight.departureDate
                        )
                        InfoItem(
                            icon = Icons.Default.Info,
                            label = "Departure",
                            value = booking.flight.departureTime
                        )
                        InfoItem(
                            icon = Icons.Default.Person,
                            label = "Fare",
                            value = booking.flight.fareFamily
                        )
                    }
                }
            }
        }
        
        // Passengers Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = VelocityColors.Accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Passengers",
                            style = MaterialTheme.typography.titleMedium,
                            color = VelocityColors.TextMain,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    booking.passengers.forEach { passenger ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${passenger.title} ${passenger.firstName} ${passenger.lastName}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = VelocityColors.TextMain
                            )
                            Text(
                                text = passenger.type,
                                style = MaterialTheme.typography.bodySmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
        
        // Payment Summary
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = VelocityColors.Accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Payment",
                            style = MaterialTheme.typography.titleMedium,
                            color = VelocityColors.TextMain,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Paid",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VelocityColors.TextMuted
                        )
                        Text(
                            text = booking.payment.totalPaidFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            color = VelocityColors.Accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (booking.payment.lastFourDigits != null) {
                        Text(
                            text = "Paid with •••• ${booking.payment.lastFourDigits}",
                            style = MaterialTheme.typography.bodySmall,
                            color = VelocityColors.TextMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
        
        // Actions
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canCheckIn) {
                    ActionButton(
                        text = "Check In",
                        icon = Icons.Default.Send,
                        primary = true,
                        onClick = onCheckIn
                    )
                }
                
                if (canModify) {
                    ActionButton(
                        text = "Change Flight",
                        icon = Icons.Default.Edit,
                        primary = false,
                        onClick = { onModify(ModificationType.CHANGE_FLIGHT) }
                    )
                }
                
                if (canCancel) {
                    ActionButton(
                        text = "Cancel Booking",
                        icon = Icons.Default.Close,
                        primary = false,
                        destructive = true,
                        onClick = onCancel
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VelocityColors.TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusBadge(status: String) {
    val colorAndText: Pair<Color, String> = when (status.uppercase()) {
        "CONFIRMED" -> VelocityColors.Accent to "Confirmed"
        "CHECKED_IN" -> Color(0xFF4CAF50) to "Checked In"
        "CANCELLED" -> MaterialTheme.colorScheme.error to "Cancelled"
        "PENDING" -> Color(0xFFFF9800) to "Pending"
        else -> VelocityColors.TextMuted to status
    }
    val color = colorAndText.first
    val text = colorAndText.second
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: ImageVector,
    primary: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = when {
        primary -> VelocityColors.Accent
        destructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        else -> VelocityColors.GlassBg
    }
    
    val contentColor = when {
        primary -> Color.White
        destructive -> MaterialTheme.colorScheme.error
        else -> VelocityColors.TextMain
    }
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ModificationView(
    booking: ManageBookingResponseDto,
    type: ModificationType,
    isLoading: Boolean,
    error: String?,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when (type) {
                ModificationType.CHANGE_FLIGHT -> "Change Flight"
                ModificationType.CHANGE_DATE -> "Change Date"
                ModificationType.NAME_CORRECTION -> "Name Correction"
                ModificationType.ADD_ANCILLARY -> "Add Services"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "This feature is coming soon!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VelocityColors.TextMain,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Modification functionality will be available in a future update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VelocityColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.GlassBg)
        ) {
            Text("Go Back", color = VelocityColors.TextMain)
        }
    }
}

@Composable
private fun ModificationCompleteView(
    result: ModifyBookingResponseDto,
    onDone: () -> Unit
) {
    SuccessView(
        title = "Modification Complete",
        message = result.message,
        onDone = onDone
    )
}

@Composable
private fun PaymentRequiredView(
    result: ModifyBookingResponseDto,
    onPay: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = VelocityColors.Accent,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Payment Required",
            style = MaterialTheme.typography.headlineMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Additional payment of ${result.priceDifferenceFormatted} is required.",
            style = MaterialTheme.typography.bodyLarge,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onPay,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
        ) {
            Text("Pay Now", fontWeight = FontWeight.SemiBold)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        TextButton(onClick = onCancel) {
            Text("Cancel", color = VelocityColors.TextMuted)
        }
    }
}

@Composable
private fun CancellationCompleteView(
    result: CancelBookingResponseDto,
    onDone: () -> Unit
) {
    SuccessView(
        title = "Booking Cancelled",
        message = result.message + (result.refundAmountFormatted?.let { "\n\nRefund: $it" } ?: ""),
        onDone = onDone
    )
}

@Composable
private fun SuccessView(
    title: String,
    message: String,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
        ) {
            Text("Done", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CancelConfirmationDialog(
    reason: String,
    isLoading: Boolean,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelocityColors.BackgroundMid,
        title = {
            Text(
                text = "Cancel Booking?",
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to cancel this booking? This action cannot be undone.",
                    color = VelocityColors.TextMuted
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelocityColors.Accent,
                        unfocusedBorderColor = VelocityColors.GlassBorder,
                        focusedTextColor = VelocityColors.TextMain,
                        unfocusedTextColor = VelocityColors.TextMain
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Cancel Booking")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Keep Booking", color = VelocityColors.TextMuted)
            }
        }
    )
}
