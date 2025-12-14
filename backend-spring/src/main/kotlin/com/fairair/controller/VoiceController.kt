package com.fairair.controller

import com.fairair.ai.voice.LexStreamingVoiceHandler
import com.fairair.contract.dto.VoiceEvent
import com.fairair.contract.dto.VoiceStartRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class VoiceController(
    private val voiceHandler: LexStreamingVoiceHandler
) {
    
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
}
