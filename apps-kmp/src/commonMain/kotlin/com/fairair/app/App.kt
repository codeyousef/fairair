package com.fairair.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.fairair.app.localization.AppLanguage
import com.fairair.app.localization.LocalLocalization
import com.fairair.app.localization.LocalizationProvider
import com.fairair.app.localization.LocalizationState
import com.fairair.app.persistence.LocalStorage
import com.fairair.app.ui.chat.ChatScreenModel
import com.fairair.app.ui.chat.GridExplosionTransition
import com.fairair.app.ui.chat.PilotFullScreen
import com.fairair.app.ui.chat.PilotOrb
import com.fairair.app.ui.screens.search.SearchScreen
import com.fairair.app.ui.theme.FairairTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

/**
 * Main application composable.
 * Sets up theming, DI context, localization, and navigation.
 */
@Composable
fun App() {
    KoinContext {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val localStorage = koinInject<LocalStorage>()
    val chatScreenModel = koinInject<ChatScreenModel>()

    // Create localization state and load saved language
    val localizationState = remember { LocalizationState() }

    // Load saved language preference on startup
    LaunchedEffect(Unit) {
        val savedLanguage = localStorage.getCurrentLanguage()
        localizationState.setLanguage(AppLanguage.fromCode(savedLanguage))
    }

    // Chat state
    val chatUiState by chatScreenModel.uiState.collectAsState()
    var showPilotAI by remember { mutableStateOf(false) }

    LocalizationProvider(localizationState = localizationState) {
        val localization = LocalLocalization.current
        val currentLocale = if (localization.isRtl) "ar-SA" else "en-US"

        FairairTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main navigator
                Navigator(
                    screen = SearchScreen(),
                    onBackPressed = { currentScreen ->
                        // Allow back navigation for all screens except the first one
                        true
                    }
                ) { navigator ->
                    SlideTransition(navigator)
                }

                // Pilot Full Screen AI with grid explosion animation
                // Squares radiate from FAB position (bottom-right)
                GridExplosionTransition(
                    visible = showPilotAI,
                    modifier = Modifier.fillMaxSize()
                ) {
                    PilotFullScreen(
                        visible = true,
                        uiState = chatUiState,
                        onSendMessage = { message ->
                            chatScreenModel.sendMessage(message, currentLocale)
                        },
                        onInputChange = { chatScreenModel.updateInputText(it) },
                        onSuggestionTapped = { chatScreenModel.onSuggestionTapped(it) },
                        onClearChat = { chatScreenModel.clearChat() },
                        onDismiss = { showPilotAI = false },
                        onVoiceClick = { chatScreenModel.toggleListening() },
                        locale = currentLocale
                    )
                }

                // Pilot AI Orb - always visible with animation
                AnimatedVisibility(
                    visible = !showPilotAI,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    PilotOrb(
                        onClick = { showPilotAI = true },
                        isListening = chatUiState.isListening,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
