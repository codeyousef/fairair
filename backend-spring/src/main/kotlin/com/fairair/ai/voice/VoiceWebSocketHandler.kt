package com.fairair.ai.voice

import com.fairair.contract.dto.VoiceEvent
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class VoiceWebSocketHandler(
    private val dialogflowHandler: DialogflowVoiceHandler
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(VoiceWebSocketHandler::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    override fun handle(session: WebSocketSession): Mono<Void> {
        val queryMap = session.handshakeInfo.uri.query?.split("&")?.associate {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        } ?: emptyMap()
        
        val userId = queryMap["userId"] ?: "anon"
        val locale = queryMap["locale"] ?: "en_US"

        logger.info("Starting voice session for user: $userId, locale: $locale")

        val inputFlow = session.receive().asFlow()
            .mapNotNull { msg ->
                if (msg.type == WebSocketMessage.Type.BINARY) {
                     val buffer = msg.payload
                     val bytes = ByteArray(buffer.readableByteCount())
                     buffer.read(bytes)
                     bytes
                } else {
                    null
                }
            }

        val outputFlow = dialogflowHandler.startVoiceSession(userId, locale, inputFlow)
            .map { event ->
                when (event) {
                    is VoiceEvent.Audio -> {
                        val buffer = session.bufferFactory().wrap(event.pcmData)
                        session.binaryMessage { buffer }
                    }
                    else -> {
                        val text = json.encodeToString(VoiceEvent.serializer(), event)
                        session.textMessage(text)
                    }
                }
            }

        return session.send(outputFlow.asFlux())
    }
}
