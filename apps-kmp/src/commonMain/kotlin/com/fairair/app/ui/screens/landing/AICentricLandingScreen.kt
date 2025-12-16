package com.fairair.app.ui.screens.landing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.fairair.app.localization.LocalStrings
import com.fairair.app.ui.chat.ChatMessage
import com.fairair.app.ui.chat.ChatUiState
import com.fairair.app.ui.theme.NotoKufiArabicFontFamily
import com.fairair.app.ui.theme.SpaceGroteskFontFamily
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.apps_kmp.generated.resources.*
import com.fairair.contract.dto.ChatUiType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.path

// Max width for content on desktop
private val MaxContentWidth = 1200.dp
private val MaxCardWidth = 900.dp

// Aurora theme colors
private val AuroraDeepSpace = Color(0xFF020617)
private val AuroraMidnight = Color(0xFF0F172A)
private val AuroraCyan = Color(0xFF22D3EE)
private val AuroraBlue = Color(0xFF3B82F6)
private val AuroraPurple = Color(0xFF8B5CF6)
private val AuroraGreen = Color(0xFF22C55E)
private val GlassWhite = Color(0x1AFFFFFF)
private val GlassDark = Color(0x66000000)  // 40% black for input fields
private val GlassBorder = Color(0x33FFFFFF)

// =============================================================================
// CUSTOM MICROPHONE ICON
// =============================================================================

/**
 * Custom Microphone icon vector (Material Design mic icon)
 */
private val MicIcon: ImageVector
    get() = ImageVector.Builder(
        name = "Mic",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1f,
            pathFillType = PathFillType.NonZero
        ) {
            // Microphone body
            moveTo(12f, 14f)
            curveTo(13.66f, 14f, 15f, 12.66f, 15f, 11f)
            lineTo(15f, 5f)
            curveTo(15f, 3.34f, 13.66f, 2f, 12f, 2f)
            curveTo(10.34f, 2f, 9f, 3.34f, 9f, 5f)
            lineTo(9f, 11f)
            curveTo(9f, 12.66f, 10.34f, 14f, 12f, 14f)
            close()
            // Microphone stand/arc
            moveTo(17f, 11f)
            curveTo(17f, 13.76f, 14.76f, 16f, 12f, 16f)
            curveTo(9.24f, 16f, 7f, 13.76f, 7f, 11f)
            lineTo(5f, 11f)
            curveTo(5f, 14.53f, 7.61f, 17.44f, 11f, 17.93f)
            lineTo(11f, 21f)
            lineTo(13f, 21f)
            lineTo(13f, 17.93f)
            curveTo(16.39f, 17.44f, 19f, 14.53f, 19f, 11f)
            lineTo(17f, 11f)
            close()
        }
    }.build()

// =============================================================================
// CITY THEMES - Dynamic background images based on city/destination
// =============================================================================

/**
 * Represents the theme for a city, including background image and accent colors.
 */
data class CityTheme(
    val backgroundImage: DrawableResource?,
    val accentColor: Color,
    val cityName: String
)

/**
 * Get city theme based on airport code. Each city has a unique background image.
 */
private fun getCityTheme(airportCode: String?): CityTheme {
    return when (airportCode?.uppercase()) {
        // Saudi Arabia cities
        "JED" -> CityTheme(
            backgroundImage = Res.drawable.bg_jed,
            accentColor = Color(0xFF60A5FA),
            cityName = "Jeddah"
        )
        "RUH" -> CityTheme(
            backgroundImage = Res.drawable.bg_ruh,
            accentColor = Color(0xFFFBBF24),
            cityName = "Riyadh"
        )
        "DMM" -> CityTheme(
            backgroundImage = Res.drawable.bg_dmm,
            accentColor = Color(0xFF2DD4BF),
            cityName = "Dammam"
        )
        
        // UAE
        "DXB" -> CityTheme(
            backgroundImage = Res.drawable.bg_dxb,
            accentColor = Color(0xFFFBBF24),
            cityName = "Dubai"
        )
        
        // Egypt
        "CAI" -> CityTheme(
            backgroundImage = Res.drawable.bg_cai,
            accentColor = Color(0xFFFCD34D),
            cityName = "Cairo"
        )
        
        // Default - no background image (use gradient)
        else -> CityTheme(
            backgroundImage = null,
            accentColor = AuroraCyan,
            cityName = ""
        )
    }
}

/**
 * AI-Centric Landing Screen - The primary interface is the AI chat input.
 * 
 * Features:
 * - Large, prominent chat input field with animated hints
 * - Dynamic content area that updates based on AI conversation
 * - Conversation history with ability to go back to previous steps
 * - Quick services grid below the dynamic area
 * - Optional "Book Manually" link for traditional flow
 */
@Composable
fun AICentricLandingScreen(
    chatState: ChatUiState,
    onSendMessage: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSuggestionTapped: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onClearChat: () -> Unit,
    onManualBookClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit = {},
    onMyBookingsClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onCheckInClick: () -> Unit = {},
    onManageBookingClick: () -> Unit = {},
    onMembershipClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onFlightSelected: (String) -> Unit = {},
    onSeatSelected: (String) -> Unit = {},
    userName: String? = null,
    isRtl: Boolean = false,
    locale: String = "en-US",
    userLocationCode: String? = null, // User's detected location airport code (e.g., "JED", "RUH")
    destinationCode: String? = null   // Current destination being discussed (e.g., "DXB")
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    // Get city theme based on location/destination
    val cityTheme = remember(userLocationCode, destinationCode) {
        // Priority: destination > user location > default
        val activeCity = destinationCode ?: userLocationCode
        println("AICentricLandingScreen: Computing cityTheme - userLocationCode=$userLocationCode, destinationCode=$destinationCode, activeCity=$activeCity")
        val theme = getCityTheme(activeCity)
        println("AICentricLandingScreen: Selected theme for ${theme.cityName.ifEmpty { "default" }} - hasImage=${theme.backgroundImage != null}")
        theme
    }
    
    // Scroll to latest message when new messages arrive
    LaunchedEffect(chatState.messages.size) {
        if (chatState.messages.isNotEmpty()) {
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }
    
    // Provide layout direction
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background - either city image or default gradient
            if (cityTheme.backgroundImage != null) {
                // City background image with frosted glass effect
                Box(modifier = Modifier.fillMaxSize()) {
                    // Base image - slightly blurred for frosted effect
                    Image(
                        painter = painterResource(cityTheme.backgroundImage),
                        contentDescription = "Background for ${cityTheme.cityName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(radius = 8.dp)
                    )
                    
                    // Frosted glass overlay - semi-transparent with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xCC0F172A), // Darker at top (80% opacity)
                                        Color(0x990F172A), // Slightly lighter middle (60% opacity)
                                        Color(0xB30F172A)  // Medium at bottom (70% opacity)
                                    ),
                                    startY = 0f,
                                    endY = Float.POSITIVE_INFINITY
                                )
                            )
                    )
                    
                    // Subtle noise/texture effect for glass feel (via very faint pattern)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.02f))
                    )
                }
            } else {
                // Default gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    VelocityColors.GradientStart,
                                    VelocityColors.GradientEnd
                                )
                            )
                        )
                )
                // Aurora animated background only for default theme
                AuroraBackgroundEffect(modifier = Modifier.fillMaxSize())
            }
            
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                AICentricHeader(
                    onLoginClick = onLoginClick,
                    onLogoutClick = onLogoutClick,
                    onMyBookingsClick = onMyBookingsClick,
                    onSettingsClick = onSettingsClick,
                    userName = userName,
                    isRtl = isRtl
                )
                
                // Main content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = MaxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    if (chatState.messages.isEmpty()) {
                        // Initial state - show hero with AI input
                        AIHeroSection(
                            inputText = chatState.inputText,
                            onInputChange = onInputChange,
                            onSendMessage = onSendMessage,
                            onVoiceClick = onVoiceClick,
                            isListening = chatState.isListening,
                            isLoading = chatState.isLoading,
                            interimText = chatState.interimText,
                            onManualBookClick = onManualBookClick,
                            isRtl = isRtl,
                            userLocationCode = userLocationCode
                        )
                    } else {
                        // Conversation mode - show chat with dynamic content
                        ConversationView(
                            messages = chatState.messages,
                            inputText = chatState.inputText,
                            onInputChange = onInputChange,
                            onSendMessage = onSendMessage,
                            onVoiceClick = onVoiceClick,
                            onSuggestionTapped = onSuggestionTapped,
                            onClearChat = onClearChat,
                            onFlightSelected = onFlightSelected,
                            onSeatSelected = onSeatSelected,
                            onLoginClick = onLoginClick,
                            isListening = chatState.isListening,
                            isLoading = chatState.isLoading,
                            interimText = chatState.interimText,
                            listState = listState,
                            isRtl = isRtl
                        )
                    }
                }
                
                // Quick Services Grid (collapsed when in conversation)
                AnimatedVisibility(
                    visible = chatState.messages.isEmpty(),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    QuickServicesGrid(
                        onCheckInClick = onCheckInClick,
                        onManageBookingClick = onManageBookingClick,
                        onMembershipClick = onMembershipClick,
                        onHelpClick = onHelpClick,
                        isRtl = isRtl
                    )
                }
            }
        }
    }
}

// =============================================================================
// AURORA BACKGROUND EFFECT
// =============================================================================

@Composable
private fun AuroraBackgroundEffect(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_time"
    )
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(80.dp)
            .graphicsLayer { alpha = 0.3f }
    ) {
        val w = size.width
        val h = size.height
        
        // Draw flowing aurora waves
        for (i in 0..2) {
            val phase = time + i * (PI.toFloat() / 3f)
            val yOffset = h * (0.3f + i * 0.15f) + sin(phase) * h * 0.1f
            
            val colors = when (i) {
                0 -> listOf(AuroraCyan.copy(alpha = 0.4f), Color.Transparent)
                1 -> listOf(AuroraBlue.copy(alpha = 0.3f), Color.Transparent)
                else -> listOf(AuroraPurple.copy(alpha = 0.25f), Color.Transparent)
            }
            
            drawCircle(
                brush = Brush.radialGradient(colors),
                radius = w * 0.6f,
                center = androidx.compose.ui.geometry.Offset(
                    x = w * (0.3f + i * 0.2f) + sin(phase * 0.7f) * w * 0.2f,
                    y = yOffset
                )
            )
        }
    }
}

// =============================================================================
// HEADER
// =============================================================================

@Composable
private fun AICentricHeader(
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onMyBookingsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    userName: String?,
    isRtl: Boolean
) {
    val strings = LocalStrings.current
    val isLoggedIn = userName != null
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "FairAir",
                    tint = AuroraCyan,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Nav buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settings,
                        tint = VelocityColors.TextMuted
                    )
                }
                
                if (isLoggedIn) {
                    Text(
                        text = "${strings.landingWelcome}, $userName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelocityColors.TextMuted
                    )
                    
                    Button(
                        onClick = onMyBookingsClick,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuroraCyan.copy(alpha = 0.1f),
                            contentColor = AuroraCyan
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.landingMyBookings)
                    }
                    
                    Button(
                        onClick = onLogoutClick,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = VelocityColors.TextMain
                        ),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(strings.landingSignOut)
                    }
                } else {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = VelocityColors.TextMain
                        ),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(strings.landingSignIn)
                    }
                }
            }
        }
    }
}

// =============================================================================
// AI HERO SECTION - Initial State with Prominent Input
// =============================================================================

@Composable
private fun AIHeroSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean,
    isLoading: Boolean,
    interimText: String,
    onManualBookClick: () -> Unit,
    isRtl: Boolean,
    userLocationCode: String? = null
) {
    val strings = LocalStrings.current
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    // Animated placeholder hints
    val hints = if (isRtl) {
        listOf(
            "أرخص رحلة إلى الرياض غداً",
            "أقرب وجهة بطقس جميل",
            "رحلة طيران إلى دبي الأسبوع القادم",
            "احجز لي رحلة إلى جدة"
        )
    } else {
        listOf(
            "Cheapest flight to Riyadh tomorrow",
            "The closest destination with nice weather",
            "Show me flights to Dubai next week",
            "Book a flight to Jeddah for 2 passengers"
        )
    }
    
    var currentHintIndex by remember { mutableStateOf(0) }
    var showHint by remember { mutableStateOf(true) }
    
    // Cycle through hints
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            showHint = false
            delay(300)
            currentHintIndex = (currentHintIndex + 1) % hints.size
            showHint = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AI Orb animation
        PulsatingOrb(isActive = isListening || isLoading)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = if (isRtl) "كيف يمكنني مساعدتك اليوم؟" else "How can I help you today?",
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isRtl) "اسألني أي شيء عن رحلتك" else "Ask me anything about your travel",
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Voice feedback indicator - shows what was heard
        val isListeningState = interimText == "Listening..." || interimText == "جاري الاستماع..."
        val isProcessingState = interimText == "Processing..." || interimText == "جاري المعالجة..."
        val isStatusMessage = isListeningState || isProcessingState || 
            interimText == "Didn't catch that..." || interimText == "Something went wrong. Try again."
        
        AnimatedVisibility(
            visible = interimText.isNotBlank(),
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 }
        ) {
            VoiceFeedbackCard(
                interimText = interimText,
                isListening = isListeningState,
                isProcessing = isProcessingState,
                isStatusMessage = isStatusMessage,
                isRtl = isRtl,
                fontFamily = fontFamily
            )
        }
        
        // Main AI Input Field
        GlassInputField(
            value = inputText,
            onValueChange = onInputChange,
            onSend = { if (inputText.isNotBlank()) onSendMessage(inputText) },
            onVoiceClick = onVoiceClick,
            isListening = isListening,
            isLoading = isLoading,
            interimText = interimText,
            placeholder = {
                AnimatedContent(
                    targetState = if (showHint) hints[currentHintIndex] else "",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith 
                        fadeOut(animationSpec = tween(300))
                    },
                    label = "hint"
                ) { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontFamily),
                        color = VelocityColors.TextMuted.copy(alpha = 0.7f)
                    )
                }
            },
            isRtl = isRtl,
            modifier = Modifier
                .widthIn(max = MaxCardWidth)
                .fillMaxWidth()
        )
        
        // Location indicator - shows detected origin
        if (userLocationCode != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = AuroraCyan.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isRtl) "الانطلاق من $userLocationCode" else "Flying from $userLocationCode",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMuted.copy(alpha = 0.7f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Manual booking link
        TextButton(
            onClick = onManualBookClick,
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
        ) {
            Text(
                text = if (isRtl) "أو احجز بالطريقة التقليدية" else "Or book the traditional way",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                color = AuroraCyan
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AuroraCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

// =============================================================================
// PULSATING ORB - Visual AI Indicator
// =============================================================================

@Composable
private fun PulsatingOrb(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 800 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isActive) 0.7f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 800 else 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val coreColor = if (isActive) AuroraCyan else AuroraBlue
    val glowColor = if (isActive) AuroraCyan else AuroraPurple
    
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow layers
        repeat(3) { index ->
            val layerScale = pulse + (index * 0.2f)
            val layerAlpha = (glowAlpha - index * 0.15f).coerceIn(0.05f, 0.5f)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(layerScale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = layerAlpha),
                                glowColor.copy(alpha = 0f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        // Core orb
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(pulse)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(coreColor, glowColor)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner bright core for depth effect
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            )
        }
    }
}

// =============================================================================
// VOICE FEEDBACK CARD - Shows transcription status
// =============================================================================

@Composable
private fun VoiceFeedbackCard(
    interimText: String,
    isListening: Boolean,
    isProcessing: Boolean,
    isStatusMessage: Boolean,
    isRtl: Boolean,
    fontFamily: androidx.compose.ui.text.font.FontFamily
) {
    val backgroundColor = if (isStatusMessage) AuroraCyan.copy(alpha = 0.15f) else AuroraGreen.copy(alpha = 0.15f)
    val borderColor = if (isStatusMessage) AuroraCyan.copy(alpha = 0.3f) else AuroraGreen.copy(alpha = 0.3f)
    
    Surface(
        modifier = Modifier
            .widthIn(max = MaxCardWidth)
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            if (isListening) {
                // Pulsating mic for listening
                val infiniteTransition = rememberInfiniteTransition(label = "listening_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Icon(
                    imageVector = MicIcon,
                    contentDescription = null,
                    tint = MicRecordingRed.copy(alpha = alpha),
                    modifier = Modifier.size(20.dp)
                )
            } else if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AuroraCyan,
                    strokeWidth = 2.dp
                )
            } else if (!isStatusMessage) {
                // Show checkmark for recognized text
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AuroraGreen,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // Error/info state
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = AuroraCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                if (!isStatusMessage) {
                    Text(
                        text = if (isRtl) "سمعت:" else "I heard:",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily),
                        color = VelocityColors.TextMuted
                    )
                }
                Text(
                    text = interimText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMain,
                    maxLines = 2
                )
            }
        }
    }
}

// =============================================================================
// GLASS INPUT FIELD - Glassmorphic Chat Input
// =============================================================================

// Microphone recording color - vibrant red
private val MicRecordingRed = Color(0xFFEF4444)

@Composable
private fun GlassInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit,
    isListening: Boolean,
    isLoading: Boolean,
    interimText: String = "",
    placeholder: @Composable () -> Unit,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    val focusRequester = remember { FocusRequester() }
    
    // Pulsating animation for microphone when listening
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )
    val micGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_glow"
    )
    
    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = GlassDark,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice/Microphone button with pulsating animation when listening
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onVoiceClick)
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center
            ) {
                // Pulsating glow background when listening
                if (isListening) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(micScale)
                            .background(
                                color = MicRecordingRed.copy(alpha = micGlow * 0.4f),
                                shape = CircleShape
                            )
                    )
                }
                // Inner circle background
                Box(
                    modifier = Modifier
                        .size(if (isListening) 40.dp else 48.dp)
                        .scale(if (isListening) micScale else 1f)
                        .background(
                            color = if (isListening) MicRecordingRed.copy(alpha = 0.2f)
                            else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MicIcon,
                        contentDescription = if (isListening) "Stop listening" else "Start voice input",
                        tint = if (isListening) MicRecordingRed else VelocityColors.TextMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(if (isListening) micScale else 1f)
                    )
                }
            }
            
            // Text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentAlignment = if (isRtl) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    placeholder()
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = VelocityColors.TextMain,
                        fontSize = 16.sp,
                        fontFamily = fontFamily,
                        textAlign = if (isRtl) TextAlign.End else TextAlign.Start
                    ),
                    cursorBrush = SolidColor(AuroraCyan),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
            }
            
            // Send button
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (value.isNotBlank()) AuroraCyan
                        else AuroraCyan.copy(alpha = 0.3f)
                    )
                    .pointerHoverIcon(PointerIcon.Hand)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
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

// =============================================================================
// CONVERSATION VIEW - Chat with Dynamic Content
// =============================================================================

@Composable
private fun ConversationView(
    messages: List<ChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSuggestionTapped: (String) -> Unit,
    onClearChat: () -> Unit,
    onFlightSelected: (String) -> Unit,
    onSeatSelected: (String) -> Unit,
    onLoginClick: () -> Unit,
    isListening: Boolean,
    isLoading: Boolean,
    interimText: String = "",
    listState: androidx.compose.foundation.lazy.LazyListState,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Conversation header with clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRtl) "المحادثة" else "Conversation",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMuted
            )
            
            TextButton(
                onClick = onClearChat,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = VelocityColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isRtl) "بداية جديدة" else "Start over",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMuted
                )
            }
        }
        
        // Messages list with dynamic content
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ConversationItem(
                    message = message,
                    onFlightSelected = onFlightSelected,
                    onSeatSelected = onSeatSelected,
                    onSuggestionTapped = onSuggestionTapped,
                    onLoginClick = onLoginClick,
                    isRtl = isRtl
                )
            }
        }
        
        // Quick suggestions from last assistant message
        val lastAssistantMessage = messages.lastOrNull { !it.isFromUser }
        if (lastAssistantMessage?.suggestions?.isNotEmpty() == true) {
            QuickSuggestionsRow(
                suggestions = lastAssistantMessage.suggestions,
                onSuggestionTapped = onSuggestionTapped,
                isRtl = isRtl
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Input field
        GlassInputField(
            value = inputText,
            onValueChange = onInputChange,
            onSend = { if (inputText.isNotBlank()) onSendMessage(inputText) },
            onVoiceClick = onVoiceClick,
            isListening = isListening,
            isLoading = isLoading,
            interimText = interimText,
            placeholder = {
                Text(
                    text = if (isRtl) "اكتب رسالتك..." else "Type your message...",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMuted.copy(alpha = 0.7f)
                )
            },
            isRtl = isRtl,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =============================================================================
// CONVERSATION ITEM - Message with Dynamic Content Card
// =============================================================================

@Composable
private fun ConversationItem(
    message: ChatMessage,
    onFlightSelected: (String) -> Unit,
    onSeatSelected: (String) -> Unit,
    onSuggestionTapped: (String) -> Unit,
    onLoginClick: () -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        // Message bubble
        if (message.isLoading) {
            LoadingIndicator()
        } else if (message.text.isNotBlank() && message.uiType == null) {
            MessageBubble(
                text = message.text,
                isFromUser = message.isFromUser,
                isError = message.isError,
                isRtl = isRtl
            )
        } else if (message.uiType != null) {
            // Show assistant text as a proper message bubble, then the dynamic card
            if (message.text.isNotBlank()) {
                MessageBubble(
                    text = message.text,
                    isFromUser = false,
                    isError = message.isError,
                    isRtl = isRtl
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Dynamic content card
            DynamicContentCard(
                uiType = message.uiType,
                uiData = message.uiData,
                onFlightSelected = onFlightSelected,
                onSeatSelected = onSeatSelected,
                onActionClick = { action -> onSuggestionTapped(action) },
                onLoginClick = onLoginClick,
                isRtl = isRtl
            )
        }
    }
}

@Composable
private fun MessageBubble(
    text: String,
    isFromUser: Boolean,
    isError: Boolean,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    val backgroundColor = when {
        isError -> Color(0x33FF5252)
        isFromUser -> AuroraCyan
        else -> GlassWhite
    }
    
    val textColor = when {
        isError -> Color(0xFFFF5252)
        isFromUser -> Color.White
        else -> VelocityColors.TextMain
    }
    
    Surface(
        modifier = Modifier.widthIn(max = 320.dp),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (isFromUser) 16.dp else 4.dp,
            bottomEnd = if (isFromUser) 4.dp else 16.dp
        ),
        color = backgroundColor,
        border = if (!isFromUser) BorderStroke(1.dp, GlassBorder) else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
            color = textColor
        )
    }
}

@Composable
private fun LoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        .background(AuroraCyan.copy(alpha = alpha), CircleShape)
                )
            }
        }
    }
}

// =============================================================================
// DYNAMIC CONTENT CARD - Renders based on ChatUiType
// =============================================================================

@Composable
private fun DynamicContentCard(
    uiType: ChatUiType,
    uiData: String?,
    onFlightSelected: (String) -> Unit,
    onSeatSelected: (String) -> Unit,
    onActionClick: (String) -> Unit,
    onLoginClick: () -> Unit,
    isRtl: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        when (uiType) {
            ChatUiType.FLIGHT_LIST -> FlightListCard(
                uiData = uiData,
                onFlightSelected = onFlightSelected,
                isRtl = isRtl
            )
            ChatUiType.SEAT_MAP -> SeatMapCard(
                uiData = uiData,
                onSeatSelected = onSeatSelected,
                isRtl = isRtl
            )
            ChatUiType.BOARDING_PASS -> BoardingPassCard(
                uiData = uiData,
                isRtl = isRtl
            )
            ChatUiType.BOOKING_SUMMARY -> BookingSummaryCard(
                uiData = uiData,
                isRtl = isRtl
            )
            ChatUiType.FLIGHT_SELECTED -> FlightSelectedCard(
                uiData = uiData,
                isRtl = isRtl
            )
            ChatUiType.BOOKING_CONFIRMED -> BookingConfirmedCard(
                uiData = uiData,
                isRtl = isRtl
            )
            ChatUiType.DESTINATION_SUGGESTIONS -> DestinationSuggestionsCard(
                uiData = uiData,
                onDestinationClick = { dest -> onActionClick("Flights to $dest") },
                isRtl = isRtl
            )
            ChatUiType.PASSENGER_FORM -> PassengerFormCard(
                uiData = uiData,
                onSubmit = { onActionClick("Confirm booking") },
                isRtl = isRtl
            )
            ChatUiType.CLARIFICATION -> ClarificationCard(
                uiData = uiData,
                onOptionClick = onActionClick,
                isRtl = isRtl
            )
            ChatUiType.SIGN_IN_REQUIRED -> SignInRequiredCard(
                onLoginClick = onLoginClick,
                isRtl = isRtl
            )
            ChatUiType.PASSENGER_SELECT -> PassengerSelectCard(
                uiData = uiData,
                onPassengerSelected = { selection -> onActionClick(selection) },
                isRtl = isRtl
            )
            else -> {
                // Fallback for unhandled types
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Content: ${uiType.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = VelocityColors.TextMuted
                    )
                }
            }
        }
    }
}

// =============================================================================
// FLIGHT LIST CARD - Horizontal scrollable flight options
// =============================================================================

@Composable
private fun FlightListCard(
    uiData: String?,
    onFlightSelected: (String) -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    // Parse flight data from JSON
    val flights = remember(uiData) { parseFlightsFromJson(uiData) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isRtl) "الرحلات المتاحة" else "Available Flights",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (flights.isEmpty()) {
            Text(
                text = if (isRtl) "لا توجد رحلات متاحة" else "No flights available",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMuted
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(flights) { flight ->
                    FlightOptionCard(
                        flight = flight,
                        onClick = { onFlightSelected(flight.flightNumber) },
                        isRtl = isRtl
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightOptionCard(
    flight: ParsedFlight,
    onClick: () -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    // Format the departure time for display
    val formattedTime = formatTime(flight.departureTime)
    val formattedDate = formatDate(flight.departureTime)
    
    Surface(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = RoundedCornerShape(16.dp),
        color = AuroraMidnight,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Flight number
            Text(
                text = flight.flightNumber,
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = fontFamily),
                color = AuroraCyan,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMuted
            )
            
            // Time
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Duration if available
            flight.duration?.let { duration ->
                Text(
                    text = duration,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMuted
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Price
            Text(
                text = flight.price,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                color = AuroraCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// =============================================================================
// ADDITIONAL CARD TYPES
// =============================================================================

@Composable
private fun SeatMapCard(uiData: String?, onSeatSelected: (String) -> Unit, isRtl: Boolean) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isRtl) "اختر مقعدك" else "Choose Your Seat",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Simplified seat map - can be expanded with full seat selection
        Text(
            text = if (isRtl) "خريطة المقاعد قادمة قريباً" else "Seat map visualization coming soon",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMuted
        )
    }
}

@Composable
private fun BoardingPassCard(uiData: String?, isRtl: Boolean) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = AuroraCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRtl) "بطاقة الصعود" else "Boarding Pass",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
        }
        // Parse and display boarding pass details from uiData
    }
}

@Composable
private fun BookingSummaryCard(uiData: String?, isRtl: Boolean) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = AuroraCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRtl) "ملخص الحجز" else "Booking Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FlightSelectedCard(uiData: String?, isRtl: Boolean) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRtl) "تم اختيار الرحلة" else "Flight Selected",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BookingConfirmedCard(uiData: String?, isRtl: Boolean) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRtl) "تم تأكيد الحجز!" else "Booking Confirmed!",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SignInRequiredCard(
    onLoginClick: () -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = AuroraCyan,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRtl) "تسجيل الدخول مطلوب" else "Sign In Required",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isRtl) 
                "سجّل دخولك لإتمام الحجز وحفظ معلوماتك" 
                else "Sign in to complete your booking and save your details",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pointerHoverIcon(PointerIcon.Hand),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuroraCyan,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRtl) "تسجيل الدخول" else "Sign In",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = fontFamily),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PassengerSelectCard(
    uiData: String?,
    onPassengerSelected: (String) -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    // Parse travelers from JSON
    val payload = remember(uiData) { parsePassengerSelectFromJson(uiData) }
    
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = if (isRtl) "من سيسافر؟" else "Who will be traveling?",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (payload.travelers.isEmpty()) {
            Text(
                text = if (isRtl) "لم يتم العثور على مسافرين محفوظين" else "No saved travelers found",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMuted
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                payload.travelers.forEach { traveler ->
                    TravelerOptionRow(
                        name = traveler.name,
                        isMainTraveler = traveler.isMainTraveler,
                        onClick = { 
                            onPassengerSelected("Book for ${traveler.name}")
                        },
                        isRtl = isRtl
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onPassengerSelected("Book for myself") },
                modifier = Modifier
                    .weight(1f)
                    .pointerHoverIcon(PointerIcon.Hand),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AuroraCyan)
            ) {
                Text(
                    text = if (isRtl) "لي فقط" else "Just me",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = fontFamily),
                    color = AuroraCyan
                )
            }
            OutlinedButton(
                onClick = { onPassengerSelected("Book for all travelers") },
                modifier = Modifier
                    .weight(1f)
                    .pointerHoverIcon(PointerIcon.Hand),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AuroraCyan)
            ) {
                Text(
                    text = if (isRtl) "الجميع" else "All",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = fontFamily),
                    color = AuroraCyan
                )
            }
        }
    }
}

@Composable
private fun TravelerOptionRow(
    name: String,
    isMainTraveler: Boolean,
    onClick: () -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand),
        shape = RoundedCornerShape(12.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = AuroraCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Medium
                )
                if (isMainTraveler) {
                    Text(
                        text = if (isRtl) "أنت" else "You",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                        color = AuroraCyan
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                contentDescription = null,
                tint = VelocityColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Data class for passenger select payload
@kotlinx.serialization.Serializable
private data class PassengerSelectPayload(
    val travelers: List<TravelerOption> = emptyList(),
    val flightNumber: String = ""
)

@kotlinx.serialization.Serializable
private data class TravelerOption(
    val id: String = "",
    val name: String = "",
    val isMainTraveler: Boolean = false
)

private fun parsePassengerSelectFromJson(json: String?): PassengerSelectPayload {
    if (json.isNullOrBlank()) return PassengerSelectPayload()
    return try {
        val parsed = kotlinx.serialization.json.Json.decodeFromString<PassengerSelectPayload>(json)
        parsed
    } catch (e: Exception) {
        PassengerSelectPayload()
    }
}

@Composable
private fun DestinationSuggestionsCard(
    uiData: String?,
    onDestinationClick: (String) -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    // Parse destinations from JSON
    val (suggestionType, destinations) = remember(uiData) { parseDestinationsFromJson(uiData) }
    
    val title = when (suggestionType) {
        "weather" -> if (isRtl) "وجهات بطقس جميل" else "Destinations with Nice Weather"
        "cheapest" -> if (isRtl) "أرخص الوجهات" else "Cheapest Destinations"
        "popular" -> if (isRtl) "وجهات شائعة" else "Popular Destinations"
        else -> if (isRtl) "وجهات مقترحة" else "Suggested Destinations"
    }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (destinations.isEmpty()) {
            Text(
                text = if (isRtl) "لا توجد وجهات متاحة" else "No destinations available",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                color = VelocityColors.TextMuted
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(destinations) { dest ->
                    DestinationCard(
                        destination = dest,
                        onClick = { onDestinationClick(dest.code) },
                        suggestionType = suggestionType,
                        isRtl = isRtl
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(
    destination: ParsedDestination,
    onClick: () -> Unit,
    suggestionType: String,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    val platformContext = LocalPlatformContext.current
    
    Surface(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = RoundedCornerShape(16.dp),
        color = AuroraMidnight,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column {
            // City image at top
            if (!destination.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(platformContext)
                            .data(destination.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = destination.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Gradient overlay for text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        AuroraMidnight.copy(alpha = 0.8f)
                                    ),
                                    startY = 50f
                                )
                            )
                    )
                }
            }
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Destination name
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold
                )
                
                // Country
                if (destination.country.isNotBlank()) {
                    Text(
                        text = destination.country,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                        color = VelocityColors.TextMuted
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Weather info - always show if available
                if (destination.temperature != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Weather icon based on condition
                        val weatherEmoji = when (destination.weatherCondition?.lowercase()) {
                            "sunny" -> "☀️"
                            "partly_cloudy" -> "⛅"
                            "cloudy" -> "☁️"
                            "rainy" -> "🌧️"
                            else -> "☀️"
                        }
                        Text(
                            text = weatherEmoji,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${destination.temperature}°C",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                            color = AuroraCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                // Reason/description
                destination.reason?.let { reason ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily),
                        color = VelocityColors.TextMuted,
                        maxLines = 2
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price if available
                if (destination.lowestPrice != null) {
                    Text(
                        text = if (isRtl) "من ${destination.lowestPrice.toInt()} ر.س" 
                               else "From SAR ${destination.lowestPrice.toInt()}",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = fontFamily),
                        color = AuroraCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PassengerFormCard(
    uiData: String?,
    onSubmit: () -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isRtl) "معلومات المسافر" else "Passenger Information",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isRtl) "نموذج المسافر قادم قريباً" else "Passenger form coming soon",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMuted
        )
    }
}

@Composable
private fun ClarificationCard(
    uiData: String?,
    onOptionClick: (String) -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    Column(modifier = Modifier.padding(16.dp)) {
        // This will show clarification questions with quick options
        Text(
            text = if (isRtl) "أحتاج مزيداً من المعلومات" else "I need more information",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMain
        )
    }
}

// =============================================================================
// QUICK SUGGESTIONS ROW
// =============================================================================

@Composable
private fun QuickSuggestionsRow(
    suggestions: List<String>,
    onSuggestionTapped: (String) -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(suggestions) { suggestion ->
            Surface(
                modifier = Modifier
                    .clickable { onSuggestionTapped(suggestion) }
                    .pointerHoverIcon(PointerIcon.Hand),
                shape = RoundedCornerShape(20.dp),
                color = AuroraCyan.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, AuroraCyan.copy(alpha = 0.3f))
            ) {
                Text(
                    text = suggestion,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily),
                    color = AuroraCyan
                )
            }
        }
    }
}

// =============================================================================
// QUICK SERVICES GRID
// =============================================================================

@Composable
private fun QuickServicesGrid(
    onCheckInClick: () -> Unit,
    onManageBookingClick: () -> Unit,
    onMembershipClick: () -> Unit,
    onHelpClick: () -> Unit,
    isRtl: Boolean
) {
    val fontFamily = if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    val services = listOf(
        Triple(Icons.AutoMirrored.Filled.Send, if (isRtl) "تسجيل الوصول" else "Check-In", onCheckInClick),
        Triple(Icons.Default.Edit, if (isRtl) "إدارة الحجز" else "Manage Booking", onManageBookingClick),
        Triple(Icons.Default.Star, if (isRtl) "العضوية" else "Membership", onMembershipClick),
        Triple(Icons.Default.Info, if (isRtl) "المساعدة" else "Help", onHelpClick)
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRtl) "خدمات سريعة" else "Quick Services",
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMuted
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            services.forEach { (icon, label, onClick) ->
                QuickServiceButton(
                    icon = icon,
                    label = label,
                    onClick = onClick,
                    fontFamily = fontFamily
                )
            }
        }
    }
}

@Composable
private fun QuickServiceButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    fontFamily: androidx.compose.ui.text.font.FontFamily
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = AuroraCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = fontFamily),
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// =============================================================================
// JSON PARSING UTILITIES
// =============================================================================

/**
 * Format an ISO timestamp (e.g., "2025-12-11T03:00:00Z") to a readable time (e.g., "03:00")
 */
private fun formatTime(isoTimestamp: String): String {
    return try {
        // Extract time from ISO format: 2025-12-11T03:00:00Z -> 03:00
        val timePart = isoTimestamp.substringAfter("T").substringBefore("Z").substringBefore("+")
        val parts = timePart.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull() ?: return isoTimestamp
            val minute = parts[1]
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            "$displayHour:$minute $amPm"
        } else {
            isoTimestamp
        }
    } catch (e: Exception) {
        isoTimestamp
    }
}

/**
 * Format an ISO timestamp to a readable date (e.g., "Dec 11")
 */
private fun formatDate(isoTimestamp: String): String {
    return try {
        // Extract date from ISO format: 2025-12-11T03:00:00Z -> Dec 11
        val datePart = isoTimestamp.substringBefore("T")
        val parts = datePart.split("-")
        if (parts.size == 3) {
            val month = when (parts[1]) {
                "01" -> "Jan"
                "02" -> "Feb"
                "03" -> "Mar"
                "04" -> "Apr"
                "05" -> "May"
                "06" -> "Jun"
                "07" -> "Jul"
                "08" -> "Aug"
                "09" -> "Sep"
                "10" -> "Oct"
                "11" -> "Nov"
                "12" -> "Dec"
                else -> parts[1]
            }
            val day = parts[2].toIntOrNull() ?: return isoTimestamp
            "$month $day"
        } else {
            isoTimestamp
        }
    } catch (e: Exception) {
        isoTimestamp
    }
}

private data class ParsedFlight(
    val flightNumber: String,
    val departureTime: String,
    val arrivalTime: String? = null,
    val price: String,
    val duration: String? = null
)

private fun parseFlightsFromJson(uiData: String?): List<ParsedFlight> {
    if (uiData.isNullOrBlank()) return emptyList()
    
    return try {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val element = json.parseToJsonElement(uiData)
        
        // Try to find flights array in the data
        val flightsArray = when {
            element is kotlinx.serialization.json.JsonArray -> element
            element is kotlinx.serialization.json.JsonObject -> {
                element["flights"]?.let { it as? kotlinx.serialization.json.JsonArray }
                    ?: element["data"]?.let { 
                        (it as? kotlinx.serialization.json.JsonObject)?.get("flights") as? kotlinx.serialization.json.JsonArray 
                    }
            }
            else -> null
        } ?: return emptyList()
        
        flightsArray.mapNotNull { flight ->
            val obj = flight as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            
            val flightNumber = obj["flightNumber"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: return@mapNotNull null
            
            val departureTime = obj["departureTime"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: ""
            
            val arrivalTime = obj["arrivalTime"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            }
            
            val price = obj["priceFormatted"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: obj["lowestPrice"]?.let { 
                val amount = (it as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "0"
                "SAR $amount"
            } ?: "SAR --"
            
            val duration = obj["duration"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            }
            
            ParsedFlight(
                flightNumber = flightNumber,
                departureTime = departureTime,
                arrivalTime = arrivalTime,
                price = price,
                duration = duration
            )
        }
    } catch (e: Exception) {
        println("Error parsing flights JSON: ${e.message}")
        emptyList()
    }
}

// Destination parsing utilities

private data class ParsedDestination(
    val code: String,
    val name: String,
    val country: String,
    val lowestPrice: Double? = null,
    val temperature: Int? = null,
    val weatherCondition: String? = null,
    val reason: String? = null,
    val imageUrl: String? = null
)

private fun parseDestinationsFromJson(uiData: String?): Pair<String, List<ParsedDestination>> {
    if (uiData.isNullOrBlank()) return Pair("", emptyList())
    
    return try {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val element = json.parseToJsonElement(uiData)
        
        val obj = element as? kotlinx.serialization.json.JsonObject ?: return Pair("", emptyList())
        
        val suggestionType = obj["suggestionType"]?.let {
            (it as? kotlinx.serialization.json.JsonPrimitive)?.content
        } ?: ""
        
        val suggestionsArray = obj["suggestions"]?.let { 
            it as? kotlinx.serialization.json.JsonArray 
        } ?: return Pair(suggestionType, emptyList())
        
        val destinations = suggestionsArray.mapNotNull { suggestion ->
            val suggObj = suggestion as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            
            val code = suggObj["destinationCode"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: return@mapNotNull null
            
            val name = suggObj["destinationName"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: code
            
            val country = suggObj["country"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: ""
            
            val lowestPrice = suggObj["lowestPrice"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
            }
            
            // Parse nested weather object
            val weatherObj = suggObj["weather"]?.let { it as? kotlinx.serialization.json.JsonObject }
            val temperature = weatherObj?.get("temperature")?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
            } ?: suggObj["temperature"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
            }
            
            val weatherCondition = weatherObj?.get("condition")?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: suggObj["weatherCondition"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            }
            
            val weatherDescription = weatherObj?.get("description")?.let {
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content
            }
            
            val reason = suggObj["reason"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            } ?: weatherDescription ?: suggObj["description"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            }
            
            val imageUrl = suggObj["imageUrl"]?.let { 
                (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
            }
            
            ParsedDestination(
                code = code,
                name = name,
                country = country,
                lowestPrice = lowestPrice,
                temperature = temperature,
                weatherCondition = weatherCondition,
                reason = reason,
                imageUrl = imageUrl
            )
        }
        
        Pair(suggestionType, destinations)
    } catch (e: Exception) {
        println("Error parsing destinations JSON: ${e.message}")
        Pair("", emptyList())
    }
}
