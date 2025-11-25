package com.flyadeal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flyadeal.app.di.DefaultPlatformConfig
import com.flyadeal.app.di.PlatformConfig
import com.flyadeal.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Main activity for the Android application.
 * Initializes Koin DI and sets up the Compose UI.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Koin if not already started
        initKoin()

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Set the Compose content
        setContent {
            App()
        }
    }

    private fun initKoin() {
        try {
            startKoin {
                androidLogger()
                androidContext(applicationContext)
                modules(appModules(androidModule))
            }
        } catch (e: IllegalStateException) {
            // Koin already started, ignore
        }
    }

    companion object {
        /**
         * Android-specific Koin module.
         * Provides platform-specific configuration.
         */
        private val androidModule = module {
            single<PlatformConfig> {
                DefaultPlatformConfig(
                    // Use 10.0.2.2 for Android emulator to access host localhost
                    apiBaseUrl = "http://10.0.2.2:8080",
                    isDebug = true
                )
            }
        }
    }
}
