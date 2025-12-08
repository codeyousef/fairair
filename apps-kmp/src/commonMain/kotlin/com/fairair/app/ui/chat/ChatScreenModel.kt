package com.fairair.app.ui.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.app.api.ApiResult
import com.fairair.app.api.FairairApiClient
import com.fairair.app.persistence.LocalStorage
import com.fairair.app.state.BookingFlowState
import com.fairair.app.voice.VoiceLanguages
import com.fairair.app.voice.VoiceService
import com.fairair.app.voice.VoiceState
import com.fairair.app.voice.createVoiceService
import com.fairair.contract.dto.ChatContextDto
import com.fairair.contract.dto.ChatResponseDto
import com.fairair.contract.dto.ChatUiType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val inputText: String = "",
    val interimText: String = "", // Real-time voice transcription preview
    val isExpanded: Boolean = false,
    val error: String? = null,
    val voiceError: String? = null,
    val currentLocale: String = "en-US" // Current language for voice
)

/**
 * ScreenModel for the Faris AI chat functionality.
 * Manages conversation state, API calls, and voice interaction.
 */
class ChatScreenModel(
    private val apiClient: FairairApiClient,
    private val localStorage: LocalStorage,
    private val bookingFlowState: BookingFlowState
) : ScreenModel {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Voice service for STT and TTS
    private val voiceService: VoiceService = createVoiceService()

    // Session ID persists across messages for conversation continuity
    @OptIn(ExperimentalUuidApi::class)
    private var sessionId: String = Uuid.random().toString()

    // Current context (PNR, screen, etc.)
    private var currentContext: ChatContextDto? = null
    
    init {
        // Collect voice state changes
        voiceService.state.onEach { voiceState ->
            _uiState.value = _uiState.value.copy(
                isListening = voiceState.isListening,
                isSpeaking = voiceState.isSpeaking,
                interimText = voiceState.interimText,
                voiceError = voiceState.error
            )
        }.launchIn(screenModelScope)
        
        // Collect transcribed text and send as message
        voiceService.transcribedText.onEach { text ->
            if (text.isNotBlank()) {
                sendMessage(text, _uiState.value.currentLocale)
            }
        }.launchIn(screenModelScope)
    }

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
     * Builds the full context for the current message, including user info and search state.
     */
    private fun buildFullContext(): ChatContextDto {
        val user = localStorage.getCurrentUser()
        val searchResult = bookingFlowState.searchResult
        val selectedFlight = bookingFlowState.selectedFlight
        
        return ChatContextDto(
            currentPnr = currentContext?.currentPnr,
            currentScreen = currentContext?.currentScreen,
            userId = user?.id,
            userEmail = user?.email,
            lastSearchId = searchResult?.searchId,
            lastFlightNumber = selectedFlight?.flight?.flightNumber,
            metadata = currentContext?.metadata ?: emptyMap()
        )
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
                context = buildFullContext()
            )

            // Remove loading message
            val messagesWithoutLoading = _uiState.value.messages.filter { it.id != "loading" }

            when (result) {
                is ApiResult.Success -> {
                    val response = result.data
                    println("ChatScreenModel: Received response - uiType=${response.uiType}, uiData length=${response.uiData?.length ?: 0}")
                    val aiMessage = ChatMessage(
                        id = Uuid.random().toString(),
                        text = response.text,
                        isFromUser = false,
                        uiType = response.uiType,
                        uiData = response.uiData,
                        suggestions = response.suggestions
                    )
                    println("ChatScreenModel: Created message - uiType=${aiMessage.uiType}")

                    _uiState.value = _uiState.value.copy(
                        messages = messagesWithoutLoading + aiMessage,
                        isLoading = false
                    )
                    
                    // Speak the AI response if voice synthesis is available
                    // Use detected language from AI response, falling back to site locale
                    if (voiceService.isSynthesisAvailable()) {
                        val ttsLanguage = when (response.detectedLanguage) {
                            "ar" -> "ar-SA"
                            "en" -> "en-US"
                            else -> _uiState.value.currentLocale
                        }
                        voiceService.speak(response.text, ttsLanguage)
                    }
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

    /**
     * Sets the current locale for voice interaction.
     * @param locale Language code - "en-US" for English, "ar-SA" for Arabic
     */
    fun setLocale(locale: String) {
        _uiState.value = _uiState.value.copy(currentLocale = locale)
    }

    /**
     * Sets the listening state (for voice input).
     * This actually starts/stops the voice recognition.
     */
    fun setListening(listening: Boolean) {
        if (listening) {
            startListening()
        } else {
            stopListening()
        }
    }

    /**
     * Toggles listening state.
     * Starts voice recognition if not listening, stops if already listening.
     */
    fun toggleListening() {
        if (_uiState.value.isListening) {
            stopListening()
        } else {
            startListening()
        }
    }
    
    /**
     * Starts voice recognition with current locale.
     */
    fun startListening() {
        // Stop any ongoing speech synthesis
        voiceService.stopSpeaking()
        
        // Start listening
        voiceService.startListening(_uiState.value.currentLocale)
    }
    
    /**
     * Stops voice recognition.
     */
    fun stopListening() {
        voiceService.stopListening()
    }
    
    /**
     * Stops any ongoing speech synthesis.
     */
    fun stopSpeaking() {
        voiceService.stopSpeaking()
    }
    
    /**
     * Checks if voice recognition is available on this platform.
     */
    fun isVoiceAvailable(): Boolean = voiceService.isRecognitionAvailable()
    
    /**
     * Checks if TTS is available on this platform.
     */
    fun isTTSAvailable(): Boolean = voiceService.isSynthesisAvailable()
}

// Platform-agnostic time function
internal expect fun currentTimeMillis(): Long
