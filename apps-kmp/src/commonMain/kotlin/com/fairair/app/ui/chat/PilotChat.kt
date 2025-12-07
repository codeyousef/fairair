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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull

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
                // Use simple text - F for Faris, mic icon when listening
                Text(
                    text = if (isListening) "M" else "F",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
    onLocaleChange: (String) -> Unit = {},
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
        onLocaleChange = onLocaleChange,
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
    onLocaleChange: (String) -> Unit = {},
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
            // Header with dismiss handle and language toggle
            PilotHeader(
                onDismiss = onDismiss,
                onClearChat = onClearChat,
                onLocaleChange = onLocaleChange,
                currentLocale = locale,
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
            
            // Show interim transcription while listening
            if (isListening && uiState.interimText.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = PilotPrimaryColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = uiState.interimText,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Voice-first input bar
            VoiceInputBar(
                inputText = uiState.inputText,
                onInputChange = onInputChange,
                onSendMessage = { onSendMessage(uiState.inputText) },
                onMicClick = onMicClick,
                isListening = isListening,
                isSpeaking = uiState.isSpeaking,
                isLoading = uiState.isLoading,
                isRtl = isRtl,
                voiceError = uiState.voiceError
            )
        }
    }
}

@Composable
private fun PilotHeader(
    onDismiss: () -> Unit,
    onClearChat: () -> Unit,
    onLocaleChange: (String) -> Unit,
    currentLocale: String,
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
                Text(
                    text = "F",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRtl) "فارس" else "Faris",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRtl) "مساعدك للسفر" else "Your travel assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Language toggle button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(PilotPrimaryColor.copy(alpha = 0.1f))
                    .clickable {
                        val newLocale = if (currentLocale.startsWith("ar")) "en-US" else "ar-SA"
                        onLocaleChange(newLocale)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (currentLocale.startsWith("ar")) "EN" else "AR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PilotPrimaryColor
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))

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
                Text(
                    text = "F",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
    // Debug logging
    println("PolymorphicChatItem: isFromUser=${message.isFromUser}, isLoading=${message.isLoading}, uiType=${message.uiType}")
    
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        when {
            message.isLoading -> LoadingIndicator()
            message.isFromUser -> UserBubble(message.text)
            message.uiType != null -> {
                println("PolymorphicChatItem: Rendering card for uiType=${message.uiType}")
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
    // Parse flight data from uiData JSON
    val flightPayload = remember(uiData) {
        uiData?.let { parseFlightListData(it) }
    }
    
    // If parsing fails, show a placeholder
    if (flightPayload == null || flightPayload.flights.isEmpty()) {
        Text(
            text = if (isRtl) "لا توجد رحلات متاحة" else "No flights available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Show route info header
        Text(
            text = if (isRtl) {
                "الرحلات المتاحة من ${flightPayload.origin} إلى ${flightPayload.destination}"
            } else {
                "Flights from ${flightPayload.origin} to ${flightPayload.destination}"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = flightPayload.date,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(flightPayload.flights) { flight ->
                FlightOptionCard(
                    flightNumber = flight.flightNumber,
                    departureTime = flight.departureTime,
                    price = "${flight.currency} ${flight.lowestPrice.toInt()}",
                    onClick = { /* Select flight */ }
                )
            }
        }
    }
}

/**
 * Data class for parsed flight list UI data
 */
private data class FlightListUiData(
    val origin: String,
    val destination: String,
    val date: String,
    val flights: List<FlightItemData>
)

private data class FlightItemData(
    val flightNumber: String,
    val departureTime: String,
    val arrivalTime: String,
    val duration: String,
    val lowestPrice: Double,
    val currency: String
)

/**
 * Parse the uiData JSON string into FlightListUiData
 */
private fun parseFlightListData(uiData: String): FlightListUiData? {
    return try {
        println("Parsing flight list data: ${uiData.take(100)}...")
        val json = Json { ignoreUnknownKeys = true }
        val jsonObj = json.parseToJsonElement(uiData).jsonObject
        
        val origin = jsonObj["origin"]?.jsonPrimitive?.contentOrNull ?: ""
        val destination = jsonObj["destination"]?.jsonPrimitive?.contentOrNull ?: ""
        val date = jsonObj["date"]?.jsonPrimitive?.contentOrNull ?: ""
        
        val flights = jsonObj["flights"]?.jsonArray?.mapNotNull { flightElement ->
            val flight = flightElement.jsonObject
            FlightItemData(
                flightNumber = flight["flightNumber"]?.jsonPrimitive?.contentOrNull ?: "",
                departureTime = formatTimeFromIso(flight["departureTime"]?.jsonPrimitive?.contentOrNull ?: ""),
                arrivalTime = formatTimeFromIso(flight["arrivalTime"]?.jsonPrimitive?.contentOrNull ?: ""),
                duration = flight["duration"]?.jsonPrimitive?.contentOrNull ?: "",
                lowestPrice = flight["lowestPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                currency = flight["currency"]?.jsonPrimitive?.contentOrNull ?: "SAR"
            )
        } ?: emptyList()
        
        println("Parsed ${flights.size} flights from $origin to $destination")
        FlightListUiData(origin, destination, date, flights)
    } catch (e: Exception) {
        println("Failed to parse flight list data: ${e.message}")
        null
    }
}

/**
 * Format ISO datetime to readable time (e.g., "2025-12-08T03:00:00Z" -> "3:00 AM")
 */
private fun formatTimeFromIso(isoDateTime: String): String {
    return try {
        // Extract time portion: "2025-12-08T03:00:00Z" -> "03:00"
        val timePart = isoDateTime.substringAfter("T").substringBefore(":")
        val hour = timePart.toIntOrNull() ?: return isoDateTime
        val minutes = isoDateTime.substringAfter("T").substringAfter(":").substringBefore(":").toIntOrNull() ?: 0
        
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$hour12:${minutes.toString().padStart(2, '0')} $amPm"
    } catch (e: Exception) {
        isoDateTime
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

/**
 * Data class for parsed seat map UI data
 */
private data class SeatMapUiData(
    val pnr: String,
    val availableSeats: List<String>,
    val windowSeats: List<String>,
    val aisleSeats: List<String>
)

/**
 * Parse the uiData JSON string into SeatMapUiData
 */
private fun parseSeatMapData(uiData: String): SeatMapUiData? {
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val jsonObj = json.parseToJsonElement(uiData).jsonObject
        
        val pnr = jsonObj["pnr"]?.jsonPrimitive?.contentOrNull ?: ""
        val availableSeats = jsonObj["availableSeats"]?.jsonArray?.mapNotNull { 
            it.jsonPrimitive.contentOrNull 
        } ?: emptyList()
        val windowSeats = jsonObj["windowSeats"]?.jsonArray?.mapNotNull { 
            it.jsonPrimitive.contentOrNull 
        } ?: emptyList()
        val aisleSeats = jsonObj["aisleSeats"]?.jsonArray?.mapNotNull { 
            it.jsonPrimitive.contentOrNull 
        } ?: emptyList()
        
        SeatMapUiData(pnr, availableSeats, windowSeats, aisleSeats)
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun SeatMapCard(uiData: String?, isRtl: Boolean) {
    // Parse seat map data from uiData JSON
    val seatMapData = remember(uiData) {
        uiData?.let { parseSeatMapData(it) }
    }
    
    // Use parsed data or empty fallback
    val availableSeats = seatMapData?.availableSeats ?: emptyList()
    
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
            // Show PNR if available
            seatMapData?.pnr?.takeIf { it.isNotBlank() }?.let { pnr ->
                Text(
                    text = if (isRtl) "حجز: $pnr" else "Booking: $pnr",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
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
                    (10..13).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("A", "B", "C").forEach { col ->
                                val seatLabel = "$row$col"
                                SeatIcon(
                                    label = seatLabel,
                                    isAvailable = availableSeats.contains(seatLabel),
                                    isHighlighted = false // Could highlight selected seat
                                )
                            }
                        }
                    }
                }
                
                // Aisle
                Spacer(modifier = Modifier.width(24.dp))
                
                // Right side (D, E, F)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (10..13).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("D", "E", "F").forEach { col ->
                                val seatLabel = "$row$col"
                                SeatIcon(
                                    label = seatLabel,
                                    isAvailable = availableSeats.contains(seatLabel),
                                    isHighlighted = false
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

/**
 * Data class for parsed boarding pass UI data
 */
private data class BoardingPassUiData(
    val pnr: String,
    val passengerName: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val gate: String,
    val seat: String,
    val boardingGroup: String,
    val qrCode: String
)

/**
 * Parse the uiData JSON string into BoardingPassUiData
 */
private fun parseBoardingPassData(uiData: String): BoardingPassUiData? {
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val jsonObj = json.parseToJsonElement(uiData).jsonObject
        
        BoardingPassUiData(
            pnr = jsonObj["pnr"]?.jsonPrimitive?.contentOrNull ?: "",
            passengerName = jsonObj["passengerName"]?.jsonPrimitive?.contentOrNull ?: "",
            flightNumber = jsonObj["flightNumber"]?.jsonPrimitive?.contentOrNull ?: "",
            origin = jsonObj["origin"]?.jsonPrimitive?.contentOrNull ?: "",
            destination = jsonObj["destination"]?.jsonPrimitive?.contentOrNull ?: "",
            departureTime = jsonObj["departureTime"]?.jsonPrimitive?.contentOrNull ?: "",
            gate = jsonObj["gate"]?.jsonPrimitive?.contentOrNull ?: "",
            seat = jsonObj["seat"]?.jsonPrimitive?.contentOrNull ?: "",
            boardingGroup = jsonObj["boardingGroup"]?.jsonPrimitive?.contentOrNull ?: "",
            qrCode = jsonObj["qrCode"]?.jsonPrimitive?.contentOrNull ?: ""
        )
    } catch (e: Exception) {
        null
    }
}

/**
 * Get city name from airport code
 */
private fun getCityName(code: String, isRtl: Boolean): String {
    return when (code.uppercase()) {
        "RUH" -> if (isRtl) "الرياض" else "Riyadh"
        "JED" -> if (isRtl) "جدة" else "Jeddah"
        "DMM" -> if (isRtl) "الدمام" else "Dammam"
        "MED" -> if (isRtl) "المدينة المنورة" else "Medina"
        "ABH" -> if (isRtl) "أبها" else "Abha"
        "GIZ" -> if (isRtl) "جيزان" else "Jizan"
        "TUU" -> if (isRtl) "تبوك" else "Tabuk"
        "ELQ" -> if (isRtl) "القصيم" else "Qassim"
        "AHB" -> if (isRtl) "أبها" else "Abha"
        else -> code
    }
}

/**
 * Extract time from ISO datetime string
 */
private fun extractTime(isoDateTime: String): String {
    return try {
        // Format: 2024-12-08T14:30:00 -> 14:30
        if (isoDateTime.contains("T")) {
            isoDateTime.substringAfter("T").take(5)
        } else {
            isoDateTime.take(5)
        }
    } catch (e: Exception) {
        isoDateTime
    }
}

@Composable
private fun BoardingPassCard(uiData: String?, isRtl: Boolean) {
    // Parse boarding pass data from uiData JSON
    val boardingPass = remember(uiData) {
        uiData?.let { parseBoardingPassData(it) }
    }
    
    // Use parsed data or fallback values
    val origin = boardingPass?.origin ?: "RUH"
    val destination = boardingPass?.destination ?: "JED"
    val flightNumber = boardingPass?.flightNumber ?: "---"
    val gate = boardingPass?.gate ?: "--"
    val seat = boardingPass?.seat ?: "--"
    val departureTime = boardingPass?.departureTime?.let { extractTime(it) } ?: "--:--"
    val passengerName = boardingPass?.passengerName ?: ""
    
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
                Column {
                    Text(
                        text = if (isRtl) "بطاقة الصعود" else "BOARDING PASS",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    if (passengerName.isNotBlank()) {
                        Text(
                            text = passengerName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
            
            // Content
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = origin,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getCityName(origin, isRtl),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Text(
                        text = "->",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PilotPrimaryColor,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = destination,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = getCityName(destination, isRtl),
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
                        value = flightNumber
                    )
                    InfoColumn(
                        label = if (isRtl) "البوابة" else "GATE",
                        value = gate
                    )
                    InfoColumn(
                        label = if (isRtl) "المقعد" else "SEAT",
                        value = seat
                    )
                    InfoColumn(
                        label = if (isRtl) "الإقلاع" else "DEPART",
                        value = departureTime
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
                        text = boardingPass?.qrCode ?: "QR CODE",
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

/**
 * Data class for parsed flight comparison UI data
 */
private data class FlightComparisonUiData(
    val pnr: String,
    val currentFlight: String,
    val newFlight: String,
    val changeFee: Double,
    val priceDifference: Double,
    val totalDue: Double,
    val currency: String
)

/**
 * Parse the uiData JSON string into FlightComparisonUiData
 */
private fun parseFlightComparisonData(uiData: String): FlightComparisonUiData? {
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val jsonObj = json.parseToJsonElement(uiData).jsonObject
        
        FlightComparisonUiData(
            pnr = jsonObj["pnr"]?.jsonPrimitive?.contentOrNull ?: "",
            currentFlight = jsonObj["currentFlight"]?.jsonPrimitive?.contentOrNull ?: "",
            newFlight = jsonObj["newFlight"]?.jsonPrimitive?.contentOrNull ?: "",
            changeFee = jsonObj["changeFee"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            priceDifference = jsonObj["priceDifference"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            totalDue = jsonObj["totalDue"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            currency = jsonObj["currency"]?.jsonPrimitive?.contentOrNull ?: "SAR"
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ComparisonCard(uiData: String?, isRtl: Boolean) {
    // Parse flight comparison data from uiData JSON
    val comparison = remember(uiData) {
        uiData?.let { parseFlightComparisonData(it) }
    }
    
    // Use parsed data or fallback values
    val currentFlight = comparison?.currentFlight ?: "---"
    val newFlight = comparison?.newFlight ?: "---"
    val totalDue = comparison?.totalDue ?: 0.0
    val currency = comparison?.currency ?: "SAR"
    
    // Format the total due with sign
    val totalDueText = if (totalDue >= 0) {
        "+$currency ${totalDue.toInt()}"
    } else {
        "$currency ${totalDue.toInt()}"
    }
    
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
                    flightNumber = currentFlight,
                    time = "", // Time not provided in backend response
                    isOld = true
                )
                
                // Arrow
                Text(
                    text = "->",
                    fontSize = 24.sp,
                    color = PilotPrimaryColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                
                // New flight
                ComparisonColumn(
                    modifier = Modifier.weight(1f),
                    title = if (isRtl) "الجديد" else "New",
                    flightNumber = newFlight,
                    time = "",
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
                        text = if (isRtl) "المبلغ المستحق" else "Total Due",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = totalDueText,
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
            if (time.isNotBlank()) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOld) MaterialTheme.colorScheme.onSurfaceVariant else PilotPrimaryColor
                )
            }
        }
    }
}

/**
 * Data class for parsed booking summary UI data
 */
private data class BookingSummaryUiData(
    val pnr: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val passengers: List<PassengerUiData>,
    val totalPaid: Double,
    val currency: String
)

private data class PassengerUiData(
    val name: String,
    val type: String
)

/**
 * Parse the uiData JSON string into BookingSummaryUiData
 */
private fun parseBookingSummaryData(uiData: String): BookingSummaryUiData? {
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val jsonObj = json.parseToJsonElement(uiData).jsonObject
        
        val passengers = jsonObj["passengers"]?.jsonArray?.mapNotNull { passengerElement ->
            val passenger = passengerElement.jsonObject
            PassengerUiData(
                name = passenger["name"]?.jsonPrimitive?.contentOrNull ?: "",
                type = passenger["type"]?.jsonPrimitive?.contentOrNull ?: "ADULT"
            )
        } ?: emptyList()
        
        BookingSummaryUiData(
            pnr = jsonObj["pnr"]?.jsonPrimitive?.contentOrNull ?: "",
            flightNumber = jsonObj["flightNumber"]?.jsonPrimitive?.contentOrNull ?: "",
            origin = jsonObj["origin"]?.jsonPrimitive?.contentOrNull ?: "",
            destination = jsonObj["destination"]?.jsonPrimitive?.contentOrNull ?: "",
            departureTime = jsonObj["departureTime"]?.jsonPrimitive?.contentOrNull ?: "",
            passengers = passengers,
            totalPaid = jsonObj["totalPaid"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            currency = jsonObj["currency"]?.jsonPrimitive?.contentOrNull ?: "SAR"
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun BookingSummaryCard(uiData: String?, isRtl: Boolean) {
    // Parse booking summary data from uiData JSON
    val booking = remember(uiData) {
        uiData?.let { parseBookingSummaryData(it) }
    }
    
    // Use parsed data or fallback values
    val pnr = booking?.pnr ?: "---"
    val flightNumber = booking?.flightNumber ?: "---"
    val origin = booking?.origin ?: "---"
    val destination = booking?.destination ?: "---"
    val departureTime = booking?.departureTime?.let { extractTime(it) } ?: "--:--"
    val passengers = booking?.passengers ?: emptyList()
    val totalPaid = booking?.totalPaid ?: 0.0
    val currency = booking?.currency ?: "SAR"
    
    // Format passenger count
    val passengerText = if (passengers.isNotEmpty()) {
        val adultCount = passengers.count { it.type.uppercase() == "ADULT" }
        val childCount = passengers.count { it.type.uppercase() == "CHILD" }
        val infantCount = passengers.count { it.type.uppercase() == "INFANT" }
        buildString {
            if (adultCount > 0) append(if (isRtl) "$adultCount بالغ" else "$adultCount Adult${if (adultCount > 1) "s" else ""}")
            if (childCount > 0) {
                if (isNotEmpty()) append(", ")
                append(if (isRtl) "$childCount طفل" else "$childCount Child${if (childCount > 1) "ren" else ""}")
            }
            if (infantCount > 0) {
                if (isNotEmpty()) append(", ")
                append(if (isRtl) "$infantCount رضيع" else "$infantCount Infant${if (infantCount > 1) "s" else ""}")
            }
        }
    } else {
        if (isRtl) "---" else "---"
    }
    
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
                        text = pnr,
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
                value = "$flightNumber • $departureTime"
            )
            SummaryRow(
                label = if (isRtl) "المسار" else "Route",
                value = "$origin - $destination"
            )
            SummaryRow(
                label = if (isRtl) "المسافرون" else "Passengers",
                value = passengerText
            )
            
            // Show passenger names if available
            passengers.takeIf { it.isNotEmpty() }?.forEach { passenger ->
                Text(
                    text = "  • ${passenger.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
            
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
                    text = "$currency ${totalPaid.toInt()}",
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
    isSpeaking: Boolean,
    isLoading: Boolean,
    isRtl: Boolean,
    voiceError: String? = null
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
    
    // Pulsing animation for speaking indicator
    val speakingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_alpha"
    )

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Voice error message
            AnimatedVisibility(
                visible = voiceError != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    text = voiceError ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
            
            // Speaking indicator
            AnimatedVisibility(
                visible = isSpeaking,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ">>>",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PilotPrimaryColor,
                        modifier = Modifier.graphicsLayer { alpha = speakingAlpha }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRtl) "فارس يتحدث..." else "Faris speaking...",
                        style = MaterialTheme.typography.labelSmall,
                        color = PilotPrimaryColor.copy(alpha = speakingAlpha)
                    )
                }
            }
            
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
                                text = if (isListening) {
                                    if (isRtl) "استمع..." else "Listening..."
                                } else {
                                    if (isRtl) "اكتب أو تكلم..." else "Type or speak..."
                                },
                                color = if (isListening) PilotPrimaryColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PilotPrimaryColor,
                            unfocusedBorderColor = if (isListening) PilotPrimaryColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                        enabled = !isLoading && !isListening
                    )

                    // Mic button (prominent) - now shows stop icon when listening
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .scale(micScale)
                            .clickable(enabled = !isLoading) { onMicClick() },
                        shape = CircleShape,
                        color = if (isListening) Color(0xFFEF4444) else PilotPrimaryColor, // Red when listening
                        shadowElevation = if (isListening) 8.dp else 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Use text symbols for reliable rendering
                            Text(
                                text = if (isListening) "II" else "O",
                                fontSize = if (isListening) 16.sp else 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
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
}
