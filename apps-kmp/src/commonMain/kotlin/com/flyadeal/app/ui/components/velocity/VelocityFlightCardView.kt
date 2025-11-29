package com.flyadeal.app.ui.components.velocity

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyadeal.app.ui.screens.results.FareFamily
import com.flyadeal.app.ui.screens.results.VelocityFlightCard
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Glassmorphic flight card displaying flight times, route, duration, and price.
 *
 * Features:
 * - Glass card background with blur effect
 * - Animated route visualization
 * - Expandable to show fare options
 * - Price display with accent color
 *
 * @param flight The flight data to display
 * @param isExpanded Whether the card is expanded to show fare options
 * @param selectedFare The currently selected fare family, if any
 * @param onClick Callback when the card is tapped
 * @param onFareSelect Callback when a fare is selected
 * @param modifier Modifier to apply to the component
 */
@Composable
fun VelocityFlightCardView(
    flight: VelocityFlightCard,
    isExpanded: Boolean = false,
    selectedFare: FareFamily? = null,
    onClick: () -> Unit,
    onFareSelect: (FareFamily) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    GlassCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Route visualization with times
            FlightRouteVisual(
                departureCode = flight.originCode,
                arrivalCode = flight.destinationCode,
                departureTime = flight.departureTime,
                arrivalTime = flight.arrivalTime,
                showAnimation = isExpanded
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Duration and price row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Duration
                Text(
                    text = flight.durationFormatted,
                    style = typography.duration,
                    color = VelocityColors.TextMuted
                )

                // Price
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "from",
                        style = typography.labelSmall,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = flight.lowestPrice.formatDisplay(),
                        style = typography.priceDisplay,
                        color = VelocityColors.Accent
                    )
                }
            }

            // Expandable fare selection
            if (isExpanded && flight.fareFamilies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                FareGrid(
                    fareFamilies = flight.fareFamilies,
                    selectedFare = selectedFare,
                    onFareSelect = onFareSelect
                )
            }
        }
    }
}

/**
 * Compact version of the flight card for list views.
 */
@Composable
fun VelocityFlightCardCompact(
    flight: VelocityFlightCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    GlassCardSmall(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Times
            Column {
                Text(
                    text = "${flight.departureTime} - ${flight.arrivalTime}",
                    style = typography.body,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = "${flight.originCode} → ${flight.destinationCode}",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }

            // Duration
            Text(
                text = flight.durationFormatted,
                style = typography.duration,
                color = VelocityColors.TextMuted
            )

            // Price
            Text(
                text = flight.lowestPrice.formatDisplay(),
                style = typography.priceDisplay,
                color = VelocityColors.Accent
            )
        }
    }
}

/**
 * Flight card skeleton for loading state.
 */
@Composable
fun VelocityFlightCardSkeleton(
    modifier: Modifier = Modifier
) {
    GlassCard(
        onClick = {},
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Skeleton route visual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(width = 60.dp, height = 24.dp)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 16.dp)
                        .glassmorphism()
                )
                SkeletonBox(width = 60.dp, height = 24.dp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Skeleton duration and price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBox(width = 48.dp, height = 16.dp)
                SkeletonBox(width = 72.dp, height = 24.dp)
            }
        }
    }
}

/**
 * Helper composable for skeleton loading placeholders.
 */
@Composable
private fun SkeletonBox(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width, height)
            .glassmorphism()
    )
}
