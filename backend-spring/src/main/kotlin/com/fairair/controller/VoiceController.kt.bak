package com.fairair.controller

import com.fairair.service.SynthesisResult
import com.fairair.service.TranscriptionResult
import com.fairair.service.VoiceService
import kotlinx.serialization.Serializable
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * REST controller for voice services (STT and TTS).
 * Provides backend-based speech processing to bypass browser Web Speech API limitations.
 * 
 * Per ai.md spec section 3.3:
 * - Input: SpeechRecognizer with locale en-US or ar-SA
 * - Output: TTS reading the text response
 */
@RestController
@RequestMapping("/api/voice")
class VoiceController(
    private val voiceService: VoiceService
) {
    
    /**
     * Transcribe audio to text (Speech-to-Text).
     * 
     * POST /api/voice/transcribe
     * 
     * @param request Contains base64-encoded audio and language code
     * @return Transcribed text with confidence score
     */
    @PostMapping("/transcribe", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun transcribe(@RequestBody request: TranscribeRequest): TranscriptionResult {
        return voiceService.transcribe(
            audioBase64 = request.audio,
            languageCode = request.language
        )
    }
    
    /**
     * Synthesize text to speech (Text-to-Speech).
     * 
     * POST /api/voice/synthesize
     * 
     * @param request Contains text to speak and language code
     * @return Base64-encoded audio with format info
     */
    @PostMapping("/synthesize", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun synthesize(@RequestBody request: SynthesizeRequest): SynthesisResult {
        return voiceService.synthesize(
            text = request.text,
            languageCode = request.language
        )
    }
    
    /**
     * Check voice service availability.
     * 
     * GET /api/voice/status
     */
    @GetMapping("/status")
    fun status(): VoiceStatus {
        return VoiceStatus(
            sttAvailable = true,
            ttsAvailable = true,
            supportedLanguages = listOf("en-US", "ar-SA")
        )
    }
}

/**
 * Request to transcribe audio to text.
 */
@Serializable
data class TranscribeRequest(
    /** Base64-encoded audio data (WebM/Opus format from browser MediaRecorder) */
    val audio: String,
    /** Language code: "en-US" or "ar-SA" */
    val language: String = "en-US"
)

/**
 * Request to synthesize speech from text.
 */
@Serializable
data class SynthesizeRequest(
    /** Text to convert to speech */
    val text: String,
    /** Language code: "en-US" or "ar-SA" */
    val language: String = "en-US"
)

/**
 * Voice service status response.
 */
@Serializable
data class VoiceStatus(
    val sttAvailable: Boolean,
    val ttsAvailable: Boolean,
    val supportedLanguages: List<String>
)
