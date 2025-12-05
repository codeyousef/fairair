package com.fairair.app.ui.components.velocity

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fairair.app.localization.AppStrings
import com.fairair.app.ui.screens.search.TripType
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Horizontal pill selector for trip type (One-way, Round trip, Multi-city).
 *
 * Features a glassmorphic design with animated selection state.
 *
 * @param selectedType The currently selected trip type
 * @param onTypeSelected Callback when a trip type is selected
 * @param strings Localized strings
 * @param modifier Modifier to apply to the component
 */
@Composable
fun TripTypeSelector(
    selectedType: TripType,
    onTypeSelected: (TripType) -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(VelocityColors.GlassBg)
            .border(
                width = 1.dp,
                color = VelocityColors.TextMuted.copy(alpha = 0.2f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TripTypePill(
            tripLabel = strings.velocityOneWay,
            isSelected = selectedType == TripType.ONE_WAY,
            onClick = { onTypeSelected(TripType.ONE_WAY) }
        )
        TripTypePill(
            tripLabel = strings.velocityRoundTrip,
            isSelected = selectedType == TripType.ROUND_TRIP,
            onClick = { onTypeSelected(TripType.ROUND_TRIP) }
        )
        TripTypePill(
            tripLabel = strings.velocityMultiCity,
            isSelected = selectedType == TripType.MULTI_CITY,
            onClick = { onTypeSelected(TripType.MULTI_CITY) }
        )
    }
}

/**
 * Individual pill button for trip type selection.
 */
@Composable
private fun TripTypePill(
    tripLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) VelocityColors.Accent else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "trip_type_bg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) VelocityColors.BackgroundDeep else VelocityColors.TextMuted,
        animationSpec = tween(durationMillis = 200),
        label = "trip_type_text"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = tripLabel,
            style = VelocityTheme.typography.body,
            color = textColor
        )
    }
}
