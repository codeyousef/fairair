package com.fairair.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.fairair.app.ui.chat.FarisChatSheet
import com.fairair.app.ui.chat.FarisFAB
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var showChatSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

                // Faris FAB - visible on all screens
                FarisFAB(
                    onClick = { showChatSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )

                // Chat bottom sheet
                if (showChatSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showChatSheet = false },
                        sheetState = sheetState,
                        dragHandle = null // We have our own header
                    ) {
                        FarisChatSheet(
                            uiState = chatUiState,
                            onSendMessage = { message ->
                                chatScreenModel.sendMessage(message, currentLocale)
                            },
                            onInputChange = { chatScreenModel.updateInputText(it) },
                            onSuggestionTapped = { chatScreenModel.onSuggestionTapped(it) },
                            onClearChat = { chatScreenModel.clearChat() },
                            onDismiss = { showChatSheet = false },
                            locale = currentLocale
                        )
                    }
                }
            }
        }
    }
}
