package com.fairair.app.voice

/**
 * Creates a platform-specific VoiceService instance.
 * 
 * - On WASM: Uses Web Speech API (SpeechRecognition + SpeechSynthesis)
 * - On Android: Uses Android Speech APIs (future implementation)
 * - On iOS: Uses AVFoundation (future implementation)
 */
expect fun createVoiceService(): VoiceService
