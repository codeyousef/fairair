package com.fairair.service

import com.google.cloud.speech.v1.*
import com.google.cloud.texttospeech.v1.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Base64

/**
 * Voice service providing Speech-to-Text (STT) and Text-to-Speech (TTS) capabilities
 * using Google Cloud APIs. This bypasses browser Web Speech API limitations.
 * 
 * Per ai.md spec:
 * - Input: Use native SpeechRecognizer with locale en-US or ar-SA
 * - Output: TTS reading the text response
 */
@Service
class VoiceService {
    
    private val log = LoggerFactory.getLogger(VoiceService::class.java)
    
    /**
     * Transcribes audio to text using Google Cloud Speech-to-Text.
     * 
     * @param audioBase64 Base64-encoded audio data (WebM/Opus from browser MediaRecorder)
     * @param languageCode Language code: "en-US" for English, "ar-SA" for Saudi Arabic
     * @return Transcribed text
     */
    suspend fun transcribe(audioBase64: String, languageCode: String): TranscriptionResult {
        return withContext(Dispatchers.IO) {
            try {
                val audioBytes = Base64.getDecoder().decode(audioBase64)
                log.info("Transcribing audio: ${audioBytes.size} bytes, language: $languageCode")
                
                if (audioBytes.size < 1000) {
                    log.warn("Audio too short: ${audioBytes.size} bytes")
                    return@withContext TranscriptionResult(
                        text = "",
                        confidence = 0f,
                        languageCode = languageCode,
                        error = "Audio too short - please speak longer"
                    )
                }
                
                SpeechClient.create().use { speechClient ->
                    val config = RecognitionConfig.newBuilder()
                        .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                        .setSampleRateHertz(48000)
                        .setLanguageCode(languageCode)
                        .setModel("latest_long") // Best model for conversational speech
                        .setEnableAutomaticPunctuation(true)
                        .build()
                    
                    val audio = RecognitionAudio.newBuilder()
                        .setContent(ByteString.copyFrom(audioBytes))
                        .build()
                    
                    val response = speechClient.recognize(config, audio)
                    
                    val transcript = response.resultsList
                        .flatMap { it.alternativesList }
                        .maxByOrNull { it.confidence }
                        ?.transcript
                        ?: ""
                    
                    val confidence = response.resultsList
                        .flatMap { it.alternativesList }
                        .maxByOrNull { it.confidence }
                        ?.confidence
                        ?: 0f
                    
                    log.info("Transcribed ($languageCode): '$transcript' (confidence: $confidence)")
                    TranscriptionResult(
                        text = transcript,
                        confidence = confidence,
                        languageCode = languageCode
                    )
                }
            } catch (e: Exception) {
                log.error("Transcription failed: ${e.message}", e)
                TranscriptionResult(
                    text = "",
                    confidence = 0f,
                    languageCode = languageCode,
                    error = e.message ?: "Transcription failed"
                )
            }
        }
    }
    
    /**
     * Synthesizes text to speech using Google Cloud Text-to-Speech.
     * 
     * @param text The text to speak
     * @param languageCode Language code: "en-US" for English, "ar-SA" for Saudi Arabic
     * @return Base64-encoded MP3 audio
     */
    suspend fun synthesize(text: String, languageCode: String): SynthesisResult {
        return withContext(Dispatchers.IO) {
            try {
                TextToSpeechClient.create().use { ttsClient ->
                    val input = SynthesisInput.newBuilder()
                        .setText(text)
                        .build()
                    
                    // Select voice based on language
                    // For Arabic: use ar-XA (Arabic, multiple regions including Saudi)
                    // Per spec: Saudi dialect (Khaleeji)
                    val voiceName = when {
                        languageCode.startsWith("ar") -> "ar-XA-Wavenet-B" // Male Arabic voice
                        else -> "en-US-Wavenet-D" // Male English voice
                    }
                    
                    val voice = VoiceSelectionParams.newBuilder()
                        .setLanguageCode(if (languageCode.startsWith("ar")) "ar-XA" else "en-US")
                        .setName(voiceName)
                        .setSsmlGender(SsmlVoiceGender.MALE)
                        .build()
                    
                    val audioConfig = AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .setSpeakingRate(1.0) // Normal speed
                        .setPitch(0.0) // Normal pitch
                        .build()
                    
                    val response = ttsClient.synthesizeSpeech(input, voice, audioConfig)
                    val audioBase64 = Base64.getEncoder().encodeToString(
                        response.audioContent.toByteArray()
                    )
                    
                    log.info("Synthesized ($languageCode): ${text.take(50)}...")
                    SynthesisResult(
                        audioBase64 = audioBase64,
                        audioFormat = "audio/mpeg",
                        durationMs = estimateDuration(text)
                    )
                }
            } catch (e: Exception) {
                log.error("Synthesis failed: ${e.message}", e)
                SynthesisResult(
                    audioBase64 = "",
                    audioFormat = "audio/mpeg",
                    durationMs = 0,
                    error = e.message ?: "Synthesis failed"
                )
            }
        }
    }
    
    /**
     * Estimates audio duration based on text length.
     * Average speaking rate is ~150 words per minute.
     */
    private fun estimateDuration(text: String): Long {
        val wordCount = text.split(Regex("\\s+")).size
        val minutesEstimate = wordCount / 150.0
        return (minutesEstimate * 60 * 1000).toLong()
    }
}

/**
 * Result of speech-to-text transcription.
 */
data class TranscriptionResult(
    val text: String,
    val confidence: Float,
    val languageCode: String,
    val error: String? = null
)

/**
 * Result of text-to-speech synthesis.
 */
data class SynthesisResult(
    val audioBase64: String,
    val audioFormat: String,
    val durationMs: Long,
    val error: String? = null
)
