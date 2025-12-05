package com.fairair.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.api.FairairApiClient
import kotlinx.browser.window
import com.fairair.app.api.FlightDto
import com.fairair.contract.dto.UserInfoDto
import com.fairair.app.localization.AppLanguage
import com.fairair.app.localization.LocalizationProvider
import com.fairair.app.localization.LocalizationState
import com.fairair.app.localization.rememberLocalizationState
import com.fairair.app.state.BookingFlowState
import com.fairair.app.persistence.LocalStorage
import com.fairair.app.ui.screens.results.*
import com.fairair.app.ui.screens.search.VelocitySearchScreen
import com.fairair.app.ui.screens.search.VelocitySearchScreenError
import com.fairair.app.ui.screens.search.VelocitySearchScreenLoading
import com.fairair.app.ui.screens.landing.LandingScreen
import com.fairair.app.ui.components.velocity.GlassCard
import com.fairair.app.ui.theme.NotoKufiArabicFontFamily
import com.fairair.app.ui.theme.SpaceGroteskFontFamily
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme
import com.fairair.app.ui.theme.VelocityThemeWithBackground
import com.fairair.app.viewmodel.*
import kotlinx.coroutines.launch
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * Wasm-specific application entry point that bypasses Voyager Navigator.
 *
 * Voyager's Navigator and Screen classes cause JsException in Wasm due to
 * internal serialization/reflection mechanisms that aren't compatible with
 * the Wasm runtime. This implementation uses direct composable rendering
 * with state-based navigation instead.
 *
 * This provides a complete booking flow:
 * Search -> Results -> Passengers -> Payment -> Confirmation
 */
@Composable
fun WasmApp() {
    KoinContext {
        WasmAppContent()
    }
}

/**
 * Navigation state for Wasm app.
 * Since we can't use Voyager, we use simple enum-based navigation.
 */
private enum class WasmScreen {
    LANDING,
    SEARCH,
    RESULTS,
    SEAT_SELECTION,
    PASSENGERS,
    PAYMENT,
    CONFIRMATION,
    SETTINGS,
    SAVED_BOOKINGS,
    LOGIN,
    CHECK_IN,
    MANAGE_BOOKING,
    MEMBERSHIP,
    HELP
}

@Composable
private fun WasmAppContent() {
    val apiClient = koinInject<FairairApiClient>()
    val bookingFlowState = koinInject<BookingFlowState>()
    val localizationState = rememberLocalizationState()
    val scope = rememberCoroutineScope()

    // Parse initial screen from URL hash
    fun parseScreenFromHash(): WasmScreen {
        val hash = window.location.hash.removePrefix("#")
        return when (hash) {
            "search" -> WasmScreen.SEARCH
            "results" -> WasmScreen.RESULTS
            "seats" -> WasmScreen.SEAT_SELECTION
            "passengers" -> WasmScreen.PASSENGERS
            "payment" -> WasmScreen.PAYMENT
            "confirmation" -> WasmScreen.CONFIRMATION
            "settings" -> WasmScreen.SETTINGS
            "bookings" -> WasmScreen.SAVED_BOOKINGS
            "login" -> WasmScreen.LOGIN
            "checkin" -> WasmScreen.CHECK_IN
            "manage" -> WasmScreen.MANAGE_BOOKING
            "membership" -> WasmScreen.MEMBERSHIP
            "help" -> WasmScreen.HELP
            else -> WasmScreen.LANDING
        }
    }
    
    // Navigation state - initialized from URL hash
    var currentScreen by remember { mutableStateOf(parseScreenFromHash()) }
    var previousScreen by remember { mutableStateOf(WasmScreen.LANDING) }
    var isNavigatingBack by remember { mutableStateOf(false) }
    
    // Helper function for navigating back - falls back to landing if no history
    fun navigateBack() {
        // If we have history (more than 1 entry), use browser back
        // Otherwise, navigate directly to landing page
        if (window.history.length > 1) {
            window.history.back()
        } else {
            // No history, navigate directly
            currentScreen = WasmScreen.LANDING
        }
    }
    
    // Update URL hash when screen changes (only if not navigating via browser back/forward)
    LaunchedEffect(currentScreen) {
        if (isNavigatingBack) {
            isNavigatingBack = false
            return@LaunchedEffect
        }
        val hash = when (currentScreen) {
            WasmScreen.LANDING -> ""
            WasmScreen.SEARCH -> "search"
            WasmScreen.RESULTS -> "results"
            WasmScreen.SEAT_SELECTION -> "seats"
            WasmScreen.PASSENGERS -> "passengers"
            WasmScreen.PAYMENT -> "payment"
            WasmScreen.CONFIRMATION -> "confirmation"
            WasmScreen.SETTINGS -> "settings"
            WasmScreen.SAVED_BOOKINGS -> "bookings"
            WasmScreen.LOGIN -> "login"
            WasmScreen.CHECK_IN -> "checkin"
            WasmScreen.MANAGE_BOOKING -> "manage"
            WasmScreen.MEMBERSHIP -> "membership"
            WasmScreen.HELP -> "help"
        }
        val newUrl = if (hash.isEmpty()) "/" else "#$hash"
        if (window.location.hash != "#$hash" && window.location.pathname + window.location.hash != newUrl) {
            window.history.pushState(null, "", newUrl)
        }
    }
    
    // Handle browser back/forward buttons (popstate event)
    DisposableEffect(Unit) {
        val callback: (org.w3c.dom.events.Event) -> Unit = {
            isNavigatingBack = true
            currentScreen = parseScreenFromHash()
        }
        window.addEventListener("popstate", callback)
        onDispose {
            window.removeEventListener("popstate", callback)
        }
    }
    
    // LocalStorage for persisting auth state
    val localStorage = koinInject<LocalStorage>()
    
    // User state - initialized from localStorage for persistence across refreshes
    var currentUser by remember { mutableStateOf(localStorage.getCurrentUser()) }
    var authToken by remember { mutableStateOf(localStorage.getAuthToken()) }
    val isEmployee = currentUser?.role == "EMPLOYEE"

    // ViewModels - created once and reused
    val searchViewModel = remember {
        WasmSearchViewModel(apiClient, bookingFlowState, scope)
    }
    val bookingViewModel = remember {
        WasmBookingViewModel(apiClient, bookingFlowState, scope)
    }

    // Wrap in localization provider
    LocalizationProvider(localizationState) {
        VelocityTheme(isRtl = localizationState.isRtl) {
            when (currentScreen) {
                WasmScreen.LANDING -> {
                    LandingScreen(
                            onFlyNowClick = { currentScreen = WasmScreen.SEARCH },
                            onLoginClick = { currentScreen = WasmScreen.LOGIN },
                            onLogoutClick = {
                                localStorage.clearAuth()
                                currentUser = null
                                authToken = null
                            },
                            onMyBookingsClick = { currentScreen = WasmScreen.SAVED_BOOKINGS },
                            onSettingsClick = {
                                previousScreen = WasmScreen.LANDING
                                currentScreen = WasmScreen.SETTINGS
                            },
                            onCheckInClick = {
                                previousScreen = WasmScreen.LANDING
                                currentScreen = WasmScreen.CHECK_IN
                            },
                            onManageBookingClick = {
                                previousScreen = WasmScreen.LANDING
                                currentScreen = WasmScreen.MANAGE_BOOKING
                            },
                            onMembershipClick = {
                                previousScreen = WasmScreen.LANDING
                                currentScreen = WasmScreen.MEMBERSHIP
                            },
                            onHotelsClick = {
                                com.fairair.app.util.UrlOpener.openUrl(
                                    com.fairair.app.util.ExternalLinks.buildHotelSearchUrl()
                                )
                            },
                            onCarRentalClick = {
                                com.fairair.app.util.UrlOpener.openUrl(
                                    com.fairair.app.util.ExternalLinks.buildCarRentalUrl()
                                )
                            },
                            onHelpClick = {
                                previousScreen = WasmScreen.LANDING
                                currentScreen = WasmScreen.HELP
                            },
                            onDealClick = { origin, destination ->
                                searchViewModel.preselectRoute(origin, destination)
                                currentScreen = WasmScreen.SEARCH
                            },
                            onDestinationClick = { destination ->
                                searchViewModel.preselectDestination(destination)
                                currentScreen = WasmScreen.SEARCH
                            },
                            userName = currentUser?.firstName,
                            isRtl = localizationState.isRtl
                        )
                }
                WasmScreen.LOGIN -> {
                    WasmLoginScreen(
                        onLogin = { user, token -> 
                            // Persist auth state to localStorage (only if values are present)
                            token?.let { localStorage.saveAuthToken(it) }
                            user?.let { localStorage.saveCurrentUser(it) }
                            currentUser = user
                            authToken = token
                            currentScreen = WasmScreen.LANDING 
                        },
                        onBack = { navigateBack() }
                    )
                }
                WasmScreen.SEARCH -> {
                    WasmSearchScreenContainer(
                        viewModel = searchViewModel,
                        localizationState = localizationState,
                        onBack = { navigateBack() },
                        onNavigateToResults = {
                            bookingViewModel.initializeResults()
                            currentScreen = WasmScreen.RESULTS
                        },
                        onNavigateToSettings = {
                            previousScreen = WasmScreen.SEARCH
                            currentScreen = WasmScreen.SETTINGS
                        },
                        onNavigateToSavedBookings = { currentScreen = WasmScreen.SAVED_BOOKINGS }
                    )
                }
                WasmScreen.RESULTS -> {
                    WasmResultsScreenContainer(
                        viewModel = bookingViewModel,
                        localizationState = localizationState,
                        isEmployee = isEmployee,
                        onBack = { navigateBack() },
                        onContinue = {
                            if (isEmployee) {
                                // Employees skip seat selection
                                bookingViewModel.initializePassengerForms()
                                currentScreen = WasmScreen.PASSENGERS
                            } else {
                                // Non-employees go to seat selection
                                bookingViewModel.initializeSeatSelection()
                                currentScreen = WasmScreen.SEAT_SELECTION
                            }
                        }
                    )
                }
                WasmScreen.SEAT_SELECTION -> {
                    WasmSeatSelectionScreen(
                        viewModel = bookingViewModel,
                        localizationState = localizationState,
                        onBack = { navigateBack() },
                        onContinue = {
                            bookingViewModel.initializePassengerForms()
                            currentScreen = WasmScreen.PASSENGERS
                        }
                    )
                }
                WasmScreen.PASSENGERS -> {
                    WasmPassengerScreenContainer(
                        viewModel = bookingViewModel,
                        localizationState = localizationState,
                        onBack = { navigateBack() },
                        onContinue = {
                            bookingViewModel.initializePayment()
                            currentScreen = WasmScreen.PAYMENT
                        }
                    )
                }
                WasmScreen.PAYMENT -> {
                    WasmPaymentScreenContainer(
                        viewModel = bookingViewModel,
                        localizationState = localizationState,
                        authToken = authToken,
                        onBack = { navigateBack() },
                        onSuccess = {
                            bookingViewModel.initializeConfirmation()
                            currentScreen = WasmScreen.CONFIRMATION
                        }
                    )
                }
                WasmScreen.CONFIRMATION -> {
                    WasmConfirmationScreenContainer(
                        viewModel = bookingViewModel,
                        localizationState = localizationState,
                        onNewBooking = {
                            bookingViewModel.resetForNewBooking()
                            searchViewModel.retry()
                            currentScreen = WasmScreen.SEARCH
                        }
                    )
                }
                WasmScreen.SETTINGS -> {
                    WasmSettingsScreen(
                        localizationState = localizationState,
                        onBack = { navigateBack() }
                    )
                }
                WasmScreen.SAVED_BOOKINGS -> {
                    WasmSavedBookingsScreen(
                        authToken = authToken,
                        onBack = { navigateBack() },
                        onTokenExpired = {
                            // Clear invalid auth data
                            localStorage.clearAuth()
                            authToken = null
                            currentUser = null
                        },
                        onNavigateToLogin = {
                            currentScreen = WasmScreen.LOGIN
                        }
                    )
                }
                WasmScreen.CHECK_IN -> {
                    WasmCheckInScreen(
                        apiClient = apiClient,
                        onBack = { navigateBack() }
                    )
                }
                WasmScreen.MANAGE_BOOKING -> {
                    WasmManageBookingScreen(
                        apiClient = apiClient,
                        onBack = { navigateBack() },
                        onNavigateToCheckIn = { currentScreen = WasmScreen.CHECK_IN }
                    )
                }
                WasmScreen.MEMBERSHIP -> {
                    WasmMembershipScreen(
                        apiClient = apiClient,
                        localStorage = localStorage,
                        onBack = { navigateBack() },
                        onNavigateToLogin = { currentScreen = WasmScreen.LOGIN }
                    )
                }
                WasmScreen.HELP -> {
                    com.fairair.app.ui.screens.help.HelpScreen(
                        onBack = { navigateBack() },
                        onContactUs = {
                            com.fairair.app.util.UrlOpener.openUrl(
                                com.fairair.app.util.ExternalLinks.buildContactUrl()
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WasmSearchScreenContainer(
    viewModel: WasmSearchViewModel,
    localizationState: LocalizationState,
    onBack: () -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSavedBookings: () -> Unit
) {
    val velocityState by viewModel.velocityState.collectAsState()
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl

    when {
        velocityState.isLoading -> {
            VelocitySearchScreenLoading(isRtl = isRtl)
        }
        velocityState.error != null && velocityState.availableOrigins.isEmpty() -> {
            VelocitySearchScreenError(
                message = velocityState.error ?: "An error occurred",
                onRetry = viewModel::retry,
                isRtl = isRtl,
                strings = strings
            )
        }
        else -> {
            VelocitySearchScreen(
                state = velocityState,
                strings = strings,
                isRtl = isRtl,
                onBack = onBack,
                onOriginSelect = viewModel::selectVelocityOrigin,
                onDestinationSelect = viewModel::selectVelocityDestination,
                onDateSelect = viewModel::selectVelocityDate,
                onPassengerSelect = viewModel::setVelocityPassengerCount,
                onFieldActivate = viewModel::setActiveField,
                onSearch = {
                    viewModel.searchFromVelocity {
                        onNavigateToResults()
                    }
                },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToSavedBookings = onNavigateToSavedBookings,
                onMonthChange = { year, month ->
                    viewModel.fetchLowFaresForMonth(year, month)
                }
            )
        }
    }
}

@Composable
private fun WasmResultsScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    isEmployee: Boolean = false,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val state by viewModel.resultsState.collectAsState()
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl

    val resultsState = VelocityResultsState(
        isVisible = true,
        isLoading = state.isLoading,
        flights = state.velocityFlights,
        expandedFlightId = state.expandedFlightId,
        selectedFare = null,
        error = state.error
    )

    VelocityThemeWithBackground(isRtl = isRtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Back button at top
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = VelocityColors.TextMain
                )
            }

            // Results overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                VelocityResultsOverlay(
                    state = resultsState,
                    originCode = state.originCode,
                    destinationCode = state.destinationCode,
                    formattedDate = state.departureDate,
                    isEmployee = isEmployee,
                    flights = state.flights,
                    onFlightClick = { velocityFlight ->
                        val flight = state.flights.find { it.flightNumber == velocityFlight.id }
                        if (flight != null) {
                            viewModel.selectFlight(flight)
                        }
                    },
                    onFareSelect = { fareFamily ->
                        if (viewModel.selectFare(fareFamily)) {
                            onContinue()
                        }
                    },
                    onStandbySelect = { flight ->
                        // Create standby booking with SAR 100 price
                        if (viewModel.selectStandbyFare(flight)) {
                            onContinue()
                        }
                    },
                    onClose = onBack,
                    onRetry = { /* reload results */ },
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun WasmSeatSelectionScreen(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val state by viewModel.seatSelectionState.collectAsState()
    val isRtl = localizationState.isRtl
    
    VelocityThemeWithBackground(isRtl = isRtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
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
                        text = "Select Seats",
                        style = VelocityTheme.typography.timeBig,
                        color = VelocityColors.TextMain
                    )
                    Text(
                        text = "${state.originCode} → ${state.destinationCode}",
                        style = VelocityTheme.typography.duration,
                        color = VelocityColors.TextMuted
                    )
                }
            }
            
            // Passenger count info
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = VelocityColors.Accent.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = VelocityColors.Accent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select ${state.passengerCount} seat${if (state.passengerCount > 1) "s" else ""}",
                            style = VelocityTheme.typography.body,
                            color = VelocityColors.TextMain
                        )
                    }
                    Text(
                        text = "${state.selectedSeats.size}/${state.passengerCount} selected",
                        style = VelocityTheme.typography.duration,
                        color = VelocityColors.Accent
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SeatLegendItem(color = VelocityColors.GlassBg, label = "Available")
                SeatLegendItem(color = VelocityColors.Accent, label = "Selected")
                SeatLegendItem(color = Color(0xFF4B5563), label = "Occupied")
                SeatLegendItem(color = Color(0xFF7C3AED), label = "Premium")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Aircraft seat map
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cockpit indicator
                    item {
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(60.dp)
                                .background(
                                    VelocityColors.GlassBg,
                                    RoundedCornerShape(topStart = 100.dp, topEnd = 100.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✈ FRONT",
                                style = VelocityTheme.typography.labelSmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Column headers
                    item {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("A", "B", "C", "", "D", "E", "F").forEach { col ->
                                Box(
                                    modifier = Modifier.size(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (col.isNotEmpty()) {
                                        Text(
                                            text = col,
                                            style = VelocityTheme.typography.duration,
                                            color = VelocityColors.TextMuted
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    
                    // Seat rows
                    items(state.seatMap.size) { index ->
                        val row = state.seatMap[index]
                        SeatRowComposable(
                            row = row,
                            selectedSeats = state.selectedSeats,
                            onSeatClick = { seat -> viewModel.toggleSeatSelection(seat) }
                        )
                        
                        // Exit row indicator
                        if (row.isExitRow) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "← EXIT",
                                    style = VelocityTheme.typography.labelSmall,
                                    color = Color(0xFF22C55E)
                                )
                                Spacer(modifier = Modifier.width(100.dp))
                                Text(
                                    text = "EXIT →",
                                    style = VelocityTheme.typography.labelSmall,
                                    color = Color(0xFF22C55E)
                                )
                            }
                        }
                    }
                    
                    // Tail indicator
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(40.dp)
                                .background(
                                    VelocityColors.GlassBg,
                                    RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "REAR",
                                style = VelocityTheme.typography.labelSmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            
            // Bottom bar with price and continue button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelocityColors.BackgroundMid,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Selected seats summary
                    if (state.selectedSeats.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Selected: ${state.selectedSeats.joinToString(", ")}",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMain
                            )
                            if (state.totalSeatPrice > 0) {
                                Text(
                                    text = "+SAR ${state.totalSeatPrice}",
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.Accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Skip button
                        OutlinedButton(
                            onClick = {
                                viewModel.skipSeatSelection()
                                onContinue()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = VelocityColors.TextMuted
                            ),
                            border = BorderStroke(1.dp, VelocityColors.GlassBorder)
                        ) {
                            Text("Skip (Random)")
                        }
                        
                        // Continue button
                        Button(
                            onClick = onContinue,
                            modifier = Modifier.weight(1f),
                            enabled = state.selectedSeats.size == state.passengerCount,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VelocityColors.Accent,
                                disabledContainerColor = VelocityColors.GlassBorder
                            )
                        ) {
                            Text(
                                text = "Continue",
                                color = if (state.selectedSeats.size == state.passengerCount) 
                                    Color.White else VelocityColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted
        )
    }
}

@Composable
private fun SeatRowComposable(
    row: com.fairair.app.viewmodel.SeatRow,
    selectedSeats: List<String>,
    onSeatClick: (com.fairair.app.viewmodel.Seat) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side seats (A, B, C)
        row.seats.take(3).forEach { seat ->
            SeatComposable(
                seat = seat,
                isSelected = seat.id in selectedSeats,
                onClick = { onSeatClick(seat) }
            )
        }
        
        // Aisle with row number
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${row.rowNumber}",
                style = VelocityTheme.typography.duration,
                color = VelocityColors.TextMuted
            )
        }
        
        // Right side seats (D, E, F)
        row.seats.drop(3).forEach { seat ->
            SeatComposable(
                seat = seat,
                isSelected = seat.id in selectedSeats,
                onClick = { onSeatClick(seat) }
            )
        }
    }
}

@Composable
private fun SeatComposable(
    seat: com.fairair.app.viewmodel.Seat,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> VelocityColors.Accent
        !seat.isAvailable -> Color(0xFF4B5563)
        seat.isPremium -> Color(0xFF7C3AED).copy(alpha = 0.3f)
        seat.isExitRow -> Color(0xFF22C55E).copy(alpha = 0.2f)
        else -> VelocityColors.GlassBg
    }
    
    val borderColor = when {
        isSelected -> VelocityColors.Accent
        !seat.isAvailable -> Color.Transparent
        seat.isPremium -> Color(0xFF7C3AED)
        seat.isExitRow -> Color(0xFF22C55E)
        else -> VelocityColors.GlassBorder
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .then(
                if (seat.isAvailable) {
                    Modifier
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(onClick = onClick)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (seat.isAvailable) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = seat.column,
                    style = VelocityTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold
                )
                if (seat.price > 0 && !isSelected) {
                    Text(
                        text = "+${seat.price}",
                        style = VelocityTheme.typography.labelSmall.copy(
                            fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)
                        ),
                        color = VelocityColors.TextMuted
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Occupied",
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun WasmPassengerScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val state by viewModel.passengerState.collectAsState()
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl
    val currentPassenger = state.currentPassenger

    VelocityThemeWithBackground(isRtl = isRtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header with glassmorphic style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Back button
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

                // Title centered
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Passengers",
                        style = VelocityTheme.typography.timeBig,
                        color = VelocityColors.TextMain
                    )
                    if (state.passengers.isNotEmpty()) {
                        Text(
                            text = "${state.currentIndex + 1} of ${state.passengers.size}",
                            style = VelocityTheme.typography.duration,
                            color = VelocityColors.TextMuted
                        )
                    }
                }
            }

            // Progress indicator - dots style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.passengers.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == state.currentIndex) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= state.currentIndex) VelocityColors.Accent
                                else VelocityColors.GlassBorder
                            )
                    )
                    if (index < state.passengers.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(
                                    if (index < state.currentIndex) VelocityColors.Accent
                                    else VelocityColors.GlassBorder
                                )
                        )
                    }
                }
            }

            if (currentPassenger != null) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Passenger type badge
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = VelocityColors.Accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = when (currentPassenger.type) {
                                            "ADULT" -> Icons.Default.Person
                                            "CHILD" -> Icons.Default.Face
                                            else -> Icons.Default.Favorite
                                        },
                                        contentDescription = null,
                                        tint = VelocityColors.Accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = currentPassenger.label,
                                        style = VelocityTheme.typography.button,
                                        color = VelocityColors.Accent
                                    )
                                }
                            }
                        }
                    }

                    // Personal Details Card
                    item {
                        VelocityFormCard(title = "Personal Details") {
                            // Title selector
                            VelocityChipSelector(
                                label = "Title",
                                options = when (currentPassenger.type) {
                                    "ADULT" -> listOf("Mr", "Mrs", "Ms", "Dr")
                                    "CHILD" -> listOf("Master", "Miss")
                                    else -> listOf("Infant")
                                },
                                selectedOption = currentPassenger.title,
                                onSelect = { viewModel.updatePassengerField(PassengerFormField.TITLE, it) }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Name fields
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.firstName,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.FIRST_NAME, it) },
                                    label = "First Name",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.lastName,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.LAST_NAME, it) },
                                    label = "Last Name",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Date of birth and nationality
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityDateTextField(
                                    value = currentPassenger.dateOfBirth,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.DATE_OF_BIRTH, it) },
                                    label = "Date of Birth",
                                    placeholder = "YYYY-MM-DD",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityGlassTextField(
                                    value = currentPassenger.nationality,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.NATIONALITY, it) },
                                    label = "Nationality",
                                    placeholder = "SA",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Travel Document Card
                    item {
                        VelocityFormCard(title = "Travel Document") {
                            // Document type chips
                            VelocityChipSelector(
                                label = "Document Type",
                                options = listOf("Passport", "National ID", "Iqama"),
                                selectedOption = when (currentPassenger.documentType) {
                                    "PASSPORT" -> "Passport"
                                    "NATIONAL_ID" -> "National ID"
                                    "IQAMA" -> "Iqama"
                                    else -> "Passport"
                                },
                                onSelect = { selected ->
                                    val code = when (selected) {
                                        "Passport" -> "PASSPORT"
                                        "National ID" -> "NATIONAL_ID"
                                        "Iqama" -> "IQAMA"
                                        else -> "PASSPORT"
                                    }
                                    viewModel.updatePassengerField(PassengerFormField.DOCUMENT_TYPE, code)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Document number and expiry
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityGlassTextField(
                                    value = currentPassenger.documentNumber,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.DOCUMENT_NUMBER, it) },
                                    label = "Document Number",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityDateTextField(
                                    value = currentPassenger.documentExpiry,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.DOCUMENT_EXPIRY, it) },
                                    label = "Expiry Date",
                                    placeholder = "YYYY-MM-DD",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Contact Info Card (only for primary adult)
                    if (currentPassenger.id == "adult_0") {
                        item {
                            VelocityFormCard(title = "Contact Information") {
                                VelocityGlassTextField(
                                    value = currentPassenger.email,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.EMAIL, it) },
                                    label = "Email Address",
                                    placeholder = "your@email.com",
                                    keyboardType = KeyboardType.Email
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                VelocityGlassTextField(
                                    value = currentPassenger.phone,
                                    onValueChange = { viewModel.updatePassengerField(PassengerFormField.PHONE, it) },
                                    label = "Phone Number",
                                    placeholder = "+966 5XX XXX XXXX",
                                    keyboardType = KeyboardType.Phone
                                )
                            }
                        }
                    }

                    // Error message
                    if (state.error != null) {
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
                                        text = state.error ?: "",
                                        style = VelocityTheme.typography.body,
                                        color = VelocityColors.Error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.clearPassengerError() }) {
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

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }

                // Bottom navigation with glassmorphic buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    VelocityColors.BackgroundDeep.copy(alpha = 0.9f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!state.isFirstPassenger) {
                            // Previous button - glass style
                            Surface(
                                onClick = { viewModel.previousPassenger() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = VelocityColors.GlassBg,
                                border = BorderStroke(1.dp, VelocityColors.GlassBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "Previous",
                                        style = VelocityTheme.typography.button,
                                        color = VelocityColors.TextMain
                                    )
                                }
                            }
                        }

                        // Continue/Next button - accent style
                        Surface(
                            onClick = {
                                if (state.isLastPassenger) {
                                    if (viewModel.validatePassengers()) {
                                        onContinue()
                                    }
                                } else {
                                    viewModel.nextPassenger()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = VelocityColors.Accent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (state.isLastPassenger) "Continue to Payment" else "Next Passenger",
                                    style = VelocityTheme.typography.button,
                                    color = VelocityColors.BackgroundDeep
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Glassmorphic form card container.
 */
@Composable
private fun VelocityFormCard(
    title: String,
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
            Text(
                text = title,
                style = VelocityTheme.typography.body,
                color = VelocityColors.TextMuted
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

/**
 * Glassmorphic text field matching Velocity design.
 */
@Composable
private fun VelocityGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            textStyle = VelocityTheme.typography.body.copy(color = VelocityColors.TextMain),
            cursorBrush = SolidColor(VelocityColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = VelocityTheme.typography.body,
                            color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Specialized date text field with auto-dash insertion and proper cursor management.
 * Handles YYYY-MM-DD format with automatic dash insertion after year and month.
 * Includes validation for month (01-12) and day (01-31 depending on month).
 */
@Composable
private fun VelocityDateTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "YYYY-MM-DD"
) {
    // Track the TextFieldValue internally to manage cursor position
    var textFieldValue by remember(value) { 
        mutableStateOf(TextFieldValue(value, TextRange(value.length))) 
    }
    
    // Validation error message
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Validate the date and return error message if invalid
    fun validateDate(dateStr: String): String? {
        if (dateStr.length < 10) return null // Not complete yet
        
        val parts = dateStr.split("-")
        if (parts.size != 3) return null
        
        val year = parts[0].toIntOrNull() ?: return "Invalid year"
        val month = parts[1].toIntOrNull() ?: return "Invalid month"
        val day = parts[2].toIntOrNull() ?: return "Invalid day"
        
        // Validate year (reasonable range)
        if (year < 1900 || year > 2100) {
            return "Year must be 1900-2100"
        }
        
        // Validate month
        if (month < 1 || month > 12) {
            return "Month must be 01-12"
        }
        
        // Validate day based on month
        val maxDays = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
            else -> 31
        }
        
        if (day < 1 || day > maxDays) {
            return when (month) {
                2 -> if (maxDays == 29) "Day must be 01-29 for Feb in leap year" else "Day must be 01-28 for Feb"
                4, 6, 9, 11 -> "Day must be 01-30 for this month"
                else -> "Day must be 01-31"
            }
        }
        
        return null
    }
    
    // Sync external value changes
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, TextRange(value.length))
        }
        errorMessage = validateDate(value)
    }
    
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newTextFieldValue ->
                val oldText = textFieldValue.text
                val newText = newTextFieldValue.text
                
                // Filter to only digits and dashes
                val filtered = newText.filter { it.isDigit() || it == '-' }
                
                // If deleting, just update without auto-dash
                if (filtered.length <= oldText.length) {
                    val limitedFiltered = filtered.take(10)
                    textFieldValue = TextFieldValue(
                        text = limitedFiltered,
                        selection = TextRange(minOf(newTextFieldValue.selection.start, limitedFiltered.length))
                    )
                    onValueChange(limitedFiltered)
                    errorMessage = validateDate(limitedFiltered)
                    return@BasicTextField
                }
                
                // Get only digits
                val digits = filtered.filter { it.isDigit() }
                
                // Build formatted string with auto-dashes
                val formatted = buildString {
                    for (i in digits.indices) {
                        if (i == 4 && length == 4) append('-')
                        if (i == 6 && length == 7) append('-')
                        if (length < 10) append(digits[i])
                    }
                }
                
                // Calculate new cursor position
                // If we added a dash, cursor should be after the dash
                val oldDigitCount = oldText.count { it.isDigit() }
                val newDigitCount = formatted.count { it.isDigit() }
                val cursorPos = when {
                    newDigitCount == 5 && oldDigitCount == 4 -> 6 // Just added 5th digit, cursor after dash
                    newDigitCount == 7 && oldDigitCount == 6 -> 9 // Just added 7th digit, cursor after dash
                    else -> formatted.length
                }
                
                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(cursorPos)
                )
                onValueChange(formatted)
                errorMessage = validateDate(formatted)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(
                    width = 1.dp, 
                    color = if (errorMessage != null) VelocityColors.Error else VelocityColors.GlassBorder.copy(alpha = 0.5f), 
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp),
            textStyle = VelocityTheme.typography.body.copy(color = VelocityColors.TextMain),
            cursorBrush = SolidColor(VelocityColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = VelocityTheme.typography.body,
                            color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // Error message tooltip
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage ?: "",
                style = VelocityTheme.typography.labelSmall,
                color = VelocityColors.Error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Specialized date text field for card expiry (MM/YY format) with auto-slash.
 * Handles MM/YY format with automatic slash insertion after month.
 * Includes validation for month (01-12) and expiry date.
 */
@Composable
private fun VelocityCardExpiryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "MM/YY"
) {
    // Track the TextFieldValue internally to manage cursor position
    var textFieldValue by remember(value) { 
        mutableStateOf(TextFieldValue(value, TextRange(value.length))) 
    }
    
    // Validation error message
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Validate the expiry date
    fun validateExpiry(expiryStr: String): String? {
        if (expiryStr.length < 5) return null // Not complete yet
        
        val parts = expiryStr.split("/")
        if (parts.size != 2) return null
        
        val month = parts[0].toIntOrNull() ?: return "Invalid month"
        val year = parts[1].toIntOrNull() ?: return "Invalid year"
        
        // Validate month
        if (month < 1 || month > 12) {
            return "Month must be 01-12"
        }
        
        // Validate not expired (assuming 20XX for the year)
        val fullYear = 2000 + year
        val currentYear = 2025 // Could use actual date
        val currentMonth = 12
        
        if (fullYear < currentYear || (fullYear == currentYear && month < currentMonth)) {
            return "Card has expired"
        }
        
        return null
    }
    
    // Sync external value changes
    LaunchedEffect(value) {
        if (textFieldValue.text != value) {
            textFieldValue = TextFieldValue(value, TextRange(value.length))
        }
        errorMessage = validateExpiry(value)
    }
    
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newTextFieldValue ->
                val oldText = textFieldValue.text
                val newText = newTextFieldValue.text
                
                // Filter to only digits and slash
                val filtered = newText.filter { it.isDigit() || it == '/' }
                
                // If deleting, just update without auto-slash
                if (filtered.length <= oldText.length) {
                    val limitedFiltered = filtered.take(5)
                    textFieldValue = TextFieldValue(
                        text = limitedFiltered,
                        selection = TextRange(minOf(newTextFieldValue.selection.start, limitedFiltered.length))
                    )
                    onValueChange(limitedFiltered)
                    errorMessage = validateExpiry(limitedFiltered)
                    return@BasicTextField
                }
                
                // Get only digits
                val digits = filtered.filter { it.isDigit() }
                
                // Build formatted string with auto-slash (MM/YY)
                val formatted = buildString {
                    for (i in digits.indices) {
                        if (i == 2 && length == 2) append('/')
                        if (length < 5) append(digits[i])
                    }
                }
                
                // Calculate new cursor position
                // If we added a slash, cursor should be after the slash
                val oldDigitCount = oldText.count { it.isDigit() }
                val newDigitCount = formatted.count { it.isDigit() }
                val cursorPos = when {
                    newDigitCount == 3 && oldDigitCount == 2 -> 4 // Just added 3rd digit, cursor after slash
                    else -> formatted.length
                }
                
                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(cursorPos)
                )
                onValueChange(formatted)
                errorMessage = validateExpiry(formatted)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = if (errorMessage != null) VelocityColors.Error else VelocityColors.GlassBorder.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp),
            textStyle = VelocityTheme.typography.body.copy(color = VelocityColors.TextMain),
            cursorBrush = SolidColor(VelocityColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = VelocityTheme.typography.body,
                            color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // Error message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage ?: "",
                style = VelocityTheme.typography.labelSmall,
                color = VelocityColors.Error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Specialized text field for credit card number with auto-spacing every 4 digits.
 * Only accepts digits and displays them as XXXX XXXX XXXX XXXX.
 */
@Composable
private fun VelocityCardNumberTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "•••• •••• •••• ••••"
) {
    // Format display value with spaces every 4 digits
    val displayValue = value.filter { it.isDigit() }.take(16).chunked(4).joinToString(" ")
    
    // Track the TextFieldValue internally to manage cursor position
    var textFieldValue by remember(displayValue) { 
        mutableStateOf(TextFieldValue(displayValue, TextRange(displayValue.length))) 
    }
    
    // Sync external value changes
    LaunchedEffect(displayValue) {
        if (textFieldValue.text != displayValue) {
            textFieldValue = TextFieldValue(displayValue, TextRange(displayValue.length))
        }
    }
    
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newTextFieldValue ->
                // Extract only digits from input
                val digits = newTextFieldValue.text.filter { it.isDigit() }.take(16)
                
                // Pass raw digits to ViewModel
                onValueChange(digits)
                
                // Format for display
                val formatted = digits.chunked(4).joinToString(" ")
                
                // Calculate cursor position
                val cursorPos = minOf(newTextFieldValue.selection.start, formatted.length)
                
                textFieldValue = TextFieldValue(
                    text = formatted,
                    selection = TextRange(cursorPos)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            textStyle = VelocityTheme.typography.body.copy(color = VelocityColors.TextMain),
            cursorBrush = SolidColor(VelocityColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = VelocityTheme.typography.body,
                            color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Specialized text field for CVV - digits only, max 4, obscured display.
 */
@Composable
private fun VelocityCvvTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "•••"
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                // Only allow digits, max 4
                val filtered = newValue.filter { it.isDigit() }.take(4)
                onValueChange(filtered)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(VelocityColors.BackgroundDeep.copy(alpha = 0.5f))
                .border(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp),
            textStyle = VelocityTheme.typography.body.copy(color = VelocityColors.TextMain),
            cursorBrush = SolidColor(VelocityColors.Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = VelocityTheme.typography.body,
                            color = VelocityColors.TextMuted.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Chip selector for options like Title, Document Type.
 */
@Composable
private fun VelocityChipSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = VelocityTheme.typography.labelSmall,
            color = VelocityColors.TextMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) VelocityColors.Accent else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder
                    )
                ) {
                    Text(
                        text = option,
                        style = VelocityTheme.typography.button,
                        color = if (isSelected) VelocityColors.BackgroundDeep else VelocityColors.TextMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WasmPaymentScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    authToken: String?,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.paymentState.collectAsState()
    val isRtl = localizationState.isRtl

    VelocityThemeWithBackground(isRtl = isRtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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
                    text = "Payment",
                    style = VelocityTheme.typography.timeBig,
                    color = VelocityColors.TextMain
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Booking summary card
                item {
                    Surface(
                        color = VelocityColors.GlassBg,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Booking Summary",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Flight",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                                Text(
                                    text = state.flightNumber,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Fare",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                                Text(
                                    text = state.fareFamily,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.Accent
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Passengers",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                                Text(
                                    text = "${state.passengerCount} × ${state.currency} ${state.pricePerPerson}",
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = VelocityColors.GlassBorder
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                                Text(
                                    text = "${state.currency} ${state.totalPrice}",
                                    style = VelocityTheme.typography.timeBig,
                                    color = VelocityColors.Accent
                                )
                            }
                        }
                    }
                }

                // Payment form card
                item {
                    Surface(
                        color = VelocityColors.GlassBg,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Card Details",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            WasmTextField(
                                value = state.cardholderName,
                                onValueChange = { viewModel.updatePaymentField(PaymentFormField.CARDHOLDER_NAME, it) },
                                label = "Cardholder Name *"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            VelocityCardNumberTextField(
                                value = state.cardNumber,
                                onValueChange = { viewModel.updatePaymentField(PaymentFormField.CARD_NUMBER, it) },
                                label = "Card Number *"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                VelocityCardExpiryTextField(
                                    value = state.expiryDate,
                                    onValueChange = { viewModel.updatePaymentField(PaymentFormField.EXPIRY, it) },
                                    label = "Expiry *",
                                    modifier = Modifier.weight(1f)
                                )
                                VelocityCvvTextField(
                                    value = state.cvv,
                                    onValueChange = { viewModel.updatePaymentField(PaymentFormField.CVV, it) },
                                    label = "CVV *",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Error message
                if (state.error != null) {
                    item {
                        Surface(
                            color = VelocityColors.Error.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.error ?: "",
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.Error,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.clearPaymentError() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = VelocityColors.Error
                                    )
                                }
                            }
                        }
                    }
                }

                // Secure payment notice
                item {
                    Surface(
                        color = VelocityColors.Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = VelocityColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Your payment information is encrypted and secure. We never store your full card details.",
                                style = VelocityTheme.typography.labelSmall,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            // Pay button
            Surface(
                color = VelocityColors.GlassBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { viewModel.processPayment(authToken, onSuccess) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !state.isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent,
                        contentColor = VelocityColors.BackgroundDeep
                    )
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = VelocityColors.BackgroundDeep,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (state.isProcessing) "Processing..." else "Pay ${state.currency} ${state.totalPrice}"
                    )
                }
            }
        }
    }
}

@Composable
private fun WasmConfirmationScreenContainer(
    viewModel: WasmBookingViewModel,
    localizationState: LocalizationState,
    onNewBooking: () -> Unit
) {
    val state by viewModel.confirmationState.collectAsState()
    val isRtl = localizationState.isRtl

    VelocityThemeWithBackground(isRtl = isRtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success header
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(VelocityColors.Success.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = VelocityColors.Success,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Booking Confirmed!",
                    style = VelocityTheme.typography.heroTitle,
                    color = VelocityColors.Success
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your flight has been successfully booked",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            }

            // PNR Card
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.Accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Booking Reference (PNR)",
                            style = VelocityTheme.typography.duration,
                            color = VelocityColors.TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.pnr,
                            style = VelocityTheme.typography.heroTitle,
                            color = VelocityColors.Accent,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Save this reference for managing your booking",
                            style = VelocityTheme.typography.labelSmall,
                            color = VelocityColors.TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Flight details card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = VelocityColors.Accent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Flight Details",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMain
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = state.departureTime,
                                    style = VelocityTheme.typography.timeBig,
                                    color = VelocityColors.TextMain
                                )
                                Text(
                                    text = state.originCode,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.Accent
                                )
                                Text(
                                    text = state.originCity,
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.flightNumber,
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.Accent
                                )
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = VelocityColors.Accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Direct",
                                    style = VelocityTheme.typography.labelSmall,
                                    color = VelocityColors.TextMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = state.arrivalTime,
                                    style = VelocityTheme.typography.timeBig,
                                    color = VelocityColors.TextMain
                                )
                                Text(
                                    text = state.destinationCode,
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.Accent
                                )
                                Text(
                                    text = state.destinationCity,
                                    style = VelocityTheme.typography.duration,
                                    color = VelocityColors.TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = VelocityColors.GlassBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = state.departureDate,
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMain
                            )
                        }
                    }
                }
            }

            // Passenger info card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = VelocityColors.Accent
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Passengers",
                                style = VelocityTheme.typography.duration,
                                color = VelocityColors.TextMuted
                            )
                            Text(
                                text = "${state.primaryPassengerName}${if (state.passengerCount > 1) " + ${state.passengerCount - 1} more" else ""}",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMain
                            )
                        }
                    }
                }
            }

            // Payment summary card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = VelocityColors.Accent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Paid",
                                    style = VelocityTheme.typography.body,
                                    color = VelocityColors.TextMain
                                )
                            }
                            Text(
                                text = "${state.currency} ${state.totalPrice}",
                                style = VelocityTheme.typography.timeBig,
                                color = VelocityColors.Accent
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = when (state.bookingStatus) {
                                "CONFIRMED" -> VelocityColors.Success.copy(alpha = 0.2f)
                                "PENDING" -> VelocityColors.Warning.copy(alpha = 0.2f)
                                else -> VelocityColors.Error.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = state.bookingStatus,
                                style = VelocityTheme.typography.labelSmall,
                                color = when (state.bookingStatus) {
                                    "CONFIRMED" -> VelocityColors.Success
                                    "PENDING" -> VelocityColors.Warning
                                    else -> VelocityColors.Error
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Action button
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNewBooking,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent,
                        contentColor = VelocityColors.BackgroundDeep
                    )
                ) {
                    Text("Book Another Flight")
                }
            }

            // Info note
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = VelocityColors.GlassBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = VelocityColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "A confirmation email with your e-ticket has been sent to your registered email address. Please check in online 24 hours before departure.",
                            style = VelocityTheme.typography.labelSmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun WasmSettingsScreen(
    localizationState: LocalizationState,
    onBack: () -> Unit
) {
    val strings = localizationState.strings
    val isRtl = localizationState.isRtl

    VelocityThemeWithBackground(isRtl = isRtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
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
                    text = strings.settings,
                    style = VelocityTheme.typography.timeBig,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Language selection
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = VelocityColors.GlassBg,
                contentColor = VelocityColors.TextMain
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = strings.language,
                        style = VelocityTheme.typography.body.copy(
                            color = VelocityColors.TextMuted
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // English option
                    LanguageOption(
                        name = "English",
                        isSelected = localizationState.currentLanguage == AppLanguage.ENGLISH,
                        onClick = { localizationState.setLanguage(AppLanguage.ENGLISH) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Arabic option
                    LanguageOption(
                        name = "العربية",
                        isSelected = localizationState.currentLanguage == AppLanguage.ARABIC,
                        onClick = { localizationState.setLanguage(AppLanguage.ARABIC) },
                        useArabicFont = true
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageOption(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    useArabicFont: Boolean = false
) {
    // Get the appropriate font family
    val fontFamily = if (useArabicFont) {
        NotoKufiArabicFontFamily()
    } else {
        SpaceGroteskFontFamily()
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = VelocityTheme.typography.body.copy(fontFamily = fontFamily)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = VelocityColors.Accent
            )
        }
    }
}

@Composable
private fun WasmSavedBookingsScreen(
    authToken: String?,
    onBack: () -> Unit,
    onTokenExpired: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val apiClient = koinInject<FairairApiClient>()
    var bookings by remember { mutableStateOf<List<com.fairair.app.api.BookingConfirmationDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isUnauthorized by remember { mutableStateOf(false) }
    
    // Fetch bookings on first composition
    LaunchedEffect(authToken) {
        if (authToken == null) {
            isLoading = false
            isUnauthorized = true
            errorMessage = "Please log in to see your bookings"
            return@LaunchedEffect
        }
        
        isLoading = true
        errorMessage = null
        isUnauthorized = false
        
        when (val result = apiClient.getMyBookings(authToken)) {
            is com.fairair.app.api.ApiResult.Success -> {
                bookings = result.data
                isLoading = false
            }
            is com.fairair.app.api.ApiResult.Error -> {
                // Check if it's an auth error (401)
                if (result.code == "HTTP_401" || result.message.contains("expired", ignoreCase = true) || result.message.contains("unauthorized", ignoreCase = true)) {
                    isUnauthorized = true
                    onTokenExpired() // Clear the invalid token
                    errorMessage = "Your session has expired. Please log in again."
                } else {
                    errorMessage = result.message
                }
                isLoading = false
            }
        }
    }
    
    VelocityThemeWithBackground(isRtl = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VelocityColors.BackgroundMid)
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
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "My Bookings",
                    style = VelocityTheme.typography.timeBig
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VelocityColors.Accent)
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = if (isUnauthorized) Icons.Default.Person else Icons.Default.Warning,
                                contentDescription = null,
                                tint = VelocityColors.TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = errorMessage ?: "Error loading bookings",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            if (isUnauthorized) {
                                Button(
                                    onClick = onNavigateToLogin,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VelocityColors.Accent
                                    )
                                ) {
                                    Text("Log In", color = Color.White)
                                }
                            }
                        }
                    }
                }
                bookings.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = VelocityColors.TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No bookings yet",
                                style = VelocityTheme.typography.body
                            )
                            Text(
                                text = "Complete a booking to see it here",
                                style = VelocityTheme.typography.duration,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookings.size) { index ->
                            val booking = bookings[index]
                            BookingCard(booking)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(booking: com.fairair.app.api.BookingConfirmationDto) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PNR
                Column {
                    Text(
                        text = "Confirmation",
                        style = VelocityTheme.typography.duration,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = booking.pnr,
                        style = VelocityTheme.typography.timeBig,
                        color = VelocityColors.TextMain
                    )
                }
                
                // Status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (booking.status) {
                                "CONFIRMED" -> Color(0xFF22C55E).copy(alpha = 0.2f)
                                "PENDING" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = booking.status,
                        style = VelocityTheme.typography.duration,
                        color = when (booking.status) {
                            "CONFIRMED" -> Color(0xFF22C55E)
                            "PENDING" -> Color(0xFFF59E0B)
                            else -> Color(0xFFEF4444)
                        }
                    )
                }
            }
            
            HorizontalDivider(color = VelocityColors.GlassBorder.copy(alpha = 0.3f))
            
            // Booking Reference
            if (booking.bookingReference.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reference:",
                        style = VelocityTheme.typography.duration,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = booking.bookingReference,
                        style = VelocityTheme.typography.body,
                        color = VelocityColors.TextMain
                    )
                }
            }
            
            // Total paid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Paid",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = booking.totalPrice,
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.Accent
                )
            }
            
            // Created date
            if (booking.createdAt.isNotEmpty()) {
                Text(
                    text = "Booked: ${booking.createdAt.take(10)}",
                    style = VelocityTheme.typography.duration,
                    color = VelocityColors.TextMuted
                )
            }
        }
    }
}

// Login Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasmLoginScreen(
    onLogin: (UserInfoDto?, String?) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val apiClient = koinInject<FairairApiClient>()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelocityColors.BackgroundDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and Title
            Text(
                text = "✈️",
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Welcome to Fairair",
                style = MaterialTheme.typography.headlineLarge,
                color = VelocityColors.TextMain
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Sign in to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = VelocityColors.TextMuted
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Login Card
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WasmTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "Enter your email",
                        keyboardType = KeyboardType.Email
                    )
                    
                    WasmTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter your password",
                        isPassword = true
                    )
                    
                    // Error Message
                    errorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Login Button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter email and password"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val result = apiClient.login(email, password)
                                    result.fold(
                                        onSuccess = { response -> onLogin(response.user, response.token) },
                                        onFailure = { errorMessage = "Invalid email or password" }
                                    )
                                } catch (e: Exception) {
                                    errorMessage = "Login failed: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VelocityColors.Accent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Sign In", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    
                    // Continue as Guest
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Continue as Guest",
                            color = VelocityColors.Accent,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Demo Users Info
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Demo Accounts",
                        style = MaterialTheme.typography.titleMedium,
                        color = VelocityColors.TextMain
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DemoUserRow("Employee", "employee@fairair.com")
                    DemoUserRow("User", "jane@test.com")
                    DemoUserRow("Admin", "admin@test.com")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Password for all: password",
                        style = MaterialTheme.typography.bodySmall,
                        color = VelocityColors.TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoUserRow(role: String, email: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = role,
            style = VelocityTheme.typography.duration,
            color = VelocityColors.TextMuted
        )
        Text(
            text = email,
            style = VelocityTheme.typography.duration,
            color = VelocityColors.Accent
        )
    }
}

// Helper composables for form fields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasmTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {{ Text(placeholder) }} else null,
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VelocityColors.Accent,
            unfocusedBorderColor = VelocityColors.GlassBorder,
            focusedLabelColor = VelocityColors.Accent,
            unfocusedLabelColor = VelocityColors.TextMuted,
            cursorColor = VelocityColors.Accent,
            focusedTextColor = VelocityColors.TextMain,
            unfocusedTextColor = VelocityColors.TextMain
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WasmDropdownField(
    value: String,
    label: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VelocityColors.Accent,
                unfocusedBorderColor = VelocityColors.GlassBorder,
                focusedLabelColor = VelocityColors.Accent,
                unfocusedLabelColor = VelocityColors.TextMuted,
                focusedTextColor = VelocityColors.TextMain,
                unfocusedTextColor = VelocityColors.TextMain
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ============================================================================
// Check-In Screen for Wasm
// ============================================================================

@Composable
private fun WasmCheckInScreen(
    apiClient: FairairApiClient,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pnr by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lookupResult by remember { mutableStateOf<com.fairair.app.api.CheckInLookupResponseDto?>(null) }
    var checkInResult by remember { mutableStateOf<com.fairair.app.api.CheckInResultDto?>(null) }
    var selectedPassengers by remember { mutableStateOf(setOf<String>()) }

    VelocityThemeWithBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
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
                    text = "Online Check-In",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    checkInResult != null -> {
                        // Success view
                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Check-In Complete!",
                                style = MaterialTheme.typography.headlineMedium,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = checkInResult!!.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = VelocityColors.TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        
                        items(checkInResult!!.checkedInPassengers) { passenger ->
                            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = passenger.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = VelocityColors.TextMain,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Seat", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                                            Text(passenger.seatNumber, style = MaterialTheme.typography.titleLarge, color = VelocityColors.Accent, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Boarding Group", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                                            Text(passenger.boardingGroup, style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextMain)
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    checkInResult = null
                                    lookupResult = null
                                    pnr = ""
                                    lastName = ""
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
                            ) {
                                Text("Done", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    
                    lookupResult != null -> {
                        // Passenger selection view
                        item {
                            Text(
                                text = "Select Passengers",
                                style = MaterialTheme.typography.headlineSmall,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Choose who to check in for ${lookupResult!!.flight.flightNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        items(lookupResult!!.passengers.filter { !it.isCheckedIn }) { passenger ->
                            val isSelected = passenger.passengerId in selectedPassengers
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        selectedPassengers = if (isSelected) {
                                            selectedPassengers - passenger.passengerId
                                        } else {
                                            selectedPassengers + passenger.passengerId
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedPassengers = if (isSelected) {
                                                selectedPassengers - passenger.passengerId
                                            } else {
                                                selectedPassengers + passenger.passengerId
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = VelocityColors.Accent)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "${passenger.firstName} ${passenger.lastName}",
                                            style = MaterialTheme.typography.titleMedium,
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
                        
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        error = null
                                        val request = com.fairair.app.api.CheckInProcessRequestDto(
                                            pnr = pnr,
                                            passengerIds = selectedPassengers.toList()
                                        )
                                        when (val result = apiClient.processCheckIn(request)) {
                                            is com.fairair.app.api.ApiResult.Success -> {
                                                checkInResult = result.data
                                            }
                                            is com.fairair.app.api.ApiResult.Error -> {
                                                error = result.message
                                            }
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = selectedPassengers.isNotEmpty() && !isLoading,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Complete Check-In", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    
                    else -> {
                        // Lookup form
                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = VelocityColors.Accent,
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Check In Online",
                                style = MaterialTheme.typography.headlineMedium,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Check in 48 hours before departure",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VelocityColors.TextMuted
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    WasmTextField(
                                        value = pnr,
                                        onValueChange = { pnr = it.uppercase().take(6) },
                                        label = "Booking Reference (PNR)",
                                        placeholder = "e.g., ABC123"
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    WasmTextField(
                                        value = lastName,
                                        onValueChange = { lastName = it },
                                        label = "Last Name",
                                        placeholder = "e.g., Smith"
                                    )
                                    
                                    if (error != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = error!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                error = null
                                                when (val result = apiClient.lookupForCheckIn(pnr, lastName)) {
                                                    is com.fairair.app.api.ApiResult.Success -> {
                                                        if (result.data.isEligibleForCheckIn) {
                                                            lookupResult = result.data
                                                            selectedPassengers = result.data.passengers
                                                                .filter { !it.isCheckedIn }
                                                                .map { it.passengerId }
                                                                .toSet()
                                                        } else {
                                                            error = result.data.eligibilityMessage ?: "Check-in not available"
                                                        }
                                                    }
                                                    is com.fairair.app.api.ApiResult.Error -> {
                                                        error = result.message
                                                    }
                                                }
                                                isLoading = false
                                            }
                                        },
                                        enabled = pnr.length == 6 && lastName.isNotBlank() && !isLoading,
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Find Booking", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Manage Booking Screen for Wasm
// ============================================================================

@Composable
private fun WasmManageBookingScreen(
    apiClient: FairairApiClient,
    onBack: () -> Unit,
    onNavigateToCheckIn: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pnr by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var booking by remember { mutableStateOf<com.fairair.app.api.ManageBookingResponseDto?>(null) }

    VelocityThemeWithBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (booking != null) {
                        booking = null
                    } else {
                        onBack()
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VelocityColors.TextMain)
                }
                Text(
                    text = "Manage Booking",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (booking != null) {
                // Booking details
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PNR: ${booking!!.pnr}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = booking!!.status,
                                    color = Color(0xFF4CAF50),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    
                    // Flight details
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, null, tint = VelocityColors.Accent, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Flight Details", style = MaterialTheme.typography.titleMedium, color = VelocityColors.TextMain, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(booking!!.flight.origin, style = MaterialTheme.typography.headlineSmall, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold)
                                        Text(booking!!.flight.originName, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(booking!!.flight.flightNumber, style = MaterialTheme.typography.bodySmall, color = VelocityColors.Accent)
                                        Icon(Icons.Default.ArrowForward, null, tint = VelocityColors.TextMuted, modifier = Modifier.size(20.dp))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(booking!!.flight.destination, style = MaterialTheme.typography.headlineSmall, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold)
                                        Text(booking!!.flight.destinationName, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = VelocityColors.GlassBorder)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.DateRange, null, tint = VelocityColors.TextMuted, modifier = Modifier.size(16.dp))
                                        Text("Date", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                                        Text(booking!!.flight.departureDate, style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMain)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Info, null, tint = VelocityColors.TextMuted, modifier = Modifier.size(16.dp))
                                        Text("Time", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                                        Text(booking!!.flight.departureTime, style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMain)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Person, null, tint = VelocityColors.TextMuted, modifier = Modifier.size(16.dp))
                                        Text("Fare", style = MaterialTheme.typography.labelSmall, color = VelocityColors.TextMuted)
                                        Text(booking!!.flight.fareFamily, style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMain)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Passengers
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = VelocityColors.Accent, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Passengers", style = MaterialTheme.typography.titleMedium, color = VelocityColors.TextMain, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                booking!!.passengers.forEach { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${p.title} ${p.firstName} ${p.lastName}", style = MaterialTheme.typography.bodyLarge, color = VelocityColors.TextMain)
                                        Text(p.type, style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Actions
                    item {
                        if ("CHECKIN" in booking!!.allowedActions) {
                            Button(
                                onClick = onNavigateToCheckIn,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
                            ) {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check In", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                // Lookup form
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Icon(Icons.Default.Search, null, tint = VelocityColors.Accent, modifier = Modifier.size(80.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Find Your Booking", style = MaterialTheme.typography.headlineMedium, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Enter your booking reference and last name", style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMuted)
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            WasmTextField(value = pnr, onValueChange = { pnr = it.uppercase().take(6) }, label = "Booking Reference (PNR)", placeholder = "e.g., ABC123")
                            Spacer(modifier = Modifier.height(16.dp))
                            WasmTextField(value = lastName, onValueChange = { lastName = it }, label = "Last Name", placeholder = "e.g., Smith")
                            
                            if (error != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        error = null
                                        when (val result = apiClient.retrieveBooking(pnr, lastName)) {
                                            is com.fairair.app.api.ApiResult.Success -> booking = result.data
                                            is com.fairair.app.api.ApiResult.Error -> error = result.message
                                        }
                                        isLoading = false
                                    }
                                },
                                enabled = pnr.length == 6 && lastName.isNotBlank() && !isLoading,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                else Text("Find Booking", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// Membership Screen for Wasm
// ============================================================================

@Composable
private fun WasmMembershipScreen(
    apiClient: FairairApiClient,
    localStorage: LocalStorage,
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<com.fairair.contract.model.MembershipPlan>>(emptyList()) }
    var subscription by remember { mutableStateOf<com.fairair.contract.model.Subscription?>(null) }
    var selectedPlan by remember { mutableStateOf<com.fairair.contract.model.MembershipPlan?>(null) }

    LaunchedEffect(Unit) {
        when (val result = apiClient.getMembershipPlans()) {
            is com.fairair.app.api.ApiResult.Success -> plans = result.data
            is com.fairair.app.api.ApiResult.Error -> error = result.message
        }
        
        localStorage.getAuthToken()?.let { token ->
            when (val result = apiClient.getSubscription(token)) {
                is com.fairair.app.api.ApiResult.Success -> subscription = result.data
                is com.fairair.app.api.ApiResult.Error -> {} // Not subscribed
            }
        }
        isLoading = false
    }

    VelocityThemeWithBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    if (selectedPlan != null) selectedPlan = null
                    else onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VelocityColors.TextMain)
                }
                Text(
                    text = if (selectedPlan != null) selectedPlan!!.name else "Membership",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = VelocityColors.Accent)
                    }
                }
                error != null -> {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(error!!, color = VelocityColors.TextMuted, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { isLoading = true; error = null }, colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)) {
                            Text("Retry")
                        }
                    }
                }
                selectedPlan != null -> {
                    // Plan details
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(selectedPlan!!.name, style = MaterialTheme.typography.headlineMedium, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold)
                                    Text("${selectedPlan!!.tripsPerMonth} trips per month", style = MaterialTheme.typography.bodyLarge, color = VelocityColors.TextMuted)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(selectedPlan!!.monthlyPrice.formatDisplay(), style = MaterialTheme.typography.displaySmall, color = VelocityColors.Accent, fontWeight = FontWeight.Bold)
                                    Text("/month", style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMuted)
                                }
                            }
                        }
                        
                        item { Text("What's included", style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold) }
                        
                        items(selectedPlan!!.benefits) { benefit ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(benefit.icon, fontSize = 20.sp, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(benefit.title, style = MaterialTheme.typography.bodyLarge, color = VelocityColors.TextMain, fontWeight = FontWeight.Medium)
                                    Text(benefit.description, style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMuted)
                                }
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (localStorage.getAuthToken() == null) {
                                        onNavigateToLogin()
                                    } else {
                                        // Subscribe logic would go here
                                    }
                                },
                                enabled = subscription == null,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
                            ) {
                                Text(if (subscription != null) "Already Subscribed" else "Subscribe Now", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
                else -> {
                    // Plans list
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                Icon(Icons.Default.Star, null, tint = VelocityColors.Accent, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("FairAir Membership", style = MaterialTheme.typography.headlineMedium, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold)
                                Text("Unlimited flights, premium benefits", style = MaterialTheme.typography.bodyLarge, color = VelocityColors.TextMuted)
                            }
                        }
                        
                        if (subscription != null) {
                            item {
                                GlassCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text("Your Subscription", style = MaterialTheme.typography.labelMedium, color = VelocityColors.Accent)
                                                Text(subscription!!.plan.name, style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold)
                                            }
                                            Surface(color = Color(0xFF4CAF50).copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                                                Text(subscription!!.status.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("Next billing: ${subscription!!.nextBillingDate}", style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMuted)
                                    }
                                }
                            }
                        }
                        
                        items(plans) { plan ->
                            val isCurrent = subscription?.plan?.id == plan.id
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isCurrent) Modifier.border(2.dp, VelocityColors.Accent, RoundedCornerShape(20.dp)) else Modifier)
                                    .clickable { selectedPlan = plan }
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                        Column {
                                            if (isCurrent) {
                                                Surface(color = VelocityColors.Accent, shape = RoundedCornerShape(12.dp)) {
                                                    Text("CURRENT PLAN", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                            Text(plan.name, style = MaterialTheme.typography.titleLarge, color = VelocityColors.TextMain, fontWeight = FontWeight.Bold)
                                            Text("${plan.tripsPerMonth} trips/month", style = MaterialTheme.typography.bodyMedium, color = VelocityColors.TextMuted)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(plan.monthlyPrice.formatDisplay(), style = MaterialTheme.typography.headlineSmall, color = VelocityColors.Accent, fontWeight = FontWeight.Bold)
                                            Text("/month", style = MaterialTheme.typography.bodySmall, color = VelocityColors.TextMuted)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        plan.benefits.take(3).forEach { benefit ->
                                            BenefitChip(benefit.title)
                                        }
                                    }
                                }
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitChip(text: String) {
    Surface(color = VelocityColors.Accent.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = VelocityColors.Accent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
