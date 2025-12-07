package com.fairair.app.ui.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.app.api.ApiResult
import com.fairair.app.api.FairairApiClient
import com.fairair.contract.dto.ChatContextDto
import com.fairair.contract.dto.ChatResponseDto
import com.fairair.contract.dto.ChatUiType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a message in the chat history.
 */
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = currentTimeMillis(),
    val uiType: ChatUiType? = null,
    val uiData: String? = null,
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isError: Boolean = false
)

/**
 * UI state for the chat screen.
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val inputText: String = "",
    val isExpanded: Boolean = false,
    val error: String? = null
)

/**
 * ScreenModel for the Faris AI chat functionality.
 * Manages conversation state and API calls.
 */
class ChatScreenModel(
    private val apiClient: FairairApiClient
) : ScreenModel {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Session ID persists across messages for conversation continuity
    @OptIn(ExperimentalUuidApi::class)
    private var sessionId: String = Uuid.random().toString()

    // Current context (PNR, screen, etc.)
    private var currentContext: ChatContextDto? = null

    /**
     * Updates the context for subsequent messages.
     */
    fun updateContext(pnr: String? = null, screen: String? = null) {
        currentContext = if (pnr != null || screen != null) {
            ChatContextDto(
                currentPnr = pnr,
                currentScreen = screen
            )
        } else {
            null
        }
    }

    /**
     * Updates the input text.
     */
    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    /**
     * Sends a message to the AI assistant.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun sendMessage(message: String = _uiState.value.inputText, locale: String = "en-US") {
        val trimmedMessage = message.trim()
        if (trimmedMessage.isEmpty()) return

        val userMessage = ChatMessage(
            id = Uuid.random().toString(),
            text = trimmedMessage,
            isFromUser = true
        )

        // Add user message and loading indicator
        val loadingMessage = ChatMessage(
            id = "loading",
            text = "",
            isFromUser = false,
            isLoading = true
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage + loadingMessage,
            inputText = "",
            isLoading = true,
            error = null
        )

        screenModelScope.launch {
            val result = apiClient.sendChatMessage(
                sessionId = sessionId,
                message = trimmedMessage,
                locale = locale,
                context = currentContext
            )

            // Remove loading message
            val messagesWithoutLoading = _uiState.value.messages.filter { it.id != "loading" }

            when (result) {
                is ApiResult.Success -> {
                    val response = result.data
                    val aiMessage = ChatMessage(
                        id = Uuid.random().toString(),
                        text = response.text,
                        isFromUser = false,
                        uiType = response.uiType,
                        uiData = response.uiData,
                        suggestions = response.suggestions
                    )

                    _uiState.value = _uiState.value.copy(
                        messages = messagesWithoutLoading + aiMessage,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    val errorMessage = ChatMessage(
                        id = Uuid.random().toString(),
                        text = "عذراً، حدث خطأ. يرجى المحاولة مرة أخرى.\n\nSorry, an error occurred. Please try again.",
                        isFromUser = false,
                        isError = true
                    )

                    _uiState.value = _uiState.value.copy(
                        messages = messagesWithoutLoading + errorMessage,
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    /**
     * Handles a quick reply suggestion being tapped.
     */
    fun onSuggestionTapped(suggestion: String) {
        sendMessage(suggestion)
    }

    /**
     * Toggles the expanded state of the chat.
     */
    fun toggleExpanded() {
        _uiState.value = _uiState.value.copy(
            isExpanded = !_uiState.value.isExpanded
        )
    }

    /**
     * Clears the chat history and starts a new session.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun clearChat() {
        screenModelScope.launch {
            apiClient.clearChatSession(sessionId)
            sessionId = Uuid.random().toString()
            _uiState.value = ChatUiState()
        }
    }

    /**
     * Sets the expanded state.
     */
    fun setExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isExpanded = expanded)
    }
}

// Platform-agnostic time function
internal expect fun currentTimeMillis(): Long
