package com.fairair.app.ui.screens.confirmation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.navigation.AppScreen
import com.fairair.app.ui.screens.search.SearchScreen
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Confirmation screen with Velocity design system.
 */
class ConfirmationScreen : Screen, AppScreen.Confirmation {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ConfirmationScreenModel>()
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
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = VelocityColors.Accent)
                        }
                    }
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error ?: "Error",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.Error
                            )
                        }
                    }
                    else -> {
                        ConfirmationContent(
                            uiState = uiState,
                            onNewBooking = {
                                screenModel.startNewBooking {
                                    navigator.replaceAll(SearchScreen())
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationContent(
    uiState: ConfirmationUiState,
    onNewBooking: () -> Unit
) {
    val typography = VelocityTheme.typography

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success Header
        item {
            Spacer(modifier = Modifier.height(24.dp))
            SuccessHeader()
        }

        // PNR Card
        item {
            PnrCard(pnr = uiState.pnr)
        }

        // Flight Details Card
        item {
            FlightDetailsCard(
                originCode = uiState.originCode,
                originCity = uiState.originCity,
                destinationCode = uiState.destinationCode,
                destinationCity = uiState.destinationCity,
                departureDate = uiState.departureDate,
                departureTime = uiState.departureTime,
                arrivalTime = uiState.arrivalTime,
                flightNumber = uiState.flightNumber
            )
        }

        // Passenger Card
        item {
            PassengerCard(
                passengerCount = uiState.passengerCount,
                primaryName = uiState.primaryPassengerName
            )
        }

        // Payment Card
        item {
            PaymentCard(
                totalPrice = uiState.totalPrice,
                currency = uiState.currency,
                status = uiState.bookingStatus
            )
        }

        // Actions
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ActionButtons(onNewBooking = onNewBooking)
        }

        // Info Note
        item {
            InfoNote()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SuccessHeader() {
    val typography = VelocityTheme.typography

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VelocityColors.Accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = VelocityColors.Accent,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Booking Confirmed!",
            style = typography.timeBig,
            color = VelocityColors.Accent
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your flight has been successfully booked",
            style = typography.body,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PnrCard(pnr: String) {
    val typography = VelocityTheme.typography

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = VelocityColors.Accent.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, VelocityColors.Accent.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Booking Reference (PNR)",
                style = typography.labelSmall,
                color = VelocityColors.Accent
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = pnr,
                style = typography.timeBig,
                color = VelocityColors.Accent,
                letterSpacing = androidx.compose.ui.unit.TextUnit(4f, androidx.compose.ui.unit.TextUnitType.Sp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Save this reference for managing your booking",
                style = typography.labelSmall,
                color = VelocityColors.Accent.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FlightDetailsCard(
    originCode: String,
    originCity: String,
    destinationCode: String,
    destinationCity: String,
    departureDate: String,
    departureTime: String,
    arrivalTime: String,
    flightNumber: String
) {
    val typography = VelocityTheme.typography

    VelocityGlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = VelocityColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Flight Details",
                style = typography.body,
                color = VelocityColors.TextMuted
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Departure
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = departureTime,
                    style = typography.timeBig,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = originCode,
                    style = typography.body,
                    color = VelocityColors.Accent
                )
                Text(
                    text = originCity,
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }

            // Flight indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = flightNumber,
                    style = typography.labelSmall,
                    color = VelocityColors.Accent
                )
                Text(
                    text = "-",
                    style = typography.body,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = "Direct",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }

            // Arrival
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = arrivalTime,
                    style = typography.timeBig,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = destinationCode,
                    style = typography.body,
                    color = VelocityColors.Accent
                )
                Text(
                    text = destinationCity,
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = VelocityColors.GlassBorder.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = VelocityColors.TextMuted
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = departureDate,
                style = typography.body,
                color = VelocityColors.TextMain
            )
        }
    }
}

@Composable
private fun PassengerCard(
    passengerCount: Int,
    primaryName: String
) {
    val typography = VelocityTheme.typography

    VelocityGlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.Accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = VelocityColors.Accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Passengers",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = "$primaryName${if (passengerCount > 1) " + ${passengerCount - 1} more" else ""}",
                    style = typography.body,
                    color = VelocityColors.TextMain
                )
            }
        }
    }
}

@Composable
private fun PaymentCard(
    totalPrice: String,
    currency: String,
    status: String
) {
    val typography = VelocityTheme.typography

    VelocityGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VelocityColors.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = VelocityColors.Accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Total Paid",
                        style = typography.labelSmall,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = "$currency $totalPrice",
                        style = typography.timeBig,
                        color = VelocityColors.Accent
                    )
                }
            }

            Surface(
                color = when (status) {
                    "CONFIRMED" -> VelocityColors.Accent.copy(alpha = 0.2f)
                    "PENDING" -> Color(0xFFFFA500).copy(alpha = 0.2f)
                    else -> VelocityColors.Error.copy(alpha = 0.2f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = status,
                    style = typography.labelSmall,
                    color = when (status) {
                        "CONFIRMED" -> VelocityColors.Accent
                        "PENDING" -> Color(0xFFFFA500)
                        else -> VelocityColors.Error
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ActionButtons(onNewBooking: () -> Unit) {
    val typography = VelocityTheme.typography

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onNewBooking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = VelocityColors.Accent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Book Another Flight",
                    style = typography.button,
                    color = VelocityColors.BackgroundDeep
                )
            }
        }

        Surface(
            onClick = { /* Would open e-ticket */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, VelocityColors.GlassBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "View E-Ticket",
                    style = typography.button,
                    color = VelocityColors.TextMain
                )
            }
        }
    }
}

@Composable
private fun InfoNote() {
    val typography = VelocityTheme.typography

    VelocityGlassCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = VelocityColors.Accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "A confirmation email with your e-ticket has been sent to your registered email address. Please check in online 24 hours before departure.",
                style = typography.labelSmall,
                color = VelocityColors.TextMuted
            )
        }
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
