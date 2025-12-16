package com.fairair.app.voice

/**
 * Creates the WASM implementation of VoiceService using backend STT/TTS APIs.
 * 
 * Records audio using browser MediaRecorder, sends to backend for processing.
 * This approach works consistently across mobile (Android/iOS) and web platforms.
 */
actual fun createVoiceService(): VoiceService = WasmVoiceService()
