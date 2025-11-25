package com.flyadeal.app

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.flyadeal.app.ui.screens.search.SearchScreen
import com.flyadeal.app.ui.theme.FlyadealTheme
import org.koin.compose.KoinContext

/**
 * Main application composable.
 * Sets up theming, DI context, and navigation.
 */
@Composable
fun App() {
    KoinContext {
        FlyadealTheme {
            Navigator(
                screen = SearchScreen(),
                onBackPressed = { currentScreen ->
                    // Allow back navigation for all screens except the first one
                    true
                }
            ) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
