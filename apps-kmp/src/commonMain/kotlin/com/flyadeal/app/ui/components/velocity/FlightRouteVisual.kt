package com.flyadeal.app.ui.components.velocity

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Visual representation of a flight route with animated accent dot.
 *
 * Shows departure and arrival airport codes connected by a dotted line
 * with an animated accent dot traveling along the line.
 *
 * @param departureCode 3-letter departure airport code (e.g., "RUH")
 * @param arrivalCode 3-letter arrival airport code (e.g., "DXB")
 * @param departureTime Formatted departure time (e.g., "14:30")
 * @param arrivalTime Formatted arrival time (e.g., "17:45")
 * @param showAnimation Whether to animate the accent dot
 * @param modifier Modifier to apply to the component
 */
@Composable
fun FlightRouteVisual(
    departureCode: String,
    arrivalCode: String,
    departureTime: String,
    arrivalTime: String,
    showAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    // Animated progress for the accent dot
    val infiniteTransition = rememberInfiniteTransition(label = "route_animation")
    val dotProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_progress"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Departure info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = departureTime,
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
            Text(
                text = departureCode,
                style = typography.labelSmall,
                color = VelocityColors.TextMuted
            )
        }

        // Route line with animated dot
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val lineY = size.height / 2
                val startX = 0f
                val endX = size.width

                // Draw the dotted line
                drawRouteLine(
                    startX = startX,
                    endX = endX,
                    y = lineY,
                    color = VelocityColors.GlassBorder
                )

                // Draw start circle (departure)
                drawCircle(
                    color = VelocityColors.TextMuted,
                    radius = 4.dp.toPx(),
                    center = Offset(startX + 4.dp.toPx(), lineY)
                )

                // Draw end circle (arrival)
                drawCircle(
                    color = VelocityColors.TextMuted,
                    radius = 4.dp.toPx(),
                    center = Offset(endX - 4.dp.toPx(), lineY)
                )

                // Draw animated accent dot
                if (showAnimation) {
                    val dotX = startX + 4.dp.toPx() + (endX - 8.dp.toPx() - startX) * dotProgress
                    drawCircle(
                        color = VelocityColors.Accent,
                        radius = 6.dp.toPx(),
                        center = Offset(dotX, lineY)
                    )
                }
            }
        }

        // Arrival info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = arrivalTime,
                style = typography.timeBig,
                color = VelocityColors.TextMain
            )
            Text(
                text = arrivalCode,
                style = typography.labelSmall,
                color = VelocityColors.TextMuted
            )
        }
    }
}

/**
 * Simplified route visual without times, just codes and line.
 */
@Composable
fun FlightRouteVisualCompact(
    departureCode: String,
    arrivalCode: String,
    showAnimation: Boolean = false,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = departureCode,
            style = typography.timeBig,
            color = VelocityColors.TextMain
        )

        // Route line
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val lineY = size.height / 2
                val startX = 0f
                val endX = size.width

                drawRouteLine(
                    startX = startX,
                    endX = endX,
                    y = lineY,
                    color = VelocityColors.GlassBorder
                )

                // Start dot
                drawCircle(
                    color = VelocityColors.TextMuted,
                    radius = 3.dp.toPx(),
                    center = Offset(startX, lineY)
                )

                // End dot
                drawCircle(
                    color = VelocityColors.TextMuted,
                    radius = 3.dp.toPx(),
                    center = Offset(endX, lineY)
                )

                // Airplane icon or accent dot in middle
                if (showAnimation) {
                    drawCircle(
                        color = VelocityColors.Accent,
                        radius = 4.dp.toPx(),
                        center = Offset(size.width / 2, lineY)
                    )
                }
            }
        }

        Text(
            text = arrivalCode,
            style = typography.timeBig,
            color = VelocityColors.TextMain
        )
    }
}

/**
 * Helper function to draw a dotted route line.
 */
private fun DrawScope.drawRouteLine(
    startX: Float,
    endX: Float,
    y: Float,
    color: Color
) {
    drawLine(
        color = color,
        start = Offset(startX, y),
        end = Offset(endX, y),
        strokeWidth = 1.dp.toPx(),
        cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
            0f
        )
    )
}
