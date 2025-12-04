package com.fairair.app.ui.components.velocity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fairair.app.ui.screens.results.FareFamily
import com.fairair.app.ui.screens.results.FareFamilyCode
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Grid layout showing available fare families for a flight.
 *
 * Displays Fly, Fly+, and FlyMax options with prices.
 * For employees, also shows a Standby option at SAR 100.
 * Selected fare is highlighted with accent border.
 *
 * @param fareFamilies List of available fare options
 * @param selectedFare Currently selected fare, if any
 * @param onFareSelect Callback when a fare is selected
 * @param isEmployee Whether to show employee standby option
 * @param onStandbySelect Callback when standby is selected
 * @param modifier Modifier to apply to the component
 */
@Composable
fun FareGrid(
    fareFamilies: List<FareFamily>,
    selectedFare: FareFamily?,
    onFareSelect: (FareFamily) -> Unit,
    isEmployee: Boolean = false,
    onStandbySelect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Regular fare options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fareFamilies.forEach { fare ->
                FareTile(
                    fare = fare,
                    isSelected = selectedFare?.id == fare.id,
                    onClick = { onFareSelect(fare) },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
        
        // Employee standby option
        if (isEmployee) {
            StandbyTile(
                onClick = onStandbySelect,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Standby fare tile for employees only.
 * Fixed price of SAR 100.
 */
@Composable
fun StandbyTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .border(
                width = 2.dp,
                color = VelocityColors.Warning,
                shape = shape
            )
            .background(VelocityColors.Warning.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "✈ Employee Standby",
                style = typography.body,
                color = VelocityColors.Warning
            )
            Text(
                text = "Subject to availability at gate",
                style = typography.labelSmall,
                color = VelocityColors.TextMuted
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "SAR ",
                    style = typography.duration,
                    color = VelocityColors.Warning
                )
                Text(
                    text = "100",
                    style = typography.priceDisplay,
                    color = VelocityColors.Warning
                )
            }
            Text(
                text = "Fixed rate",
                style = typography.labelSmall,
                color = VelocityColors.TextMuted
            )
        }
    }
}

/**
 * Individual fare option tile.
 *
 * @param fare The fare family data
 * @param isSelected Whether this fare is currently selected
 * @param onClick Callback when tapped
 * @param modifier Modifier to apply to the component
 */
@Composable
fun FareTile(
    fare: FareFamily,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography
    val shape = RoundedCornerShape(12.dp)

    val borderColor = if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder
    val backgroundColor = if (isSelected) {
        VelocityColors.Accent.copy(alpha = 0.15f)
    } else {
        VelocityColors.GlassBg
    }

    Column(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Fare family name
        Text(
            text = fare.displayName,
            style = typography.labelSmall,
            color = if (isSelected) VelocityColors.Accent else VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Price
        Text(
            text = fare.price.formatAmount(),
            style = typography.body,
            color = if (isSelected) VelocityColors.Accent else VelocityColors.TextMain,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // Currency
        Text(
            text = fare.price.currency.name,
            style = typography.labelSmall,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Horizontal fare options for compact layouts.
 */
@Composable
fun FareOptionsRow(
    fareFamilies: List<FareFamily>,
    selectedFare: FareFamily?,
    onFareSelect: (FareFamily) -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        fareFamilies.forEach { fare ->
            val isSelected = selectedFare?.id == fare.id
            val shape = RoundedCornerShape(20.dp)
            val borderColor = if (isSelected) VelocityColors.Accent else VelocityColors.GlassBorder

            Row(
                modifier = Modifier
                    .clip(shape)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor,
                        shape = shape
                    )
                    .background(VelocityColors.GlassBg)
                    .clickable { onFareSelect(fare) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = fare.displayName,
                    style = typography.labelSmall,
                    color = if (isSelected) VelocityColors.Accent else VelocityColors.TextMuted
                )
                Text(
                    text = fare.price.formatDisplay(),
                    style = typography.body,
                    color = if (isSelected) VelocityColors.Accent else VelocityColors.TextMain
                )
            }
        }
    }
}
