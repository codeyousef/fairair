package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.contract.dto.ChatMessageRequestDto
import com.fairair.contract.dto.ChatResponseDto
import com.fairair.service.ChatService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

/**
 * REST controller for the Pilot AI assistant chat endpoints.
 * 
 * Handles:
 * - Message exchange with the AI (with tool execution)
 * - Session management
 * - History clearing
 */
@RestController
@RequestMapping(ApiRoutes.Chat.BASE)
class ChatController(
    private val chatService: ChatService
) {
    private val log = LoggerFactory.getLogger(ChatController::class.java)

    /**
     * POST /api/v1/chat/message
     *
     * Sends a message to the Pilot AI assistant and receives a response.
     * The AI may execute tools (search flights, get booking, etc.) to answer.
     * 
     * @param request The chat message with sessionId
     * @return ChatResponseDto containing the AI's response and any UI hints
     */
    @PostMapping("/message")
    suspend fun sendMessage(@RequestBody request: ChatMessageRequestDto): ResponseEntity<ChatResponseDto> {
        // Validate request
        if (request.sessionId.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required")
        }
        if (request.message.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "message cannot be empty")
        }

        log.info("POST /chat/message: sessionId=${request.sessionId}, message='${request.message.take(50)}...'")

        return try {
            val response = chatService.processMessage(request)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            log.error("Chat error for session ${request.sessionId}", e)
            ResponseEntity.ok(
                ChatResponseDto(
                    text = "عذراً، حدث خطأ. يرجى المحاولة مرة أخرى.\n\nSorry, an error occurred. Please try again.",
                    isPartial = false
                )
            )
        }
    }

    /**
     * DELETE /api/v1/chat/sessions/{sessionId}
     *
     * Clears the conversation history for a session.
     * This allows the user to start a fresh conversation.
     *
     * @param sessionId The session ID to clear
     * @return 204 No Content on success
     */
    @DeleteMapping("/sessions/{sessionId}")
    suspend fun clearSession(@PathVariable sessionId: String): ResponseEntity<Unit> {
        log.info("DELETE /chat/sessions/$sessionId")
        chatService.clearSession(sessionId)
        return ResponseEntity.noContent().build()
    }

    /**
     * GET /api/v1/chat/sessions/{sessionId}/history
     *
     * Gets the conversation history for a session.
     * Useful for restoring chat state on app restart.
     *
     * @param sessionId The session ID
     * @return List of previous messages in the session
     */
    @GetMapping("/sessions/{sessionId}/history")
    suspend fun getSessionHistory(@PathVariable sessionId: String): ResponseEntity<List<ChatResponseDto>> {
        log.info("GET /chat/sessions/$sessionId/history")
        val history = chatService.getHistory(sessionId)
        return ResponseEntity.ok(history)
    }
}
