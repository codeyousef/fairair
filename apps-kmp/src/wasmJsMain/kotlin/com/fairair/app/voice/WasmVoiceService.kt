package com.fairair.app.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Web Voice Service implementation using backend STT/TTS APIs.
 * Records audio using browser MediaRecorder, sends to backend for processing.
 * This bypasses browser Web Speech API limitations (network errors, etc).
 * 
 * Continuous listening mode: Once started, keeps listening until explicitly stopped.
 */
class WasmVoiceService : VoiceService {
    
    private val _state = MutableStateFlow(VoiceState())
    override val state: StateFlow<VoiceState> = _state.asStateFlow()
    
    private val _transcribedText = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val transcribedText: Flow<String> = _transcribedText.asSharedFlow()
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var currentLanguage: String = VoiceLanguages.ENGLISH_US
    
    // Flag to track if we should continue listening after transcription
    private var continuousListening: Boolean = false
    
    override fun startListening(language: String) {
        if (!isRecordingAvailable()) {
            _state.value = _state.value.copy(error = "Audio recording not supported")
            return
        }
        
        // Interrupt any ongoing TTS when user starts speaking
        if (_state.value.isSpeaking) {
            stopSpeaking()
        }
        
        currentLanguage = language
        continuousListening = true // Enable continuous listening mode
        _state.value = _state.value.copy(isListening = true, error = null, interimText = "Listening...")
        
        startContinuousRecording(language)
    }
    
    /**
     * Start or restart recording for continuous listening.
     */
    private fun startContinuousRecording(language: String) {
        if (!continuousListening) return
        
        println("WasmVoiceService: Starting continuous recording for language: $language")
        
        startRecording(
            onAudioData = { audioBase64 ->
                println("WasmVoiceService: Got audio data, length: ${audioBase64.length}")
                scope.launch {
                    println("WasmVoiceService: Inside coroutine, calling transcribeAudio")
                    _state.value = _state.value.copy(interimText = "Processing...")
                    
                    // Send audio to backend for transcription
                    transcribeAudio(
                        audioBase64 = audioBase64,
                        language = language,
                        onSuccess = { text ->
                            println("WasmVoiceService: Transcription success: $text")
                            if (text.isNotBlank()) {
                                // Show what was heard before sending
                                _state.value = _state.value.copy(
                                    interimText = text,
                                    isListening = false
                                )
                                // Emit the text to be sent as a message
                                _transcribedText.tryEmit(text)
                                // Clear after a short delay
                                scope.launch {
                                    delay(1500)
                                    _state.value = _state.value.copy(interimText = "")
                                }
                            } else {
                                // No text recognized, show feedback
                                _state.value = _state.value.copy(
                                    interimText = "Didn't catch that...",
                                    isListening = false
                                )
                                scope.launch {
                                    delay(1500)
                                    _state.value = _state.value.copy(interimText = "")
                                }
                            }
                            
                            // Stop continuous mode after successful transcription
                            continuousListening = false
                        },
                        onError = { error ->
                            println("WasmVoiceService: Transcription error: $error")
                            // Show error feedback and stop
                            _state.value = _state.value.copy(
                                isListening = false,
                                interimText = "Something went wrong. Try again.",
                                error = null
                            )
                            continuousListening = false
                            // Clear error message after delay
                            scope.launch {
                                delay(2000)
                                _state.value = _state.value.copy(interimText = "")
                            }
                        }
                    )
                }
            },
            onError = { error ->
                println("WasmVoiceService: Recording error: $error")
                // On recording error, try to restart if in continuous mode
                if (continuousListening) {
                    scope.launch {
                        kotlinx.coroutines.delay(500) // Brief delay before retry
                        if (continuousListening) {
                            startContinuousRecording(language)
                        }
                    }
                } else {
                    _state.value = _state.value.copy(
                        isListening = false,
                        error = "Recording failed: $error"
                    )
                }
            }
        )
    }
    
    override fun stopListening() {
        continuousListening = false // Disable continuous listening mode
        stopRecording()
        _state.value = _state.value.copy(isListening = false, interimText = "")
    }
    
    override fun speak(text: String, language: String) {
        _state.value = _state.value.copy(isSpeaking = true)
        
        // Call backend TTS API
        synthesizeSpeech(
            text = text,
            language = language,
            onSuccess = { audioBase64 ->
                // Play the audio
                playAudioBase64(
                    audioBase64 = audioBase64,
                    onEnd = {
                        _state.value = _state.value.copy(isSpeaking = false)
                    },
                    onError = { error ->
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            error = "Playback failed: $error"
                        )
                    }
                )
            },
            onError = { error ->
                _state.value = _state.value.copy(
                    isSpeaking = false,
                    error = "TTS failed: $error"
                )
            }
        )
    }
    
    override fun stopSpeaking() {
        stopAudioPlayback()
        _state.value = _state.value.copy(isSpeaking = false)
    }
    
    override fun isRecognitionAvailable(): Boolean = isRecordingAvailable()
    
    override fun isSynthesisAvailable(): Boolean = true // Backend TTS always available
}

// ============================================================================
// JavaScript Bridge Functions - Audio Recording
// ============================================================================

@JsFun("() => typeof navigator !== 'undefined' && navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function'")
private external fun isRecordingAvailable(): Boolean

@JsFun("""
(onAudioData, onError) => {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        onError('MediaDevices not supported');
        return;
    }
    
    navigator.mediaDevices.getUserMedia({ audio: true })
        .then(stream => {
            // Check supported mime types
            let mimeType = 'audio/webm;codecs=opus';
            if (!MediaRecorder.isTypeSupported(mimeType)) {
                mimeType = 'audio/webm';
                if (!MediaRecorder.isTypeSupported(mimeType)) {
                    mimeType = 'audio/ogg;codecs=opus';
                    if (!MediaRecorder.isTypeSupported(mimeType)) {
                        mimeType = ''; // Let browser choose
                    }
                }
            }
            console.log('Using mime type:', mimeType || 'default');
            
            const options = mimeType ? { mimeType } : {};
            window._fairairMediaRecorder = new MediaRecorder(stream, options);
            window._fairairAudioChunks = [];
            window._fairairStream = stream;
            
            window._fairairMediaRecorder.ondataavailable = (event) => {
                console.log('Data available, size:', event.data.size);
                if (event.data.size > 0) {
                    window._fairairAudioChunks.push(event.data);
                }
            };
            
            window._fairairMediaRecorder.onstop = () => {
                console.log('MediaRecorder stopped, chunks:', window._fairairAudioChunks.length);
                
                // Clear the auto-stop timer
                if (window._fairairRecordingTimer) {
                    clearTimeout(window._fairairRecordingTimer);
                    window._fairairRecordingTimer = null;
                }
                
                if (window._fairairAudioChunks.length === 0) {
                    console.error('No audio chunks recorded');
                    onError('No audio recorded');
                    stream.getTracks().forEach(track => track.stop());
                    return;
                }
                
                const blob = new Blob(window._fairairAudioChunks, { type: mimeType || 'audio/webm' });
                console.log('Audio blob size:', blob.size);
                
                const reader = new FileReader();
                reader.onloadend = () => {
                    console.log('FileReader complete');
                    // Extract base64 from data URL
                    const base64 = reader.result.split(',')[1];
                    console.log('Base64 length:', base64.length);
                    onAudioData(base64);
                };
                reader.onerror = () => {
                    console.error('FileReader error');
                    onError('Failed to read audio data');
                };
                reader.readAsDataURL(blob);
                
                // Stop all tracks
                stream.getTracks().forEach(track => track.stop());
            };
            
            window._fairairMediaRecorder.onerror = (event) => {
                console.error('MediaRecorder error:', event.error);
                onError('Recording error: ' + (event.error?.message || 'unknown'));
            };
            
            // Start recording - request data every 1 second
            window._fairairMediaRecorder.start(1000);
            console.log('Recording started, state:', window._fairairMediaRecorder.state);
            
            // Auto-stop after 8 seconds for processing (continuous listening will restart)
            window._fairairRecordingTimer = setTimeout(() => {
                if (window._fairairMediaRecorder && window._fairairMediaRecorder.state === 'recording') {
                    console.log('Auto-stopping recording for processing');
                    window._fairairMediaRecorder.stop();
                }
            }, 8000);
        })
        .catch(err => {
            console.error('getUserMedia error:', err);
            onError(err.message || 'Microphone access denied');
        });
}
""")
private external fun startRecording(
    onAudioData: (audioBase64: String) -> Unit,
    onError: (error: String) -> Unit
)

@JsFun("""
() => {
    if (window._fairairMediaRecorder && window._fairairMediaRecorder.state === 'recording') {
        window._fairairMediaRecorder.stop();
        console.log('Recording stopped');
    }
}
""")
private external fun stopRecording()

// ============================================================================
// JavaScript Bridge Functions - Backend API Calls
// ============================================================================

@JsFun("""
(audioBase64, language, onSuccess, onError) => {
    console.log('transcribeAudio called - language:', language, 'audioLength:', audioBase64.length);
    
    fetch('http://localhost:8080/api/voice/transcribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ audio: audioBase64, language: language })
    })
    .then(response => {
        console.log('Transcribe response status:', response.status);
        if (!response.ok) throw new Error('Transcription request failed: ' + response.status);
        return response.json();
    })
    .then(data => {
        console.log('Transcribe response data:', data);
        if (data.error) {
            console.error('Transcribe API error:', data.error);
            onError(data.error);
        } else {
            console.log('Transcribe success, text:', data.text);
            onSuccess(data.text || '');
        }
    })
    .catch(err => {
        console.error('Transcribe fetch error:', err);
        onError(err.message || 'Transcription failed');
    });
}
""")
private external fun transcribeAudio(
    audioBase64: String,
    language: String,
    onSuccess: (text: String) -> Unit,
    onError: (error: String) -> Unit
)

@JsFun("""
(text, language, onSuccess, onError) => {
    fetch('http://localhost:8080/api/voice/synthesize', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: text, language: language })
    })
    .then(response => {
        if (!response.ok) throw new Error('Synthesis request failed');
        return response.json();
    })
    .then(data => {
        if (data.error) {
            onError(data.error);
        } else {
            onSuccess(data.audioBase64 || '');
        }
    })
    .catch(err => {
        console.error('Synthesize error:', err);
        onError(err.message || 'Synthesis failed');
    });
}
""")
private external fun synthesizeSpeech(
    text: String,
    language: String,
    onSuccess: (audioBase64: String) -> Unit,
    onError: (error: String) -> Unit
)

// ============================================================================
// JavaScript Bridge Functions - Audio Playback
// ============================================================================

@JsFun("""
(audioBase64, onEnd, onError) => {
    try {
        // Stop any existing audio first
        if (window._fairairAudio) {
            window._fairairAudio.pause();
            window._fairairAudio.src = '';
            window._fairairAudio = null;
        }
        
        const audio = new Audio();
        window._fairairAudio = audio;
        
        // Set up event handlers before setting src
        audio.onended = () => {
            console.log('Audio playback ended');
            onEnd();
        };
        audio.onerror = (e) => {
            console.error('Audio error event:', e);
            onError('Audio playback error');
        };
        
        // Wait for audio to be ready before playing
        audio.oncanplaythrough = () => {
            console.log('Audio ready to play, duration:', audio.duration);
            audio.play().then(() => {
                console.log('Audio playback started');
            }).catch(err => {
                console.error('Audio play error:', err);
                onError(err.message || 'Playback failed');
            });
        };
        
        // Set the source - this triggers loading
        audio.src = 'data:audio/mp3;base64,' + audioBase64;
        audio.load();
        
    } catch (err) {
        console.error('Audio creation error:', err);
        onError(err.message || 'Failed to create audio');
    }
}
""")
private external fun playAudioBase64(
    audioBase64: String,
    onEnd: () -> Unit,
    onError: (error: String) -> Unit
)

@JsFun("""
() => {
    if (window._fairairAudio) {
        window._fairairAudio.pause();
        window._fairairAudio = null;
    }
}
""")
private external fun stopAudioPlayback()
