package com.fairair.app.voice

/**
 * Creates the WASM implementation of VoiceService using Web Speech API.
 */
actual fun createVoiceService(): VoiceService = WasmVoiceService()
