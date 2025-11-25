package com.flyadeal.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.flyadeal.app.di.DefaultPlatformConfig
import com.flyadeal.app.di.PlatformConfig
import com.flyadeal.app.di.appModules
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * WasmJs-specific Koin module.
 */
private val wasmJsModule = module {
    single<PlatformConfig> {
        DefaultPlatformConfig(
            apiBaseUrl = "http://localhost:8080",
            isDebug = true
        )
    }
}

/**
 * Main entry point for the WasmJs application.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initialize Koin
    startKoin {
        modules(appModules(wasmJsModule))
    }

    // Start the Compose application
    CanvasBasedWindow(
        canvasElementId = "ComposeTarget",
        title = "flyadeal"
    ) {
        App()
    }
}
