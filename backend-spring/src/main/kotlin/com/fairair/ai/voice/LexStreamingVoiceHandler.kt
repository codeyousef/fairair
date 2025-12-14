package com.fairair.ai.voice

import com.fairair.ai.booking.service.BookingOrchestrator
import com.fairair.contract.dto.VoiceEvent
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.asFlux
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.core.async.SdkPublisher
import software.amazon.awssdk.services.lexruntimev2.LexRuntimeV2AsyncClient
import software.amazon.awssdk.services.lexruntimev2.model.AudioInputEvent
import software.amazon.awssdk.services.lexruntimev2.model.AudioResponseEvent
import software.amazon.awssdk.services.lexruntimev2.model.ConversationMode
import software.amazon.awssdk.services.lexruntimev2.model.IntentResultEvent
import software.amazon.awssdk.services.lexruntimev2.model.PlaybackInterruptionEvent
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationRequest
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationRequestEventStream
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationResponse
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationResponseEventStream
import software.amazon.awssdk.services.lexruntimev2.model.StartConversationResponseHandler
import software.amazon.awssdk.services.lexruntimev2.model.TextInputEvent
import software.amazon.awssdk.services.lexruntimev2.model.TextResponseEvent
import software.amazon.awssdk.services.lexruntimev2.model.TranscriptEvent

@Component
class LexStreamingVoiceHandler(
    private val bookingOrchestrator: BookingOrchestrator,
    private val lexClient: LexRuntimeV2AsyncClient,
    @Value("\${fairair.ai.lex.bot-id:PROD_BOT}") private val botId: String,
    @Value("\${fairair.ai.lex.bot-alias-id:PROD}") private val botAliasId: String
) {
    private val logger = LoggerFactory.getLogger(LexStreamingVoiceHandler::class.java)

    fun startVoiceSession(
        userId: String,
        locale: String,
        audioInputFlow: Flow<ByteArray>
    ): Flow<VoiceEvent> = callbackFlow {
        
        val sessionId = userId.ifBlank { UUID.randomUUID().toString() }
        
        var lastTranscript = ""
        
        // Channel to merge user audio and system text events into the Lex input stream
        val inputChannel = Channel<StartConversationRequestEventStream>(Channel.UNLIMITED)

        // Launch job to forward user audio to the channel
        val audioJob = launch {
            try {
                audioInputFlow.collect { chunk ->
                    inputChannel.send(
                        AudioInputEvent.builder()
                            .audioChunk(SdkBytes.fromByteArray(chunk))
                            .contentType("audio/l16; rate=16000; channels=1") // Assuming PCM 16k mono
                            .build()
                    )
                }
            } catch (e: Exception) {
                logger.error("Error collecting audio input", e)
            }
        }

        // Convert channel to Publisher for Lex
        val requestPublisher = inputChannel.consumeAsFlow().asFlux()

        val request = StartConversationRequest.builder()
            .botId(botId)
            .botAliasId(botAliasId)
            .localeId(locale)
            .sessionId(sessionId)
            .conversationMode(ConversationMode.AUDIO)
            .build()

        val responseHandler = object : StartConversationResponseHandler {
            override fun responseReceived(response: StartConversationResponse) {
                logger.info("Lex conversation started: $response")
            }

            override fun onEventStream(publisher: SdkPublisher<StartConversationResponseEventStream>) {
                publisher.subscribe(object : Subscriber<StartConversationResponseEventStream> {
                    override fun onSubscribe(s: Subscription) {
                        s.request(Long.MAX_VALUE)
                    }

                    override fun onNext(event: StartConversationResponseEventStream) {
                        when (event) {
                            is TranscriptEvent -> {
                                event.transcript()?.takeIf { it.isNotBlank() }?.let { text ->
                                    lastTranscript = text
                                    trySend(VoiceEvent.Transcription(text))
                                }
                            }
                            is AudioResponseEvent -> {
                                event.audioChunk()?.let { sdkBytes ->
                                    trySend(VoiceEvent.Audio(sdkBytes.asByteArray()))
                                }
                            }
                            is PlaybackInterruptionEvent -> {
                                logger.info("Playback interrupted")
                                trySend(VoiceEvent.Interruption)
                            }
                            is IntentResultEvent -> {
                                val text = lastTranscript
                                if (text.isNotBlank()) {
                                    logger.info("Intent recognized: '$text'")
                                    // Process with Koog
                                    launch {
                                        try {
                                            val result = bookingOrchestrator.handleUserRequest(text, null)
                                            val responseText: String = result.response
                                            
                                            // Send response text to Lex for synthesis
                                            inputChannel.send(
                                                TextInputEvent.builder()
                                                    .text(responseText)
                                                    .build()
                                            )
                                        } catch (e: Exception) {
                                            logger.error("Koog processing failed", e)
                                        }
                                    }
                                }
                            }
                            is TextResponseEvent -> {
                                // Log text response from Lex (if any)
                                event.messages()?.forEach { msg ->
                                    logger.debug("Lex Text Response: ${msg.content()}")
                                }
                            }
                        }
                    }

                    override fun onError(t: Throwable) {
                        logger.error("Lex response stream error", t)
                        close(t)
                    }

                    override fun onComplete() {
                        logger.info("Lex response stream complete")
                        close()
                    }
                })
            }

            override fun exceptionOccurred(throwable: Throwable) {
                logger.error("Lex startConversation exception", throwable)
                close(throwable)
            }

            override fun complete() {
                // Handled in onEventStream onComplete
            }
        }

        val future = lexClient.startConversation(request, requestPublisher, responseHandler)

        awaitClose {
            logger.info("Closing voice session")
            future.cancel(true)
            audioJob.cancel()
            inputChannel.close()
        }
    }
}
