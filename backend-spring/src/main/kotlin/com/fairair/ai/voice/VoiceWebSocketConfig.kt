package com.fairair.ai.voice

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class VoiceWebSocketConfig(
    private val voiceWebSocketHandler: VoiceWebSocketHandler
) {
    @Bean
    fun voiceWebSocketHandlerMapping(): HandlerMapping {
        val map = mapOf("/api/voice/socket" to voiceWebSocketHandler)
        val mapping = SimpleUrlHandlerMapping()
        mapping.urlMap = map
        mapping.order = -1 // High priority
        return mapping
    }

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}
