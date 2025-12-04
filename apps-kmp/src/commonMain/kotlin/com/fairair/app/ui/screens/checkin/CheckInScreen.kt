package com.fairair.app.ui.screens.checkin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.api.*
import com.fairair.app.navigation.AppScreen
import com.fairair.app.ui.components.velocity.GlassCard
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Check-in screen with Velocity design system.
 * Allows passengers to check in online using PNR and last name.
 */
class CheckInScreen : Screen, AppScreen.CheckIn {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<CheckInScreenModel>()
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
                    CheckInHeader(onBack = { navigator.pop() })

                    // Content based on step
                    when (uiState.step) {
                        CheckInStep.ENTER_DETAILS -> {
                            CheckInForm(
                                pnr = uiState.pnr,
                                lastName = uiState.lastName,
                                isLoading = uiState.isLoading,
                                error = uiState.error,
                                onPnrChange = screenModel::updatePnr,
                                onLastNameChange = screenModel::updateLastName,
                                onSubmit = screenModel::initiateCheckIn,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        CheckInStep.SELECT_PASSENGERS -> {
                            uiState.lookupResult?.let { lookup ->
                                PassengerSelectionView(
                                    lookup = lookup,
                                    selectedPassengerIds = uiState.selectedPassengerIds,
                                    isLoading = uiState.isLoading,
                                    error = uiState.error,
                                    onPassengerToggle = screenModel::togglePassenger,
                                    onCompleteCheckIn = screenModel::completeCheckIn,
                                    onBack = screenModel::goBackToForm,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        CheckInStep.COMPLETE -> {
                            uiState.checkInResult?.let { result ->
                                CheckInSuccessView(
                                    result = result,
                                    onViewBoardingPass = { passengerId ->
                                        screenModel.getBoardingPass(passengerId)
                                    },
                                    onDone = { navigator.pop() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        CheckInStep.ERROR -> {
                            CheckInErrorView(
                                message = uiState.error ?: "An error occurred",
                                onRetry = screenModel::goBackToForm,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Boarding Pass Dialog
                if (uiState.currentBoardingPass != null) {
                    BoardingPassDialog(
                        boardingPass = uiState.currentBoardingPass!!,
                        onDismiss = screenModel::closeBoardingPass
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInHeader(onBack: () -> Unit) {
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
                text = "Online Check-In",
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
            Text(
                text = "Get your boarding pass",
                style = typography.duration,
                color = VelocityColors.TextMuted
            )
        }
    }
}

@Composable
private fun CheckInForm(
    pnr: String,
    lastName: String,
    isLoading: Boolean,
    error: String?,
    onPnrChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VelocityColors.Accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = VelocityColors.Accent
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ready to check in?",
            style = typography.timeBig,
            color = VelocityColors.TextMain,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your booking reference and last name to check in for your flight",
            style = typography.body,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // PNR Input
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            contentPadding = 0.dp
        ) {
            OutlinedTextField(
                value = pnr,
                onValueChange = { if (it.length <= 6) onPnrChange(it.uppercase()) },
                label = { Text("Booking Reference (PNR)") },
                placeholder = { Text("e.g., ABC123") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Next
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VelocityColors.TextMain,
                    unfocusedTextColor = VelocityColors.TextMain,
                    focusedBorderColor = VelocityColors.Accent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = VelocityColors.Accent,
                    unfocusedLabelColor = VelocityColors.TextMuted,
                    cursorColor = VelocityColors.Accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Last Name Input
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 16.dp,
            contentPadding = 0.dp
        ) {
            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last Name") },
                placeholder = { Text("As shown on booking") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSubmit() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VelocityColors.TextMain,
                    unfocusedTextColor = VelocityColors.TextMain,
                    focusedBorderColor = VelocityColors.Accent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = VelocityColors.Accent,
                    unfocusedLabelColor = VelocityColors.TextMuted,
                    cursorColor = VelocityColors.Accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // Error message
        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = VelocityColors.Error.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = VelocityColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        style = typography.body,
                        color = VelocityColors.Error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Submit button
        Button(
            onClick = onSubmit,
            enabled = pnr.length == 6 && lastName.length >= 2 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerHoverIcon(PointerIcon.Hand),
            colors = ButtonDefaults.buttonColors(
                containerColor = VelocityColors.Accent,
                contentColor = VelocityColors.BackgroundDeep,
                disabledContainerColor = VelocityColors.Accent.copy(alpha = 0.3f),
                disabledContentColor = VelocityColors.BackgroundDeep.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = VelocityColors.BackgroundDeep,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Find Booking",
                    style = typography.button
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PassengerSelectionView(
    lookup: CheckInLookupResponseDto,
    selectedPassengerIds: Set<String>,
    isLoading: Boolean,
    error: String?,
    onPassengerToggle: (String) -> Unit,
    onCompleteCheckIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography
    val flight = lookup.flight

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Flight summary
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = flight.flightNumber,
                        style = typography.timeBig,
                        color = VelocityColors.Accent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = flight.origin,
                                style = typography.body,
                                color = VelocityColors.TextMain
                            )
                            Text(
                                text = flight.departureTime,
                                style = typography.labelSmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = VelocityColors.TextMuted
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = flight.destination,
                                style = typography.body,
                                color = VelocityColors.TextMain
                            )
                            Text(
                                text = flight.arrivalTime,
                                style = typography.labelSmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = VelocityColors.GlassBorder.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Date: ${flight.departureDate}",
                        style = typography.body,
                        color = VelocityColors.TextMain
                    )
                }
            }
        }

        // Check-in status
        item {
            val statusColor = if (lookup.isEligibleForCheckIn) VelocityColors.Success else VelocityColors.Warning
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (lookup.isEligibleForCheckIn) Icons.Default.CheckCircle else Icons.Default.DateRange,
                        contentDescription = null,
                        tint = statusColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (lookup.isEligibleForCheckIn) 
                            "Check-in is open" 
                        else 
                            lookup.eligibilityMessage ?: "Check-in not yet available",
                        style = typography.body,
                        color = statusColor
                    )
                }
            }
        }

        // Error message
        if (error != null) {
            item {
                Surface(
                    color = VelocityColors.Error.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = VelocityColors.Error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = typography.body,
                            color = VelocityColors.Error
                        )
                    }
                }
            }
        }

        // Passengers section header
        item {
            Text(
                text = "Select Passengers",
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
        }

        // Passenger cards
        items(lookup.passengers) { passenger ->
            PassengerCheckInCard(
                passenger = passenger,
                isSelected = passenger.passengerId in selectedPassengerIds,
                onToggle = { onPassengerToggle(passenger.passengerId) },
                enabled = !passenger.isCheckedIn && lookup.isEligibleForCheckIn
            )
        }

        // Complete check-in button
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VelocityColors.TextMain
                    ),
                    border = BorderStroke(1.dp, VelocityColors.GlassBorder),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Back")
                }
                Button(
                    onClick = onCompleteCheckIn,
                    enabled = selectedPassengerIds.isNotEmpty() && lookup.isEligibleForCheckIn && !isLoading,
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp)
                        .pointerHoverIcon(PointerIcon.Hand),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent,
                        contentColor = VelocityColors.BackgroundDeep
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = VelocityColors.BackgroundDeep,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Complete Check-In")
                    }
                }
            }
        }
    }
}

@Composable
private fun PassengerCheckInCard(
    passenger: CheckInPassengerDto,
    isSelected: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean = true
) {
    val typography = VelocityTheme.typography

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onToggle() },
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    enabled = enabled,
                    colors = CheckboxDefaults.colors(
                        checkedColor = VelocityColors.Accent,
                        uncheckedColor = VelocityColors.TextMuted,
                        checkmarkColor = VelocityColors.BackgroundDeep
                    )
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = "${passenger.firstName} ${passenger.lastName}",
                        style = typography.body,
                        color = if (enabled) VelocityColors.TextMain else VelocityColors.TextMuted
                    )
                    Text(
                        text = passenger.type.replaceFirstChar { it.uppercase() },
                        style = typography.labelSmall,
                        color = VelocityColors.TextMuted
                    )
                }
            }
            
            // Status indicator
            if (passenger.isCheckedIn) {
                Surface(
                    color = VelocityColors.Success.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VelocityColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Checked In",
                            style = typography.labelSmall,
                            color = VelocityColors.Success
                        )
                    }
                }
            }
            
            // Show seat if assigned
            passenger.seatAssignment?.let { seat ->
                Surface(
                    color = VelocityColors.Accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Seat $seat",
                        style = typography.labelSmall,
                        color = VelocityColors.Accent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInSuccessView(
    result: CheckInResultDto,
    onViewBoardingPass: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Success icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(VelocityColors.Success.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = VelocityColors.Success
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Check-In Complete!",
            style = typography.timeBig,
            color = VelocityColors.TextMain,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = result.message,
            style = typography.body,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Checked-in passengers
        result.checkedInPassengers.forEach { passenger ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp
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
                            tint = VelocityColors.Accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = passenger.name,
                                style = typography.body,
                                color = VelocityColors.TextMain
                            )
                            Row {
                                Text(
                                    text = "Seat ${passenger.seatNumber}",
                                    style = typography.labelSmall,
                                    color = VelocityColors.Accent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• Group ${passenger.boardingGroup}",
                                    style = typography.labelSmall,
                                    color = VelocityColors.TextMuted
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { onViewBoardingPass(passenger.passengerId) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View Boarding Pass",
                            tint = VelocityColors.Accent
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        // Done button
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerHoverIcon(PointerIcon.Hand),
            colors = ButtonDefaults.buttonColors(
                containerColor = VelocityColors.Accent,
                contentColor = VelocityColors.BackgroundDeep
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Done",
                style = typography.button
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BoardingPassDialog(
    boardingPass: BoardingPassDto,
    onDismiss: () -> Unit
) {
    val typography = VelocityTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelocityColors.BackgroundMid,
        title = {
            Text(
                text = "Boarding Pass",
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Passenger name
                Text(
                    text = boardingPass.passengerName,
                    style = typography.body,
                    color = VelocityColors.TextMain
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Flight details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = boardingPass.origin,
                            style = typography.timeBig,
                            color = VelocityColors.TextMain
                        )
                        Text(
                            text = boardingPass.departureTime,
                            style = typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = VelocityColors.Accent
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = boardingPass.destination,
                            style = typography.timeBig,
                            color = VelocityColors.TextMain
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                HorizontalDivider(color = VelocityColors.GlassBorder)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Details grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FLIGHT",
                            style = typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                        Text(
                            text = boardingPass.flightNumber,
                            style = typography.body,
                            color = VelocityColors.TextMain
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SEAT",
                            style = typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                        Text(
                            text = boardingPass.seatNumber,
                            style = typography.body,
                            color = VelocityColors.Accent
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "GROUP",
                            style = typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                        Text(
                            text = boardingPass.boardingGroup,
                            style = typography.body,
                            color = VelocityColors.TextMain
                        )
                    }
                }
                
                if (boardingPass.gate != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GATE",
                                style = typography.labelSmall,
                                color = VelocityColors.TextMuted
                            )
                            Text(
                                text = boardingPass.gate!!,
                                style = typography.body,
                                color = VelocityColors.TextMain
                            )
                        }
                        if (boardingPass.boardingTime != null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "BOARDING",
                                    style = typography.labelSmall,
                                    color = VelocityColors.TextMuted
                                )
                                Text(
                                    text = boardingPass.boardingTime!!,
                                    style = typography.body,
                                    color = VelocityColors.TextMain
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // QR Code placeholder
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(120.dp),
                        tint = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = boardingPass.pnr,
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = VelocityColors.Accent
                )
            }
        }
    )
}

@Composable
private fun CheckInErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Error icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(VelocityColors.Error.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = VelocityColors.Error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Check-In Failed",
            style = typography.timeBig,
            color = VelocityColors.TextMain,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = typography.body,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .pointerHoverIcon(PointerIcon.Hand),
            colors = ButtonDefaults.buttonColors(
                containerColor = VelocityColors.Accent,
                contentColor = VelocityColors.BackgroundDeep
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Try Again",
                style = typography.button
            )
        }
    }
}
