package com.fairair.controller

import com.fairair.ai.voice.LexStreamingVoiceHandler
import com.fairair.contract.dto.VoiceStartRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.polly.PollyAsyncClient
import software.amazon.awssdk.services.polly.model.Engine
import software.amazon.awssdk.services.polly.model.OutputFormat
import software.amazon.awssdk.services.polly.model.SynthesizeSpeechRequest
import software.amazon.awssdk.services.polly.model.VoiceId
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient
import software.amazon.awssdk.services.transcribestreaming.model.*
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.io.File
import java.util.Base64
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Voice REST endpoints for Speech-to-Text (STT) and Text-to-Speech (TTS).
 * 
 * Uses ffmpeg to convert WebM/Opus to PCM, then AWS Transcribe Streaming for STT.
 * Uses AWS Polly for TTS.
 */
@RestController
class VoiceController(
    private val voiceHandler: LexStreamingVoiceHandler,
    private val pollyClient: PollyAsyncClient,
    private val transcribeStreamingClient: TranscribeStreamingAsyncClient
) {
    private val logger = LoggerFactory.getLogger(VoiceController::class.java)
    
    @PostMapping("/api/voice/start", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun startVoiceSession(@RequestBody request: VoiceStartRequest): Flow<ServerSentEvent<Any>> {
        return voiceHandler.startVoiceSession(request.userId, request.locale, emptyFlow<ByteArray>())
            .map { event ->
                ServerSentEvent.builder<Any>()
                    .event(event::class.simpleName ?: "VoiceEvent")
                    .data(event)
                    .build()
            }
    }
    
    /**
     * Transcribe audio to text.
     * Converts WebM/Opus to PCM using ffmpeg, then sends to AWS Transcribe.
     */
    @PostMapping("/api/voice/transcribe")
    suspend fun transcribeAudio(@RequestBody request: TranscribeRequest): TranscribeResponse {
        logger.info("Transcribe request - audio length: ${request.audio.length}")
        
        return try {
            val audioBytes = Base64.getDecoder().decode(request.audio)
            logger.info("Decoded audio bytes: ${audioBytes.size}")
            
            // Convert WebM to PCM using ffmpeg
            val pcmBytes = convertWebmToPcm(audioBytes)
            if (pcmBytes == null) {
                logger.error("Failed to convert audio format")
                return TranscribeResponse(error = "Failed to convert audio format")
            }
            logger.info("Converted to PCM: ${pcmBytes.size} bytes")
            
            // Try English first, then Arabic
            val englishResult = transcribeWithPcm(pcmBytes, LanguageCode.EN_US)
            logger.info("English transcription result: '$englishResult'")
            
            if (englishResult.isNotBlank()) {
                return TranscribeResponse(text = englishResult, detectedLanguage = "en")
            }
            
            // If English didn't produce results, try Arabic
            val arabicResult = transcribeWithPcm(pcmBytes, LanguageCode.fromValue("ar-SA"))
            logger.info("Arabic transcription result: '$arabicResult'")
            
            if (arabicResult.isNotBlank()) {
                return TranscribeResponse(text = arabicResult, detectedLanguage = "ar")
            }
            
            // Try to detect language from any results
            logger.warn("No transcription results from either language")
            TranscribeResponse(text = "", detectedLanguage = "")
        } catch (e: Exception) {
            logger.error("Transcription failed", e)
            TranscribeResponse(error = "Transcription failed: ${e.message}")
        }
    }
    
    /**
     * Convert WebM/Opus audio to 16-bit PCM at 16kHz using ffmpeg.
     */
    private suspend fun convertWebmToPcm(webmBytes: ByteArray): ByteArray? {
        return withContext(Dispatchers.IO) {
            val tempId = UUID.randomUUID().toString()
            val inputFile = File.createTempFile("audio_$tempId", ".webm")
            val outputFile = File.createTempFile("audio_$tempId", ".pcm")
            
            try {
                // Write WebM to temp file
                inputFile.writeBytes(webmBytes)
                
                // Run ffmpeg to convert to 16-bit PCM, 16kHz, mono
                val process = ProcessBuilder(
                    "ffmpeg", "-y",
                    "-i", inputFile.absolutePath,
                    "-f", "s16le",           // 16-bit signed little-endian PCM
                    "-acodec", "pcm_s16le",
                    "-ar", "16000",          // 16kHz sample rate
                    "-ac", "1",              // Mono
                    outputFile.absolutePath
                )
                    .redirectErrorStream(true)
                    .start()
                
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val error = process.inputStream.bufferedReader().readText()
                    logger.error("ffmpeg failed with exit code $exitCode: $error")
                    return@withContext null
                }
                
                // Read the PCM output
                outputFile.readBytes()
            } catch (e: Exception) {
                logger.error("Audio conversion failed", e)
                null
            } finally {
                // Clean up temp files
                inputFile.delete()
                outputFile.delete()
            }
        }
    }
    
    /**
     * Transcribe PCM audio with AWS Transcribe Streaming.
     */
    private suspend fun transcribeWithPcm(pcmBytes: ByteArray, languageCode: LanguageCode): String {
        val transcriptResult = AtomicReference("")
        val completionFuture = CompletableFuture<String>()
        
        val responseHandler = StartStreamTranscriptionResponseHandler.builder()
            .onResponse { response ->
                logger.debug("Transcribe session started for $languageCode: ${response.sessionId()}")
            }
            .onError { error ->
                logger.warn("Transcribe error for $languageCode: ${error.message}")
                if (!completionFuture.isDone) {
                    completionFuture.complete("") // Return empty on error
                }
            }
            .onComplete {
                if (!completionFuture.isDone) {
                    completionFuture.complete(transcriptResult.get())
                }
            }
            .subscriber { event ->
                when (event) {
                    is TranscriptEvent -> {
                        event.transcript()?.results()?.forEach { result ->
                            if (!result.isPartial) {
                                result.alternatives()?.firstOrNull()?.transcript()?.let { text ->
                                    val current = transcriptResult.get()
                                    transcriptResult.set(if (current.isEmpty()) text else "$current $text")
                                    logger.debug("Got transcript: $text")
                                }
                            }
                        }
                    }
                }
            }
            .build()
        
        // Create audio publisher that sends PCM data in chunks
        val audioStream = object : Publisher<AudioStream> {
            override fun subscribe(subscriber: Subscriber<in AudioStream>) {
                subscriber.onSubscribe(object : Subscription {
                    private var sent = false
                    override fun request(n: Long) {
                        if (!sent && n > 0) {
                            sent = true
                            try {
                                // Send audio in chunks (8KB chunks recommended)
                                val chunkSize = 8192
                                var offset = 0
                                while (offset < pcmBytes.size) {
                                    val end = minOf(offset + chunkSize, pcmBytes.size)
                                    val chunk = pcmBytes.copyOfRange(offset, end)
                                    subscriber.onNext(
                                        AudioEvent.builder()
                                            .audioChunk(SdkBytes.fromByteArray(chunk))
                                            .build()
                                    )
                                    offset = end
                                }
                                subscriber.onComplete()
                            } catch (e: Exception) {
                                logger.error("Error sending audio", e)
                                subscriber.onError(e)
                            }
                        }
                    }
                    override fun cancel() {}
                })
            }
        }
        
        // Build transcription request for PCM audio
        val transcribeRequest = StartStreamTranscriptionRequest.builder()
            .languageCode(languageCode)
            .mediaEncoding(MediaEncoding.PCM)
            .mediaSampleRateHertz(16000)
            .build()
        
        logger.info("Starting Transcribe stream for $languageCode with PCM encoding, 16kHz")
        
        withContext(Dispatchers.IO) {
            transcribeStreamingClient.startStreamTranscription(
                transcribeRequest,
                audioStream,
                responseHandler
            )
        }
        
        // Wait for completion with timeout
        return withContext(Dispatchers.IO) {
            try {
                completionFuture.get(15, TimeUnit.SECONDS).trim()
            } catch (e: java.util.concurrent.TimeoutException) {
                logger.warn("Transcription timeout for $languageCode")
                transcriptResult.get().trim()
            }
        }
    }
    
    /**
     * Synthesize text to speech using AWS Polly.
     */
    @PostMapping("/api/voice/synthesize")
    suspend fun synthesizeSpeech(@RequestBody request: SynthesizeRequest): SynthesizeResponse {
        logger.info("Synthesize request - text: ${request.text.take(50)}...")
        
        return try {
            // Auto-detect language from text content
            val isArabic = request.text.any { char -> char in '\u0600'..'\u06FF' || char in '\u0750'..'\u077F' }
            
            // Select voice based on detected language
            val voiceId = if (isArabic) VoiceId.ZEINA else VoiceId.JOANNA
            val engine = if (isArabic) Engine.STANDARD else Engine.NEURAL
            
            logger.info("TTS using voice: $voiceId (isArabic=$isArabic)")
            
            val pollyRequest = SynthesizeSpeechRequest.builder()
                .outputFormat(OutputFormat.MP3)
                .text(request.text)
                .voiceId(voiceId)
                .engine(engine)
                .build()
            
            val response = pollyClient.synthesizeSpeech(pollyRequest, 
                software.amazon.awssdk.core.async.AsyncResponseTransformer.toBytes()
            ).await()
            
            val audioBase64 = Base64.getEncoder().encodeToString(response.asByteArray())
            
            logger.info("TTS successful - audio size: ${response.asByteArray().size} bytes")
            
            SynthesizeResponse(audioBase64 = audioBase64)
        } catch (e: Exception) {
            logger.error("TTS failed", e)
            SynthesizeResponse(error = "Speech synthesis failed: ${e.message}")
        }
    }
}

// Request/Response DTOs
data class TranscribeRequest(
    val audio: String, // Base64-encoded audio (WebM/Opus from browser)
    val language: String = "auto"
)

data class TranscribeResponse(
    val text: String = "",
    val detectedLanguage: String = "",
    val error: String? = null
)

data class SynthesizeRequest(
    val text: String,
    val language: String = "auto"
)

data class SynthesizeResponse(
    val audioBase64: String = "",
    val error: String? = null
)
