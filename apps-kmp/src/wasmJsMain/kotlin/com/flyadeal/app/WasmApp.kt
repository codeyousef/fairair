package com.flyadeal.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.flyadeal.app.api.FlyadealApiClient
import com.flyadeal.app.localization.AppLanguage
import com.flyadeal.app.localization.LocalizationProvider
import com.flyadeal.app.localization.LocalizationState
import com.flyadeal.app.localization.rememberLocalizationState
import com.flyadeal.app.state.BookingFlowState
import com.flyadeal.app.ui.screens.search.VelocitySearchScreen
import com.flyadeal.app.ui.screens.search.VelocitySearchScreenError
import com.flyadeal.app.ui.screens.search.VelocitySearchScreenLoading
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme
import com.flyadeal.app.ui.theme.VelocityThemeWithBackground
import com.flyadeal.app.viewmodel.WasmSearchViewModel
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
 * For now, this only renders the Search screen since that's the main entry
 * point. Additional screens can be added using state-based navigation when
 * the full booking flow is needed in the web app.
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
    SEARCH,
    RESULTS,
    SETTINGS,
    SAVED_BOOKINGS
}

@Composable
private fun WasmAppContent() {
    // ULTRA MINIMAL TEST - no custom themes, no fonts, no animations
    // Just basic Material3 to verify Compose rendering works at all
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "flyadeal",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Wasm UI Rendering Test",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "If you see this, basic Compose works!",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = { /* no-op */ }) {
                    Text("Test Button")
                }
            }
        }
    }
}

/**
 * Simple static search screen for testing Wasm rendering.
 * No data loading, just static UI to verify the rendering works.
 */
@Composable
private fun WasmSimpleSearchScreen(
    localizationState: LocalizationState,
    onNavigateToSettings: () -> Unit,
    onNavigateToSavedBookings: () -> Unit
) {
    val isRtl = localizationState.isRtl

    VelocityThemeWithBackground(isRtl = isRtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Simple header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToSavedBookings) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Saved Bookings",
                            tint = VelocityColors.TextMuted
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = VelocityColors.TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.1f))

                // Hero title
                Text(
                    text = "flyadeal",
                    style = VelocityTheme.typography.heroTitle,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Simple message
                Text(
                    text = "Wasm UI is rendering!",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.Accent,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Text(
                    text = "This proves the Voyager-free approach works.",
                    style = VelocityTheme.typography.duration,
                    color = VelocityColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun WasmSearchScreenContainer(
    viewModel: WasmSearchViewModel,
    localizationState: LocalizationState,
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
                onNavigateToSavedBookings = onNavigateToSavedBookings
            )
        }
    }
}

@Composable
private fun WasmResultsPlaceholder(onBack: () -> Unit) {
    VelocityThemeWithBackground(isRtl = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Flight Results",
                    style = VelocityTheme.typography.heroTitle
                )
                Text(
                    text = "Coming soon to web",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMuted
                )
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent,
                        contentColor = VelocityColors.BackgroundDeep
                    )
                ) {
                    Text("Back to Search")
                }
            }
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
                        imageVector = Icons.Default.ArrowBack,
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
                        onClick = { localizationState.setLanguage(AppLanguage.ARABIC) }
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = VelocityTheme.typography.body
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
private fun WasmSavedBookingsPlaceholder(onBack: () -> Unit) {
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
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = VelocityColors.TextMain
                    )
                }
                Text(
                    text = "Saved Bookings",
                    style = VelocityTheme.typography.timeBig,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No saved bookings",
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
    }
}
