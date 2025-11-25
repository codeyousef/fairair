package com.flyadeal.app

import androidx.compose.ui.window.ComposeUIViewController
import com.flyadeal.app.di.DefaultPlatformConfig
import com.flyadeal.app.di.PlatformConfig
import com.flyadeal.app.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

/**
 * iOS-specific Koin module.
 */
private val iosModule = module {
    single<PlatformConfig> {
        DefaultPlatformConfig(
            apiBaseUrl = "http://localhost:8080",
            isDebug = true
        )
    }
}

/**
 * Initializes Koin for iOS.
 * Should be called from iOS AppDelegate or similar.
 */
fun initKoin() {
    startKoin {
        modules(appModules(iosModule))
    }
}

/**
 * Creates the main UIViewController for iOS.
 * This is called from Swift code.
 */
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
