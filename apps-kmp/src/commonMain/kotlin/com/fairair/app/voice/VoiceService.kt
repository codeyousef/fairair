package com.fairair.app.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Voice service interface for 2-way voice interaction.
 * Supports Speech-to-Text (STT) and Text-to-Speech (TTS) for English and Arabic.
 */
interface VoiceService {
    
    /**
     * Current state of the voice service
     */
    val state: StateFlow<VoiceState>
    
    /**
     * Flow of transcribed text from speech recognition
     */
    val transcribedText: Flow<String>
    
    /**
     * Start listening for speech input
     * @param language Language code - "en-US" for English, "ar-SA" for Arabic
     */
    fun startListening(language: String = "en-US")
    
    /**
     * Stop listening for speech input
     */
    fun stopListening()
    
    /**
     * Speak text aloud using text-to-speech
     * @param text The text to speak
     * @param language Language code - "en-US" for English, "ar-SA" for Arabic
     */
    fun speak(text: String, language: String = "en-US")
    
    /**
     * Stop any ongoing speech synthesis
     */
    fun stopSpeaking()
    
    /**
     * Check if speech recognition is available on this platform
     */
    fun isRecognitionAvailable(): Boolean
    
    /**
     * Check if speech synthesis is available on this platform
     */
    fun isSynthesisAvailable(): Boolean
}

/**
 * Voice service state
 */
data class VoiceState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val error: String? = null,
    val interimText: String = "" // Real-time transcription preview
)

/**
 * Supported languages for voice
 */
object VoiceLanguages {
    const val ENGLISH_US = "en-US"
    const val ARABIC_SA = "ar-SA"
    
    fun fromLocale(locale: String?): String {
        return when {
            locale?.startsWith("ar") == true -> ARABIC_SA
            else -> ENGLISH_US
        }
    }
}
