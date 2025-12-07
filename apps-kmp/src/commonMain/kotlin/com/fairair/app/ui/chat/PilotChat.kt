package com.fairair.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.contract.dto.ChatUiType
import kotlinx.coroutines.launch

// Pilot brand colors - using airline theme
private val PilotPrimaryColor = Color(0xFF0EA5E9) // Sky blue
private val PilotSecondaryColor = Color(0xFF6366F1) // Indigo
private val PilotAccentColor = Color(0xFF22D3EE) // Cyan
private val PilotGradient = Brush.radialGradient(
    colors = listOf(PilotAccentColor, PilotPrimaryColor, PilotSecondaryColor)
)

// =============================================================================
// PILOT ORB - The Floating Action Button
// =============================================================================

/**
 * The "Pilot" Orb - a pulsing, glowing orb that opens the voice assistant.
 * Per spec: "Animation: Pulses when listening"
 */
@Composable
fun PilotOrb(
    onClick: () -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pilot_orb")
    
    // Breathing pulse animation
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 800 else 2000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Glow intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isListening) 0.9f else 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isListening) 600 else 1500,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Rotation for the inner ring when listening
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow layers
        repeat(3) { index ->
            val layerScale = breatheScale + (index * 0.1f)
            val layerAlpha = (glowAlpha * (1f - index * 0.3f)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(layerScale)
                    .background(
                        brush = PilotGradient,
                        shape = CircleShape,
                        alpha = layerAlpha * 0.3f
                    )
            )
        }

        // Inner rotating ring (visible when listening)
        if (isListening) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .graphicsLayer { rotationZ = rotation }
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                PilotAccentColor,
                                Color.Transparent,
                                Color.Transparent,
                                PilotAccentColor
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main orb
        Surface(
            modifier = Modifier
                .size(48.dp)
                .shadow(8.dp, CircleShape),
            shape = CircleShape,
            color = PilotPrimaryColor
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PilotAccentColor.copy(alpha = 0.6f),
                                PilotPrimaryColor
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Airplane icon
                Icon(
                    imageVector = if (isListening) Icons.Default.Settings else Icons.Default.Star,
                    contentDescription = "Pilot Assistant",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// =============================================================================
// PILOT CHAT SHEET - Wrapper for compatibility
// =============================================================================

/**
 * Wrapper function that matches the FarisChatSheet API for easy migration.
 * Delegates to PilotOverlay with the new voice-first design.
 */
@Composable
fun PilotChatSheet(
    uiState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSuggestionTapped: (String) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit,
    onVoiceClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    locale: String = "en-US"
) {
    PilotOverlay(
        uiState = uiState,
        isListening = uiState.isListening,
        onMicClick = onVoiceClick,
        onSendMessage = onSendMessage,
        onInputChange = onInputChange,
        onSuggestionTapped = onSuggestionTapped,
        onClearChat = onClearChat,
        onDismiss = onDismiss,
        modifier = modifier,
        locale = locale
    )
}

// =============================================================================
// PILOT OVERLAY - Half-height Bottom Sheet
// =============================================================================

/**
 * Half-height bottom sheet for voice-first interaction.
 * Per spec: "Opens half-height BottomSheet (Chat Overlay)"
 */
@Composable
fun PilotOverlay(
    uiState: ChatUiState,
    isListening: Boolean,
    onMicClick: () -> Unit,
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

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f), // Half-height as per spec
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with dismiss handle
            PilotHeader(
                onDismiss = onDismiss,
                onClearChat = onClearChat,
                isRtl = isRtl
            )

            // Content area - either polymorphic cards or welcome
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.messages.isEmpty()) {
                    PilotWelcome(isRtl = isRtl)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            PolymorphicChatItem(
                                message = message,
                                isRtl = isRtl
                            )
                        }
                    }
                }
            }

            // Quick suggestions
            val lastAssistantMessage = uiState.messages.lastOrNull { !it.isFromUser }
            if (lastAssistantMessage?.suggestions?.isNotEmpty() == true) {
                QuickSuggestions(
                    suggestions = lastAssistantMessage.suggestions,
                    onSuggestionTapped = onSuggestionTapped,
                    isRtl = isRtl
                )
            }

            // Voice-first input bar
            VoiceInputBar(
                inputText = uiState.inputText,
                onInputChange = onInputChange,
                onSendMessage = { onSendMessage(uiState.inputText) },
                onMicClick = onMicClick,
                isListening = isListening,
                isLoading = uiState.isLoading,
                isRtl = isRtl
            )
        }
    }
}

@Composable
private fun PilotHeader(
    onDismiss: () -> Unit,
    onClearChat: () -> Unit,
    isRtl: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag handle
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    RoundedCornerShape(2.dp)
                )
        )
        
        // Title row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pilot avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(PilotAccentColor, PilotPrimaryColor)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Pilot",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRtl) "مساعدك للسفر" else "Your travel assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onClearChat) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun PilotWelcome(isRtl: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated orb
        val infiniteTransition = rememberInfiniteTransition(label = "welcome")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PilotAccentColor.copy(alpha = 0.3f),
                                PilotPrimaryColor.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(PilotAccentColor, PilotPrimaryColor)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isRtl) "هلا! أنا Pilot" else "Hey! I'm Pilot",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRtl) {
                "اضغط على الميكروفون وتكلم معي"
            } else {
                "Tap the mic and talk to me"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Example prompts
        val prompts = if (isRtl) {
            listOf("أبغى رحلة لجدة", "غيّر مقعدي", "وريني البوردنق")
        } else {
            listOf("Find me a flight to Jeddah", "Change my seat", "Show my boarding pass")
        }

        prompts.forEach { prompt ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "\"$prompt\"",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =============================================================================
// POLYMORPHIC UI ITEMS - Render based on uiType
// =============================================================================

@Composable
private fun PolymorphicChatItem(
    message: ChatMessage,
    isRtl: Boolean
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        when {
            message.isLoading -> LoadingIndicator()
            message.isFromUser -> UserBubble(message.text)
            message.uiType != null -> {
                // Render specialized UI based on type
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (message.text.isNotBlank()) {
                        AssistantBubble(message.text)
                    }
                    PolymorphicCard(
                        uiType = message.uiType,
                        uiData = message.uiData,
                        isRtl = isRtl
                    )
                }
            }
            else -> AssistantBubble(message.text)
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
            color = PilotPrimaryColor
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 150),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
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
}

// =============================================================================
// POLYMORPHIC CARDS - FlightCarousel, SeatMap, BoardingPass, etc.
// =============================================================================

@Composable
private fun PolymorphicCard(
    uiType: ChatUiType,
    uiData: String?,
    isRtl: Boolean
) {
    when (uiType) {
        ChatUiType.FLIGHT_LIST -> FlightCarouselCard(uiData, isRtl)
        ChatUiType.SEAT_MAP -> SeatMapCard(uiData, isRtl)
        ChatUiType.BOARDING_PASS -> BoardingPassCard(uiData, isRtl)
        ChatUiType.FLIGHT_COMPARISON -> ComparisonCard(uiData, isRtl)
        ChatUiType.BOOKING_SUMMARY -> BookingSummaryCard(uiData, isRtl)
        else -> {
            // Fallback for other types
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "UI: ${uiType.name}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FlightCarouselCard(uiData: String?, isRtl: Boolean) {
    // Mock flight data for demo - in production, parse uiData JSON
    val flights = listOf(
        Triple("FA 101", "09:00", "SAR 450"),
        Triple("FA 203", "14:30", "SAR 380"),
        Triple("FA 305", "19:15", "SAR 520")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isRtl) "الرحلات المتاحة" else "Available Flights",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(flights) { (flightNum, time, price) ->
                FlightOptionCard(
                    flightNumber = flightNum,
                    departureTime = time,
                    price = price,
                    onClick = { /* Select flight */ }
                )
            }
        }
    }
}

@Composable
private fun FlightOptionCard(
    flightNumber: String,
    departureTime: String,
    price: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = flightNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PilotPrimaryColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = departureTime,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PilotPrimaryColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = price,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = PilotPrimaryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SeatMapCard(uiData: String?, isRtl: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isRtl) "اختر مقعدك" else "Select Your Seat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mini seat map visualization
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // Left side (A, B, C)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("A", "B", "C").forEach { col ->
                                SeatIcon(
                                    label = "${row + 10}$col",
                                    isAvailable = (row + col.hashCode()) % 3 != 0,
                                    isHighlighted = row == 2 && col == "C"
                                )
                            }
                        }
                    }
                }
                
                // Aisle
                Spacer(modifier = Modifier.width(24.dp))
                
                // Right side (D, E, F)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(4) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("D", "E", "F").forEach { col ->
                                SeatIcon(
                                    label = "${row + 10}$col",
                                    isAvailable = (row + col.hashCode()) % 2 != 0,
                                    isHighlighted = row == 2 && col == "F"
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = PilotPrimaryColor, label = if (isRtl) "متاح" else "Available")
                LegendItem(color = Color.Gray, label = if (isRtl) "محجوز" else "Occupied")
                LegendItem(color = PilotAccentColor, label = if (isRtl) "مقعدك" else "Your seat")
            }
        }
    }
}

@Composable
private fun SeatIcon(
    label: String,
    isAvailable: Boolean,
    isHighlighted: Boolean
) {
    val color = when {
        isHighlighted -> PilotAccentColor
        isAvailable -> PilotPrimaryColor
        else -> Color.Gray.copy(alpha = 0.5f)
    }
    
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.takeLast(1),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun BoardingPassCard(uiData: String?, isRtl: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(PilotPrimaryColor, PilotSecondaryColor)
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isRtl) "بطاقة الصعود" else "BOARDING PASS",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "RUH",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRtl) "الرياض" else "Riyadh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PilotPrimaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "JED",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isRtl) "جدة" else "Jeddah",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoColumn(
                        label = if (isRtl) "الرحلة" else "FLIGHT",
                        value = "FA 203"
                    )
                    InfoColumn(
                        label = if (isRtl) "البوابة" else "GATE",
                        value = "A12"
                    )
                    InfoColumn(
                        label = if (isRtl) "المقعد" else "SEAT",
                        value = "12F"
                    )
                    InfoColumn(
                        label = if (isRtl) "الإقلاع" else "DEPART",
                        value = "14:30"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // QR Code placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR CODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ComparisonCard(uiData: String?, isRtl: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isRtl) "مقارنة الرحلات" else "Flight Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Old flight
                ComparisonColumn(
                    modifier = Modifier.weight(1f),
                    title = if (isRtl) "الحالي" else "Current",
                    flightNumber = "FA 101",
                    time = "09:00",
                    isOld = true
                )
                
                // Arrow
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PilotPrimaryColor,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically)
                )
                
                // New flight
                ComparisonColumn(
                    modifier = Modifier.weight(1f),
                    title = if (isRtl) "الجديد" else "New",
                    flightNumber = "FA 203",
                    time = "14:30",
                    isOld = false
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Price difference
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = PilotPrimaryColor.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRtl) "الفرق" else "Difference",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "+SAR 70",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PilotPrimaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonColumn(
    modifier: Modifier = Modifier,
    title: String,
    flightNumber: String,
    time: String,
    isOld: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isOld) 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else 
            PilotPrimaryColor.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = flightNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = time,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOld) MaterialTheme.colorScheme.onSurfaceVariant else PilotPrimaryColor
            )
        }
    }
}

@Composable
private fun BookingSummaryCard(uiData: String?, isRtl: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRtl) "ملخص الحجز" else "Booking Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PilotAccentColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "ABC123",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = PilotPrimaryColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            SummaryRow(
                label = if (isRtl) "الرحلة" else "Flight",
                value = "FA 203 • 14:30"
            )
            SummaryRow(
                label = if (isRtl) "المسار" else "Route",
                value = "RUH → JED"
            )
            SummaryRow(
                label = if (isRtl) "المسافرون" else "Passengers",
                value = "2 Adults"
            )
            SummaryRow(
                label = if (isRtl) "المقاعد" else "Seats",
                value = "12E, 12F"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isRtl) "المجموع" else "Total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SAR 760",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PilotPrimaryColor
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// =============================================================================
// INPUT BAR - Voice-first with text fallback
// =============================================================================

@Composable
private fun QuickSuggestions(
    suggestions: List<String>,
    onSuggestionTapped: (String) -> Unit,
    isRtl: Boolean
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.take(4).forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionTapped(suggestion) },
                    label = { 
                        Text(
                            text = suggestion,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = PilotPrimaryColor.copy(alpha = 0.1f),
                        labelColor = PilotPrimaryColor
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
private fun VoiceInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean,
    isLoading: Boolean,
    isRtl: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

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
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Text input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = if (isRtl) "اكتب أو تكلم..." else "Type or speak...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PilotPrimaryColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                    enabled = !isLoading && !isListening
                )

                // Mic button (prominent)
                Surface(
                    modifier = Modifier
                        .size(52.dp)
                        .scale(micScale)
                        .clickable(enabled = !isLoading) { onMicClick() },
                    shape = CircleShape,
                    color = if (isListening) PilotAccentColor else PilotPrimaryColor,
                    shadowElevation = if (isListening) 8.dp else 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (isListening) "Stop" else "Speak",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Send button (only show if there's text)
                AnimatedVisibility(
                    visible = inputText.isNotBlank() && !isListening,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = onSendMessage,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .background(PilotPrimaryColor, CircleShape)
                    ) {
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
