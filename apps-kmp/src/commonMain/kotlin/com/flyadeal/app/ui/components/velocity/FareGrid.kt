package com.flyadeal.app.ui.components.velocity

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
import com.flyadeal.app.ui.screens.results.FareFamily
import com.flyadeal.app.ui.screens.results.FareFamilyCode
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Grid layout showing available fare families for a flight.
 *
 * Displays Fly, Fly+, and FlyMax options with prices.
 * Selected fare is highlighted with accent border.
 *
 * @param fareFamilies List of available fare options
 * @param selectedFare Currently selected fare, if any
 * @param onFareSelect Callback when a fare is selected
 * @param modifier Modifier to apply to the component
 */
@Composable
fun FareGrid(
    fareFamilies: List<FareFamily>,
    selectedFare: FareFamily?,
    onFareSelect: (FareFamily) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fareFamilies.forEach { fare ->
            FareTile(
                fare = fare,
                isSelected = selectedFare?.id == fare.id,
                onClick = { onFareSelect(fare) },
                modifier = Modifier.weight(1f)
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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Fare family name
        Text(
            text = fare.displayName,
            style = typography.labelSmall,
            color = if (isSelected) VelocityColors.Accent else VelocityColors.TextMuted,
            textAlign = TextAlign.Center
        )

        // Price
        Text(
            text = fare.price.formatAmount(),
            style = typography.body,
            color = if (isSelected) VelocityColors.Accent else VelocityColors.TextMain,
            textAlign = TextAlign.Center
        )

        // Currency
        Text(
            text = fare.price.currency.name,
            style = typography.labelSmall,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center
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
