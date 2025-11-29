package com.flyadeal.app.ui.components.velocity

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flyadeal.app.ui.theme.VelocityColors
import com.flyadeal.app.ui.theme.VelocityTheme

/**
 * Header for the flight results overlay.
 *
 * Displays the route (origin → destination), formatted date, and a close button.
 *
 * @param originCode 3-letter origin airport code
 * @param destinationCode 3-letter destination airport code
 * @param formattedDate Formatted departure date (e.g., "Dec 01")
 * @param resultCount Number of flights found
 * @param onClose Callback when the close button is tapped
 * @param modifier Modifier to apply to the component
 */
@Composable
fun ResultsHeader(
    originCode: String,
    destinationCode: String,
    formattedDate: String,
    resultCount: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Route and date
        Column {
            // Route: RUH → DXB
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = originCode,
                    style = typography.timeBig,
                    color = VelocityColors.TextMain
                )
                Text(
                    text = "→",
                    style = typography.body,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = destinationCode,
                    style = typography.timeBig,
                    color = VelocityColors.TextMain
                )
            }

            // Date and result count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formattedDate,
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = "•",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = "$resultCount flight${if (resultCount != 1) "s" else ""}",
                    style = typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }
        }

        // Close button
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close results",
                tint = VelocityColors.TextMuted
            )
        }
    }
}

/**
 * Compact header for smaller screens.
 */
@Composable
fun ResultsHeaderCompact(
    originCode: String,
    destinationCode: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typography = VelocityTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compact route display
        Text(
            text = "$originCode → $destinationCode",
            style = typography.body,
            color = VelocityColors.TextMain
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = VelocityColors.TextMuted
            )
        }
    }
}

/**
 * Drag handle for the bottom sheet.
 */
@Composable
fun ResultsDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .glassmorphismSmall()
        )
    }
}
