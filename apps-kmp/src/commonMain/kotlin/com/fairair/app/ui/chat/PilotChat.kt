package com.fairair.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// Brand colors for Pilot
private val PilotPrimaryColor = Color(0xFF6366F1) // Indigo
private val PilotSecondaryColor = Color(0xFF8B5CF6) // Purple
private val PilotGradient = Brush.linearGradient(
    colors = listOf(PilotPrimaryColor, PilotSecondaryColor)
)

/**
 * Grid explosion transition effect.
 * Creates an animated grid of squares that expand/contract from the bottom-right corner.
 */
@Composable
fun GridExplosionTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = if (visible) FastOutSlowInEasing else FastOutLinearInEasing
        ),
        label = "grid_explosion"
    )

    Box(modifier = modifier) {
        if (visible || animatedProgress > 0f) {
            // Grid background animation
            if (animatedProgress < 1f) {
                GridBackground(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Content with fade
            AnimatedVisibility(
                visible = animatedProgress > 0.5f,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                content()
            }
        }
    }
}

@Composable
private fun GridBackground(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val gridColor = PilotPrimaryColor
    
    Box(
        modifier = modifier.background(
            color = gridColor.copy(alpha = progress * 0.95f)
        )
    )
}

/**
 * Full-screen Pilot AI interface.
 * Shows the complete chat experience with header, messages, and input.
 */
@Composable
fun PilotFullScreen(
    visible: Boolean,
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSuggestionTapped: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit,
    onVoiceClick: () -> Unit,
    locale: String = "en-US",
    modifier: Modifier = Modifier
) {
    if (!visible) return
    
    val isRtl = locale.startsWith("ar")
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            PilotFullScreenHeader(
                onClearChat = onClearChat,
                onDismiss = onDismiss,
                isRtl = isRtl
            )

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Welcome message if empty
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeMessage(isRtl = isRtl)
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        isRtl = isRtl
                    )
                }
            }

            // Quick suggestions
            if (uiState.messages.isNotEmpty()) {
                val lastMessage = uiState.messages.lastOrNull { !it.isFromUser }
                if (lastMessage?.suggestions?.isNotEmpty() == true) {
                    SuggestionsRow(
                        suggestions = lastMessage.suggestions,
                        onSuggestionTapped = onSuggestionTapped
                    )
                }
            }

            // Input bar with voice button
            PilotFullScreenInputBar(
                inputText = uiState.inputText,
                onInputChange = onInputChange,
                onSendMessage = { onSendMessage(uiState.inputText) },
                onVoiceClick = onVoiceClick,
                isLoading = uiState.isLoading,
                isListening = uiState.isListening,
                isRtl = isRtl
            )
        }
    }
}

@Composable
private fun PilotFullScreenHeader(
    onClearChat: () -> Unit,
    onDismiss: () -> Unit,
    isRtl: Boolean
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = PilotGradient)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pilot avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRtl) "بايلوت" else "Pilot",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRtl) "مساعدك الذكي" else "Your AI Assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Clear chat button
            IconButton(onClick = onClearChat) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear chat",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun PilotFullScreenInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onVoiceClick: () -> Unit,
    isLoading: Boolean,
    isListening: Boolean,
    isRtl: Boolean
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice input button
                IconButton(
                    onClick = onVoiceClick,
                    enabled = !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isListening) PilotPrimaryColor else PilotPrimaryColor.copy(alpha = 0.1f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isListening) "Stop listening" else "Voice input",
                        tint = if (isListening) Color.White else PilotPrimaryColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Text input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isRtl) "اكتب رسالتك..." else "Type a message...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PilotPrimaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                    enabled = !isLoading && !isListening
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                IconButton(
                    onClick = onSendMessage,
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (inputText.isNotBlank() && !isLoading)
                                PilotPrimaryColor else PilotPrimaryColor.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated orb button for the Pilot AI assistant.
 * Shows a pulsing animation and breathing effect.
 */
@Composable
fun PilotOrb(
    onClick: () -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pilot_orb")
    
    // Breathing/pulsing animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 800 else 2000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isListening) 0.7f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 800 else 2000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer glow
        Box(
            modifier = Modifier
                .size(80.dp * scale)
                .background(
                    brush = PilotGradient,
                    shape = CircleShape,
                    alpha = glowAlpha * 0.3f
                )
        )
        
        // Inner glow
        Box(
            modifier = Modifier
                .size(68.dp * scale)
                .background(
                    brush = PilotGradient,
                    shape = CircleShape,
                    alpha = glowAlpha * 0.5f
                )
        )

        // Main orb button
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = PilotPrimaryColor,
            contentColor = Color.White,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Close else Icons.Default.Star,
                contentDescription = "Chat with Pilot",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Floating Action Button for the Pilot AI assistant.
 * Shows a pulsing animation and opens the chat bottom sheet when tapped.
 */
@Composable
fun PilotFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasUnreadMessages: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pilot_pulse")
    
    // Subtle pulsing animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(72.dp * scale)
                .background(
                    brush = PilotGradient,
                    shape = CircleShape,
                    alpha = glowAlpha * 0.3f
                )
        )

        // Main FAB
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = PilotPrimaryColor,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Chat with Pilot",
                modifier = Modifier.size(28.dp)
            )
        }

        // Unread indicator
        if (hasUnreadMessages) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(12.dp)
                    .background(Color.Red, CircleShape)
            )
        }
    }
}

/**
 * Chat bottom sheet for the Pilot AI assistant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilotChatSheet(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSuggestionTapped: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    locale: String = "en-US"
) {
    val isRtl = locale.startsWith("ar")
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            ChatHeader(
                onClearChat = onClearChat,
                onDismiss = onDismiss,
                isRtl = isRtl
            )

            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // Welcome message if empty
                if (uiState.messages.isEmpty()) {
                    item {
                        WelcomeMessage(isRtl = isRtl)
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        isRtl = isRtl
                    )
                }
            }

            // Quick suggestions
            if (uiState.messages.isNotEmpty()) {
                val lastMessage = uiState.messages.lastOrNull { !it.isFromUser }
                if (lastMessage?.suggestions?.isNotEmpty() == true) {
                    SuggestionsRow(
                        suggestions = lastMessage.suggestions,
                        onSuggestionTapped = onSuggestionTapped
                    )
                }
            }

            // Input bar
            ChatInputBar(
                inputText = uiState.inputText,
                onInputChange = onInputChange,
                onSendMessage = { onSendMessage(uiState.inputText) },
                isLoading = uiState.isLoading,
                isRtl = isRtl
            )
        }
    }
}

@Composable
private fun ChatHeader(
    onClearChat: () -> Unit,
    onDismiss: () -> Unit,
    isRtl: Boolean
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = PilotGradient
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pilot icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRtl) "بايلوت" else "Pilot",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRtl) "مساعدك الذكي" else "Your AI Assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Clear chat button
            IconButton(onClick = onClearChat) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear chat",
                    tint = Color.White
                )
            }

            // Close button
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun WelcomeMessage(isRtl: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pilot avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .shadow(8.dp, CircleShape)
                .background(brush = PilotGradient, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isRtl) "مرحباً! أنا بايلوت" else "Hello! I'm Pilot",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRtl) {
                "كيف يمكنني مساعدتك اليوم؟\nيمكنني البحث عن رحلات، إدارة حجزك، أو الإجابة عن أسئلتك."
            } else {
                "How can I help you today?\nI can search flights, manage your booking, or answer your questions."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Initial suggestions
        val suggestions = if (isRtl) {
            listOf("بحث عن رحلة", "إدارة حجز", "تسجيل دخول الرحلة")
        } else {
            listOf("Search for a flight", "Manage my booking", "Check in")
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            suggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { /* TODO: handle */ },
                    label = { Text(suggestion, maxLines = 1) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = PilotPrimaryColor.copy(alpha = 0.1f),
                        labelColor = PilotPrimaryColor
                    )
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isRtl: Boolean
) {
    // Detect if text contains Arabic characters to choose the correct font and direction
    val containsArabic = message.text.any { char -> char in '\u0600'..'\u06FF' || char in '\u0750'..'\u077F' || char in '\uFB50'..'\uFDFF' || char in '\uFE70'..'\uFEFF' }
    val textDirection = if (containsArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromUser) {
        PilotPrimaryColor
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (message.isFromUser) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = alignment
        ) {
            if (message.isLoading) {
                LoadingBubble()
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(max = 300.dp)
                        .background(
                            color = if (message.isError) MaterialTheme.colorScheme.errorContainer else bubbleColor,
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                            )
                        )
                        .padding(12.dp)
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides textDirection
                    ) {
                        Text(
                            text = message.text,
                            color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            PilotPrimaryColor.copy(alpha = alpha),
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun SuggestionsRow(
    suggestions: List<String>,
    onSuggestionTapped: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.take(3).forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionTapped(suggestion) },
                label = { Text(suggestion, maxLines = 1) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = PilotPrimaryColor.copy(alpha = 0.1f),
                    labelColor = PilotPrimaryColor
                )
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean,
    isRtl: Boolean
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice input button (placeholder)
                IconButton(
                    onClick = { /* TODO: Voice input */ },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Voice input",
                        tint = PilotPrimaryColor
                    )
                }

                // Text input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isRtl) "اكتب رسالتك..." else "Type a message...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PilotPrimaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                IconButton(
                    onClick = onSendMessage,
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (inputText.isNotBlank() && !isLoading) 
                                PilotPrimaryColor else PilotPrimaryColor.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
