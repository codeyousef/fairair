package com.fairair.app.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of VoiceService.
 * Uses Android's SpeechRecognizer and TextToSpeech APIs.
 * 
 * TODO: Implement full Android voice support using:
 * - android.speech.SpeechRecognizer for STT
 * - android.speech.tts.TextToSpeech for TTS
 */
class AndroidVoiceService : VoiceService {
    
    private val _state = MutableStateFlow(VoiceState())
    override val state: StateFlow<VoiceState> = _state.asStateFlow()
    
    private val _transcribedText = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val transcribedText: Flow<String> = _transcribedText.asSharedFlow()
    
    override fun startListening(language: String) {
        // TODO: Implement using SpeechRecognizer
        _state.value = _state.value.copy(
            error = "Voice recognition not yet implemented for Android. Use text input."
        )
    }
    
    override fun stopListening() {
        _state.value = _state.value.copy(isListening = false)
    }
    
    override fun speak(text: String, language: String) {
        // TODO: Implement using TextToSpeech
        _state.value = _state.value.copy(
            error = "Text-to-speech not yet implemented for Android."
        )
    }
    
    override fun stopSpeaking() {
        _state.value = _state.value.copy(isSpeaking = false)
    }
    
    override fun isRecognitionAvailable(): Boolean = false
    
    override fun isSynthesisAvailable(): Boolean = false
}

/**
 * Creates the Android implementation of VoiceService.
 */
actual fun createVoiceService(): VoiceService = AndroidVoiceService()
