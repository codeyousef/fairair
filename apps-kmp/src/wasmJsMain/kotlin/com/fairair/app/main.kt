package com.fairair.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.CanvasBasedWindow
import com.fairair.app.di.DefaultPlatformConfig
import com.fairair.app.di.PlatformConfig
import com.fairair.app.di.appModules
import kotlinx.coroutines.await
import org.khronos.webgl.Int8Array
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.js.Promise
import kotlin.wasm.unsafe.UnsafeWasmMemoryApi
import kotlin.wasm.unsafe.withScopedMemoryAllocator

/**
 * Determines if we're running in debug mode for WasmJs.
 * Debug mode is when running on localhost.
 */
private val isDebugMode: Boolean
    get() = kotlinx.browser.window.location.hostname == "localhost"

/**
 * Determines the API base URL based on the current hostname.
 * - localhost -> http://localhost:8080 (development)
 * - fairair.yousef.codes -> https://api.fairair.yousef.codes (production)
 */
private fun getApiBaseUrl(): String {
    val hostname = kotlinx.browser.window.location.hostname
    return when {
        hostname == "localhost" || hostname == "127.0.0.1" -> "http://localhost:8080"
        hostname.contains("fairair.yousef.codes") -> "https://api.fairair.yousef.codes"
        else -> "https://api.fairair.yousef.codes" // Default to production
    }
}

/**
 * WasmJs-specific Koin module.
 */
private val wasmJsModule = module {
    single<PlatformConfig> {
        DefaultPlatformConfig(
            apiBaseUrl = getApiBaseUrl(),
            isDebug = isDebugMode
        )
    }
}

// Global font families that will be populated after loading
// These are mutable state so composables can react to changes
private val _arabicFontFamily = mutableStateOf<FontFamily>(FontFamily.Default)
private val _latinFontFamily = mutableStateOf<FontFamily>(FontFamily.Default)

val ArabicFontFamily: FontFamily get() = _arabicFontFamily.value
val LatinFontFamily: FontFamily get() = _latinFontFamily.value

// JS interop functions for font loading
@JsFun("(url) => fetch(url).then(r => r.arrayBuffer()).then(b => new Int8Array(b))")
private external fun jsFetchAsInt8Array(url: String): Promise<Int8Array>

@JsFun("(msg) => console.log(msg)")
private external fun jsConsoleLog(msg: String)

@JsFun(
    """ (src, size, dstAddr) => {
        const mem8 = new Int8Array(wasmExports.memory.buffer, dstAddr, size);
        mem8.set(src);
    }
"""
)
private external fun jsExportInt8ArrayToWasm(src: Int8Array, size: Int, dstAddr: Int)

/**
 * Load a font file from the resources as bytes.
 */
private suspend fun loadFontBytes(path: String): ByteArray {
    jsConsoleLog("Loading font: $path")
    val int8Array = jsFetchAsInt8Array(path).await<Int8Array>()
    jsConsoleLog("Font loaded, size: ${int8Array.length}")
    return int8Array.toByteArray()
}

@OptIn(UnsafeWasmMemoryApi::class)
private fun Int8Array.toByteArray(): ByteArray {
    val size = length
    return withScopedMemoryAllocator { allocator ->
        val memBuffer = allocator.allocate(size)
        val dstAddress = memBuffer.address.toInt()
        jsExportInt8ArrayToWasm(this, size, dstAddress)
        ByteArray(size) { i -> (memBuffer + i).loadByte() }
    }
}

/**
 * Preload all fonts before rendering the app.
 */
private suspend fun preloadFonts() {
    val resourceBase = "./composeResources/com.fairair.apps_kmp.generated.resources/font"
    
    jsConsoleLog("Starting font preload...")
    
    // Load Arabic fonts
    val arabicLight = loadFontBytes("$resourceBase/noto_kufi_arabic_light.ttf")
    val arabicRegular = loadFontBytes("$resourceBase/noto_kufi_arabic_regular.ttf")
    val arabicSemiBold = loadFontBytes("$resourceBase/noto_kufi_arabic_semibold.ttf")
    val arabicBold = loadFontBytes("$resourceBase/noto_kufi_arabic_bold.ttf")
    
    _arabicFontFamily.value = FontFamily(
        Font(identity = "NotoKufiArabicLight", data = arabicLight, weight = FontWeight.Light),
        Font(identity = "NotoKufiArabicRegular", data = arabicRegular, weight = FontWeight.Normal),
        Font(identity = "NotoKufiArabicSemiBold", data = arabicSemiBold, weight = FontWeight.SemiBold),
        Font(identity = "NotoKufiArabicBold", data = arabicBold, weight = FontWeight.Bold)
    )
    jsConsoleLog("Arabic fonts loaded")
    
    // Load Latin fonts
    val latinLight = loadFontBytes("$resourceBase/space_grotesk_light.ttf")
    val latinRegular = loadFontBytes("$resourceBase/space_grotesk_regular.ttf")
    val latinMedium = loadFontBytes("$resourceBase/space_grotesk_medium.ttf")
    val latinSemiBold = loadFontBytes("$resourceBase/space_grotesk_semibold.ttf")
    val latinBold = loadFontBytes("$resourceBase/space_grotesk_bold.ttf")
    
    _latinFontFamily.value = FontFamily(
        Font(identity = "SpaceGroteskLight", data = latinLight, weight = FontWeight.Light),
        Font(identity = "SpaceGroteskRegular", data = latinRegular, weight = FontWeight.Normal),
        Font(identity = "SpaceGroteskMedium", data = latinMedium, weight = FontWeight.Medium),
        Font(identity = "SpaceGroteskSemiBold", data = latinSemiBold, weight = FontWeight.SemiBold),
        Font(identity = "SpaceGroteskBold", data = latinBold, weight = FontWeight.Bold)
    )
    jsConsoleLog("Latin fonts loaded")
    jsConsoleLog("All fonts preloaded successfully!")
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
        title = "FairAir"
    ) {
        var fontsReady by remember { mutableStateOf(false) }
        var loadError by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(Unit) {
            try {
                preloadFonts()
                fontsReady = true
            } catch (e: Exception) {
                jsConsoleLog("Font loading error: ${e.message}")
                loadError = e.message ?: "Unknown error loading fonts"
                // Continue anyway with default fonts
                fontsReady = true
            }
        }
        
        if (fontsReady) {
            WasmApp()
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF06B6D4))
            }
        }
    }
}
