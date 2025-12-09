package com.fairair.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.ui.theme.NotoKufiArabicFontFamily
import com.fairair.app.ui.theme.SpaceGroteskFontFamily
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.contract.dto.ChatUiType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

// CompositionLocal for flight selection callback (avoids threading through many layers)
private val LocalOnFlightSelected = staticCompositionLocalOf<(String) -> Unit> { {} }

// =============================================================================
// AURORA THEME COLORS - Deep space with ethereal gradients
// =============================================================================

// Background colors
private val AuroraDeepSpace = Color(0xFF020617)  // slate-950
private val AuroraMidnight = Color(0xFF0F172A)   // slate-900

// Orb state colors
private val OrbIdle = Color(0xFF3B82F6)          // Blue - calm
private val OrbIdleGlow = Color(0xFF1E40AF)      // Deeper blue
private val OrbListening = Color(0xFF60A5FA)     // Bright blue - active recording
private val OrbListeningGlow = Color(0xFF2563EB) // Vivid blue
private val OrbProcessing = Color(0xFFA855F7)    // Purple - thinking  
private val OrbProcessingGlow = Color(0xFF7E22CE) // Deep purple
private val OrbSpeaking = Color(0xFF06B6D4)      // Cyan - speaking
private val OrbSpeakingGlow = Color(0xFF0891B2)  // Deep cyan

// Accent colors
private val AuroraCyan = Color(0xFF22D3EE)
private val AuroraBlue = Color(0xFF3B82F6)
private val AuroraPurple = Color(0xFF8B5CF6)
private val AuroraRose = Color(0xFFFB7185)
private val AuroraGreen = Color(0xFF10B981)

// Glass/card colors
private val GlassWhite = Color(0x1AFFFFFF)       // 10% white
private val GlassBorder = Color(0x33FFFFFF)      // 20% white
private val GlassStrong = Color(0x26FFFFFF)      // 15% white

// Legacy aliases for compatibility
private val PilotPrimaryColor = AuroraBlue
private val PilotSecondaryColor = AuroraPurple
private val PilotAccentColor = AuroraCyan
private val PilotGradient = Brush.radialGradient(
    colors = listOf(AuroraCyan, AuroraBlue, AuroraPurple)
)

// Grid explosion animation constants
private const val GRID_COLS = 12
private const val GRID_ROWS = 16
private const val ANIMATION_DURATION_MS = 800

/**
 * AI State enum for the orb visual states.
 */
enum class AIState {
    IDLE,       // Default calm state
    LISTENING,  // Actively recording user speech
    PROCESSING, // Thinking/waiting for response
    SPEAKING    // Playing TTS audio
}

/**
 * Gets the appropriate font family based on RTL mode.
 */
@Composable
private fun chatFontFamily(isRtl: Boolean): FontFamily {
    return if (isRtl) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
}

/**
 * Detects if text contains Arabic characters.
 */
private fun containsArabic(text: String): Boolean {
    return text.any { char ->
        val code = char.code
        (code in 0x0600..0x06FF) ||  // Arabic
        (code in 0x0750..0x077F) ||  // Arabic Supplement
        (code in 0x08A0..0x08FF) ||  // Arabic Extended-A
        (code in 0xFB50..0xFDFF) ||  // Arabic Presentation Forms-A
        (code in 0xFE70..0xFEFF)     // Arabic Presentation Forms-B
    }
}

/**
 * Strips emojis and problematic Unicode characters that don't render.
 */
private fun stripEmojis(text: String): String {
    return text.filter { char ->
        val code = char.code
        code < 0x2000 ||  // Basic multilingual plane common chars
        (code in 0x0600..0x06FF) ||  // Arabic
        (code in 0x0750..0x077F) ||  // Arabic Supplement  
        (code in 0x08A0..0x08FF) ||  // Arabic Extended-A
        (code in 0xFB50..0xFDFF) ||  // Arabic Presentation Forms-A
        (code in 0xFE70..0xFEFF) ||  // Arabic Presentation Forms-B
        (code in 0x2000..0x206F)     // General punctuation
    }
}

// =============================================================================
// AURORA BACKGROUND - Animated flowing light curtains
// =============================================================================

/**
 * Living aurora background with animated sine wave gradients.
 * Uses a single Canvas for GPU-accelerated rendering.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")
    
    // Time-based animation for wave movement
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
            .blur(60.dp)  // Soften the waves into glowing clouds
    ) {
        val w = size.width
        val h = size.height
        
        // Draw multiple layered waves with different colors and speeds
        drawAuroraWave(
            canvasWidth = w,
            canvasHeight = h,
            yBase = h * 0.4f,
            amplitude = 80f,
            frequency = 0.001f,
            time = time * 1f,
            colorStart = Color(0x1A4C1D95),  // Purple transparent
            colorEnd = Color(0x66581C87)     // Purple
        )
        
        drawAuroraWave(
            canvasWidth = w,
            canvasHeight = h,
            yBase = h * 0.5f,
            amplitude = 60f,
            frequency = 0.002f,
            time = time * 1.5f,
            colorStart = Color(0x1A06B6D4),  // Cyan transparent
            colorEnd = Color(0x4D3B82F6)     // Blue
        )
        
        drawAuroraWave(
            canvasWidth = w,
            canvasHeight = h,
            yBase = h * 0.6f,
            amplitude = 40f,
            frequency = 0.003f,
            time = time * 2f,
            colorStart = Color(0x0D2DD4BF),  // Teal transparent
            colorEnd = Color(0x3310B981)     // Green
        )
    }
}

/**
 * Draw a single aurora wave layer.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAuroraWave(
    canvasWidth: Float,
    canvasHeight: Float,
    yBase: Float,
    amplitude: Float,
    frequency: Float,
    time: Float,
    colorStart: Color,
    colorEnd: Color
) {
    val path = Path()
    path.moveTo(0f, canvasHeight)
    
    // Create wave points
    var x = 0f
    while (x <= canvasWidth) {
        val y = yBase + 
            sin(x * frequency + time) * amplitude +
            sin(x * frequency * 2 + time * 1.5f) * (amplitude / 2)
        path.lineTo(x, y)
        x += 10f
    }
    
    // Close the path at the bottom
    path.lineTo(canvasWidth, canvasHeight)
    path.lineTo(0f, canvasHeight)
    path.close()
    
    // Draw with gradient
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colors = listOf(colorStart, colorEnd),
            startY = 0f,
            endY = canvasHeight
        )
    )
}

// =============================================================================
// MICROPHONE ICON - Custom drawn microphone
// =============================================================================

/**
 * Custom microphone icon drawn with Canvas.
 */
@Composable
private fun MicrophoneIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Microphone body (rounded rectangle/pill shape)
        val micWidth = w * 0.4f
        val micHeight = h * 0.55f
        val micLeft = (w - micWidth) / 2
        val micTop = h * 0.05f
        val cornerRadius = micWidth / 2
        
        drawRoundRect(
            color = tint,
            topLeft = Offset(micLeft, micTop),
            size = Size(micWidth, micHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
        )
        
        // Microphone holder arc (U shape)
        val arcStrokeWidth = w * 0.08f
        val arcWidth = w * 0.6f
        val arcHeight = h * 0.35f
        val arcLeft = (w - arcWidth) / 2
        val arcTop = h * 0.35f
        
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(arcLeft, arcTop),
            size = Size(arcWidth, arcHeight),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = arcStrokeWidth)
        )
        
        // Stand (vertical line)
        val standWidth = w * 0.08f
        val standLeft = (w - standWidth) / 2
        val standTop = h * 0.7f
        val standHeight = h * 0.18f
        
        drawRect(
            color = tint,
            topLeft = Offset(standLeft, standTop),
            size = Size(standWidth, standHeight)
        )
        
        // Base (horizontal line)
        val baseWidth = w * 0.4f
        val baseHeight = w * 0.08f
        val baseLeft = (w - baseWidth) / 2
        val baseTop = h * 0.88f
        
        drawRoundRect(
            color = tint,
            topLeft = Offset(baseLeft, baseTop),
            size = Size(baseWidth, baseHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(baseHeight / 2)
        )
    }
}

// =============================================================================
// AI CORE ORB - Central animated orb with state-based appearance
// =============================================================================

/**
 * The AI Core - a glowing orb that changes appearance based on AI state.
 * States: idle (blue), listening (rose), processing (purple), speaking (cyan)
 */
@Composable
fun AICore(
    state: AIState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_core")
    
    // Pulse animation - more dramatic for listening
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (state) {
            AIState.LISTENING -> 1.25f  // More dramatic pulse when listening
            AIState.PROCESSING -> 1.08f
            AIState.SPEAKING -> 1.1f
            else -> 1.05f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AIState.LISTENING -> 500  // Faster pulse when listening
                    AIState.PROCESSING -> 400
                    AIState.SPEAKING -> 800
                    else -> 2000
                },
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Glow intensity animation for listening
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = if (state == AIState.LISTENING) 0.3f else 0.2f,
        targetValue = if (state == AIState.LISTENING) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AIState.LISTENING) 500 else 2000,
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_intensity"
    )
    
    // Rotation for rings
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Color animation
    val (coreColor, glowColor) = when (state) {
        AIState.LISTENING -> OrbListening to OrbListeningGlow
        AIState.PROCESSING -> OrbProcessing to OrbProcessingGlow
        AIState.SPEAKING -> OrbSpeaking to OrbSpeakingGlow
        else -> OrbIdle to OrbIdleGlow
    }
    
    Box(
        modifier = modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring 1 - pulses when listening
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(if (state == AIState.LISTENING) pulse * 0.95f else 1f)
                .graphicsLayer { rotationZ = rotation }
                .border(
                    width = if (state == AIState.LISTENING) 2.dp else 1.dp,
                    color = when (state) {
                        AIState.LISTENING -> OrbListening.copy(alpha = glowIntensity * 0.5f)
                        AIState.SPEAKING -> AuroraCyan.copy(alpha = 0.3f)
                        else -> GlassBorder.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
        )
        
        // Outer ring 2 (counter-rotating)
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(if (state == AIState.LISTENING) pulse * 0.9f else 1f)
                .graphicsLayer { rotationZ = -rotation * 0.6f }
                .border(
                    width = 1.dp,
                    color = when (state) {
                        AIState.LISTENING -> OrbListening.copy(alpha = glowIntensity * 0.3f)
                        AIState.PROCESSING -> GlassBorder.copy(alpha = 0.4f)
                        else -> GlassBorder.copy(alpha = 0.15f)
                    },
                    shape = CircleShape
                )
        )
        
        // Pulsating rings when listening (similar to speaking waves)
        if (state == AIState.LISTENING) {
            repeat(3) { index ->
                val delay = index * 200
                val ringAnim = rememberInfiniteTransition(label = "listen_ring_$index")
                val ringScale by ringAnim.animateFloat(
                    initialValue = 1f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, delayMillis = delay, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ring_scale_$index"
                )
                val ringAlpha by ringAnim.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, delayMillis = delay, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ring_alpha_$index"
                )
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(ringScale)
                        .border(
                            width = 3.dp,
                            color = OrbListening.copy(alpha = ringAlpha),
                            shape = CircleShape
                        )
                )
            }
        }
        
        // Glow layers
        repeat(3) { index ->
            val layerScale = pulse + (index * 0.15f)
            val layerAlpha = ((if (state == AIState.LISTENING) glowIntensity else 0.3f) - index * 0.1f).coerceIn(0.05f, 0.5f)
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
        
        // The Core orb
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(pulse)
                .shadow(
                    elevation = if (state == AIState.LISTENING) 32.dp else 24.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = coreColor
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor,
                            glowColor
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner noise texture effect (subtle)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .graphicsLayer { 
                        rotationZ = rotation * 0.2f 
                        alpha = 0.15f
                    }
                    .background(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        // Audio waves when speaking
        if (state == AIState.SPEAKING) {
            repeat(2) { index ->
                val delay = index * 150
                val pingAnim = rememberInfiniteTransition(label = "ping_$index")
                val pingScale by pingAnim.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, delayMillis = delay),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ping_scale_$index"
                )
                val pingAlpha by pingAnim.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, delayMillis = delay),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ping_alpha_$index"
                )
                
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pingScale)
                        .border(
                            width = 2.dp,
                            color = AuroraCyan.copy(alpha = pingAlpha),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// =============================================================================
// GRID EXPLOSION ANIMATION - Single Canvas "Shader" Approach
// =============================================================================

/**
 * High-performance staggered grid reveal animation.
 * 
 * Uses a single Canvas to draw a mask overlay that reveals content
 * in a wave pattern emanating from the FAB (bottom-right corner).
 * 
 * The "Golden Rule": No individual Composable nodes per tile.
 * All animation math is computed in the draw loop, running on GPU via Skia.
 */
@Composable
fun GridExplosionTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    
    // Single animation state driving everything (0f to 1f)
    val transition = updateTransition(targetState = visible, label = "GridReveal")
    val progress by transition.animateFloat(
        transitionSpec = { 
            if (targetState) {
                tween(durationMillis = ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
            } else {
                tween(durationMillis = 400, easing = FastOutLinearInEasing)
            }
        },
        label = "Progress"
    ) { isVisible -> if (isVisible) 1f else 0f }
    
    // Track content visibility
    LaunchedEffect(visible, progress) {
        if (visible) {
            showContent = true
        } else if (progress == 0f) {
            showContent = false
        }
    }
    
    if (!showContent && !visible) return
    
    // The mask color (background that gets revealed away)
    val maskColor = VelocityColors.BackgroundDeep
    
    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: The actual content (always rendered when visible)
        content()
        
        // Layer 2: Canvas mask overlay that reveals the content
        // When progress = 0, mask is fully opaque (hides content)
        // When progress = 1, mask is fully transparent (shows content)
        if (progress < 1f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellWidth = size.width / GRID_COLS
                val cellHeight = size.height / GRID_ROWS
                
                // FAB is at bottom-right
                val fabX = size.width - 36f  // Approximate FAB center
                val fabY = size.height - 36f
                
                // Maximum distance from FAB to furthest corner (top-left)
                val maxDist = sqrt(
                    (fabX * fabX) + (fabY * fabY)
                )
                
                for (row in 0 until GRID_ROWS) {
                    for (col in 0 until GRID_COLS) {
                        // Calculate cell center
                        val cellCx = (col * cellWidth) + (cellWidth / 2)
                        val cellCy = (row * cellHeight) + (cellHeight / 2)
                        
                        // Distance from FAB to this cell
                        val dx = cellCx - fabX
                        val dy = cellCy - fabY
                        val dist = sqrt(dx * dx + dy * dy)
                        
                        // Normalize distance (0.0 = at FAB, 1.0 = furthest)
                        val distFraction = dist / maxDist
                        
                        // Staggered timing: cells close to FAB animate first
                        // Wave travels from 0.0 to 1.0 over the animation
                        val waveStart = distFraction * 0.6f
                        val waveEnd = waveStart + 0.4f
                        
                        // Map global progress to local cell progress
                        val cellProgress = ((progress - waveStart) / (waveEnd - waveStart)).coerceIn(0f, 1f)
                        
                        // Cell alpha: 1 = fully masked, 0 = fully revealed
                        val cellAlpha = 1f - cellProgress
                        
                        if (cellAlpha > 0.01f) {
                            // Draw mask tile with scale effect
                            val scale = 0.5f + (0.5f * (1f - cellProgress))
                            val scaledWidth = cellWidth * scale
                            val scaledHeight = cellHeight * scale
                            val offsetX = (cellWidth - scaledWidth) / 2
                            val offsetY = (cellHeight - scaledHeight) / 2
                            
                            drawRect(
                                color = maskColor.copy(alpha = cellAlpha),
                                topLeft = Offset(
                                    col * cellWidth + offsetX,
                                    row * cellHeight + offsetY
                                ),
                                size = Size(scaledWidth, scaledHeight)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// PILOT ORB - The Floating Action Button

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
// PILOT FULL SCREEN - Aurora-themed AI interface
// =============================================================================

/**
 * Full-screen Aurora-themed Pilot AI interface.
 * Features:
 * - Living aurora background
 * - Central AI Core orb that responds to state
 * - Holographic cards appearing behind the orb
 * - Chat messages in bottom overlay
 * - Voice-first design
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
    onVoiceClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    locale: String = "en-US"
) {
    val isRtl = locale.startsWith("ar")
    val messagesScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    // Determine AI state based on UI state
    val aiState = when {
        uiState.isSpeaking -> AIState.SPEAKING
        uiState.isLoading -> AIState.PROCESSING
        uiState.isListening -> AIState.LISTENING
        else -> AIState.IDLE
    }
    
    // Check if there's active content to show (flight card, boarding pass, etc.)
    val lastAssistantMessage = uiState.messages.lastOrNull { !it.isFromUser }
    // Only show content card for types that have visual representations
    val hasActiveContent = lastAssistantMessage?.uiType in listOf(
        ChatUiType.FLIGHT_LIST,
        ChatUiType.BOARDING_PASS,
        ChatUiType.SEAT_MAP,
        ChatUiType.BOOKING_SUMMARY,
        ChatUiType.FLIGHT_COMPARISON
    )
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        scope.launch {
            messagesScrollState.animateScrollTo(messagesScrollState.maxValue)
        }
    }

    CompositionLocalProvider(
        LocalOnFlightSelected provides { flightNumber ->
            onSuggestionTapped("Select flight $flightNumber")
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AuroraDeepSpace)
        ) {
            // Layer 1: Aurora Background
            AuroraBackground(modifier = Modifier.fillMaxSize())
            
            // Layer 2: Main Content
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Dynamic Content Layer (Behind the orb for depth)
                // Shows holographic cards when available
                // Debug: log the state
                LaunchedEffect(lastAssistantMessage?.uiType, hasActiveContent) {
                    println("PilotChat: lastAssistantMessage uiType=${lastAssistantMessage?.uiType}, hasActiveContent=$hasActiveContent, uiData length=${lastAssistantMessage?.uiData?.length ?: 0}")
                }
                
                AnimatedVisibility(
                    visible = hasActiveContent,
                    enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.9f),
                    exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp)  // Position above center
                ) {
                    lastAssistantMessage?.let { message ->
                        HolographicContentCard(
                            uiType = message.uiType,
                            uiData = message.uiData,
                            isRtl = isRtl
                        )
                    }
                }
                
                // The AI Core - moves up and shrinks when content is shown
                val orbOffsetY by animateDpAsState(
                    targetValue = if (hasActiveContent) (-180).dp else (-60).dp,
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "orb_offset"
                )
                val orbScale by animateFloatAsState(
                    targetValue = if (hasActiveContent) 0.5f else 1f,
                    animationSpec = tween(700, easing = FastOutSlowInEasing),
                    label = "orb_scale"
                )
                
                AICore(
                    state = aiState,
                    modifier = Modifier
                        .offset(y = orbOffsetY)
                        .scale(orbScale)
                )
            }
            
            // Layer 3: Close button (top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(onClick = onClearChat) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Clear",
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Layer 4: Chat Interface (bottom overlay)
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Messages container (scrollable, max 40% height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(messagesScrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        uiState.messages.forEach { message ->
                            AuroraChatBubble(
                                message = message,
                                isRtl = isRtl
                            )
                        }
                        
                        // Show interim transcription immediately while listening
                        if (uiState.interimText.isNotBlank()) {
                            InterimTranscriptionBubble(
                                text = uiState.interimText,
                                isRtl = isRtl
                            )
                        }
                    }
                }
                
                // Quick suggestions
                if (lastAssistantMessage?.suggestions?.isNotEmpty() == true) {
                    AuroraQuickHints(
                        hints = lastAssistantMessage.suggestions,
                        onHintClick = onSuggestionTapped,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (uiState.messages.isEmpty()) {
                    // Default hints when no messages
                    AuroraQuickHints(
                        hints = if (isRtl) {
                            listOf("أبغى رحلة لجدة", "حالة رحلتي", "وريني البوردنق")
                        } else {
                            listOf("Find a flight to Jeddah", "Check flight status", "Show boarding pass")
                        },
                        onHintClick = onSuggestionTapped,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                // Input controls
                AuroraInputBar(
                    inputText = uiState.inputText,
                    onInputChange = onInputChange,
                    onSendMessage = { onSendMessage(uiState.inputText) },
                    onMicClick = onVoiceClick,
                    aiState = aiState,
                    isRtl = isRtl
                )
            }
        }
    }
}

// =============================================================================
// HOLOGRAPHIC CONTENT CARD - Glass-morphism flight info display
// =============================================================================

/**
 * Holographic glass card for displaying flight info, boarding pass, etc.
 */
@Composable
private fun HolographicContentCard(
    uiType: ChatUiType?,
    uiData: String?,
    isRtl: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp),
        shape = RoundedCornerShape(24.dp),
        color = GlassWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            when (uiType) {
                ChatUiType.FLIGHT_LIST -> HolographicFlightCard(uiData, isRtl)
                ChatUiType.BOARDING_PASS -> HolographicBoardingPass(uiData, isRtl)
                ChatUiType.SEAT_MAP -> HolographicSeatMap(uiData, isRtl)
                ChatUiType.BOOKING_SUMMARY -> HolographicBookingSummary(uiData, isRtl)
                // These types don't need visual cards - the text message is sufficient
                ChatUiType.FLIGHT_SELECTED,
                ChatUiType.BOOKING_CONFIRMED,
                ChatUiType.PAYMENT_CONFIRM,
                null -> {
                    // No visual card needed
                }
                else -> {
                    // Other types that might have data to display
                    if (!uiData.isNullOrBlank()) {
                        Text(
                            text = "Processing...",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Holographic flight list/selection display.
 * Shows a scrollable list of available flights when multiple options exist.
 */
@Composable
private fun HolographicFlightCard(uiData: String?, isRtl: Boolean) {
    val flights = remember(uiData) { parseFlightsFromUiData(uiData) }
    val onFlightSelected = LocalOnFlightSelected.current
    
    println("HolographicFlightCard: rendering ${flights.size} flights")
    
    if (flights.isEmpty()) {
        Text(
            text = "No flights to display",
            color = Color.White.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRtl) "الرحلات المتاحة" else "AVAILABLE FLIGHTS",
                style = MaterialTheme.typography.labelSmall,
                color = AuroraCyan,
                letterSpacing = 2.sp
            )
            Text(
                text = "${flights.size} ${if (isRtl) "رحلات" else "options"}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Flight options - scrollable row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(flights) { flight ->
                HolographicFlightOption(
                    flight = flight,
                    isRtl = isRtl,
                    onClick = { onFlightSelected(flight.flightNumber) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hint text
        Text(
            text = if (isRtl) "اضغط على رحلة للاختيار" else "Tap a flight to select",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

/**
 * Individual flight option card in the holographic list.
 */
@Composable
private fun HolographicFlightOption(
    flight: ParsedFlight,
    isRtl: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Flight number
            Text(
                text = flight.flightNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AuroraCyan
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Date
            if (flight.date.isNotBlank()) {
                Text(
                    text = flight.date,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Departure time
            Text(
                text = flight.departureTime,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Price
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AuroraCyan.copy(alpha = 0.15f)
            ) {
                Text(
                    text = flight.price,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AuroraCyan
                )
            }
        }
    }
}

@Composable
private fun HolographicInfoCell(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (highlight) AuroraCyan else Color.White
            )
        }
    }
}

@Composable
private fun HolographicBoardingPass(uiData: String?, isRtl: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // QR Code placeholder
        Surface(
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Simple QR pattern simulation
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(8) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(8) { col ->
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(
                                            if ((row + col) % 2 == 0 || (row * col) % 3 == 0)
                                                Color.Black
                                            else
                                                Color.Transparent
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isRtl) "محمد الأحمد" else "Hassan Al-Ahmed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Flight FA 203 • Business Class",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HolographicSeatMap(uiData: String?, isRtl: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRtl) "اختر مقعدك" else "Select Your Seat",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Simplified seat grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("A", "B", "C").forEach { col ->
                            val isSelected = row == 1 && col == "A"
                            val isOccupied = (row + col.hashCode()) % 3 == 0
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    isSelected -> AuroraCyan
                                    isOccupied -> Color.Gray.copy(alpha = 0.3f)
                                    else -> AuroraBlue.copy(alpha = 0.6f)
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${row + 1}$col",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("D", "E", "F").forEach { col ->
                            val isOccupied = (row + col.hashCode()) % 2 == 0
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = if (isOccupied) 
                                    Color.Gray.copy(alpha = 0.3f) 
                                else 
                                    AuroraBlue.copy(alpha = 0.6f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${row + 1}$col",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Holographic booking summary card.
 */
@Composable
private fun HolographicBookingSummary(uiData: String?, isRtl: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRtl) "ملخص الحجز" else "Booking Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AuroraCyan
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Parse booking data if available
        val bookingInfo = remember(uiData) { parseBookingSummary(uiData) }
        
        if (bookingInfo != null) {
            // PNR
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AuroraCyan.copy(alpha = 0.15f)
            ) {
                Text(
                    text = bookingInfo.pnr,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuroraCyan,
                    letterSpacing = 4.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Flight info
            Text(
                text = bookingInfo.flightNumber,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            
            Text(
                text = "${bookingInfo.origin} → ${bookingInfo.destination}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Text(
                text = bookingInfo.dateTime,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f)
            )
        } else {
            Text(
                text = if (isRtl) "جاري تحميل التفاصيل..." else "Loading details...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private data class BookingSummaryInfo(
    val pnr: String,
    val flightNumber: String,
    val origin: String,
    val destination: String,
    val dateTime: String
)

private fun parseBookingSummary(uiData: String?): BookingSummaryInfo? {
    if (uiData.isNullOrBlank()) return null
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(uiData).jsonObject
        BookingSummaryInfo(
            pnr = root["pnr"]?.jsonPrimitive?.contentOrNull ?: "",
            flightNumber = root["flightNumber"]?.jsonPrimitive?.contentOrNull ?: "",
            origin = root["origin"]?.jsonPrimitive?.contentOrNull ?: "",
            destination = root["destination"]?.jsonPrimitive?.contentOrNull ?: "",
            dateTime = root["dateTime"]?.jsonPrimitive?.contentOrNull ?: ""
        )
    } catch (e: Exception) {
        null
    }
}

// =============================================================================
// AURORA CHAT BUBBLES - Modern chat message design
// =============================================================================

/**
 * Aurora-styled chat bubble with glowing indicator.
 */
@Composable
private fun AuroraChatBubble(
    message: ChatMessage,
    isRtl: Boolean
) {
    val cleanText = stripEmojis(message.text)
    val isArabic = containsArabic(cleanText)
    val fontFamily = if (isArabic) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    val isAi = !message.isFromUser
    
    if (message.isLoading) {
        AuroraLoadingIndicator()
        return
    }
    
    // Skip empty messages or messages that only have UI content
    if (cleanText.isBlank() && message.uiType != null) return
    if (cleanText.isBlank()) return
    
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
        ) {
            Row(
                modifier = Modifier.widthIn(max = 300.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isAi) Arrangement.Start else Arrangement.End
            ) {
                // Glowing indicator bar
                if (isAi) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(AuroraCyan)
                            .shadow(4.dp, spotColor = AuroraCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // Bubble
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isAi) 4.dp else 16.dp,
                        bottomEnd = if (isAi) 16.dp else 4.dp
                    ),
                    color = if (isAi) 
                        Color.Black.copy(alpha = 0.4f) 
                    else 
                        GlassWhite,
                    border = if (isAi) 
                        androidx.compose.foundation.BorderStroke(1.dp, GlassBorder) 
                    else 
                        null
                ) {
                    Text(
                        text = cleanText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = fontFamily,
                            lineHeight = 22.sp
                        ),
                        color = if (isAi) 
                            Color(0xFFE0F2FE)  // Cyan-tinted white
                        else 
                            Color.White
                    )
                }
                
                // User indicator
                if (!isAi) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(AuroraRose)
                            .shadow(4.dp, spotColor = AuroraRose)
                    )
                }
            }
        }
    }
}

/**
 * Shows interim transcription text while the user is speaking.
 * Appears with a pulsing animation to indicate it's still being processed.
 */
@Composable
private fun InterimTranscriptionBubble(
    text: String,
    isRtl: Boolean
) {
    val cleanText = stripEmojis(text)
    val isArabic = containsArabic(cleanText)
    val fontFamily = if (isArabic) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    val infiniteTransition = rememberInfiniteTransition(label = "interim_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "interim_alpha"
    )
    
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .graphicsLayer { this.alpha = alpha },
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier.widthIn(max = 300.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                // Bubble with dashed border to indicate interim
                Surface(
                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                    color = GlassWhite.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        OrbListening.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Microphone indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(OrbListening, CircleShape)
                        )
                        Text(
                            text = cleanText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = fontFamily,
                                lineHeight = 22.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(OrbListening)
                        .shadow(4.dp, spotColor = OrbListening)
                )
            }
        }
    }
}

@Composable
private fun AuroraLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(AuroraCyan)
            )
            Spacer(modifier = Modifier.width(8.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
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
                                    AuroraCyan.copy(alpha = alpha),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// AURORA INPUT BAR - Modern voice-first input
// =============================================================================

/**
 * Quick hint chips for suggestions.
 */
@Composable
private fun AuroraQuickHints(
    hints: List<String>,
    onHintClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        hints.take(3).forEach { hint ->
            Surface(
                modifier = Modifier.clickable { onHintClick(hint) },
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
                Text(
                    text = hint,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = AuroraCyan.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Aurora-styled input bar with voice button.
 */
@Composable
private fun AuroraInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onMicClick: () -> Unit,
    aiState: AIState,
    isRtl: Boolean
) {
    val focusRequester = remember { FocusRequester() }
    val isListening = aiState == AIState.LISTENING
    val isProcessing = aiState == AIState.PROCESSING
    
    Row(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Text input
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(28.dp),
            color = GlassWhite,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (inputText.isNotEmpty()) AuroraCyan.copy(alpha = 0.5f) else GlassBorder
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                    enabled = !isProcessing && !isListening,
                    decorationBox = { innerTextField ->
                        Box {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (isRtl) "اكتب أو تكلم..." else "Ask Pilot...",
                                    style = TextStyle(
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 16.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                
                // Send button (appears when text is entered)
                AnimatedVisibility(
                    visible = inputText.isNotBlank(),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    IconButton(
                        onClick = onSendMessage,
                        modifier = Modifier
                            .size(36.dp)
                            .background(GlassWhite, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        // Voice button
        val micColor by animateColorAsState(
            targetValue = when {
                isListening -> OrbListening
                isProcessing -> OrbProcessing
                else -> GlassWhite
            },
            animationSpec = tween(300),
            label = "mic_color"
        )
        
        val micScale by animateFloatAsState(
            targetValue = if (isListening) 1.1f else 1f,
            animationSpec = if (isListening) {
                infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                )
            } else {
                tween(200)
            },
            label = "mic_scale"
        )
        
        Surface(
            modifier = Modifier
                .size(56.dp)
                .scale(micScale)
                .clickable(
                    enabled = !isProcessing,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onMicClick() },
            shape = CircleShape,
            color = micColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            shadowElevation = if (isListening) 8.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isListening) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // Microphone icon drawn with Canvas
                    MicrophoneIcon(
                        tint = AuroraCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Legacy wrapper for compatibility
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

    // Provide flight selection callback via CompositionLocal
    CompositionLocalProvider(
        LocalOnFlightSelected provides { flightNumber ->
            onSuggestionTapped("Select flight $flightNumber")
        }
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f), // Half-height as per spec
            color = VelocityColors.BackgroundMid,
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
    } // End CompositionLocalProvider
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
                    VelocityColors.TextMuted.copy(alpha = 0.4f),
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
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = if (isRtl) "مساعدك للسفر" else "Your travel assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = VelocityColors.TextMuted
                )
            }

            IconButton(onClick = onClearChat) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Clear chat",
                    tint = VelocityColors.TextMuted
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = VelocityColors.TextMuted
                )
            }
        }
        
        HorizontalDivider(color = VelocityColors.GlassBorder)
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
            textAlign = TextAlign.Center,
            color = VelocityColors.TextMain
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRtl) {
                "اضغط على الميكروفون وتكلم معي"
            } else {
                "Tap the mic and talk to me"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = VelocityColors.TextMuted,
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
                color = VelocityColors.GlassBg
            ) {
                Text(
                    text = "\"$prompt\"",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = VelocityColors.TextMuted,
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
    isRtl: Boolean,
    hideTextWhenUiPresent: Boolean = false
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        when {
            message.isLoading -> LoadingIndicator()
            message.isFromUser -> UserBubble(message.text)
            message.uiType != null -> {
                // Render specialized UI based on type
                // When hideTextWhenUiPresent is true, only show the UI card (text is for TTS only)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!hideTextWhenUiPresent && message.text.isNotBlank()) {
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
    val cleanText = stripEmojis(text)
    val isArabic = containsArabic(cleanText)
    val fontFamily = if (isArabic) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
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
                    text = cleanText,
                    modifier = Modifier.padding(12.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily)
                )
            }
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    val cleanText = stripEmojis(text)
    val isArabic = containsArabic(cleanText)
    val fontFamily = if (isArabic) NotoKufiArabicFontFamily() else SpaceGroteskFontFamily()
    
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                color = VelocityColors.GlassBg
            ) {
                Text(
                    text = cleanText,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
                    color = VelocityColors.TextMain
                )
            }
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
            color = VelocityColors.GlassBg
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
                color = VelocityColors.GlassBg
            ) {
                Text(
                    text = "UI: ${uiType.name}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = VelocityColors.TextMuted
                )
            }
        }
    }
}

@Composable
private fun FlightCarouselCard(uiData: String?, isRtl: Boolean) {
    val onFlightSelected = LocalOnFlightSelected.current
    
    // Parse flights from uiData JSON, with fallback to empty list
    val flights = remember(uiData) {
        parseFlightsFromUiData(uiData)
    }
    
    if (flights.isEmpty()) {
        // Show "no flights" message if parsing failed or no flights
        Text(
            text = if (isRtl) "لا توجد رحلات متاحة" else "No flights available",
            style = MaterialTheme.typography.bodyMedium,
            color = VelocityColors.TextMuted
        )
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = if (isRtl) "الرحلات المتاحة" else "Available Flights",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = VelocityColors.TextMain,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(flights) { flight ->
                FlightOptionCard(
                    flightNumber = flight.flightNumber,
                    departureTime = flight.departureTime,
                    price = flight.price,
                    onClick = { onFlightSelected(flight.flightNumber) }
                )
            }
        }
    }
}

/**
 * Data class for parsed flight info from uiData JSON.
 */
private data class ParsedFlight(
    val flightNumber: String,
    val departureTime: String,
    val date: String,
    val price: String
)

/**
 * Parse flights from the uiData JSON string.
 * Expected format: {"flights": [{"flightNumber": "FA101", "departureTime": "09:00", "lowestPrice": 450, "currency": "SAR"}, ...]}
 */
private fun parseFlightsFromUiData(uiData: String?): List<ParsedFlight> {
    if (uiData.isNullOrBlank()) {
        println("parseFlightsFromUiData: uiData is null or blank")
        return emptyList()
    }
    
    println("parseFlightsFromUiData: Parsing uiData (${uiData.length} chars): ${uiData.take(200)}")
    
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(uiData).jsonObject
        val flightsArray = root["flights"]?.jsonArray
        
        if (flightsArray == null) {
            println("parseFlightsFromUiData: No 'flights' array found in JSON")
            return emptyList()
        }
        
        println("parseFlightsFromUiData: Found ${flightsArray.size} flights in array")
        
        // Get the flight date from the root object
        val flightDate = root["date"]?.jsonPrimitive?.contentOrNull?.let { formatDateForDisplay(it) } ?: ""
        
        flightsArray.mapNotNull { element ->
            val obj = element.jsonObject
            val flightNumber = obj["flightNumber"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val departureTime = obj["departureTime"]?.jsonPrimitive?.contentOrNull?.let { formatTimeForDisplay(it) } ?: ""
            val lowestPrice = obj["lowestPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            val currency = obj["currency"]?.jsonPrimitive?.contentOrNull ?: "SAR"
            
            ParsedFlight(
                flightNumber = flightNumber,
                departureTime = departureTime,
                date = flightDate,
                price = "$currency ${lowestPrice.toInt()}"
            )
        }
    } catch (e: Exception) {
        // Log error in debug, return empty list
        println("parseFlightsFromUiData: Failed to parse: ${e.message}")
        emptyList()
    }
}

/**
 * Format time string for display (extract HH:mm from ISO datetime if needed).
 */
private fun formatTimeForDisplay(time: String): String {
    // If it's an ISO datetime like "2025-12-10T09:00:00", extract the time part (HH:mm)
    return if (time.contains("T")) {
        // Extract time after T, then take first 5 chars (HH:mm)
        time.substringAfter("T").take(5)
    } else {
        time.take(5) // Already HH:mm format
    }
}

/**
 * Format date string for display (e.g., "2025-12-10" -> "Dec 10").
 */
private fun formatDateForDisplay(date: String): String {
    return try {
        val parts = date.split("-")
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
            val day = parts[2].toIntOrNull() ?: parts[2]
            "$month $day"
        } else {
            date
        }
    } catch (e: Exception) {
        date
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
        color = VelocityColors.BackgroundMid,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VelocityColors.GlassBorder
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
                color = VelocityColors.Accent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = departureTime,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = VelocityColors.TextMain
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = VelocityColors.Primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = price,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = VelocityColors.Primary,
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
        color = VelocityColors.BackgroundMid,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VelocityColors.GlassBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isRtl) "اختر مقعدك" else "Select Your Seat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VelocityColors.TextMain
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
            style = MaterialTheme.typography.labelSmall,
            color = VelocityColors.TextMuted
        )
    }
}

@Composable
private fun BoardingPassCard(uiData: String?, isRtl: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VelocityColors.BackgroundMid,
        shadowElevation = 4.dp
    ) {
        Column {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(VelocityColors.Primary, VelocityColors.Accent)
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
                            fontWeight = FontWeight.Bold,
                            color = VelocityColors.TextMain
                        )
                        Text(
                            text = if (isRtl) "الرياض" else "Riyadh",
                            style = MaterialTheme.typography.bodySmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = VelocityColors.Accent,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "JED",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = VelocityColors.TextMain
                        )
                        Text(
                            text = if (isRtl) "جدة" else "Jeddah",
                            style = MaterialTheme.typography.bodySmall,
                            color = VelocityColors.TextMuted
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
                            VelocityColors.GlassBg,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "QR CODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = VelocityColors.TextMuted
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
            color = VelocityColors.TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = VelocityColors.TextMain
        )
    }
}

@Composable
private fun ComparisonCard(uiData: String?, isRtl: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VelocityColors.BackgroundMid,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VelocityColors.GlassBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isRtl) "مقارنة الرحلات" else "Flight Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VelocityColors.TextMain
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
                color = VelocityColors.Primary.copy(alpha = 0.15f)
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelocityColors.TextMain
                    )
                    Text(
                        text = "+SAR 70",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = VelocityColors.Primary
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
            VelocityColors.GlassBg
        else 
            VelocityColors.Primary.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = VelocityColors.TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = flightNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = VelocityColors.TextMain
            )
            Text(
                text = time,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isOld) VelocityColors.TextMuted else VelocityColors.Primary
            )
        }
    }
}

@Composable
private fun BookingSummaryCard(uiData: String?, isRtl: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VelocityColors.BackgroundMid,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VelocityColors.GlassBorder
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
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.TextMain
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = VelocityColors.Accent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "ABC123",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = VelocityColors.Accent,
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
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = "SAR 760",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VelocityColors.Accent
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
            color = VelocityColors.TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = VelocityColors.TextMain
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
                        containerColor = VelocityColors.Primary.copy(alpha = 0.15f),
                        labelColor = VelocityColors.Primary
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
    val focusRequester = remember { FocusRequester() }
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

    // Auto-focus text field when it's the user's turn to type
    LaunchedEffect(isLoading, isListening) {
        if (!isLoading && !isListening) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Focus request may fail if component not ready
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VelocityColors.BackgroundMid,
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
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = if (isRtl) "اكتب أو تكلم..." else "Type or speak...",
                            color = VelocityColors.TextMuted.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VelocityColors.Primary,
                        unfocusedBorderColor = VelocityColors.GlassBorder,
                        focusedTextColor = VelocityColors.TextMain,
                        unfocusedTextColor = VelocityColors.TextMain,
                        cursorColor = VelocityColors.Primary
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
                    color = if (isListening) VelocityColors.Accent else VelocityColors.Primary,
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
                            .background(VelocityColors.Primary, CircleShape)
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
