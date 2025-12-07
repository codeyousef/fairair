package com.fairair.app.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Web Speech API implementation for WASM platform.
 * Uses browser's native SpeechRecognition and SpeechSynthesis APIs.
 * 
 * NOTE: This implementation has issues with network errors in some environments.
 * Use WasmVoiceService (backend-based) instead for more reliable operation.
 */
class WasmVoiceServiceWebSpeech : VoiceService {
    
    private val _state = MutableStateFlow(VoiceState())
    override val state: StateFlow<VoiceState> = _state.asStateFlow()
    
    private val _transcribedText = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val transcribedText: Flow<String> = _transcribedText.asSharedFlow()
    
    private var currentLanguage: String = VoiceLanguages.ENGLISH_US
    
    override fun startListening(language: String) {
        if (!isRecognitionAvailable()) {
            _state.value = _state.value.copy(error = "Speech recognition not supported in this browser")
            return
        }
        
        currentLanguage = language
        
        try {
            // Start recognition via JS bridge
            startSpeechRecognition(
                language = language,
                onResult = { transcript, isFinal ->
                    if (isFinal) {
                        _transcribedText.tryEmit(transcript)
                        _state.value = _state.value.copy(interimText = "")
                    } else {
                        _state.value = _state.value.copy(interimText = transcript)
                    }
                },
                onError = { error ->
                    // Provide user-friendly error messages
                    val friendlyError = when (error) {
                        "network" -> "Network error - check internet connection"
                        "not-allowed" -> "Microphone access denied"
                        "no-speech" -> "No speech detected - try again"
                        "aborted" -> "Speech recognition stopped"
                        "audio-capture" -> "No microphone found"
                        "service-not-allowed" -> "Speech service not available"
                        else -> "Voice error: $error"
                    }
                    _state.value = _state.value.copy(
                        isListening = false,
                        error = friendlyError
                    )
                },
                onStart = {
                    _state.value = _state.value.copy(isListening = true, error = null)
                },
                onEnd = {
                    _state.value = _state.value.copy(isListening = false, interimText = "")
                }
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "Failed to start: ${e.message}")
        }
    }
    
    override fun stopListening() {
        try {
            stopSpeechRecognition()
        } catch (e: Exception) {
            // Ignore stop errors
        }
        _state.value = _state.value.copy(isListening = false)
    }
    
    override fun speak(text: String, language: String) {
        if (!isSynthesisAvailable()) {
            _state.value = _state.value.copy(error = "Speech synthesis not supported")
            return
        }
        
        // Stop any ongoing speech
        stopSpeaking()
        
        try {
            speakText(
                text = text,
                language = language,
                onStart = {
                    _state.value = _state.value.copy(isSpeaking = true)
                },
                onEnd = {
                    _state.value = _state.value.copy(isSpeaking = false)
                },
                onError = { error ->
                    _state.value = _state.value.copy(
                        isSpeaking = false,
                        error = "TTS error: $error"
                    )
                }
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "TTS failed: ${e.message}")
        }
    }
    
    override fun stopSpeaking() {
        try {
            cancelSpeech()
        } catch (e: Exception) {
            // Ignore
        }
        _state.value = _state.value.copy(isSpeaking = false)
    }
    
    override fun isRecognitionAvailable(): Boolean {
        return isSpeechRecognitionSupported()
    }
    
    override fun isSynthesisAvailable(): Boolean {
        return isSpeechSynthesisSupported()
    }
}

// ============================================================================
// JavaScript Bridge Functions - Use callbacks instead of complex return types
// ============================================================================

@JsFun("() => typeof window !== 'undefined' && ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)")
private external fun isSpeechRecognitionSupported(): Boolean

@JsFun("() => typeof window !== 'undefined' && 'speechSynthesis' in window")
private external fun isSpeechSynthesisSupported(): Boolean

@JsFun("""
(language, onResult, onError, onStart, onEnd) => {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        onError('SpeechRecognition not available');
        return;
    }
    
    // Store recognition globally so we can stop it later
    window._fairairRecognition = new SpeechRecognition();
    window._fairairRecognition.continuous = false;
    window._fairairRecognition.interimResults = true;
    window._fairairRecognition.maxAlternatives = 1;
    window._fairairRecognition.lang = language;
    
    window._fairairRecognition.onresult = (event) => {
        const lastIdx = event.results.length - 1;
        const result = event.results[lastIdx];
        const transcript = result[0].transcript;
        const isFinal = result.isFinal;
        onResult(transcript, isFinal);
    };
    
    window._fairairRecognition.onerror = (event) => {
        onError(event.error || 'Unknown error');
    };
    
    window._fairairRecognition.onstart = () => {
        onStart();
    };
    
    window._fairairRecognition.onend = () => {
        onEnd();
    };
    
    window._fairairRecognition.start();
}
""")
private external fun startSpeechRecognition(
    language: String,
    onResult: (transcript: String, isFinal: Boolean) -> Unit,
    onError: (error: String) -> Unit,
    onStart: () -> Unit,
    onEnd: () -> Unit
)

@JsFun("""
() => {
    if (window._fairairRecognition) {
        window._fairairRecognition.stop();
    }
}
""")
private external fun stopSpeechRecognition()

@JsFun("""
(text, language, onStart, onEnd, onError) => {
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = language;
    utterance.rate = 1.0;
    utterance.pitch = 1.0;
    
    // Try to find a matching voice
    const voices = window.speechSynthesis.getVoices();
    const langPrefix = language.substring(0, 2);
    const matchingVoice = voices.find(v => v.lang.startsWith(langPrefix));
    if (matchingVoice) {
        utterance.voice = matchingVoice;
    }
    
    utterance.onstart = () => onStart();
    utterance.onend = () => onEnd();
    utterance.onerror = (event) => onError(event.error || 'Unknown TTS error');
    
    window.speechSynthesis.speak(utterance);
}
""")
private external fun speakText(
    text: String,
    language: String,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onError: (error: String) -> Unit
)

@JsFun("() => window.speechSynthesis.cancel()")
private external fun cancelSpeech()
