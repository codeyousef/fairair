package com.fairair.ai.voice

import com.fairair.config.FairairProperties
import com.fairair.contract.dto.VoiceEvent
import com.google.api.gax.rpc.BidiStreamObserver
import com.google.api.gax.rpc.StreamController
import com.google.cloud.dialogflow.cx.v3.*
import com.google.protobuf.ByteString
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID
import javax.annotation.PreDestroy

@Component
class DialogflowVoiceHandler(
    private val properties: FairairProperties
) {
    private val logger = LoggerFactory.getLogger(DialogflowVoiceHandler::class.java)
    
    private val sessionsClient: SessionsClient by lazy {
        val settings = SessionsSettings.newBuilder()
            .setEndpoint("${properties.ai.location}-dialogflow.googleapis.com:443")
            .build()
        SessionsClient.create(settings)
    }

    @PreDestroy
    fun cleanup() {
        try {
            sessionsClient.close()
        } catch (e: Exception) {
            logger.warn("Error closing SessionsClient", e)
        }
    }

    fun startVoiceSession(
        userId: String,
        locale: String,
        audioInputFlow: Flow<ByteArray>
    ): Flow<VoiceEvent> = callbackFlow {
        val sessionId = userId.ifBlank { UUID.randomUUID().toString() }
        val sessionName = SessionName.of(properties.ai.projectId, properties.ai.location, properties.ai.agentId, sessionId)

        val responseObserver = object : BidiStreamObserver<StreamingDetectIntentRequest, StreamingDetectIntentResponse> {
            override fun onStart(controller: StreamController?) {}

            override fun onNext(response: StreamingDetectIntentResponse) {
                // Transcription
                if (response.recognitionResult.transcript.isNotBlank()) {
                    trySend(VoiceEvent.Transcription(response.recognitionResult.transcript))
                }
                
                // Audio response (if any)
                if (response.detectIntentResponse.outputAudio != null && !response.detectIntentResponse.outputAudio.isEmpty) {
                    trySend(VoiceEvent.Audio(response.detectIntentResponse.outputAudio.toByteArray()))
                }
            }

            override fun onError(t: Throwable) {
                logger.error("Dialogflow stream error", t)
                close(t)
            }

            override fun onComplete() {
                close()
            }
        }

        val requestObserver = sessionsClient.streamingDetectIntentCallable().splitCall(responseObserver)

        // Send initial config
        val configRequest = StreamingDetectIntentRequest.newBuilder()
            .setSession(sessionName.toString())
            .setQueryInput(QueryInput.newBuilder()
                .setAudio(AudioInput.newBuilder()
                    .setConfig(InputAudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_LINEAR_16)
                        .setSampleRateHertz(16000)
                        .build())
                    .build())
                .setLanguageCode(locale)
                .build())
            .setEnablePartialResponse(true)
            .setOutputAudioConfig(OutputAudioConfig.newBuilder()
                .setAudioEncoding(OutputAudioEncoding.OUTPUT_AUDIO_ENCODING_LINEAR_16)
                .setSampleRateHertz(16000)
                .build())
            .build()
        
        requestObserver.onNext(configRequest)

        // Stream audio
        val audioJob = launch {
            try {
                audioInputFlow.collect { chunk ->
                    val audioRequest = StreamingDetectIntentRequest.newBuilder()
                        .setQueryInput(QueryInput.newBuilder()
                            .setAudio(AudioInput.newBuilder()
                                .setAudio(ByteString.copyFrom(chunk))
                                .build())
                            .build())
                        .build()
                    requestObserver.onNext(audioRequest)
                }
                requestObserver.onCompleted()
            } catch (e: Exception) {
                logger.error("Error sending audio to Dialogflow", e)
                requestObserver.onError(e)
            }
        }
        
        awaitClose {
            audioJob.cancel()
        }
    }
}
