package com.fairair.app.ui.screens.saved

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.contract.dto.BookingConfirmationDto
import com.fairair.app.navigation.AppScreen
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Saved bookings screen with Velocity design system.
 */
class SavedBookingsScreen : Screen, AppScreen.SavedBookings {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<SavedBookingsScreenModel>()
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
                    VelocitySavedHeader(onBack = { navigator.pop() })

                    // Content
                    SavedBookingsContent(
                        uiState = uiState,
                        onBookingClick = screenModel::selectBooking,
                        onDeleteBooking = screenModel::deleteBooking,
                        onClearError = screenModel::clearError,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Booking detail dialog
                uiState.selectedBooking?.let { booking ->
                    BookingDetailDialog(
                        booking = booking,
                        onDismiss = screenModel::clearSelectedBooking
                    )
                }
            }
        }
    }
}

@Composable
private fun VelocitySavedHeader(onBack: () -> Unit) {
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
                text = "Saved Bookings",
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
            Text(
                text = "Access offline",
                style = typography.duration,
                color = VelocityColors.TextMuted
            )
        }
    }
}

@Composable
private fun SavedBookingsContent(
    uiState: SavedBookingsUiState,
    onBookingClick: (BookingConfirmationDto) -> Unit,
    onDeleteBooking: (String) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = VelocityColors.Accent)
            }
        }
        uiState.isEmpty -> {
            EmptyBookingsDisplay(modifier = modifier)
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${uiState.bookings.size} saved booking${if (uiState.bookings.size > 1) "s" else ""}",
                        style = typography.labelSmall,
                        color = VelocityColors.TextMuted
                    )
                }

                items(uiState.bookings, key = { it.pnr }) { booking ->
                    SavedBookingCard(
                        booking = booking,
                        onClick = { onBookingClick(booking) },
                        onDelete = { onDeleteBooking(booking.pnr) }
                    )
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
            }
        }
    }
}

@Composable
private fun EmptyBookingsDisplay(modifier: Modifier = Modifier) {
    val typography = VelocityTheme.typography

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(VelocityColors.GlassBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = VelocityColors.TextMuted
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "No saved bookings",
            style = typography.timeBig,
            color = VelocityColors.TextMain
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your completed bookings will appear here for offline access",
            style = typography.body,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SavedBookingCard(
    booking: BookingConfirmationDto,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val typography = VelocityTheme.typography
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = VelocityColors.GlassBg,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PNR
                Column {
                    Text(
                        text = "PNR",
                        style = typography.labelSmall,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = booking.pnr,
                        style = typography.timeBig,
                        color = VelocityColors.Accent
                    )
                }

                // Status badge
                Surface(
                    color = when (booking.status) {
                        "CONFIRMED" -> VelocityColors.Accent.copy(alpha = 0.2f)
                        "PENDING" -> Color(0xFFFFA500).copy(alpha = 0.2f)
                        else -> VelocityColors.Error.copy(alpha = 0.2f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = booking.status,
                        style = typography.labelSmall,
                        color = when (booking.status) {
                            "CONFIRMED" -> VelocityColors.Accent
                            "PENDING" -> Color(0xFFFFA500)
                            else -> VelocityColors.Error
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = VelocityColors.GlassBorder.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Total paid
                Column {
                    Text(
                        text = "Total Paid",
                        style = typography.labelSmall,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = "${booking.currency} ${booking.totalPrice}",
                        style = typography.body,
                        color = VelocityColors.TextMain
                    )
                }

                // Booking date
                if (booking.createdAt.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Booked",
                            style = typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                        Text(
                            text = booking.createdAt.take(10),
                            style = typography.body,
                            color = VelocityColors.TextMain
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Delete button
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable { showDeleteConfirm = true }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = VelocityColors.Error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Remove",
                    style = typography.labelSmall,
                    color = VelocityColors.Error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = VelocityColors.GlassBg,
            titleContentColor = VelocityColors.TextMain,
            textContentColor = VelocityColors.TextMuted,
            title = { Text("Remove Booking?") },
            text = { Text("This will remove the booking from your saved list. You can always re-fetch it from the server using the PNR.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Remove", color = VelocityColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = VelocityColors.TextMuted)
                }
            }
        )
    }
}

@Composable
private fun BookingDetailDialog(
    booking: BookingConfirmationDto,
    onDismiss: () -> Unit
) {
    val typography = VelocityTheme.typography

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelocityColors.GlassBg,
        titleContentColor = VelocityColors.TextMain,
        textContentColor = VelocityColors.TextMuted,
        title = {
            Column {
                Text(
                    text = "Booking Details",
                    style = typography.timeBig
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PNR: ${booking.pnr}",
                    style = typography.body,
                    color = VelocityColors.Accent
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Status", booking.status)
                DetailRow("Total Paid", "${booking.currency} ${booking.totalPrice}")
                if (booking.bookingReference.isNotEmpty()) {
                    DetailRow("Reference", booking.bookingReference)
                }
                if (booking.createdAt.isNotEmpty()) {
                    DetailRow("Created", booking.createdAt)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VelocityColors.Accent)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    val typography = VelocityTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = typography.body,
            color = VelocityColors.TextMuted
        )
        Text(
            text = value,
            style = typography.body,
            color = VelocityColors.TextMain
        )
    }
}
